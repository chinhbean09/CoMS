package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.dtos.contract.ContractComparisonDTO;
import com.capstone.contractmanagement.dtos.contract.ContractDTO;
import com.capstone.contractmanagement.dtos.contract.ContractUpdateDTO;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.contract.ContractComparisonResponse;
import com.capstone.contractmanagement.responses.contract.ContractResponse;
import com.capstone.contractmanagement.responses.contract.GetAllContractReponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IContractService {

    Optional<ContractResponse> getContractById(Long id) throws DataNotFoundException;


    void deleteContract(Long id);

    Contract createContractFromTemplate(ContractDTO dto) throws DataNotFoundException;

    Page<GetAllContractReponse> getAllContracts(Pageable pageable,
                                                       String keyword,
                                                       List<ContractStatus> statuses,  // Thay đổi thành danh sách
                                                       Long contractTypeId,
                                                       User currentUser);

    Contract duplicateContract(Long contractId);

    boolean softDelete(Long id);

    ContractStatus updateContractStatus(Long id, ContractStatus status) throws DataNotFoundException;


    Contract updateContract(Long contractId, ContractUpdateDTO dto);

    Contract rollbackContract(Long originalContractId, int targetVersion);

    List<ContractComparisonDTO> compareVersions(Long originalContractId, Integer version1, Integer version2) throws DataNotFoundException;


    Page<GetAllContractReponse> getAllVersionsByOriginalContractId(Long originalContractId, Pageable pageable, User currentUser);


    public List<ContractResponse> getContractsByOriginalIdAndVersions(Long originalContractId, Integer version1, Integer version2);

    Page<GetAllContractReponse> getAllContractsByPartnerId(
            Long partnerId,
            Pageable pageable,
            String keyword,
            ContractStatus status,
            LocalDateTime signingDate);

    Contract duplicateContractWithPartner(Long contractId, Long partnerId);

    void uploadSignedContract(Long contractId, List<MultipartFile> files) throws DataNotFoundException;

    List<String> getSignedContractUrl(Long contractId) throws DataNotFoundException;

    void uploadSignedContractBase64(Long contractId, String file, String fileName) throws DataNotFoundException, IOException;
    }
