package com.toeic.toeic_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toeic.toeic_app.model.Question;
import com.toeic.toeic_app.repository.QuestionRepo;
import jakarta.annotation.Resource;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/question")
public class QuestionController {
    @Autowired
    private QuestionRepo questionRepo;

    private static final String AUDIO_DIRECTORY = "/data/uploads/audio";
    private static final String IMG_DIRECTORY = "/data/uploads/img";

    @PostMapping("/test")
    public ResponseEntity<?> saveQuestion(@RequestParam("file") MultipartFile file,
                                          @RequestParam("questionImg") MultipartFile questionImg,
                                          @RequestParam("test") Number test,
                                          @RequestParam("part") Number part,
                                          @RequestParam("questionText") String questionText,
                                          @RequestParam("options") String optionsJson) {
        try {
            // Xử lý file âm thanh
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file name.");
            }
            String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String audioFileName = new ObjectId().toString() + "_" + sanitizedFileName;
            Path audioFilePath = Paths.get(AUDIO_DIRECTORY + File.separator + audioFileName);
            Files.write(audioFilePath, file.getBytes());

            // Xử lý ảnh câu hỏi
            String originalImgName = questionImg.getOriginalFilename();
            if (originalImgName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image file name.");
            }
            String sanitizedImgName = originalImgName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String imgFileName = new ObjectId().toString() + "_" + sanitizedImgName;
            Path imgFilePath = Paths.get(IMG_DIRECTORY + File.separator + imgFileName);
            Files.write(imgFilePath, questionImg.getBytes());

            // Parse JSON string to List<com.toeic.toeic_app.model.Question.Option>
            ObjectMapper mapper = new ObjectMapper();
            List<com.toeic.toeic_app.model.Question.Option> options = Arrays.asList(mapper.readValue(optionsJson, com.toeic.toeic_app.model.Question.Option[].class));

            // Tạo mới đối tượng Question
            Question question = new Question();
            question.setTest(test);
            question.setPart(part);
            question.setQuestionText(questionText);
            question.setQuestionAudio(audioFilePath.toString());
            question.setQuestionImg(imgFilePath.toString());
            question.setOptions(options);

            Date currentDate = new Date();
            question.setCreatedDate(currentDate);
            question.setUpdatedDate(currentDate);

            // Lưu đối tượng Question vào MongoDB
            Question savedQuestion = questionRepo.save(question);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data provided.");
        }
    }

    @GetMapping("/audio/{id}")
    public ResponseEntity<?> getAudioByQuestionId(@PathVariable String id) {
        try {
            // Chuyển đổi chuỗi ID thành ObjectId
            ObjectId objectId = new ObjectId(id);

            // Tìm kiếm bản ghi Question theo ObjectId
            Optional<Question> optionalQuestion = questionRepo.findById(objectId);

            if (!optionalQuestion.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Lấy đường dẫn tới file âm thanh
            Question question = optionalQuestion.get();
            String audioFilePath = question.getQuestionAudio();

            // Kiểm tra xem file có tồn tại không
            Path path = Paths.get(audioFilePath);
            if (!Files.exists(path)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Tạo đối tượng FileSystemResource
            FileSystemResource fileResource = new FileSystemResource(path.toFile());

            // Trả về file âm thanh cho client
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .body(fileResource);

        } catch (IllegalArgumentException e) {
            // Trả về trạng thái lỗi
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


    @GetMapping("/{id}/image")
    public ResponseEntity<?> getImageByQuestionId(@PathVariable String id) {
        try {
            // Convert the string ID to ObjectId
            ObjectId objectId = new ObjectId(id);

            // Find the Question record by ObjectId
            Optional<Question> optionalQuestion = questionRepo.findById(objectId);

            if (!optionalQuestion.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("khong có question");
            }

            // Get the path to the image file
            Question question = optionalQuestion.get();
            String imgFilePath = question.getQuestionImg();

            // Check if the file exists
            Path path = Paths.get(imgFilePath);
            if (!Files.exists(path)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("khong co path");
            }

            // Create a FileSystemResource
            FileSystemResource fileResource = new FileSystemResource(path.toFile());

            // Return the image file to the client
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                    .contentType(MediaType.IMAGE_JPEG) // Use appropriate MediaType for image format
                    .body(fileResource);

        } catch (IllegalArgumentException e) {
            // Return an error status
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

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
