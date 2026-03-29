package com.finance.tracker.service;

import org.springframework.stereotype.Service;

import com.finance.tracker.entity.User;
import com.finance.tracker.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
	
	public User registerUser(User user) {
	    return userRepository.save(user);
	}

}
