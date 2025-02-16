package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.party.CreatePartyDTO;
import com.capstone.contractmanagement.dtos.party.UpdatePartyDTO;
import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IPartyRepository;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.party.CreatePartyResponse;
import com.capstone.contractmanagement.responses.party.ListPartyResponse;
import com.capstone.contractmanagement.services.party.IPartyService;
import com.capstone.contractmanagement.utils.MessageKeys;
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

    @PostMapping("/create")
    public ResponseEntity<ResponseObject> createParty(@RequestBody CreatePartyDTO createPartyDTO) {
        CreatePartyResponse response = partyService.createParty(createPartyDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message(MessageKeys.CREATE_PARTY_SUCCESSFULLY)
                .data(response)
                .build());
    }

    @PutMapping("/update/{partyId}")
    public ResponseEntity<ResponseObject> updateParty(@PathVariable Long partyId, @RequestBody UpdatePartyDTO createPartyDTO) throws DataNotFoundException {
        CreatePartyResponse response = partyService.updateParty(partyId, createPartyDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.UPDATE_PARTY_SUCCESSFULLY)
                .data(response)
                .build());
    }

    @GetMapping("/get-all")
    public ResponseEntity<ResponseObject> getAllParties() {
        List<ListPartyResponse> response = partyService.getAllParties();
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_ALL_PARTIES_SUCCESSFULLY)
                .data(response)
                .build());
    }

    @GetMapping("/get-by-id/{partyId}")
    public ResponseEntity<ResponseObject> getPartyById(@PathVariable Long partyId) throws DataNotFoundException {
        ListPartyResponse response = partyService.getPartyById(partyId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_PARTY_SUCCESSFULLY)
                .data(response)
                .build());
    }

    @DeleteMapping("/delete/{partyId}")
    public ResponseEntity<ResponseObject> deleteParty(@PathVariable Long partyId) throws DataNotFoundException {
        partyService.deleteParty(partyId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.DELETE_PARTY_SUCCESSFULLY)
                .build());
    }
}

