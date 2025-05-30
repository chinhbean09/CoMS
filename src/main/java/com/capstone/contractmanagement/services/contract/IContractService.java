package com.capstone.contractmanagement.services.contract;

import com.capstone.contractmanagement.dtos.FileBase64DTO;
import com.capstone.contractmanagement.dtos.contract.*;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.contract.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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

    void uploadSignedContractBase64(Long contractId, FileBase64DTO fileBase64DTO, String fileName) throws DataNotFoundException, IOException;

    void notifyNextApprover(Long contractId) throws DataNotFoundException;

    Page<GetAllContractReponse> getAllContractsNearlyExpiryDate(
            int days,
            String keyword,
            int page,
            int size
    );

    void cancelContract(Long contractId, List<MultipartFile> files, ContractCancelDTO contractCancelDTO) throws DataNotFoundException;

    CancelContractResponse getContractCancelReason(Long contractId) throws DataNotFoundException;

    ResponseEntity<ResponseObject> signContract(SignContractRequest request);

    void liquidateContract(Long contractId, List<MultipartFile> files, ContractLiquidateDTO liquidateDTO) throws DataNotFoundException;

    ContractLiquidationResponse getContractLiquidateReason(Long contractId) throws DataNotFoundException;

    }
