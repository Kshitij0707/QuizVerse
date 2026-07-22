package com.example.QuizVerse.service;

import com.example.QuizVerse.dto.CategoryRequest;
import com.example.QuizVerse.dto.CategoryResponse;
import com.example.QuizVerse.model.Category;
import com.example.QuizVerse.repository.CategoryRepository;
import com.example.QuizVerse.repository.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final QuizRepository quizRepository;

    public CategoryService(CategoryRepository categoryRepository, QuizRepository quizRepository) {
        this.categoryRepository = categoryRepository;
        this.quizRepository = quizRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        if (categoryRepository.existsByName(req.getName().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with name '" + req.getName() + "' already exists");
        }
        Category cat = new Category(req.getName().trim(), req.getDescription());
        Category saved = categoryRepository.save(cat);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        return categoryRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id " + id));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> search(String query) {
        return categoryRepository.findByNameContainingIgnoreCase(query).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest req) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id " + id));

        // Check if new name conflicts with existing category (excluding self)
        if (!cat.getName().equals(req.getName().trim()) && categoryRepository.existsByName(req.getName().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with name '" + req.getName() + "' already exists");
        }

        cat.setName(req.getName().trim());
        cat.setDescription(req.getDescription());
        Category updated = categoryRepository.save(cat);
        return toDto(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id " + id);
        }

        // Safety check: prevent deletion if category has quizzes
        boolean hasQuizzes = quizRepository.existsByCategoryId(id);
        if (hasQuizzes) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete category with id " + id + " because it contains quizzes. Delete or move quizzes first.");
        }

        categoryRepository.deleteById(id);
    }

    private CategoryResponse toDto(Category cat) {
        return new CategoryResponse(cat.getId(), cat.getName(), cat.getDescription());
    }
}

