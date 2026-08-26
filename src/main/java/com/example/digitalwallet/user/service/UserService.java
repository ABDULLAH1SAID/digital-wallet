package com.example.digitalwallet.user.service;

import com.example.digitalwallet.common.exception.UserNotFoundException;
import com.example.digitalwallet.user.entity.User;
import com.example.digitalwallet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        return userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public long countUsers() {
        return userRepository.count();
    }
}