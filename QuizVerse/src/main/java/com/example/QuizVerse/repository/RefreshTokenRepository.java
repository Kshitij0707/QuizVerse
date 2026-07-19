package com.example.QuizVerse.repository;

import com.example.QuizVerse.model.RefreshToken;
import com.example.QuizVerse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserAndRevokedFalse(User user);

    int deleteByUser(User user);

    void deleteByUserAndRevokedTrue(User user);
}

