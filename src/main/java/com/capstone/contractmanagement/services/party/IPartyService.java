package com.capstone.contractmanagement.services.party;

import com.capstone.contractmanagement.dtos.party.CreatePartnerDTO;
import com.capstone.contractmanagement.dtos.party.UpdatePartnerDTO;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.party.CreatePartnerResponse;
import com.capstone.contractmanagement.responses.party.ListPartnerResponse;
import org.springframework.data.domain.Page;

public interface IPartyService {
    CreatePartnerResponse createParty(CreatePartnerDTO createPartnerDTO);

    CreatePartnerResponse updateParty(Long id, UpdatePartnerDTO updatePartnerDTO) throws DataNotFoundException;
    void deleteParty(Long id) throws DataNotFoundException;
//
    Page<ListPartnerResponse> getAllParties(String search, int page, int size);
    ListPartnerResponse getPartyById(Long id) throws DataNotFoundException;

    void updatePartyStatus(Long partyId, Boolean isDeleted) throws DataNotFoundException;

}
