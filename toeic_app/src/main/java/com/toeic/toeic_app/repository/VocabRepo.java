package com.toeic.toeic_app.repository;

import com.toeic.toeic_app.model.Vocabulary;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface VocabRepo extends MongoRepository<Vocabulary, ObjectId> {
    // Tìm kiếm từ vựng bằng văn bản (text) với biểu thức chính quy
    @Query("{ 'text': { $regex: ?0, $options: 'i' } }")
    List<Vocabulary> findByText(String textQuery);

    // Tìm kiếm từ vựng bằng văn bản (text) và chủ đề (topic)
    @Query("{ 'text': { $regex: ?0, $options: 'i' }, 'topic': ?1 }")
    List<Vocabulary> findByTextAndTopic(String textQuery, Number topic);
}
