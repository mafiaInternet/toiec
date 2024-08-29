package com.toeic.toeic_app.repository;

import com.toeic.toeic_app.model.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends MongoRepository<User, ObjectId> {
    Optional<User> findByEmail(String email);
}
