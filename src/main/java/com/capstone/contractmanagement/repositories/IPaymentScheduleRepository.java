package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IPaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {
    List<PaymentSchedule> findByPaymentDateBeforeAndStatus(LocalDateTime dateTime, PaymentStatus status);
    List<PaymentSchedule> findByPaymentDateBetweenAndStatus(LocalDateTime start, LocalDateTime end, PaymentStatus status);

    @Query("SELECT ps.billUrls FROM PaymentSchedule ps WHERE ps.id = :paymentId")
    List<String> findBillUrlsByPaymentId(@Param("paymentId") Long paymentId);
    List<PaymentSchedule> findByStatus(PaymentStatus status);
}
