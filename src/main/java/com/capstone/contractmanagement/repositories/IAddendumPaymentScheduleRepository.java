package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.addendum.Addendum;
import com.capstone.contractmanagement.entities.addendum.AddendumPaymentSchedule;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IAddendumPaymentScheduleRepository extends JpaRepository<AddendumPaymentSchedule, Long> {
    @Query("SELECT aps FROM AddendumPaymentSchedule aps " +
            "JOIN aps.addendum a " +
            "WHERE a.contract = :contract " +
            "AND a.status IN ('APPROVED', 'SIGNED')")
    List<AddendumPaymentSchedule> findByContractAndApproved(@Param("contract") Contract contract);

    @Query("SELECT aps FROM AddendumPaymentSchedule aps WHERE aps.addendum.contract = :contract AND aps.status = :status")
    List<AddendumPaymentSchedule> findByContractAndStatus(@Param("contract") Contract contract, @Param("status") PaymentStatus status);


}
