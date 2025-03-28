package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.dtos.contract_partner.ContractPartnerDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.contract_partner.ContractPartnerResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IContractPartnerService {
    void createContractPartner(ContractPartnerDTO contractDTO);

    String uploadPdfToCloudinary(MultipartFile file) throws IOException;

    Page<ContractPartnerResponse> getAllContractPartners(String search, int page, int size);

    void deleteContractPartner(Long contractPartnerId) throws DataNotFoundException;

    void updateContractPartner(Long contractPartnerId, ContractPartnerDTO contractDTO) throws DataNotFoundException;

    void uploadPaymentBillUrl(Long contractPartnerId, MultipartFile file) throws DataNotFoundException;
}
