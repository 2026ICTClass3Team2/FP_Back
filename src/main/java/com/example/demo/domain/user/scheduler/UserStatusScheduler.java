package com.example.demo.domain.user.scheduler;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.UserStatus;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserStatusScheduler {

    private final UserRepository userRepository;

    // Run every day at 3 AM
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void anonymizeDeletedUsers() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        // Find users who have been deleted and their last update was more than 30 days ago
        List<User> deletedUsers = userRepository.findByStatusAndUpdatedAtBefore(UserStatus.deleted, thirtyDaysAgo);
        
        for (User user : deletedUsers) {
            if (!user.getNickname().startsWith("deletedUser_")) {
                String randomString = UUID.randomUUID().toString().substring(0, 8);
                user.setNickname("deletedUser_" + randomString);
                user.setUsername("deletedUser_" + randomString);
                user.setEmail("deletedUser_" + randomString + "@deleted.com"); // Email must be unique, so add random string here too
                userRepository.save(user);
            }
        }
    }
}
