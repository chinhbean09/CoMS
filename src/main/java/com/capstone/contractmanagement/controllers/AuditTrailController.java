package com.capstone.contractmanagement.controllers;

import com.capstone.contractmanagement.entities.AuditTrail;
import com.capstone.contractmanagement.repositories.IAuditTrailRepository;
import com.capstone.contractmanagement.responses.ResponseObject;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/audit-trails")
@RequiredArgsConstructor
public class AuditTrailController {

    private final IAuditTrailRepository auditTrailRepository;

    @GetMapping
    public ResponseEntity<ResponseObject> getAllAuditTrails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<AuditTrail> auditTrails = auditTrailRepository.findAll(pageable);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.GET_ALL_AUDIT_TRAILS_SUCCESSFULLY)
                    .data(auditTrails)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Lỗi khi lấy danh sách audit trails: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    // API lấy audit trails theo contractId với phân trang
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<ResponseObject> getAuditTrailsByContractId(
            @PathVariable Long contractId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<AuditTrail> auditTrails = auditTrailRepository.findByContractId(contractId, pageable);
            if (auditTrails.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.NOT_FOUND)
                                .message(MessageKeys.AUDIT_TRAILS_NOT_FOUND_FOR_CONTRACT + contractId)
                                .data(null)
                                .build());
            }
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.GET_AUDIT_TRAILS_BY_CONTRACT_SUCCESSFULLY)
                    .data(auditTrails)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Lỗi khi lấy audit trails theo contract: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    // API lấy audit trails trong khoảng thời gian với phân trang
    @GetMapping("/time-range")
    public ResponseEntity<ResponseObject> getAuditTrailsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<AuditTrail> auditTrails = auditTrailRepository.findByChangedAtBetween(start, end, pageable);
            if (auditTrails.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.NOT_FOUND)
                                .message(MessageKeys.AUDIT_TRAILS_NOT_FOUND_IN_TIME_RANGE)
                                .data(null)
                                .build());
            }
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(MessageKeys.GET_AUDIT_TRAILS_BY_TIME_RANGE_SUCCESSFULLY)
                    .data(auditTrails)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Lỗi khi lấy audit trails trong khoảng thời gian: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/original-contract/{originalContractId}")
    public ResponseEntity<ResponseObject> getAuditTrailsByOriginalContractId(
            @PathVariable Long originalContractId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            // Xác định cách sắp xếp (tăng dần hoặc giảm dần)
            Sort sort = order.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            // Gọi repository để lấy dữ liệu
            Page<AuditTrail> auditTrails = auditTrailRepository.findByOriginalContractId(originalContractId, pageable);

            // Kiểm tra nếu không có dữ liệu
            if (auditTrails.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.NOT_FOUND)
                                .message("Không tìm thấy audit trails cho originalContractId: " + originalContractId)
                                .data(null)
                                .build());
            }

            // Trả về kết quả thành công
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Lấy audit trails theo originalContractId thành công")
                    .data(auditTrails)
                    .build());
        } catch (Exception e) {
            // Xử lý lỗi
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("Lỗi khi lấy audit trails: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/original-contract/{originalContractId}/entity/{entityName}")
    public ResponseEntity<ResponseObject> getAuditTrailsByEntity(
            @PathVariable Long originalContractId,
            @PathVariable String entityName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {

        Sort sort = order.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditTrail> auditTrails = auditTrailRepository.findByOriginalContractIdAndEntityName(
                originalContractId, entityName, pageable);

        if (auditTrails.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseObject.builder()
                            .status(HttpStatus.NOT_FOUND)
                            .message("Không tìm thấy audit trails")
                            .data(null)
                            .build());
        }

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Lấy audit trails thành công")
                .data(auditTrails)
                .build());
    }

}
