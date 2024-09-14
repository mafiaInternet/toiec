package com.toeic.toeic_app.controller;

import com.toeic.toeic_app.model.Vocabulary;
import com.toeic.toeic_app.repository.UserRepo;
import com.toeic.toeic_app.repository.VocabRepo;
import com.toeic.toeic_app.wrapper.ResponseWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vocab")
public class VocabController {
    @Autowired
    private VocabRepo vocabRepo;

    @PostMapping("save")
    public ResponseEntity<?> addVocabulary(@RequestBody Vocabulary vocabulary) {
        try {
            if (vocabulary == null) {
                return ResponseEntity.status(HttpStatus.OK).body(null);
            }
            Vocabulary createdVocabulary = vocabRepo.save(vocabulary);
            return ResponseEntity.status(HttpStatus.OK).body(createdVocabulary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK).body(null);
        }
    }

    @GetMapping("all")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(vocabRepo.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseWrapper<?>> searchVocabulary(
            @RequestParam("key") String key,
            @RequestParam(value = "topic", required = false) Number topic) {
        try {
            if (key == null || key.isEmpty()) {
                return ResponseEntity.ok(new ResponseWrapper<>(vocabRepo.findAll(), 1));
            }
            List<Vocabulary> results;
            if (topic != null) {
                results = vocabRepo.findByTextAndTopic(key, topic);
            } else {
                results = vocabRepo.findByText(key);
            }
            return ResponseEntity.ok(new ResponseWrapper<>(results, 1));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseWrapper<>(null, 2));
        }
    }
}