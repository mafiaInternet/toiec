package com.toeic.toeic_app.repository;

import com.toeic.toeic_app.model.Question;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepo extends MongoRepository<Question, ObjectId> {
    @Query("{ 'part': ?0 }")
    List<Question> findByPart(int part);

    // Additional method to fetch limited documents
    default List<Question> findQuestionsByPart(int part, int limit) {
        long count = countByPart(part);
        int skip = (int) (Math.random() * (count - limit));
        return findByPart(part, limit, skip);
    }

    @Query("{ 'part': ?0 }")
    List<Question> findByPart(int part, int limit, int skip);

    long countByPart(int part);
}
