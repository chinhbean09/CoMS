package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Notification;
import com.capstone.contractmanagement.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface INotificationRepository extends JpaRepository<Notification, Long> {
    // tìm notification theo người dùng và isRead = false
    List<Notification> findByUserAndIsReadFalse(User user);

    Page<Notification> findByUser(User user, Pageable pageable); // tìm notification theo người dùng
}
