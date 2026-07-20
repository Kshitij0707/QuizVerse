package com.example.QuizVerse.repository;

import com.example.QuizVerse.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}

