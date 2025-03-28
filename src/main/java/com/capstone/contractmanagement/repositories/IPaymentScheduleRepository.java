package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IPaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {
    List<PaymentSchedule> findByPaymentDateBeforeAndStatus(LocalDateTime dateTime, PaymentStatus status);
    List<PaymentSchedule> findByPaymentDateBetweenAndStatus(LocalDateTime start, LocalDateTime end, PaymentStatus status);
}
