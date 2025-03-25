package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.repositories.IContractPartnerRepository;
import com.capstone.contractmanagement.repositories.IPaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractPartnerScheduleService implements IContractPartnerScheduleService {
    private final IPaymentScheduleRepository paymentScheduleRepository;
    private final IContractPartnerRepository contractPartnerRepository;
}
