package com.capstone.contractmanagement.services.contract_type;

import com.capstone.contractmanagement.entities.ContractType;
import com.capstone.contractmanagement.repositories.IContractTypeRepository;
import com.capstone.contractmanagement.services.contract.IContractService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
                .orElseThrow(() -> new EntityNotFoundException("ContractType not found")));
    }
    @Override
    public ContractType save(ContractType contractType) {
        boolean exists = contractTypeRepository.existsByName(contractType.getName());
        if (exists) {
            throw new IllegalArgumentException("exist");
        }
        return contractTypeRepository.save(contractType);
    }

    @Override
    public ContractType update(Long id, ContractType contractType) {
        return contractTypeRepository.findById(id)
                .map(existingType -> {
                    // Kiểm tra xem tên mới đã tồn tại đối với một ContractType khác chưa
                    if (contractTypeRepository.existsByNameAndIdNot(contractType.getName(), id)) {
                        throw new IllegalArgumentException("exist");
                    }
                    existingType.setName(contractType.getName());
                    return contractTypeRepository.save(existingType);
                })
                .orElseThrow(() -> new EntityNotFoundException("ContractType not found with id: " + id));
    }


    @Override
    public void delete(Long id) {
        if (!contractTypeRepository.existsById(id)) {
            throw new RuntimeException("ContractType not found");
        }
        contractTypeRepository.deleteById(id);
    }

    @Override
    public void updateDeleteStatus(Long id, Boolean isDeleted) {
        ContractType contractType = contractTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ContractType not found with id: " + id));
        contractType.setDeleted(isDeleted);
        contractTypeRepository.save(contractType);
    }


}
