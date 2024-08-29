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
import java.util.List;

@Document(collection = "QUESTION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;
    private Number test;
    private Number part;
    private String questionText;
    private String questionImg;
    private String questionAudio;
    private List<Option> options;
    private Date createdDate;
    private Date updatedDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String optionText;
        private Boolean isCorrect;
    }
}
