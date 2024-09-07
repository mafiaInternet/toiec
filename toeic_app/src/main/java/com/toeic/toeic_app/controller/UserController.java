package com.toeic.toeic_app.controller;

import com.toeic.toeic_app.model.User;
import com.toeic.toeic_app.repository.UserRepo;
import com.toeic.toeic_app.wrapper.ResponseWrapper;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JavaMailSender emailSender;

    @PostMapping("/send-code")
    public ResponseEntity<?> sendResetCode(@RequestParam String email) {
        Optional<User> optionalUser = userRepo.findByEmail(email);
        if (optionalUser.isEmpty()) {
            ResponseWrapper<?> response = new ResponseWrapper<>(null, 2);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        User user = optionalUser.get(); // Lấy đối tượng User từ Optional
        String code = generateVerificationCode();
        sendEmail(email, code);
        user.setResetCode(code);
        user.setResetCodeExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000));
        userRepo.save(user);
        Map<String, String> content = new HashMap<>();
        content.put("code", code);
        ResponseWrapper<Map<String, String>> response = new ResponseWrapper<>(content, 1);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    private String generateVerificationCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000); // 4-digit code
        return String.valueOf(code);
    }

    private void sendEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Code");
        message.setText("Your password reset code is: " + code);
        emailSender.send(message);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        Optional<User> optionalUser = userRepo.findByEmail(email);
        if (optionalUser.isEmpty()) {
            ResponseWrapper<?> response = new ResponseWrapper<>(null, 2); // Code 2: Email không tồn tại
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        User user = optionalUser.get();
        if (new Date().after(user.getResetCodeExpiry())) {
            ResponseWrapper<?> response = new ResponseWrapper<>(null, 2); // Code 2: Mã reset không hợp lệ hoặc đã hết hạn
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        // Cập nhật mật khẩu mới với mã hóa MD5
        user.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        user.setResetCode(null); // Xóa mã reset
        user.setResetCodeExpiry(null); // Xóa hạn mã reset
        userRepo.save(user);

        // Trả về phản hồi thành công
        ResponseWrapper<?> response = new ResponseWrapper<>(null, 1); // Code 1: Đặt lại mật khẩu thành công
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<ResponseWrapper<User>> login(@RequestBody User loginRequest) {
        try {
            Optional<User> userOptional = userRepo.findByEmail(loginRequest.getEmail());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                String hashedPassword = DigestUtils.md5DigestAsHex(loginRequest.getPassword().getBytes());
                if (hashedPassword.equals(user.getPassword())) {
                    ResponseWrapper<User> response = new ResponseWrapper<>(user, 1);
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                } else {
                    ResponseWrapper<User> response = new ResponseWrapper<>(null, 2);
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                }
            } else {
                ResponseWrapper<User> response = new ResponseWrapper<>(null, 2);
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }
        } catch (Exception e) {
            ResponseWrapper<User> response = new ResponseWrapper<>(null, 3);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
    }


    @PostMapping("/register")
    public ResponseEntity<ResponseWrapper<User>> saveUser(@RequestBody User user) {
        try {
            Optional<User> existingUser = userRepo.findByEmail(user.getEmail());
            if (existingUser.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseWrapper<>(null, 2));
            }
            if (user.getName() == null || user.getName().isEmpty() ||
                    user.getEmail() == null || user.getEmail().isEmpty() ||
                    user.getPassword() == null || user.getPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseWrapper<>(null, 2));
            }
            Date currentDate = new Date();
            user.setCreatedDate(currentDate);
            user.setUpdatedDate(currentDate);
            user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
            user.setRole("user");
            User savedUser = userRepo.save(user);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseWrapper<>(savedUser, 1));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseWrapper<>(null, 3));
        }
    }



    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userRepo.findAll();
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            }
            return ResponseEntity.status(HttpStatus.OK).body(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") String id) {
        try {
            Optional<User> user = userRepo.findById(new ObjectId(id));
            if (user.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while fetching the user.");
        }
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") String id, @RequestBody User userDetails) {
        try {
            Optional<User> userOptional = userRepo.findById(new ObjectId(id));
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (userDetails.getEmail() != null) {
                    user.setEmail(userDetails.getEmail());
                }
                if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                    user.setPassword(DigestUtils.md5DigestAsHex(userDetails.getPassword().getBytes()));
                }
                if (userDetails.getName() != null) {
                    user.setName(userDetails.getName());
                }
                if (userDetails.getPhone() != null) {
                    user.setPhone(userDetails.getPhone());
                }
                if (userDetails.getDateOfBirth() != null) {
                    user.setDateOfBirth(userDetails.getDateOfBirth());
                }
                if (userDetails.getSex() != null) {
                    user.setSex(userDetails.getSex());
                }
                if (userDetails.getLocation() != null) {
                    user.setLocation(userDetails.getLocation());
                }
                user.setUpdatedDate(new Date());
                User updatedUser = userRepo.save(user);
                return ResponseEntity.status(HttpStatus.OK).body(updatedUser);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user ID format or invalid data.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating the user.");
        }
    }



    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") String id) {
        Optional<User> userOptional = userRepo.findById(new ObjectId(id));
        if (userOptional.isPresent()) {
            userRepo.delete(userOptional.get());
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
