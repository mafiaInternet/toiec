package com.toeic.toeic_app.repository;

import com.toeic.toeic_app.model.Question;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface QuestionRepo extends MongoRepository<Question, ObjectId> {
    List<Question> findAllByPart(String part);
    @Query("{ 'test': ?0, 'part': ?1 }")
    List<Question> findByTestAndPart(Number test, Number part);


    @Aggregation(pipeline = {
            "{ '$match': { 'part': ?0 } }",
            "{ '$group': { '_id': '$stt', 'questions': { '$push': '$$ROOT' } } }"
    })
    List<Map<String, Object>> groupByStt(String part);
}
