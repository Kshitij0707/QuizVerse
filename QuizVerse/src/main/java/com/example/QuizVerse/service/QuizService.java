package com.example.QuizVerse.service;

import com.example.QuizVerse.dto.*;
import com.example.QuizVerse.model.*;
import com.example.QuizVerse.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

    @Transactional
    public Quiz createQuiz(QuizRequest req, String authorUsername) {
        Quiz quiz = new Quiz(req.getTitle(), req.getDescription());

        // Resolve category by id -> name -> default
        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with id " + req.getCategoryId()));
        } else if (req.getCategoryName() != null && !req.getCategoryName().isBlank()) {
            String name = req.getCategoryName().trim();
            category = categoryRepository.findByName(name)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found with name '" + name + "'."));
        } else {
            // assign default category "Uncategorized" (create if missing)
            String defaultName = "Uncategorized";
            category = categoryRepository.findByName(defaultName).orElseGet(() -> {
                try {
                    return categoryRepository.save(new Category(defaultName, "Default category"));
                } catch (DataIntegrityViolationException ex) {
                    // race: another thread created it -> fetch
                    return categoryRepository.findByName(defaultName)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resolve default category"));
                }
            });
        }
        quiz.setCategory(category);

        if (authorUsername != null) {
            userRepository.findByUsername(authorUsername).ifPresent(quiz::setAuthor);
        }

        if (req.getQuestions() != null) {
            for (QuestionRequest qr : req.getQuestions()) {
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

        return quizRepository.save(quiz);
    }

    @Transactional(readOnly = true)
    public Optional<QuizResponse> getQuiz(Long id) {
        return quizRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listAll() {
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

