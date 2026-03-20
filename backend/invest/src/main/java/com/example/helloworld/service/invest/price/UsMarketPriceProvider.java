package com.example.helloworld.service.invest.price;

import com.example.helloworld.entity.invest.Stock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class UsMarketPriceProvider implements MarketPriceProvider {

    private static final ZoneId NEW_YORK_ZONE = ZoneId.of("America/New_York");

    private final HttpClient httpClient;
    private final String stooqUrlTemplate;
    private final String userAgent;

    public UsMarketPriceProvider(@Value("${invest.price-provider.timeout-ms:8000}") long timeoutMs,
                                 @Value("${invest.price-provider.us.stooq-url-template:https://stooq.com/q/l/?s=%s&i=d}") String stooqUrlTemplate,
                                 @Value("${invest.price-provider.user-agent:InvestAdmin/1.0}") String userAgent) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .build();
        this.stooqUrlTemplate = stooqUrlTemplate;
        this.userAgent = userAgent;
    }

    @Override
    public boolean supportsMarket(String market) {
        return "US".equalsIgnoreCase(market);
    }

    @Override
    public Optional<PriceQuoteSnapshot> fetchLatestQuote(Stock stock) {
        String symbol = buildStooqSymbol(stock.getTicker());
        String url = String.format(stooqUrlTemplate, URLEncoder.encode(symbol, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "text/csv")
            .header("User-Agent", userAgent)
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return Optional.empty();
            }
            return parseCsv(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private Optional<PriceQuoteSnapshot> parseCsv(String csv) {
        String[] lines = csv.split("\\R");
        if (lines.length == 0) {
            return Optional.empty();
        }

        String dataLine = resolveDataLine(lines);
        if (dataLine == null || dataLine.isBlank()) {
            return Optional.empty();
        }

        String[] values = dataLine.split(",");
        if (values.length < 8) {
            return Optional.empty();
        }

        BigDecimal open = decimalValue(values[3]);
        BigDecimal high = decimalValue(values[4]);
        BigDecimal low = decimalValue(values[5]);
        BigDecimal close = decimalValue(values[6]);
        Long volume = longValue(values[7]);

        if (close == null) {
            return Optional.empty();
        }

        LocalDate tradeDate = parseDate(values[1]).orElse(LocalDate.now(NEW_YORK_ZONE));
        LocalDateTime fetchedAt = LocalDateTime.now(NEW_YORK_ZONE);

        PriceQuoteSnapshot snapshot = new PriceQuoteSnapshot();
        snapshot.setTradeDate(tradeDate);
        snapshot.setOpenPrice(open == null ? close : open);
        snapshot.setHighPrice(high == null ? close : high);
        snapshot.setLowPrice(low == null ? close : low);
        snapshot.setClosePrice(close);
        snapshot.setVolume(volume == null ? 0L : volume);
        snapshot.setChangeAmount(null);
        snapshot.setChangePercent(null);
        snapshot.setDataSource("US_PROVIDER");
        snapshot.setFetchedAt(fetchedAt);
        snapshot.setLatencyType("DELAYED");
        return Optional.of(snapshot);
    }

    private String resolveDataLine(String[] lines) {
        if (lines.length == 1) {
            return lines[0];
        }
        String first = lines[0].trim().toLowerCase();
        if (first.startsWith("symbol")) {
            return lines[1];
        }
        return lines[0];
    }

    private String buildStooqSymbol(String ticker) {
        String normalized = ticker == null ? "" : ticker.trim().toLowerCase();
        if (normalized.isBlank()) {
            return normalized;
        }
        if (normalized.contains(".")) {
            return normalized;
        }
        return normalized + ".us";
    }

    private Optional<LocalDate> parseDate(String rawDate) {
        if (rawDate == null) {
            return Optional.empty();
        }
        String value = rawDate.trim();
        if (value.isEmpty() || "N/D".equalsIgnoreCase(value)) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } catch (Exception e) {
            try {
                return Optional.of(LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    private BigDecimal decimalValue(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || "N/D".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long longValue(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || "N/D".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
