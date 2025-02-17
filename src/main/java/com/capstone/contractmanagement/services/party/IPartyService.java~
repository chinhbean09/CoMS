package com.capstone.contractmanagement.services.party;

import com.capstone.contractmanagement.dtos.party.CreatePartyDTO;
import com.capstone.contractmanagement.dtos.party.UpdatePartyDTO;
import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.responses.party.CreatePartyResponse;
import com.capstone.contractmanagement.responses.party.ListPartyResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IPartyService {
    CreatePartyResponse createParty(CreatePartyDTO createPartyDTO);

    CreatePartyResponse updateParty(Long id, UpdatePartyDTO updatePartyDTO) throws DataNotFoundException;
    void deleteParty(Long id) throws DataNotFoundException;
//
    Page<ListPartyResponse> getAllParties(String search, int page, int size);
    ListPartyResponse getPartyById(Long id) throws DataNotFoundException;

}
