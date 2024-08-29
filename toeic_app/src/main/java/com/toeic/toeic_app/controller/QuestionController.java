package com.toeic.toeic_app.controller;

import com.toeic.toeic_app.model.Question;
import com.toeic.toeic_app.repository.QuestionRepo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/question")
public class QuestionController {
    @Autowired
    private QuestionRepo questionRepo;

    @PostMapping("save")
    public ResponseEntity<?> saveQuestion(@RequestBody Question question) {
        try {
            Date currentDate = new Date();
            if (question.getCreatedDate() == null) {
                question.setCreatedDate(currentDate);
                question.setUpdatedDate(currentDate);
            }
            Question savedQuestion = questionRepo.save(question);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data provided.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while saving the question.");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllQuestions() {
        try {
            List<Question> questions = questionRepo.findAll();
            if (questions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No questions found.");
            }
            return ResponseEntity.status(HttpStatus.OK).body(questions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrieving questions.");
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestionById(@PathVariable("id") String id) {
        try {
            if (!ObjectId.isValid(id)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid ID format.");
            }
            Optional<Question> question = questionRepo.findById(new ObjectId(id));
            if (question.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(question.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Question not found.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data provided.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrieving the question.");
        }
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable("id") String id, @RequestBody Question questionDetails) {
        try {
            if (!ObjectId.isValid(id)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid ID format.");
            }
            Optional<Question> questionOptional = questionRepo.findById(new ObjectId(id));
            if (questionOptional.isPresent()) {
                Question question = questionOptional.get();
                question.setTest(questionDetails.getTest());
                question.setPart(questionDetails.getPart());
                question.setQuestionText(questionDetails.getQuestionText());
                question.setQuestionImg(questionDetails.getQuestionImg());
                question.setQuestionAudio(questionDetails.getQuestionAudio());
                question.setOptions(questionDetails.getOptions());
                question.setUpdatedDate(new Date());
                Question updatedQuestion = questionRepo.save(question);
                return ResponseEntity.status(HttpStatus.OK).body(updatedQuestion);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Question not found.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data provided.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating the question.");
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

    @GetMapping("/byPart/{part}")
    public ResponseEntity<?> findQuestionsByPart(@PathVariable("part") String part) {
        try {
            int partNumber;
            try {
                partNumber = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Part must be a valid number.");
            }
            List<?> questions = questionRepo.findQuestionsByPart(partNumber);
            if (questions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No questions found for the specified part.");
            }
            return ResponseEntity.status(HttpStatus.OK).body(questions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrieving the questions.");
        }
    }

    @GetMapping("/randomByPart/{part}")
    public ResponseEntity<?> findRandomQuestionsByPart(@PathVariable("part") String part) {
        try {
            int partNumber;
            try {
                partNumber = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Part must be a valid number.");
            }
            List<Question> questions = questionRepo.findRandomQuestionsByPart(partNumber, 6);
            if (questions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No questions found for the specified part.");
            }
            return ResponseEntity.status(HttpStatus.OK).body(questions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrieving the random questions.");
        }
    }

}
