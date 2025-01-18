package com.capstone.contractmanagement.services.party;

import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.repositories.IPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyService implements IPartyService{
    private final IPartyRepository partyRepository;
    // Create a new Party
    public Party createParty(Party party) {
        return partyRepository.save(party);
    }

    // Get all Parties
    public List<Party> getAllParties() {
        return partyRepository.findAll();
    }

    // Get a Party by ID
    public Party getPartyById(Long id) {
        return partyRepository.findById(id).orElseThrow(() -> new RuntimeException("Party not found with id: " + id));
    }

    // Update a Party
    public Party updateParty(Long id, Party partyDetails) {
        Party party = getPartyById(id);
        party.setPartyType(partyDetails.getPartyType());
        party.setName(partyDetails.getName());
        party.setAddress(partyDetails.getAddress());
        party.setTaxCode(partyDetails.getTaxCode());
        party.setIdentityCard(partyDetails.getIdentityCard());
        party.setRepresentative(partyDetails.getRepresentative());
        party.setContactInfo(partyDetails.getContactInfo());
        return partyRepository.save(party);
    }

    // Delete a Party
    public void deleteParty(Long id) {
        Party party = getPartyById(id);
        partyRepository.delete(party);
    }

}
