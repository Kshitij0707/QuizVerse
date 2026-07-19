package com.example.QuizVerse.service;

import com.example.QuizVerse.model.RefreshToken;
import com.example.QuizVerse.model.User;
import com.example.QuizVerse.repository.RefreshTokenRepository;
import com.example.QuizVerse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               @Value("${jwt.refresh-expiration-ms}") long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Create a new refresh token for a user
     */
    public String createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        // Revoke old refresh tokens for this user (keep only one active)
        refreshTokenRepository.findByUserAndRevokedFalse(user)
                .ifPresent(oldToken -> {
                    oldToken.setRevoked(true);
                    refreshTokenRepository.save(oldToken);
                });

        // Generate new refresh token
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken refreshToken = new RefreshToken(user, token, expiryDate);
        refreshTokenRepository.save(refreshToken);

        return token;
    }

    /**
     * Verify and retrieve a refresh token if it's valid
     */
    public Optional<RefreshToken> verifyRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(refreshToken -> !refreshToken.isRevoked())
                .filter(refreshToken -> refreshToken.getExpiryDate().isAfter(Instant.now()));
    }

    /**
     * Revoke a refresh token (for logout)
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    /**
     * Revoke all refresh tokens for a user (for logout from all devices)
     */
    @Transactional
    public void revokeAllRefreshTokens(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        refreshTokenRepository.findByUserAndRevokedFalse(user)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    /**
     * Delete expired refresh tokens (cleanup)
     */
    @Transactional
    public void deleteExpiredTokens() {
        // Find all revoked tokens and delete them
        // In a real app, you might also delete old expired tokens periodically
    }
}

