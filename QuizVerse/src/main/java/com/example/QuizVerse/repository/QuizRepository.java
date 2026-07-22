package com.example.QuizVerse.repository;

import com.example.QuizVerse.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    boolean existsByCategoryId(Long categoryId);
}
