package com.toeic.toeic_app.controller;

import com.toeic.toeic_app.model.Vocabulary;
import com.toeic.toeic_app.repository.UserRepo;
import com.toeic.toeic_app.repository.VocabRepo;
import com.toeic.toeic_app.wrapper.ResponseWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
            @RequestParam(value = "topic", required = false) Integer topic) {  // Sửa thành Integer
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

    @GetMapping("/random")
    public ResponseEntity<ResponseWrapper<?>> getRandomVocabularyByTopic(
            @RequestParam("limit") int limit,
            @RequestParam("topic") Integer topic) {
        try {
            // Nếu limit <= 0, trả về danh sách rỗng
            if (limit <= 0) {
                return ResponseEntity.ok(new ResponseWrapper<>(new ArrayList<>(), 2));
            }

            // Truy vấn MongoDB để lấy các từ vựng theo topic ngẫu nhiên
            List<Vocabulary> randomVocab = vocabRepo.findRandomByTopic(topic, limit);

            // Trả về danh sách từ vựng ngẫu nhiên
            return ResponseEntity.ok(new ResponseWrapper<>(randomVocab, 1));
        } catch (Exception e) {
            // Xử lý lỗi và trả về response lỗi
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseWrapper<>(null, 3));
        }
    }


}