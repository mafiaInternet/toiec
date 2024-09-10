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
                // Nếu key là null hoặc rỗng, trả về response với code = 1 và content là null
                return ResponseEntity.ok(new ResponseWrapper<>(vocabRepo.findAll(), 1));
            }

            List<Vocabulary> results;

            // Tìm kiếm từ vựng bằng văn bản và có thể theo chủ đề
            if (topic != null) {
                results = vocabRepo.findByTextAndTopic(key, topic);
            } else {
                results = vocabRepo.findByText(key);
            }

            // Tạo response với code = 0 và content là dữ liệu tìm được
            return ResponseEntity.ok(new ResponseWrapper<>(results, 1));

        } catch (Exception e) {
            // Ghi log lỗi (nếu cần)
            // logger.error("Error occurred while searching vocabulary", e);

            // Trả về lỗi 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseWrapper<>(null, 2)); // Code 1 cho lỗi
        }
    }
}