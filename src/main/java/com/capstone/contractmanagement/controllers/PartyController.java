package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.repositories.IPartyRepository;
import com.capstone.contractmanagement.services.party.IPartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/parties")
@RequiredArgsConstructor
public class PartyController {
    private final IPartyRepository partyRepository;
    private final IPartyService partyService;
//    @PostMapping
//    public ResponseEntity<Party> createParty(@RequestBody PartyDTO request) {
//        Party party = Party.builder()
//                .partyType(request.getPartyType())
//                .name(request.getName())
//                .address(request.getAddress())
//                .taxCode(request.getTaxCode())
//                .identityCard(request.getIdentityCard())
//                .representative(request.getRepresentative())
//                .contactInfo(request.getContactInfo())
//                .build();
//
//        Party savedParty = partyRepository.save(party);
//        return ResponseEntity.status(HttpStatus.CREATED).body(savedParty);
//    }
// Create a new Party
@PostMapping
public ResponseEntity<Party> createParty(@RequestBody Party party) {
    Party createdParty = partyService.createParty(party);
    return ResponseEntity.ok(createdParty);
}

    // Get all Parties
    @GetMapping
    public ResponseEntity<List<Party>> getAllParties() {
        List<Party> parties = partyService.getAllParties();
        return ResponseEntity.ok(parties);
    }

    // Get a Party by ID
    @GetMapping("/{id}")
    public ResponseEntity<Party> getPartyById(@PathVariable Long id) {
        Party party = partyService.getPartyById(id);
        return ResponseEntity.ok(party);
    }

    // Update a Party
    @PutMapping("/{id}")
    public ResponseEntity<Party> updateParty(@PathVariable Long id, @RequestBody Party party) {
        Party updatedParty = partyService.updateParty(id, party);
        return ResponseEntity.ok(updatedParty);
    }

    // Delete a Party
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParty(@PathVariable Long id) {
        partyService.deleteParty(id);
        return ResponseEntity.noContent().build();
    }
}

