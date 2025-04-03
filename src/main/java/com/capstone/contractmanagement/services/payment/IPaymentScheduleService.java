package com.capstone.contractmanagement.services.payment;

import com.capstone.contractmanagement.dtos.payment.CreatePaymentScheduleDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;

import java.util.List;

public interface IPaymentScheduleService {

    String createPaymentSchedule(Long contractId, CreatePaymentScheduleDTO createPaymentScheduleDTO) throws DataNotFoundException;
    void checkPaymentDue();
    List<String> getBillUrlsByPaymentId(Long paymentId) throws DataNotFoundException;
}
