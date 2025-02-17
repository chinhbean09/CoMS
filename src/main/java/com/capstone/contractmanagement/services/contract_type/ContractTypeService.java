package com.capstone.contractmanagement.services.contract_type;

import com.capstone.contractmanagement.entities.ContractType;
import com.capstone.contractmanagement.repositories.IContractTypeRepository;
import com.capstone.contractmanagement.services.contract.IContractService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractTypeService implements IContractTypeService {
     private final IContractTypeRepository contractTypeRepository;

    @Override
    public List<ContractType> findAll() {
        return contractTypeRepository.findAll();
    }

    @Override
    public ContractType findById(Long id) {
        return contractTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ContractType không tồn tại với id: " + id));
    }

    @Override
    public ContractType save(ContractType contractType) {
        return contractTypeRepository.save(contractType);
    }

    @Override
    public ContractType update(Long id, ContractType contractType) {
        ContractType existing = findById(id);
        existing.setName(contractType.getName());
        // Nếu có các trường khác cần cập nhật, hãy cập nhật tương ứng
        return contractTypeRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        ContractType existing = findById(id);
        contractTypeRepository.delete(existing);
    }
}
