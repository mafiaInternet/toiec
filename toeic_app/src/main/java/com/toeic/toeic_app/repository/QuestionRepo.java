package com.toeic.toeic_app.repository;

import com.toeic.toeic_app.model.Question;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface QuestionRepo extends MongoRepository<Question, ObjectId> {
    default List<?> findQuestionsByPart(int part) {
        if (part == 3 || part == 4) {
            return findQuestionsGroupedByAudio(part);
        } else {
            return findByPart(part);
        }
    }

    @Query("{ 'part': ?0 }")
    List<Question> findByPart(int part);

    @Query("{ 'part': ?0 }")
    List<Question> findAllByPart(int part);

    default List<List<Question>> findQuestionsGroupedByAudio(int part) {
        List<Question> questions = findAllByPart(part);

        Map<String, List<Question>> audioGroupedQuestions = questions.stream()
                .collect(Collectors.groupingBy(Question::getQuestionAudio));

        List<List<Question>> groupedQuestionsList = new ArrayList<>();
        for (List<Question> group : audioGroupedQuestions.values()) {
            for (int i = 0; i < group.size(); i += 3) {
                groupedQuestionsList.add(group.subList(i, Math.min(i + 3, group.size())));
            }
        }

        return groupedQuestionsList;
    }

    //random
    default List<Question> findRandomQuestionsByPart(int part, int limit) {
        if (part == 3 || part == 4) {
            if (limit % 3 != 0) {
                throw new IllegalArgumentException("Limit must be a multiple of 3 for parts 3 and 4.");
            }
        }
        List<Question> allQuestions = findAllByPart(part);
        Collections.shuffle(allQuestions);
        List<Question> randomQuestions = new ArrayList<>();

        if (part == 3 || part == 4) {
            Map<String, List<Question>> audioGroupedQuestions = allQuestions.stream()
                    .collect(Collectors.groupingBy(Question::getQuestionAudio));
            for (List<Question> group : audioGroupedQuestions.values()) {
                if (group.size() >= 3) {
                    randomQuestions.addAll(group.subList(0, 3));
                    if (randomQuestions.size() >= limit) {
                        return randomQuestions.subList(0, limit);
                    }
                }
            }
        } else {
            for (Question question : allQuestions) {
                randomQuestions.add(question);
                if (randomQuestions.size() >= limit) {
                    return randomQuestions.subList(0, limit);
                }
            }
        }
        return randomQuestions;
    }

}
