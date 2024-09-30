package com.toeic.toeic_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toeic.toeic_app.model.Question;
import com.toeic.toeic_app.repository.QuestionRepo;
import com.toeic.toeic_app.wrapper.ResponseWrapper;
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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/question")
public class QuestionController {
    @Autowired
    private QuestionRepo questionRepo;

    private static final String AUDIO_DIRECTORY = "/data/uploads/audio";
    private static final String IMG_DIRECTORY = "/data/uploads/img";

    @PostMapping("/randomByPart")
    public ResponseEntity<?> getRandomQuestionsByPart(@RequestParam("part") String part,
                                                      @RequestParam("limit") int limit) {
        try {
            if (limit <= 0) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseWrapper<>(null, 2));
            }

            if (!part.equals("3") && !part.equals("4") && !part.equals("1") && !part.equals("2")) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseWrapper<>(null, 2));
            }

            List<Question> allQuestions = questionRepo.findAllByPart(part);
            if (allQuestions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseWrapper<>(null, 2));
            }

            Collections.shuffle(allQuestions);
            List<Question> randomQuestions = new ArrayList<>();

            if (part.equals("3") || part.equals("4")) {
                // Nhóm câu hỏi theo audio
                Map<String, List<Question>> audioGroupedQuestions = allQuestions.stream()
                        .collect(Collectors.groupingBy(Question::getQuestionAudio));

                // Chọn các nhóm có ít nhất 3 câu hỏi
                List<List<Question>> validGroups = audioGroupedQuestions.values().stream()
                        .filter(group -> group.size() >= 3)
                        .collect(Collectors.toList());

                for (List<Question> group : validGroups) {
                    if (randomQuestions.size() + group.size() >= limit) {
                        randomQuestions.addAll(group.subList(0, limit - randomQuestions.size()));
                        return ResponseEntity.status(HttpStatus.OK).body(randomQuestions);
                    }
                    randomQuestions.addAll(group);
                }
            } else {
                for (Question question : allQuestions) {
                    randomQuestions.add(question);
                    if (randomQuestions.size() >= limit) {
                        return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(randomQuestions.subList(0, limit), 1));
                    }
                }
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(randomQuestions, 1));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseWrapper<>(null, 3));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveQuestion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("questionImg") MultipartFile questionImg,
            @RequestParam("test") Number test,
            @RequestParam("part") Number part,
            @RequestParam("questionText") String questionText,
            @RequestParam("options") String optionsJson) {
        try {
            String serverBaseUrl = "http://18.216.169.143:8081";
            String audioFileUrl = null;

            // Nếu part là 3, kiểm tra xem đã có URL audio chưa
            if (part.intValue() == 3) {
                // Tìm các câu hỏi Part 3 đã lưu và lấy URL audio của chúng
                List<Question> part3Questions = questionRepo.findByTestAndPart(test, part);

                if (part3Questions != null && !part3Questions.isEmpty()) {
                    // Nếu đã có câu hỏi Part 3 trước đó, lấy URL âm thanh của câu hỏi trước
                    audioFileUrl = part3Questions.get(0).getQuestionAudio();
                } else {
                    // Nếu chưa có câu hỏi Part 3 nào, xử lý upload file âm thanh mới
                    String originalFileName = file.getOriginalFilename();
                    if (originalFileName == null) {
                        return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
                    }
                    String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                    String audioFileName = new ObjectId().toString() + "_" + sanitizedFileName;
                    Path audioFilePath = Paths.get(AUDIO_DIRECTORY + File.separator + audioFileName);
                    Files.write(audioFilePath, file.getBytes());
                    audioFileUrl = serverBaseUrl + "/audio/" + audioFileName;
                }
            } else {
                // Nếu không phải Part 3, upload âm thanh như bình thường
                String originalFileName = file.getOriginalFilename();
                if (originalFileName == null) {
                    return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
                }
                String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                String audioFileName = new ObjectId().toString() + "_" + sanitizedFileName;
                Path audioFilePath = Paths.get(AUDIO_DIRECTORY + File.separator + audioFileName);
                Files.write(audioFilePath, file.getBytes());
                audioFileUrl = serverBaseUrl + "/audio/" + audioFileName;
            }

            // Xử lý ảnh câu hỏi
            String originalImgName = questionImg.getOriginalFilename();
            if (originalImgName == null) {
                return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
            }
            String sanitizedImgName = originalImgName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
            String imgFileName = new ObjectId().toString() + "_" + sanitizedImgName;
            Path imgFilePath = Paths.get(IMG_DIRECTORY + File.separator + imgFileName);
            Files.write(imgFilePath, questionImg.getBytes());
            String imgFileUrl = serverBaseUrl + "/img/" + imgFileName;

            // Chuyển đổi JSON options thành danh sách Option
            ObjectMapper mapper = new ObjectMapper();
            List<com.toeic.toeic_app.model.Question.Option> options = Arrays.asList(mapper.readValue(optionsJson, com.toeic.toeic_app.model.Question.Option[].class));

            // Tạo câu hỏi mới và lưu
            Question question = new Question();
            question.setTest(test);
            question.setPart(part);
            question.setQuestionText(questionText);
            question.setQuestionAudio(audioFileUrl); // Sử dụng audioFileUrl từ logic trên
            question.setQuestionImg(imgFileUrl);
            question.setOptions(options);

            Date currentDate = new Date();
            question.setCreatedDate(currentDate);
            question.setUpdatedDate(currentDate);

            Question savedQuestion = questionRepo.save(question);

            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(savedQuestion, 1));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 3));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
        }
    }


//    @PostMapping("/save")
//    public ResponseEntity<?> saveQuestion(@RequestParam("file") MultipartFile file,
//                                          @RequestParam("questionImg") MultipartFile questionImg,
//                                          @RequestParam("test") Number test,
//                                          @RequestParam("part") Number part,
//                                          @RequestParam("questionText") String questionText,
//                                          @RequestParam("options") String optionsJson) {
//        try {
//            // Xử lý file âm thanh
//            String originalFileName = file.getOriginalFilename();
//            if (originalFileName == null) {
//                return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
//            }
//            String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
//            String audioFileName = new ObjectId().toString() + "_" + sanitizedFileName;
//            Path audioFilePath = Paths.get(AUDIO_DIRECTORY + File.separator + audioFileName);
//            Files.write(audioFilePath, file.getBytes());
//
//            // Xử lý ảnh câu hỏi
//            String originalImgName = questionImg.getOriginalFilename();
//            if (originalImgName == null) {
//                return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
//            }
//            String sanitizedImgName = originalImgName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
//            String imgFileName = new ObjectId().toString() + "_" + sanitizedImgName;
//            Path imgFilePath = Paths.get(IMG_DIRECTORY + File.separator + imgFileName);
//            Files.write(imgFilePath, questionImg.getBytes());
//
//            ObjectMapper mapper = new ObjectMapper();
//            List<com.toeic.toeic_app.model.Question.Option> options = Arrays.asList(mapper.readValue(optionsJson, com.toeic.toeic_app.model.Question.Option[].class));
//
//            Question question = new Question();
//
//            // Chuyển đổi Number về Integer trước khi lưu
//            question.setTest(test.intValue());
//            question.setPart(part.intValue());
//
//            question.setQuestionText(questionText);
//            question.setQuestionAudio(audioFilePath.toString());
//            question.setQuestionImg(imgFilePath.toString());
//            question.setOptions(options);
//
//            Date currentDate = new Date();
//            question.setCreatedDate(currentDate);
//            question.setUpdatedDate(currentDate);
//
//            Question savedQuestion = questionRepo.save(question);
//
//            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(savedQuestion, 1));
//
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 3));
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
//        }
//    }


    @GetMapping("/audio/{id}")
    public ResponseEntity<?> getAudioByQuestionId(@PathVariable String id) {
        try {
            ObjectId objectId = new ObjectId(id);

            Optional<Question> optionalQuestion = questionRepo.findById(objectId);

            if (!optionalQuestion.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Question question = optionalQuestion.get();
            String audioFilePath = question.getQuestionAudio();

            // Kiểm tra xem file có tồn tại không
            Path path = Paths.get(audioFilePath);
            if (!Files.exists(path)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            FileSystemResource fileResource = new FileSystemResource(path.toFile());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .body(fileResource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


    @GetMapping("/image/{id}")
    public ResponseEntity<?> getImageByQuestionId(@PathVariable String id) {
        try {
            ObjectId objectId = new ObjectId(id);

            Optional<Question> optionalQuestion = questionRepo.findById(objectId);

            if (!optionalQuestion.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("không có question");
            }

            Question question = optionalQuestion.get();
            String imgFilePath = question.getQuestionImg();

            // Tạo đối tượng FileSystemResource
            Path path = Paths.get(imgFilePath);
            FileSystemResource fileResource = new FileSystemResource(path.toFile());

            if (fileResource.exists() && fileResource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                        .contentType(MediaType.IMAGE_JPEG) // Hoặc MediaType.IMAGE_PNG nếu là PNG
                        .body(fileResource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("không có file ảnh");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid ID format");
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

//    @GetMapping("/byPart/{part}")
//    public ResponseEntity<?> findQuestionsByPart(@PathVariable("part") String part) {
//        try {
//            int partNumber;
//            try {
//                partNumber = Integer.parseInt(part);
//            } catch (NumberFormatException e) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Part must be a valid number.");
//            }
//            List<?> questions = questionRepo.findQuestionsByPart(partNumber);
//            if (questions.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No questions found for the specified part.");
//            }
//            return ResponseEntity.status(HttpStatus.OK).body(questions);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while retrieving the questions.");
//        }
//    }



}
