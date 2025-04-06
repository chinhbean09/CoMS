package com.capstone.contractmanagement.services.party;

import com.capstone.contractmanagement.dtos.party.CreatePartnerDTO;
import com.capstone.contractmanagement.dtos.party.UpdatePartnerDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.OperationNotPermittedException;
import com.capstone.contractmanagement.responses.party.CreatePartnerResponse;
import com.capstone.contractmanagement.responses.party.ListPartnerResponse;
import org.springframework.data.domain.Page;

public interface IPartnerService {
    CreatePartnerResponse createPartner(CreatePartnerDTO createPartnerDTO);

    CreatePartnerResponse updatePartner(Long id, UpdatePartnerDTO updatePartnerDTO) throws DataNotFoundException, OperationNotPermittedException;
    void deleteParty(Long id) throws DataNotFoundException;
//
    Page<ListPartnerResponse> getAllPartners(String search, int page, int size);
    ListPartnerResponse getPartnerById(Long id) throws DataNotFoundException;

    void updatePartnerStatus(Long partyId, Boolean isDeleted) throws DataNotFoundException, OperationNotPermittedException;


}
