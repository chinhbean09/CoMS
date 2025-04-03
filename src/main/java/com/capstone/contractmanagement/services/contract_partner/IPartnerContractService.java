package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.dtos.contract_partner.PartnerContractDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.contract_partner.PartnerContractResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IPartnerContractService {
    void createContractPartner(PartnerContractDTO contractDTO);

    String uploadPdfToCloudinary(MultipartFile file) throws IOException;

    Page<PartnerContractResponse> getAllContractPartners(String search, int page, int size);

    void deleteContractPartner(Long contractPartnerId) throws DataNotFoundException;

    void updateContractPartner(Long contractPartnerId, PartnerContractDTO contractDTO) throws DataNotFoundException;

    void uploadPaymentBillUrls(Long paymentScheduleId, List<MultipartFile> files) throws DataNotFoundException;
}
