package com.capstone.contractmanagement.services.contract_type;

import com.capstone.contractmanagement.entities.contract.ContractType;
import com.capstone.contractmanagement.repositories.IContractTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContractTypeService implements IContractTypeService {
     private final IContractTypeRepository contractTypeRepository;

    @Override
    public List<ContractType> findAll() {
        return contractTypeRepository.findAllByIsDeletedFalse();
    }

    @Override
    public Optional<ContractType> findById(Long id) {
        return Optional.ofNullable(contractTypeRepository.findById(id)
                .filter(contractType -> !contractType.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại hợp đồng")));
    }
    @Override
    @Transactional
    public ContractType save(ContractType contractType) {
//        boolean exists = contractTypeRepository.existsByNqame(contractType.getName());

        Optional<ContractType> existingOpt = contractTypeRepository.findByNameAndIsDeletedFalse(contractType.getName());

        // Nếu tồn tại bản ghi chưa bị xóa với tên trùng, ném lỗi
        if (existingOpt.isPresent()) {
            throw new IllegalArgumentException("Loại hợp đồng với tên '" + contractType.getName() + "' đã tồn tại.");
        }

        // Nếu không tìm thấy bản ghi trùng tên và chưa bị xóa, lưu bản ghi mới
        return contractTypeRepository.save(contractType);

    }

    @Override
    public ContractType update(Long id, ContractType contractType) {
        return contractTypeRepository.findById(id)
                .map(existingType -> {
                    // Kiểm tra xem tên mới đã tồn tại đối với một ContractType khác chưa
                    if (contractTypeRepository.existsByNameAndIdNotAndIsDeletedFalse(contractType.getName(), id)) {
                        throw new IllegalArgumentException("exist");
                    }
                    existingType.setName(contractType.getName());
                    return contractTypeRepository.save(existingType);
                })
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại hợp đồng"));
    }


    @Override
    public void delete(Long id) {
        if (!contractTypeRepository.existsById(id)) {
            throw new RuntimeException("ContractType not found");
        }
        contractTypeRepository.deleteById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDeleteStatus(Long id, Boolean isDeleted) {
        ContractType contractType = contractTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(" Không tìm thấy loại hợp đồng"));

        if (isDeleted) {
            // Check if any ContractTemplate is using this ContractType
            if (!contractType.getTemplates().isEmpty() || !contractType.getContracts().isEmpty()) {
                throw new IllegalStateException("Không thể xóa loại hợp đồng này vì nó đang được sử dụng trong hợp đồng/mẫu hợp đồng");
            }
        }

        contractType.setDeleted(isDeleted);
        contractTypeRepository.save(contractType);
    }


}
