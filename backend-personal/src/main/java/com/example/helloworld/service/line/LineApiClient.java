package com.example.helloworld.service.line;

import com.example.helloworld.config.PersonalLineBotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

@Service
public class LineApiClient {

    private static final Logger log = LoggerFactory.getLogger(LineApiClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final PersonalLineBotConfig config;

    public LineApiClient(PersonalLineBotConfig config) {
        this.config = config;
    }

    public <T> T execute(String actionName, Supplier<T> apiCall) {
        try {
            return apiCall.get();
        } catch (HttpClientErrorException.TooManyRequests e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("monthly limit")) {
                log.warn("⚠️ LINE MONTHLY QUOTA EXCEEDED [{}]", actionName);
                log.warn("⚠️ body={}", body);
                return null;
            }
            throw e;
        }
    }

    public ResponseEntity<String> reply(Object body) {
        return execute("reply", () -> {
            HttpEntity<Object> entity = new HttpEntity<>(body, headers());
            return restTemplate.postForEntity(
                    "https://api.line.me/v2/bot/message/reply",
                    entity,
                    String.class
            );
        });
    }

    public ResponseEntity<String> push(Object body) {
        return execute("push", () -> {
            HttpEntity<Object> entity = new HttpEntity<>(body, headers());
            return restTemplate.postForEntity(
                    "https://api.line.me/v2/bot/message/push",
                    entity,
                    String.class
            );
        });
    }

    public ResponseEntity<String> multicast(Object body) {
        return execute("multicast", () -> {
            HttpEntity<Object> entity = new HttpEntity<>(body, headers());
            return restTemplate.postForEntity(
                    "https://api.line.me/v2/bot/message/multicast",
                    entity,
                    String.class
            );
        });
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getChannelToken());
        return headers;
    }
}
