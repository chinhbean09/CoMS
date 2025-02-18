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
        return contractTypeRepository.findAll();
    }

    @Override
    public Optional<ContractType> findById(Long id) {
        return contractTypeRepository.findById(id);
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
                    existingType.setName(contractType.getName());
                    return contractTypeRepository.save(existingType);
                }).orElseThrow(() -> new RuntimeException("ContractType not found"));
    }

    @Override
    public void delete(Long id) {
        if (!contractTypeRepository.existsById(id)) {
            throw new RuntimeException("ContractType not found");
        }
        contractTypeRepository.deleteById(id);
    }
}
