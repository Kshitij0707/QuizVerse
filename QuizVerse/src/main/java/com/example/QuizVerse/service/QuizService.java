package com.example.QuizVerse.service;

import com.example.QuizVerse.dto.*;
import com.example.QuizVerse.model.*;
import com.example.QuizVerse.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public QuizService(QuizRepository quizRepository,
                       CategoryRepository categoryRepository,
                       UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Resolve category by id -> name -> default
     * Helper method to avoid code duplication
     */
    private Category resolveCategory(Long categoryId, String categoryName) {
        if (categoryId != null) {
            return categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id " + categoryId));
        } else if (categoryName != null && !categoryName.isBlank()) {
            String name = categoryName.trim();
            return categoryRepository.findByName(name)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with name '" + name + "'."));
        } else {
            // Assign default category "Uncategorized" (create if missing)
            String defaultName = "Uncategorized";
            return categoryRepository.findByName(defaultName).orElseGet(() -> {
                try {
                    return categoryRepository.save(new Category(defaultName, "Default category"));
                } catch (DataIntegrityViolationException ex) {
                    // Race condition: another thread created it -> fetch
                    return categoryRepository.findByName(defaultName)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resolve default category"));
                }
            });
        }
    }

    /**
     * Check if user has ADMIN role
     */
    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

    /**
     * Build questions from request list
     */
    private void buildQuestions(Quiz quiz, List<QuestionRequest> questionRequests) {
        if (questionRequests != null) {
            for (QuestionRequest qr : questionRequests) {
                Question q = new Question(qr.getText(), Question.Type.valueOf(qr.getType()));
                q.setPoints(qr.getPoints());
                q.setQuiz(quiz);
                if (qr.getOptions() != null) {
                    for (OptionRequest or : qr.getOptions()) {
                        Option o = new Option(or.getText(), or.isCorrect());
                        o.setQuestion(q);
                        q.getOptions().add(o);
                    }
                }
                quiz.getQuestions().add(q);
            }
        }
    }

    /**
     * Create quiz - role-aware creation
     * - If user is ADMIN: marks quiz as builtIn=true
     * - If user is USER: marks quiz as builtIn=false
     * - Category resolved by id -> name -> default
     * - Questions built from request
     */
    @Transactional
    public Quiz createQuiz(QuizRequest req, String authorUsername, Authentication auth) {
        Quiz quiz = new Quiz(req.getTitle(), req.getDescription());
        
        // Auto-detect builtIn status based on user role
        boolean isAdmin = isAdmin(auth);
        quiz.setBuiltIn(isAdmin);

        // Resolve category
        Category category = resolveCategory(req.getCategoryId(), req.getCategoryName());
        quiz.setCategory(category);

        // Set author
        if (authorUsername != null) {
            userRepository.findByUsername(authorUsername).ifPresent(quiz::setAuthor);
        }

        // Build questions
        buildQuestions(quiz, req.getQuestions());

        return quizRepository.save(quiz);
    }

    /**
     * Update quiz - validates ownership or admin privilege
     * - If user is ADMIN: can update any quiz
     * - If user is not ADMIN: can only update own quiz
     */
    @Transactional
    public Quiz updateQuiz(Long id, QuizRequest req, String authorUsername, Authentication auth) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found with id " + id));

        // Authorization check: must be admin or quiz author
        boolean isAdmin = isAdmin(auth);
        boolean isOwner = quiz.getAuthor() != null && quiz.getAuthor().getUsername().equals(authorUsername);

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this quiz");
        }

        // Update fields
        quiz.setTitle(req.getTitle());
        quiz.setDescription(req.getDescription());

        // Update category (optional in request)
        if (req.getCategoryId() != null || req.getCategoryName() != null) {
            Category category = resolveCategory(req.getCategoryId(), req.getCategoryName());
            quiz.setCategory(category);
        }

        // Update questions
        quiz.getQuestions().clear();
        buildQuestions(quiz, req.getQuestions());

        quiz.setUpdatedAt(new java.util.Date());
        return quizRepository.save(quiz);
    }

    /**
     * Delete quiz - validates ownership or admin privilege
     * - If user is ADMIN: can delete any quiz
     * - If user is not ADMIN: can only delete own quiz
     */
    @Transactional
    public void deleteQuiz(Long id, String authorUsername, Authentication auth) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found with id " + id));

        // Authorization check: must be admin or quiz author
        boolean isAdmin = isAdmin(auth);
        boolean isOwner = quiz.getAuthor() != null && quiz.getAuthor().getUsername().equals(authorUsername);

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this quiz");
        }

        quizRepository.deleteById(id);
    }

    /**
     * Admin-only: delete any quiz (built-in or custom)
     */
    @Transactional
    public void deleteQuizAsAdmin(Long id) {
        if (!quizRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found with id " + id);
        }
        quizRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<QuizResponse> getQuiz(Long id) {
        return quizRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listAll() {
        return quizRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listBuiltInQuizzes() {
        return quizRepository.findByBuiltInTrue().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listCustomQuizzes() {
        return quizRepository.findByBuiltInFalse().stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Admin-only: list all quizzes (both built-in and custom)
     */
    @Transactional(readOnly = true)
    public List<QuizResponse> listAllQuizzesForAdmin() {
        return quizRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    private QuizResponse toDto(Quiz q) {
        QuizResponse r = new QuizResponse();
        r.setId(q.getId());
        r.setTitle(q.getTitle());
        r.setDescription(q.getDescription());
        r.setAuthor(q.getAuthor() != null ? q.getAuthor().getUsername() : null);
        r.setCategory(q.getCategory() != null ? q.getCategory().getName() : null);
        List<QuestionResponse> questions = new ArrayList<>();
        for (Question qu : q.getQuestions()) {
            QuestionResponse qr = new QuestionResponse();
            qr.setId(qu.getId());
            qr.setText(qu.getText());
            qr.setType(qu.getType().name());
            qr.setPoints(qu.getPoints());
            List<OptionResponse> opts = new ArrayList<>();
            for (Option o : qu.getOptions()) {
                opts.add(new OptionResponse(o.getId(), o.getText()));
            }
            qr.setOptions(opts);
            questions.add(qr);
        }
        r.setQuestions(questions);
        return r;
    }
}

