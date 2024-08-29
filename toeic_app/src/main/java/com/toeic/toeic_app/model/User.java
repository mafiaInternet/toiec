package com.toeic.toeic_app.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "USER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;
    private String email;
    private String password;
    private String name;
    private String phone;
    private String dateOfBirth;
    private String sex;
    private String nationality;
    private String location;
    private Date createdDate;
    private Date updatedDate;
    private String role;
    private String resetCode;
    private Date resetCodeExpiry;
}
