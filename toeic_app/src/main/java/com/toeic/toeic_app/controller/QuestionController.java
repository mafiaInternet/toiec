package com.toeic.toeic_app.controller;

import com.toeic.toeic_app.model.Question;
import com.toeic.toeic_app.repository.QuestionRepo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/question")
public class QuestionController {
    @Autowired
    private QuestionRepo questionRepo;

    @PostMapping("save")
    public ResponseEntity<Question> saveQuestion(@RequestBody Question question) {
        Question savedQuestion = questionRepo.save(question);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Question>> getAllQuestions() {
        List<Question> questions = questionRepo.findAll();
        return ResponseEntity.status(HttpStatus.OK).body(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable("id") String id) {
        Optional<Question> question = questionRepo.findById(new ObjectId(id));
        if (question.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(question.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable("id") String id, @RequestBody Question questionDetails) {
        Optional<Question> questionOptional = questionRepo.findById(new ObjectId(id));
        if (questionOptional.isPresent()) {
            Question question = questionOptional.get();
            question.setTestId(questionDetails.getTestId());
            question.setPart(questionDetails.getPart());
            question.setQuestionText(questionDetails.getQuestionText());
            question.setQuestionImg(questionDetails.getQuestionImg());
            question.setQuestionAudio(questionDetails.getQuestionAudio());
            question.setOptions(questionDetails.getOptions());
            question.setUpdatedDate(questionDetails.getUpdatedDate());
            Question updatedQuestion = questionRepo.save(question);
            return ResponseEntity.status(HttpStatus.OK).body(updatedQuestion);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable("id") String id) {
        Optional<Question> questionOptional = questionRepo.findById(new ObjectId(id));
        if (questionOptional.isPresent()) {
            questionRepo.delete(questionOptional.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/part/{num}")
    public ResponseEntity<List<Question>> getRandomPart1Questions(@PathVariable("num") String num) {
        List<Question> questions = questionRepo.findQuestionsByPart(Integer.parseInt(num), 10);
        return ResponseEntity.status(HttpStatus.OK).body(questions);
    }
}
