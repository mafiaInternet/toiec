package com.toeic.toeic_app.controller;

import com.toeic.toeic_app.model.User;
import com.toeic.toeic_app.repository.UserRepo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email not found.");
        }
        User user = optionalUser.get(); // Lấy đối tượng User từ Optional
        String code = generateVerificationCode();
        sendEmail(email, code);
        user.setResetCode(code);
        user.setResetCodeExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // 15 minutes expiry
        userRepo.save(user);
        return ResponseEntity.status(HttpStatus.OK).body("Reset code sent.");
    }


    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }

    private void sendEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Code");
        message.setText("Your password reset code is: " + code);
        emailSender.send(message);
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String code, @RequestParam String newPassword) {
        Optional<User> optionalUser = userRepo.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email not found.");
        }
        User user = optionalUser.get();
        if (user.getResetCode() == null || !code.equals(user.getResetCode()) || new Date().after(user.getResetCodeExpiry())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired code.");
        }
        user.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        userRepo.save(user);
        return ResponseEntity.status(HttpStatus.OK).body("Password updated successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User loginRequest) {
        Optional<User> userOptional = userRepo.findByEmail(loginRequest.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String hashedPassword = DigestUtils.md5DigestAsHex(loginRequest.getPassword().getBytes());
            if (hashedPassword.equals(user.getPassword())) {
                return ResponseEntity.status(HttpStatus.OK).body(user);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("save")
    public ResponseEntity<User> saveUser(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        Date currentDate = new Date();
        if (user.getCreatedDate() == null) {
            user.setCreatedDate(currentDate);
            user.setUpdatedDate(currentDate);
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setRole("user");
        User savedUser = userRepo.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
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
                if (userDetails.getNationality() != null) {
                    user.setNationality(userDetails.getNationality());
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
