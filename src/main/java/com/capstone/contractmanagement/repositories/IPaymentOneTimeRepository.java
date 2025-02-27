package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.PaymentOneTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPaymentOneTimeRepository extends JpaRepository<PaymentOneTime, Long> {
}
