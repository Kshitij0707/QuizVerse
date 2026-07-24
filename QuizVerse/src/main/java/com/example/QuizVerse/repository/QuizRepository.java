package com.example.QuizVerse.repository;

import com.example.QuizVerse.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    boolean existsByCategoryId(Long categoryId);
    
    @Query("SELECT q FROM Quiz q WHERE q.builtIn = true")
    List<Quiz> findByBuiltInTrue();
    
    @Query("SELECT q FROM Quiz q WHERE q.builtIn = false")
    List<Quiz> findByBuiltInFalse();
    
    @Query("SELECT q FROM Quiz q WHERE q.builtIn = :builtIn")
    List<Quiz> findByBuiltIn(@Param("builtIn") boolean builtIn);
}
