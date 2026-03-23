package com.example.helloworld.service.invest.price;

import com.example.helloworld.entity.invest.Stock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TwMarketPriceProvider implements MarketPriceProvider {

    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String twseUrl;
    private final String tpexUrl;
    private final String twseHistoryUrlTemplate;
    private final String userAgent;

    public TwMarketPriceProvider(ObjectMapper objectMapper,
                                 @Value("${invest.price-provider.timeout-ms:8000}") long timeoutMs,
                                 @Value("${invest.price-provider.tw.twse-url:https://openapi.twse.com.tw/v1/exchangeReport/STOCK_DAY_ALL}") String twseUrl,
                                 @Value("${invest.price-provider.tw.tpex-url:https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes}") String tpexUrl,
                                 @Value("${invest.price-provider.tw.twse-history-url-template:https://www.twse.com.tw/rwd/zh/afterTrading/STOCK_DAY?date=%s&stockNo=%s&response=json}") String twseHistoryUrlTemplate,
                                 @Value("${invest.price-provider.user-agent:InvestAdmin/1.0}") String userAgent) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .build();
        this.twseUrl = twseUrl;
        this.tpexUrl = tpexUrl;
        this.twseHistoryUrlTemplate = twseHistoryUrlTemplate;
        this.userAgent = userAgent;
    }

    @Override
    public boolean supportsMarket(String market) {
        return "TW".equalsIgnoreCase(market);
    }

    @Override
    public Optional<PriceQuoteSnapshot> fetchLatestQuote(Stock stock) {
        String ticker = stock.getTicker();
        LocalDateTime fetchedAt = LocalDateTime.now(TAIPEI_ZONE);

        Optional<Map<String, Object>> twseRow = findFromTwse(ticker);
        if (twseRow.isPresent()) {
            return buildSnapshotFromTwse(twseRow.get(), fetchedAt);
        }

        Optional<Map<String, Object>> tpexRow = findFromTpex(ticker);
        if (tpexRow.isPresent()) {
            return buildSnapshotFromTpex(tpexRow.get(), fetchedAt);
        }

        return Optional.empty();
    }

    @Override
    public List<PriceQuoteSnapshot> fetchHistoricalQuotes(Stock stock, int days) {
        int safeDays = Math.max(1, days);
        String ticker = stock.getTicker();
        if (ticker == null || ticker.isBlank()) {
            return List.of();
        }

        Map<LocalDate, PriceQuoteSnapshot> resultMap = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.now(TAIPEI_ZONE);
        int monthLoopLimit = 12;

        for (int i = 0; i < monthLoopLimit && resultMap.size() < safeDays; i++) {
            List<PriceQuoteSnapshot> monthRows = fetchTwseHistoryByMonth(ticker, cursor);
            for (PriceQuoteSnapshot row : monthRows) {
                if (row.getTradeDate() == null) {
                    continue;
                }
                resultMap.putIfAbsent(row.getTradeDate(), row);
            }
            cursor = cursor.minusMonths(1);
        }

        return resultMap.values().stream()
            .sorted(Comparator.comparing(PriceQuoteSnapshot::getTradeDate).reversed())
            .limit(safeDays)
            .toList();
    }

    private Optional<Map<String, Object>> findFromTwse(String ticker) {
        List<Map<String, Object>> rows = fetchJsonArray(twseUrl);
        return rows.stream()
            .filter(row -> ticker.equalsIgnoreCase(stringValue(row.get("Code"))))
            .findFirst();
    }

    private Optional<Map<String, Object>> findFromTpex(String ticker) {
        List<Map<String, Object>> rows = fetchJsonArray(tpexUrl);
        return rows.stream()
            .filter(row -> {
                String code = firstNonBlank(
                    stringValue(row.get("SecuritiesCompanyCode")),
                    stringValue(row.get("StockNo")),
                    stringValue(row.get("Code"))
                );
                return ticker.equalsIgnoreCase(code);
            })
            .findFirst();
    }

    private Optional<PriceQuoteSnapshot> buildSnapshotFromTwse(Map<String, Object> row, LocalDateTime fetchedAt) {
        BigDecimal close = decimalValue(row.get("ClosingPrice"));
        if (close == null) {
            return Optional.empty();
        }

        PriceQuoteSnapshot snapshot = new PriceQuoteSnapshot();
        snapshot.setTradeDate(resolveTradeDate(row.get("Date"), fetchedAt.toLocalDate()));
        snapshot.setClosePrice(close);
        snapshot.setOpenPrice(orDefault(decimalValue(row.get("OpeningPrice")), close));
        snapshot.setHighPrice(orDefault(decimalValue(row.get("HighestPrice")), close));
        snapshot.setLowPrice(orDefault(decimalValue(row.get("LowestPrice")), close));
        snapshot.setVolume(orDefaultLong(longValue(row.get("TradeVolume")), 0L));
        snapshot.setChangeAmount(decimalValue(firstNonNull(row.get("PriceDifference"), row.get("Change"))));
        snapshot.setChangePercent(decimalValue(row.get("ChangePercent")));
        snapshot.setDataSource("TWSE");
        snapshot.setFetchedAt(fetchedAt);
        snapshot.setLatencyType("DELAYED");
        return Optional.of(snapshot);
    }

    private Optional<PriceQuoteSnapshot> buildSnapshotFromTpex(Map<String, Object> row, LocalDateTime fetchedAt) {
        BigDecimal close = firstNonNullDecimal(
            decimalValue(row.get("Close")),
            decimalValue(row.get("ClosingPrice")),
            decimalValue(row.get("LatestPrice")),
            decimalValue(row.get("ClosePrice"))
        );
        if (close == null) {
            return Optional.empty();
        }

        PriceQuoteSnapshot snapshot = new PriceQuoteSnapshot();
        snapshot.setTradeDate(resolveTradeDate(firstNonNull(row.get("Date"), row.get("TradeDate")), fetchedAt.toLocalDate()));
        snapshot.setClosePrice(close);
        snapshot.setOpenPrice(orDefault(firstNonNullDecimal(decimalValue(row.get("Open")), decimalValue(row.get("OpeningPrice"))), close));
        snapshot.setHighPrice(orDefault(firstNonNullDecimal(decimalValue(row.get("High")), decimalValue(row.get("HighestPrice"))), close));
        snapshot.setLowPrice(orDefault(firstNonNullDecimal(decimalValue(row.get("Low")), decimalValue(row.get("LowestPrice"))), close));
        snapshot.setVolume(orDefaultLong(firstNonNullLong(longValue(row.get("Volume")), longValue(row.get("TradeVolume"))), 0L));
        snapshot.setChangeAmount(firstNonNullDecimal(decimalValue(row.get("Change")), decimalValue(row.get("PriceDifference"))));
        snapshot.setChangePercent(decimalValue(row.get("ChangePercent")));
        snapshot.setDataSource("TPEX");
        snapshot.setFetchedAt(fetchedAt);
        snapshot.setLatencyType("DELAYED");
        return Optional.of(snapshot);
    }

    private List<Map<String, Object>> fetchJsonArray(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(response.body(), LIST_TYPE);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    private List<PriceQuoteSnapshot> fetchTwseHistoryByMonth(String ticker, YearMonth yearMonth) {
        String dateKey = yearMonth.atDay(1).format(DateTimeFormatter.BASIC_ISO_DATE);
        String url = String.format(twseHistoryUrlTemplate, dateKey, ticker);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return List.of();
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            String stat = stringValue(payload.get("stat"));
            if (stat == null || !stat.contains("OK")) {
                return List.of();
            }

            Object rawData = payload.get("data");
            if (!(rawData instanceof List<?> rows)) {
                return List.of();
            }

            LocalDateTime fetchedAt = LocalDateTime.now(TAIPEI_ZONE);
            List<PriceQuoteSnapshot> monthRows = new ArrayList<>();
            for (Object row : rows) {
                if (!(row instanceof List<?> values) || values.size() < 8) {
                    continue;
                }

                LocalDate tradeDate = resolveTradeDate(values.get(0), null);
                BigDecimal close = decimalValue(values.get(6));
                if (tradeDate == null || close == null) {
                    continue;
                }

                PriceQuoteSnapshot snapshot = new PriceQuoteSnapshot();
                snapshot.setTradeDate(tradeDate);
                snapshot.setClosePrice(close);
                snapshot.setOpenPrice(orDefault(decimalValue(values.get(3)), close));
                snapshot.setHighPrice(orDefault(decimalValue(values.get(4)), close));
                snapshot.setLowPrice(orDefault(decimalValue(values.get(5)), close));
                snapshot.setVolume(orDefaultLong(longValue(values.get(1)), 0L));
                snapshot.setChangeAmount(decimalValue(values.get(7)));
                snapshot.setChangePercent(null);
                snapshot.setDataSource("TWSE");
                snapshot.setFetchedAt(fetchedAt);
                snapshot.setLatencyType("DELAYED");
                monthRows.add(snapshot);
            }
            return monthRows;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    private LocalDate resolveTradeDate(Object rawDate, LocalDate fallback) {
        String value = stringValue(rawDate);
        if (value == null) {
            return fallback;
        }
        String normalized = value.replace("/", "").replace("-", "").trim();
        if (normalized.matches("\\d{8}")) {
            return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE);
        }
        if (normalized.matches("\\d{7}")) {
            int rocYear = Integer.parseInt(normalized.substring(0, 3));
            int month = Integer.parseInt(normalized.substring(3, 5));
            int day = Integer.parseInt(normalized.substring(5, 7));
            return LocalDate.of(rocYear + 1911, month, day);
        }
        return fallback;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private BigDecimal firstNonNullDecimal(BigDecimal... values) {
        if (values == null) {
            return null;
        }
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long firstNonNullLong(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal orDefault(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private Long orDefaultLong(Long value, Long fallback) {
        return value == null ? fallback : value;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "--".equals(text) || "-".equals(text) || "N/A".equalsIgnoreCase(text)) {
            return null;
        }
        return text;
    }

    private BigDecimal decimalValue(Object value) {
        String text = stringValue(value);
        if (text == null) {
            return null;
        }
        String normalized = text.replace(",", "").replace("+", "");
        if (normalized.startsWith("X")) {
            normalized = normalized.substring(1);
        }
        normalized = normalized.replace("－", "-");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long longValue(Object value) {
        String text = stringValue(value);
        if (text == null) {
            return null;
        }
        String normalized = text.replace(",", "");
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
