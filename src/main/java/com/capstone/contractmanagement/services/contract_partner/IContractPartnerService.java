package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.dtos.contract_partner.ContractPartnerDTO;
import com.capstone.contractmanagement.responses.contract_partner.ContractPartnerResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IContractPartnerService {
    void createContractPartner(ContractPartnerDTO contractDTO);

    String uploadPdfToCloudinary(MultipartFile file) throws IOException;

    List<ContractPartnerResponse> getAllContractPartners();
}
