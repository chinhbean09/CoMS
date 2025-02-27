package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.payment.CreatePaymentScheduleDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.services.payment.IPaymentScheduleService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/payment-schedules")
@RequiredArgsConstructor
public class PaymentScheduleController {
    private final IPaymentScheduleService paymentScheduleService;

    // CREATE A NEW PAYMENT SCHEDULE
    @PostMapping("/create/{contractId}")
    public ResponseEntity<String> createPaymentSchedule(@PathVariable Long contractId, @RequestBody CreatePaymentScheduleDTO createPaymentScheduleDTO) throws DataNotFoundException {
        paymentScheduleService.createPaymentSchedule(contractId, createPaymentScheduleDTO);
        return ResponseEntity.ok(MessageKeys.CREATE_PAYMENT_SCHEDULE_SUCCESSFULLY);
    }
}
