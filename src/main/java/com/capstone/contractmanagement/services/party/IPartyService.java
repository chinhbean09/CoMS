package com.capstone.contractmanagement.services.party;

import com.capstone.contractmanagement.entities.Party;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface IPartyService {
    Party createParty(Party party);

    List<Party> getAllParties();

    // Get a Party by ID
     Party getPartyById(Long id) ;

    // Update a Party
     Party updateParty(Long id, Party partyDetails) ;

    // Delete a Party
     void deleteParty(Long id) ;

    }
