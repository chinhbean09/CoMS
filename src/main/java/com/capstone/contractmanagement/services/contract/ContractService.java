package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.components.SecurityUtils;
import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.*;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService implements IContractService{


    private final IContractRepository contractRepository;
    private final IContractTemplateRepository contractTemplateRepository;
    private final IUserRepository userRepository;
    private final IPartyRepository partyRepository;
    private final ITermRepository termRepository;
    private final SecurityUtils currentUser;

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
    public Contract createContractFromTemplate(Long templateId, ContractDTO dto) throws DataNotFoundException {
        // Fetch Template với tất cả quan hệ cần thiết
        ContractTemplate template = contractTemplateRepository.findWithTermsById(templateId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy mẫu hợp đồng với id: " + templateId));

        // Tạo Contract mới và sao chép thông tin từ Template
        Contract contract = new Contract();
        contract.setTitle(template.getContractTitle());
        contract.setSpecialTermsA(template.getSpecialTermsA());
        contract.setSpecialTermsB(template.getSpecialTermsB());
        contract.setContractContent(template.getContractContent());
        contract.setAppendixEnabled(template.getAppendixEnabled());
        contract.setTransferEnabled(template.getTransferEnabled());
        contract.setAutoAddVAT(template.getAutoAddVAT());
        contract.setVatPercentage(template.getVatPercentage());
        contract.setAutoRenew(template.getAutoRenew());

        contract.setContractNumber(dto.getContractNumber());
        contract.setStatus(dto.getStatus());

        contract.setStartDate(LocalDateTime.now());

        Set<Term> allTerms = new HashSet<>();

        allTerms.addAll(template.getLegalBasisTerms());
        allTerms.addAll(template.getGeneralTerms());
        allTerms.addAll(template.getOtherTerms());

        // Thu thập các Term từ additionalTermConfigs
        Set<Long> additionalTermIds = new HashSet<>();
        for (ContractTemplateAdditionalTermDetail config : template.getAdditionalTermConfigs()) {
            additionalTermIds.addAll(config.getCommonTermIds());
            additionalTermIds.addAll(config.getATermIds());
            additionalTermIds.addAll(config.getBTermIds());
        }
        List<Term> additionalTerms = termRepository.findAllById(additionalTermIds);
        allTerms.addAll(additionalTerms);

        // Gán danh sách Term vào Contract
        contract.setTerms(new ArrayList<>(allTerms));

        User user = currentUser.getLoggedInUser();

        // Liên kết Contract với Template và các entity khác
        contract.setTemplate(template);
        contract.setUser(user); // Giả sử currentUser được lấy từ security context
        contract.setParty(partyRepository.findById(dto.getPartyId()) // Giả sử partyId từ DTO
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy đối tác")));

        // Lưu Contract
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
