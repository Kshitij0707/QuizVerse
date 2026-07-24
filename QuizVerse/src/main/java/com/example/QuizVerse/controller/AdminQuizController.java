package com.example.QuizVerse.controller;

import com.example.QuizVerse.dto.QuizRequest;
import com.example.QuizVerse.dto.QuizResponse;
import com.example.QuizVerse.model.Quiz;
import com.example.QuizVerse.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Admin-only endpoints for managing all quizzes (both built-in and custom)
 * Admins have full control over all quizzes regardless of who created them
 */
@RestController
@RequestMapping("/api/admin/quizzes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuizController {

    private final QuizService quizService;

    public AdminQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * Admin: Create a quiz (marked as builtIn=true)
     */
    @PostMapping
    public ResponseEntity<?> createQuiz(@Valid @RequestBody QuizRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
        
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Quiz created = quizService.createQuiz(request, username, auth);
        return ResponseEntity.created(URI.create("/api/admin/quizzes/" + created.getId())).build();
    }

    /**
     * Admin: List all quizzes (both built-in and custom)
     */
    @GetMapping
    public ResponseEntity<List<QuizResponse>> listAll() {
        return ResponseEntity.ok(quizService.listAllQuizzesForAdmin());
    }

    /**
     * Admin: Get a specific quiz by ID (both custom and built-in)
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> getQuiz(@PathVariable Long id) {
        return quizService.getQuiz(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Admin: Update any quiz (built-in or custom) regardless of owner
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
        
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            quizService.updateQuiz(id, request, username, auth);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Admin: Delete any quiz (built-in or custom) regardless of owner
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id) {
        try {
            quizService.deleteQuizAsAdmin(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
}

