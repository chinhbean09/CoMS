package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.dtos.party.CreatePartnerDTO;
import com.capstone.contractmanagement.dtos.party.UpdatePartnerDTO;
import com.capstone.contractmanagement.enums.PartnerType;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.OperationNotPermittedException;
import com.capstone.contractmanagement.repositories.IPartnerRepository;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.responses.party.CreatePartnerResponse;
import com.capstone.contractmanagement.responses.party.ListPartnerResponse;
import com.capstone.contractmanagement.services.party.IPartnerService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/parties")
@RequiredArgsConstructor
public class PartyController {
    private final IPartnerRepository partyRepository;
    private final IPartnerService partyService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> createParty(@RequestBody CreatePartnerDTO createPartnerDTO) {
        CreatePartnerResponse response = partyService.createPartner(createPartnerDTO);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.CREATED)
                .message(MessageKeys.CREATE_PARTY_SUCCESSFULLY)
                .data(response)
                .build());
    }

    @PutMapping("/update/{partyId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> updateParty(@PathVariable Long partyId, @RequestBody UpdatePartnerDTO updatePartnerDTO) {
        try {
            CreatePartnerResponse response = partyService.updatePartner(partyId, updatePartnerDTO);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.UPDATE_PARTY_SUCCESSFULLY)
                    .data(response)
                    .build());
        } catch (DataNotFoundException e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Không tìm thấy đối tác")
                    .build());
        } catch (OperationNotPermittedException e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message(e.getMessage()) // Trả về thông báo cụ thể từ ngoại lệ
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Đã xảy ra lỗi: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/get-all")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getAllParties(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int size,
            @RequestParam(required = false) PartnerType partnerType) {

        Page<ListPartnerResponse> response = partyService.getAllPartners(keyword, page, size, partnerType);
        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message(MessageKeys.GET_ALL_PARTIES_SUCCESSFULLY)
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/get-by-id/{partyId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> getPartyById(@PathVariable Long partyId) throws DataNotFoundException {
        ListPartnerResponse response = partyService.getPartnerById(partyId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.GET_PARTY_SUCCESSFULLY)
                .data(response)
                .build());
    }

    @DeleteMapping("/delete/{partyId}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<ResponseObject> deleteParty(@PathVariable Long partyId) throws DataNotFoundException {
        partyService.deleteParty(partyId);
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(MessageKeys.DELETE_PARTY_SUCCESSFULLY)
                .build());
    }

    // update partner status
    @PutMapping("/update-status/{partyId}/{isDeleted}")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_DIRECTOR')")
    public ResponseEntity<String> updatePartyStatus(@PathVariable Long partyId, @PathVariable Boolean isDeleted) {
        try {
            partyService.updatePartnerStatus(partyId, isDeleted);
            String message = isDeleted ? "Xóa mềm đối tác" : "Khôi phục đối tác";
            return ResponseEntity.ok(message);
        } catch (DataNotFoundException e) {
            return ResponseEntity.badRequest().body("Không tìm thấy đối tác");
        } catch (OperationNotPermittedException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Trả về thông báo cụ thể từ ngoại lệ
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Đã xảy ra lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/check-exists")
    public ResponseEntity<ResponseObject> checkPartnerExists(
            @RequestParam(required = true) String taxCode,
            @RequestParam(required = true) PartnerType partnerType
    ) {
        boolean exists = false;

        if (taxCode != null && !taxCode.trim().isEmpty()) {
            exists = partyService.existsByTaxCodeAndPartnerType(taxCode, partnerType);
        } else {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.BAD_REQUEST)
                    .message("Bạn phải cung cấp taxCode")
                    .build());
        }

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Kiểm tra tồn tại đối tác thành công")
                .data(exists)
                .build());
    }
}

