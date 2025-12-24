package com.example.helloworld.service.church.checkin;

import com.example.helloworld.entity.church.checkin.SessionToken;
import com.example.helloworld.repository.church.checkin.SessionTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TokenService {
    private final SessionTokenRepository repo;

    public TokenService(SessionTokenRepository repo) {
        this.repo = repo;
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public SessionToken issue(Long sessionId) {
        repo.deleteExpired(sessionId, LocalDateTime.now());

        SessionToken t = new SessionToken();
        t.setSessionId(sessionId);
        t.setToken(UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        t.setExpiresAt(LocalDateTime.now().plusSeconds(60));
        SessionToken saved = repo.save(t);

        repo.keepLatestN(sessionId, 5);
        return saved;
    }

    public boolean validate(Long sessionId, String token) {
        return repo.findFirstBySessionIdAndTokenAndExpiresAtAfter(sessionId, token, LocalDateTime.now()).isPresent();
    }
}

