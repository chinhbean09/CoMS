package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.addendum.AddendumPaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAddendumPaymentScheduleRepository extends JpaRepository<AddendumPaymentSchedule, Long> {
}
