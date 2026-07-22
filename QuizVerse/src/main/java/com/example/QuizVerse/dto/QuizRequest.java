package com.example.QuizVerse.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class QuizRequest {

    @NotBlank
    private String title;

    private String description;

    // category can be provided either by id (admin/frontend mapping) or by name (user-friendly)
    private Long categoryId;

    private String categoryName;

    private List<QuestionRequest> questions;

    public QuizRequest() {}

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
}

