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
            List<Question> allQuestions = questionRepo.findAllByPart(part);

            if (allQuestions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseWrapper<>(null, 2)); // Không tìm thấy câu hỏi
            }

            // Nếu part là 3, 4, 6, hoặc 7, thì kiểm tra limit và nhóm câu hỏi
            if (part.equals("3") || part.equals("4") || part.equals("6") || part.equals("7")) {
                if (limit <= 0 || limit % 3 != 0) {
                    return ResponseEntity.status(HttpStatus.OK)
                            .body(new ResponseWrapper<>(null, 2)); // Kiểm tra limit phải là bội số của 3
                }

                // Nhóm câu hỏi theo stt
                Map<String, List<Question>> groupedQuestions = allQuestions.stream()
                        .collect(Collectors.groupingBy(Question::getStt));

                List<List<Question>> resultGroups = new ArrayList<>();

                // Lặp qua các nhóm và thêm vào kết quả
                for (List<Question> group : groupedQuestions.values()) {
                    Collections.shuffle(group); // Trộn ngẫu nhiên nhóm câu hỏi
                    for (int i = 0; i < group.size(); i += 3) {
                        if (i + 3 <= group.size()) {
                            resultGroups.add(group.subList(i, i + 3)); // Thêm nhóm 3 câu hỏi
                        }
                    }
                }

                // Kiểm tra số lượng câu hỏi đủ với limit
                if (resultGroups.size() < limit / 3) {
                    return ResponseEntity.status(HttpStatus.OK)
                            .body(new ResponseWrapper<>(null, 2)); // Không đủ nhóm câu hỏi
                }

                // Chỉ trả về số lượng nhóm yêu cầu
                List<List<Question>> limitedGroups = resultGroups.subList(0, limit / 3);
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseWrapper<>(limitedGroups, 1));

            } else {
                // Với các part khác, trả về câu hỏi ngẫu nhiên với số lượng giới hạn
                Collections.shuffle(allQuestions); // Trộn ngẫu nhiên danh sách câu hỏi
                List<Question> limitedQuestions = allQuestions.stream()
                        .limit(limit) // Giới hạn số lượng câu hỏi theo limit
                        .collect(Collectors.toList());

                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseWrapper<>(limitedQuestions, 1));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseWrapper<>(null, 3)); // Lỗi không xác định
        }
    }









    @PostMapping("/save")
    public ResponseEntity<?> saveQuestion(@RequestParam(value = "file", required = false) MultipartFile file,
                                          @RequestParam(value = "questionImg", required = false) MultipartFile questionImg,
                                          @RequestParam("test") Number test,
                                          @RequestParam("part") Number part,
                                          @RequestParam("questionText") String questionText,
                                          @RequestParam("options") String optionsJson,
                                          @RequestParam("stt") String stt) {
        try {
            // Địa chỉ gốc của máy chủ
            String serverBaseUrl = "http://18.216.169.143:8081";

            // Tạo đối tượng Question
            Question question = new Question();
            question.setTest(test);
            question.setPart(part);
            question.setQuestionText(questionText);
            question.setStt(stt);

            // Xử lý file âm thanh (nếu có)
            if (file != null && !file.isEmpty()) {
                String originalFileName = file.getOriginalFilename();
                if (originalFileName != null) {
                    String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                    String audioFileName = new ObjectId().toString() + "_" + sanitizedFileName;
                    Path audioFilePath = Paths.get(AUDIO_DIRECTORY + File.separator + audioFileName);
                    Files.write(audioFilePath, file.getBytes());
                    // Tạo URL đầy đủ cho file âm thanh
                    String audioFileUrl = serverBaseUrl + "/audio/" + audioFileName;
                    question.setQuestionAudio(audioFileUrl); // Lưu URL đầy đủ của audio
                }
            } else {
                question.setQuestionAudio(null); // Nếu không có file, đặt giá trị null
            }

            // Xử lý ảnh câu hỏi (nếu có)
            if (questionImg != null && !questionImg.isEmpty()) {
                String originalImgName = questionImg.getOriginalFilename();
                if (originalImgName != null) {
                    String sanitizedImgName = originalImgName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                    String imgFileName = new ObjectId().toString() + "_" + sanitizedImgName;
                    Path imgFilePath = Paths.get(IMG_DIRECTORY + File.separator + imgFileName);
                    Files.write(imgFilePath, questionImg.getBytes());
                    // Tạo URL đầy đủ cho ảnh câu hỏi
                    String imgFileUrl = serverBaseUrl + "/img/" + imgFileName;
                    question.setQuestionImg(imgFileUrl); // Lưu URL đầy đủ của hình ảnh
                }
            } else {
                question.setQuestionImg(null); // Nếu không có ảnh, đặt giá trị null
            }

            // Chuyển đổi chuỗi JSON thành danh sách options
            ObjectMapper mapper = new ObjectMapper();
            List<com.toeic.toeic_app.model.Question.Option> options = Arrays.asList(mapper.readValue(optionsJson, com.toeic.toeic_app.model.Question.Option[].class));
            question.setOptions(options);

            Date currentDate = new Date();
            question.setCreatedDate(currentDate);
            question.setUpdatedDate(currentDate);

            // Lưu câu hỏi vào database
            Question savedQuestion = questionRepo.save(question);

            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(savedQuestion, 1));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 3));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
        }
    }









//    @PostMapping("/save")
//    public ResponseEntity<?> saveQuestion(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("questionImg") MultipartFile questionImg,
//            @RequestParam("test") Number test,
//            @RequestParam("part") Number part,
//            @RequestParam("questionText") String questionText,
//            @RequestParam("options") String optionsJson) {
//        try {
//            String serverBaseUrl = "http://18.216.169.143:8081";
//            String audioFileUrl = null;
//
//            // Nếu part là 3, kiểm tra xem đã có URL audio chưa
//            if (part.intValue() == 3) {
//                // Tìm các câu hỏi Part 3 đã lưu và lấy URL audio của chúng
//                List<Question> part3Questions = questionRepo.findByTestAndPart(test, part);
//
//                if (part3Questions != null && !part3Questions.isEmpty()) {
//                    // Nếu đã có câu hỏi Part 3 trước đó, lấy URL âm thanh của câu hỏi trước
//                    audioFileUrl = part3Questions.get(0).getQuestionAudio();
//                } else {
//                    // Nếu chưa có câu hỏi Part 3 nào, xử lý upload file âm thanh mới
//                    String originalFileName = file.getOriginalFilename();
//                    if (originalFileName == null) {
//                        return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
//                    }
//                    String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
//                    String audioFileName = new ObjectId().toString() + "_" + sanitizedFileName;
//                    Path audioFilePath = Paths.get(AUDIO_DIRECTORY + File.separator + audioFileName);
//                    Files.write(audioFilePath, file.getBytes());
//                    audioFileUrl = serverBaseUrl + "/audio/" + audioFileName;
//                }
//            } else {
//                // Nếu không phải Part 3, upload âm thanh như bình thường
//                String originalFileName = file.getOriginalFilename();
//                if (originalFileName == null) {
//                    return ResponseEntity.status(HttpStatus.OK).body(new ResponseWrapper<>(null, 2));
//                }
//                String sanitizedFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
//                String audioFileName = new ObjectId().toString() + "_" + sanitizedFileName;
//                Path audioFilePath = Paths.get(AUDIO_DIRECTORY + File.separator + audioFileName);
//                Files.write(audioFilePath, file.getBytes());
//                audioFileUrl = serverBaseUrl + "/audio/" + audioFileName;
//            }
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
//            String imgFileUrl = serverBaseUrl + "/img/" + imgFileName;
//
//            // Chuyển đổi JSON options thành danh sách Option
//            ObjectMapper mapper = new ObjectMapper();
//            List<com.toeic.toeic_app.model.Question.Option> options = Arrays.asList(mapper.readValue(optionsJson, com.toeic.toeic_app.model.Question.Option[].class));
//
//            // Tạo câu hỏi mới và lưu
//            Question question = new Question();
//            question.setTest(test);
//            question.setPart(part);
//            question.setQuestionText(questionText);
//            question.setQuestionAudio(audioFileUrl); // Sử dụng audioFileUrl từ logic trên
//            question.setQuestionImg(imgFileUrl);
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
