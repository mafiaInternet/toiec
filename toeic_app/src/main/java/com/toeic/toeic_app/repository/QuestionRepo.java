package com.toeic.toeic_app.repository;

import com.toeic.toeic_app.model.Question;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepo extends MongoRepository<Question, ObjectId> {
    List<Question> findAllByPart(String part);
}
