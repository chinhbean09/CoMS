package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.dtos.contract_partner.ContractPartnerDTO;
import com.capstone.contractmanagement.entities.ContractPartner;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IContractPartnerRepository;
import com.capstone.contractmanagement.repositories.IPaymentScheduleRepository;
import com.capstone.contractmanagement.responses.contract_partner.ContractPartnerResponse;
import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import com.capstone.contractmanagement.utils.MessageKeys;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractPartnerService implements IContractPartnerService {
    private final IContractPartnerRepository contractPartnerRepository;
    private final IPaymentScheduleRepository paymentScheduleRepository;
    private final Cloudinary cloudinary;

    @Override
    @Transactional
    public void createContractPartner(ContractPartnerDTO contractDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        // Convert DTO to entity
        ContractPartner contractPartner = new ContractPartner();
        contractPartner.setContractNumber(contractDTO.getContractNumber());
        contractPartner.setAmount(contractDTO.getAmount());
        contractPartner.setPartnerName(contractDTO.getPartnerName());
        contractPartner.setTitle(contractDTO.getTitle());

        // Convert date list [year, month, day, hour, minute, second] to LocalDateTime
        contractPartner.setSigningDate(convertToLocalDateTime(contractDTO.getSigningDate()));
        contractPartner.setEffectiveDate(convertToLocalDateTime(contractDTO.getEffectiveDate()));
        contractPartner.setFileUrl(contractDTO.getFileUrl());
        contractPartner.setExpiryDate(convertToLocalDateTime(contractDTO.getExpiryDate()));
        contractPartner.setUser(currentUser);

        // Save contract
        contractPartner = contractPartnerRepository.save(contractPartner);

        // Handle payment schedules
        if (contractDTO.getPaymentSchedules() != null) {
            ContractPartner finalContractPartner = contractPartner;
            List<PaymentSchedule> paymentSchedules = contractDTO.getPaymentSchedules().stream()
                    .map(dto -> {
                        PaymentSchedule paymentSchedule = new PaymentSchedule();
                        paymentSchedule.setAmount(dto.getAmount());
                        paymentSchedule.setPaymentMethod(dto.getPaymentMethod());
                        paymentSchedule.setPaymentDate(convertToLocalDateTime(dto.getPaymentDate()));
                        paymentSchedule.setContractPartner(finalContractPartner);
                        paymentSchedule.setStatus(PaymentStatus.UNPAID);
                        paymentSchedule.setOverdueEmailSent(false);
                        paymentSchedule.setReminderEmailSent(false);
                        return paymentSchedule;
                    })
                    .collect(Collectors.toList());

           paymentScheduleRepository.saveAll(paymentSchedules);
        }
    }

    @Override
    public String uploadPdfToCloudinary(MultipartFile file) throws IOException {
        // Kiểm tra xem file có phải là PDF hay không
        if (!file.getContentType().equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        // Upload file lên Cloudinary vào thư mục contract_partner
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "raw",  // Cho phép Cloudinary tự động nhận dạng loại file
                "folder", "contract_partner"  // Đặt thư mục lưu trữ là contract_partner
        ));

        // Lấy public ID và URL trả về từ Cloudinary
        String publicId = (String) uploadResult.get("public_id");

        // Tạo URL bảo mật cho file PDF
        String secureUrl = cloudinary.url()
                .resourceType("raw") // Xác định là file raw (không phải hình ảnh)
                .publicId(publicId)
                .secure(true) // Tạo URL bảo mật
                .generate(); // Tạo URL

        // Trả về URL bảo mật của file đã upload
        return secureUrl;
    }

    @Override
    @Transactional
    public Page<ContractPartnerResponse> getAllContractPartners(String search, int page, int size) {
        // Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Thiết lập phân trang
        Pageable pageable = PageRequest.of(page, size);

        // Nếu tham số search là null thì chuyển về chuỗi rỗng
        String searchKeyword = search != null ? search : "";

        // Gọi repository để lấy dữ liệu theo tiêu chí tìm kiếm và phân trang
        Page<ContractPartner> contractPartnersPage = contractPartnerRepository
                .searchByUserAndKeyword(currentUser, searchKeyword, pageable);

        // Chuyển đổi entity sang DTO response
        return contractPartnersPage.map(contractPartner -> ContractPartnerResponse.builder()
                .contractPartnerId(contractPartner.getId())
                .contractNumber(contractPartner.getContractNumber())
                .amount(contractPartner.getAmount())
                .partnerName(contractPartner.getPartnerName())
                .title(contractPartner.getTitle())
                .fileUrl(contractPartner.getFileUrl())
                .signingDate(contractPartner.getSigningDate())
                .effectiveDate(contractPartner.getEffectiveDate())
                .expiryDate(contractPartner.getExpiryDate())
                .paymentSchedules(contractPartner.getPaymentSchedules().stream()
                        .map(paymentSchedule -> PaymentScheduleResponse.builder()
                                .id(paymentSchedule.getId())
                                .amount(paymentSchedule.getAmount())
                                .paymentMethod(paymentSchedule.getPaymentMethod())
                                .paymentDate(paymentSchedule.getPaymentDate())
                                .status(paymentSchedule.getStatus())
                                .overdueEmailSent(paymentSchedule.isOverdueEmailSent())
                                .reminderEmailSent(paymentSchedule.isReminderEmailSent())
                                .build())
                        .collect(Collectors.toList()))
                .build());
    }

    @Override
    @Transactional
    public void deleteContractPartner(Long contractPartnerId) throws DataNotFoundException {
        ContractPartner contractPartner = contractPartnerRepository.findById(contractPartnerId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.CONTRACT_PARTNER_NOT_FOUND));
        contractPartnerRepository.delete(contractPartner);
    }

    @Override
    public void updateContractPartner(Long contractPartnerId, ContractPartnerDTO contractDTO) throws DataNotFoundException {
        ContractPartner contractPartner = contractPartnerRepository.findById(contractPartnerId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.CONTRACT_PARTNER_NOT_FOUND));
        contractPartner.setContractNumber(contractDTO.getContractNumber());
        contractPartner.setAmount(contractDTO.getAmount());
        contractPartner.setPartnerName(contractDTO.getPartnerName());
        contractPartner.setTitle(contractDTO.getTitle());
        contractPartner.setSigningDate(convertToLocalDateTime(contractDTO.getSigningDate()));
        contractPartner.setEffectiveDate(convertToLocalDateTime(contractDTO.getEffectiveDate()));
        contractPartner.setExpiryDate(convertToLocalDateTime(contractDTO.getExpiryDate()));
        contractPartner.setFileUrl(contractDTO.getFileUrl());
        contractPartnerRepository.save(contractPartner);
    }

    private LocalDateTime convertToLocalDateTime(List<Integer> dateTimeList) {
        return LocalDateTime.of(
                dateTimeList.get(0), // Year
                dateTimeList.get(1), // Month
                dateTimeList.get(2), // Day
                dateTimeList.get(3), // Hour
                dateTimeList.get(4), // Minute
                dateTimeList.get(5)  // Second
        );
    }
}
