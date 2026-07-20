package com.example.QuizVerse.controller;

import com.example.QuizVerse.dto.QuizRequest;
import com.example.QuizVerse.dto.QuizResponse;
import com.example.QuizVerse.model.Quiz;
import com.example.QuizVerse.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping
    public ResponseEntity<?> createQuiz(@Valid @RequestBody QuizRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : null;
        Quiz created = quizService.createQuiz(request, username);
        return ResponseEntity.created(URI.create("/api/quizzes/" + created.getId())).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable Long id) {
        return quizService.getQuiz(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<QuizResponse>> listAll() {
        return ResponseEntity.ok(quizService.listAll());
    }
}

