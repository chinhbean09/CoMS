package com.capstone.contractmanagement.services.addendum;

import com.capstone.contractmanagement.dtos.addendum.AddendumDTO;
import com.capstone.contractmanagement.entities.Addendum;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IAddendumRepository;
import com.capstone.contractmanagement.repositories.IContractRepository;
import com.capstone.contractmanagement.responses.addendum.AddendumResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddendumService implements IAddendumService{
    private final IAddendumRepository addendumRepository;
    private final IContractRepository contractRepository;

    @Override
    @Transactional
    public AddendumResponse createAddendum(AddendumDTO addendumDTO) throws DataNotFoundException {
        Contract contract = contractRepository.findById(addendumDTO.getContractId())
                .orElseThrow(() -> new DataNotFoundException("Contract not found"));

        if (contract.getStatus() == ContractStatus.ACTIVE){
            Addendum addendum = Addendum.builder()
                    .title(addendumDTO.getTitle())
                    .content(addendumDTO.getContent())
                    .effectiveDate(addendumDTO.getEffectiveDate())
                    .status(addendumDTO.getStatus())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(null)
                    .contract(contract)
                    .build();
            addendumRepository.save(addendum);

            return AddendumResponse.builder()
                    .addendumId(addendum.getId())
                    .title(addendum.getTitle())
                    .content(addendum.getContent())
                    .effectiveDate(addendum.getEffectiveDate())
                    .createdAt(addendum.getCreatedAt())
                    .updatedAt(addendum.getUpdatedAt())
                    .build();
        }
        throw new DataNotFoundException("Cannot create addendum: Contract is not ACTIVE");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddendumResponse> getAllByContractId(Long contractId) throws DataNotFoundException {
        // Kiểm tra hợp đồng có tồn tại không
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new DataNotFoundException("Contract not found with id: " + contractId));

        // Lấy danh sách phụ lục theo contract id (giả sử repository có method: findByContract_Id)
        List<Addendum> addenda = addendumRepository.findByContract(contract);

        // Nếu không có phụ lục, có thể trả về danh sách rỗng hoặc ném ngoại lệ
        if (addenda.isEmpty()) {
            throw new DataNotFoundException("No addendum found for contract id: " + contractId);
        }

        // Map entity thành DTO
        return addenda.stream()
                .map(addendum -> AddendumResponse.builder()
                        .addendumId(addendum.getId())
                        .title(addendum.getTitle())
                        .content(addendum.getContent())
                        .effectiveDate(addendum.getEffectiveDate())
                        .status(addendum.getStatus())
                        .createdAt(addendum.getCreatedAt())
                        .updatedAt(addendum.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String updateAddendum(Long addendumId, AddendumDTO addendumDTO) throws DataNotFoundException {
        // Tìm phụ lục theo id
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found with id: " + addendumId));

        // Cập nhật thông tin phụ lục (không cập nhật createdAt)
        addendum.setTitle(addendumDTO.getTitle());
        addendum.setContent(addendumDTO.getContent());
        addendum.setEffectiveDate(addendumDTO.getEffectiveDate());
        addendum.setStatus(addendumDTO.getStatus());
        addendum.setUpdatedAt(LocalDateTime.now());

        addendumRepository.save(addendum);
        return "Addendum updated successfully.";
    }

    @Override
    @Transactional
    public void deleteAddendum(Long addendumId) throws DataNotFoundException {
        // Tìm phụ lục theo id
        Addendum addendum = addendumRepository.findById(addendumId)
                .orElseThrow(() -> new DataNotFoundException("Addendum not found with id: " + addendumId));
        // Xóa phụ lục
        addendumRepository.delete(addendum);
    }
}
