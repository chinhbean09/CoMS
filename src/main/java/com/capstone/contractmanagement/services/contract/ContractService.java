package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService implements IContractService{


    private final IContractRepository contractRepository;
    private final IUserRepository userRepository;
    private final IPartyRepository partyRepository;
    private final ITermRepository termRepository;

    @Override
    public List<ContractResponse> getAllContracts() {
        return contractRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ContractResponse getContractById(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with id: " + id));
        return convertToResponse(contract);
    }
    private void validateParty(Party party) {
        if (party.getPartnerName() == null || party.getPartnerName().isEmpty()) {
            throw new IllegalArgumentException("Party must have a valid name");
        }
        if (party.getAddress() == null || party.getAddress().isEmpty()) {
            throw new IllegalArgumentException("Party must have a valid address");
        }
    }
    @Override
    public Contract createContract(ContractDTO request) throws DataNotFoundException {
        // Validate các trường bắt buộc
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new DataNotFoundException("User not found"));
        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(() -> new DataNotFoundException("Party not found"));
        validateParty(party); // Kiểm tra thêm nếu cần
//        Template template = templateRepository.findById(request.getTemplateId())
//                .orElse(null);

        if (contractRepository.existsByContractNumber(request.getContractNumber())) {
            throw new IllegalArgumentException("Contract number already exists: " + request.getContractNumber());
        }
        // Build Contract object
        Contract contract = Contract.builder()
                .title(request.getTitle())
                .contractNumber(request.getContractNumber())
                .description(request.getDescription())
                .status(request.getStatus())
                .startDate(request.getStartDate())
                .createdBy(request.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .scope(request.getScope())
                .configuration(request.getConfiguration())
                .sla(request.getSla())
                .confidentiality(request.getConfidentiality())
                .obligations(request.getObligations())
                .amount(request.getAmount())
                .user(user)
                .party(party)
                .build();

        // Thêm các Term liên quan
        if (request.getTermIds() != null) {
            List<Term> terms = termRepository.findAllById(request.getTermIds());
            contract.getTerms().addAll(terms);
        }

        // Thêm PaymentSchedules
        if (request.getPaymentSchedules() != null) {
            List<PaymentSchedule> schedules = request.getPaymentSchedules().stream()
                    .map(scheduleRequest -> PaymentSchedule.builder()
                            .contract(contract)
                            .dueDate(scheduleRequest.getDueDate())
                            .amount(scheduleRequest.getAmount())
                            .build())
                    .toList();
            contract.getPaymentSchedules().addAll(schedules);
        }

        // Thêm PaymentOneTime
        if (request.getPaymentOneTime() != null) {
            PaymentOneTime paymentOneTime = PaymentOneTime.builder()
                    .contract(contract)
                    .dueDate(request.getPaymentOneTime().getDueDate())
                    .amount(request.getPaymentOneTime().getAmount())
                    .build();
            contract.setPaymentOneTime(paymentOneTime);
        }

        // Lưu hợp đồng
        return contractRepository.save(contract);

    }


    @Override
    public ContractResponse updateContract(Long id, ContractDTO contractDTO) {
        Contract existingContract = contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found with id: " + id));

        existingContract.setTitle(contractDTO.getTitle());
        existingContract.setDescription(contractDTO.getDescription());
        existingContract.setStatus(contractDTO.getStatus());
//        existingContract.setUpdatedAt(contractDTO.getUpdatedAt());

        contractRepository.save(existingContract);
        return convertToResponse(existingContract);
    }

    @Override
    public void deleteContract(Long id) {
        contractRepository.deleteById(id);
    }

    private ContractResponse convertToResponse(Contract contract) {
        return ContractResponse.builder()
                .id(contract.getId())
                .title(contract.getTitle())
                .contractNumber(contract.getContractNumber())
                .description(contract.getDescription())
                .status(contract.getStatus())
                .startDate(contract.getStartDate())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .createdBy(contract.getCreatedBy())
                .build();
    }

    private Contract convertToEntity(ContractDTO contractDTO) {
        return Contract.builder()
                .title(contractDTO.getTitle())
                .contractNumber(contractDTO.getContractNumber())
                .description(contractDTO.getDescription())
                .status(contractDTO.getStatus())
                .startDate(contractDTO.getStartDate())
                .createdBy(contractDTO.getCreatedBy())
//                .createdAt(contractDTO.getCreatedAt())
//                .updatedAt(contractDTO.getUpdatedAt())
                .build();
    }
}
