package com.bookingapp.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.bookingapp.model.User;

public interface UserRepository extends MongoRepository<User, Integer> {

	Optional<User> findByEmail(String email);

}
