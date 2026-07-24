package com.example.QuizVerse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BuiltInQuizRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Long categoryId;

    private String categoryName;

    @NotEmpty(message = "Questions cannot be empty")
    @Valid
    private List<QuestionRequest> questions;

    private boolean featured = false; // optional flag for featured/promoted built-in quizzes

    public BuiltInQuizRequest() {}

    public BuiltInQuizRequest(String title, String description, Long categoryId, List<QuestionRequest> questions) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.questions = questions;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<QuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionRequest> questions) {
        this.questions = questions;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }
}

