package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.components.LocalizationUtils;
import com.capstone.contractmanagement.dtos.contract_partner.PartnerContractDTO;
import com.capstone.contractmanagement.entities.PartnerContract;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.contract.ContractItem;
import com.capstone.contractmanagement.enums.ContractStatus;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.InvalidParamException;
import com.capstone.contractmanagement.repositories.IContractItemRepository;
import com.capstone.contractmanagement.repositories.IPartnerContractRepository;
import com.capstone.contractmanagement.repositories.IPaymentScheduleRepository;
import com.capstone.contractmanagement.responses.contract_partner.PartnerContractItemResponse;
import com.capstone.contractmanagement.responses.contract_partner.PartnerContractResponse;
import com.capstone.contractmanagement.responses.payment_schedule.PaymentScheduleResponse;
import com.capstone.contractmanagement.utils.MessageKeys;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerContractService implements IPartnerContractService {
    private final IPartnerContractRepository contractPartnerRepository;
    private final IPaymentScheduleRepository paymentScheduleRepository;
    private final Cloudinary cloudinary;
    private final IContractItemRepository contractItemRepository;
    private final LocalizationUtils localizationUtils;
    private static final Logger logger = LoggerFactory.getLogger(PartnerContractService.class);

    @Override
    @Transactional
    public void createContractPartner(PartnerContractDTO contractDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        // Convert DTO to entity
        PartnerContract partnerContract = new PartnerContract();
        partnerContract.setContractNumber(contractDTO.getContractNumber());
        partnerContract.setAmount(contractDTO.getTotalValue());
        partnerContract.setPartnerName(contractDTO.getPartnerName());
        partnerContract.setTitle(contractDTO.getTitle());

        // Convert date list [year, month, day, hour, minute, second] to LocalDateTime
        partnerContract.setSigningDate(convertToLocalDateTime(contractDTO.getSigningDate()));
        partnerContract.setEffectiveDate(convertToLocalDateTime(contractDTO.getEffectiveDate()));
        partnerContract.setFileUrl(contractDTO.getFileUrl());
        partnerContract.setExpiryDate(convertToLocalDateTime(contractDTO.getExpiryDate()));
        partnerContract.setUser(currentUser);

        // Save contract
        partnerContract = contractPartnerRepository.save(partnerContract);

        if (contractDTO.getItems() != null) {
            PartnerContract finalPartnerContract = partnerContract;
            AtomicInteger itemOrder = new AtomicInteger();
            List<ContractItem> contractItems = contractDTO.getItems().stream()
                    .map(dto -> {
                        ContractItem contractItem = new ContractItem();
                        contractItem.setPartnerContract(finalPartnerContract);
                        contractItem.setDescription(dto.getDescription());
                        contractItem.setItemOrder(itemOrder.getAndIncrement());
                        contractItem.setAmount(dto.getAmount());
                        return contractItem;
                    }).collect(Collectors.toList());

            contractItemRepository.saveAll(contractItems);
        }
        // Handle payment schedules
        if (contractDTO.getPaymentSchedules() != null) {
            PartnerContract finalPartnerContract = partnerContract;
            List<PaymentSchedule> paymentSchedules = contractDTO.getPaymentSchedules().stream()
                    .map(dto -> {
                        PaymentSchedule paymentSchedule = new PaymentSchedule();
                        paymentSchedule.setAmount(dto.getAmountItem());
                        paymentSchedule.setPaymentMethod(dto.getPaymentMethod());
                        paymentSchedule.setPaymentDate(convertToLocalDateTime(dto.getPaymentDate()));
                        paymentSchedule.setPaymentOrder(dto.getPaymentOrder());
                        paymentSchedule.setPartnerContract(finalPartnerContract);
                        paymentSchedule.setPaymentPercentage(dto.getPaymentPercentage());
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
        String contentType = file.getContentType();

        // Kiểm tra định dạng file hợp lệ: PDF hoặc Word
        if (!isSupportedFileType(contentType)) {
            throw new IllegalArgumentException("Only PDF or Word files are allowed.");
        }

        // Upload file lên Cloudinary vào thư mục "contract_partner"
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "raw",      // Cho phép upload file dạng raw
                "folder", "contract_partner",
                "use_filename", true,        // Sử dụng tên file gốc làm public_id
                "unique_filename", false     // Không thêm ký tự ngẫu nhiên
        ));

        // Lấy public ID của file đã upload
        String publicId = (String) uploadResult.get("public_id");

        // Lấy tên file gốc và chuẩn hóa (loại bỏ dấu, ký tự không hợp lệ)
        String originalFilename = file.getOriginalFilename();
        String customFilename = normalizeFilename(originalFilename);

        // URL-encode tên file (một lần encoding là đủ khi tên đã là ASCII)
        String encodedFilename = URLEncoder.encode(customFilename, "UTF-8");

        // Tạo URL bảo mật với transformation flag attachment:<custom_filename>
        // Khi tải file về, trình duyệt sẽ đặt tên file theo customFilename
        String secureUrl = cloudinary.url()
                .resourceType("raw")
                .publicId(publicId)
                .secure(true)
                .transformation(new Transformation().flags("attachment:" + encodedFilename))
                .generate();

        return secureUrl;
    }

    // Hàm kiểm tra định dạng file hỗ trợ (PDF, Word 2003, Word 2007+)
    private boolean isSupportedFileType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("application/msword") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }

    // Hàm chuẩn hóa tên file tiếng Việt: bỏ dấu, loại bỏ ký tự không hợp lệ, chuyển khoảng trắng thành dấu gạch dưới
    private String normalizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }
        // Loại bỏ extension nếu có
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1) {
            filename = filename.substring(0, dotIndex);
        }
        // Chuẩn hóa Unicode: tách dấu
        String normalized = Normalizer.normalize(filename, Normalizer.Form.NFD);
        // Loại bỏ dấu (diacritics)
        normalized = normalized.replaceAll("\\p{M}", "");
        // Giữ lại chữ, số, dấu gạch dưới, dấu gạch ngang, khoảng trắng và dấu chấm than
        normalized = normalized.replaceAll("[^\\w\\-\\s!]", "");
        // Chuyển khoảng trắng thành dấu gạch dưới và trim
        normalized = normalized.trim().replaceAll("\\s+", "_");
        return normalized;
    }

    @Override
    @Transactional
    public Page<PartnerContractResponse> getAllContractPartners(String search, int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Thiết lập phân trang
        Pageable pageable = PageRequest.of(page, size);

        // Nếu tham số search là null thì chuyển về chuỗi rỗng
        String searchKeyword = search != null ? search : "";

        // Gọi repository để lấy dữ liệu theo tiêu chí tìm kiếm và phân trang
        Page<PartnerContract> contractPartnersPage = contractPartnerRepository
                .searchByUserAndKeyword(currentUser, searchKeyword, pageable);

        // Chuyển đổi entity sang DTO response
        return contractPartnersPage.map(contractPartner -> {
            // Lấy danh sách ContractItem của contractPartner hiện tại
            List<PartnerContractItemResponse> items = contractItemRepository.findByPartnerContract(contractPartner)
                    .stream()
                    .map(item -> PartnerContractItemResponse.builder()
                            .id(item.getId())
                            .amount(item.getAmount())
                            .description(item.getDescription())
                            .build())
                    .collect(Collectors.toList());

            return PartnerContractResponse.builder()
                    .partnerContractId(contractPartner.getId())
                    .contractNumber(contractPartner.getContractNumber())
                    .totalValue(contractPartner.getAmount())
                    .partnerName(contractPartner.getPartnerName())
                    .title(contractPartner.getTitle())
                    .fileUrl(contractPartner.getFileUrl())
                    .signingDate(contractPartner.getSigningDate())
                    .effectiveDate(contractPartner.getEffectiveDate())
                    .expiryDate(contractPartner.getExpiryDate())
                    .items(items)
                    .paymentSchedules(contractPartner.getPaymentSchedules().stream()
                            .map(paymentSchedule -> PaymentScheduleResponse.builder()
                                    .id(paymentSchedule.getId())
                                    .amount(paymentSchedule.getAmount())
                                    .paymentMethod(paymentSchedule.getPaymentMethod())
                                    .paymentDate(paymentSchedule.getPaymentDate())
                                    .paymentOrder(paymentSchedule.getPaymentOrder())
                                    .paymentPercentage(paymentSchedule.getPaymentPercentage())
                                    .billUrls(paymentSchedule.getBillUrls())
                                    .status(paymentSchedule.getStatus())
                                    .overdueEmailSent(paymentSchedule.isOverdueEmailSent())
                                    .reminderEmailSent(paymentSchedule.isReminderEmailSent())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        });
    }

    @Override
    @Transactional
    public void deleteContractPartner(Long contractPartnerId) throws DataNotFoundException {
        PartnerContract partnerContract = contractPartnerRepository.findById(contractPartnerId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.CONTRACT_PARTNER_NOT_FOUND));
        contractPartnerRepository.delete(partnerContract);
    }

    @Override
    public void updateContractPartner(Long contractPartnerId, PartnerContractDTO contractDTO) throws DataNotFoundException {
        PartnerContract partnerContract = contractPartnerRepository.findById(contractPartnerId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.CONTRACT_PARTNER_NOT_FOUND));
        partnerContract.setContractNumber(contractDTO.getContractNumber());
        partnerContract.setAmount(contractDTO.getTotalValue());
        partnerContract.setPartnerName(contractDTO.getPartnerName());
        partnerContract.setTitle(contractDTO.getTitle());
        partnerContract.setSigningDate(convertToLocalDateTime(contractDTO.getSigningDate()));
        partnerContract.setEffectiveDate(convertToLocalDateTime(contractDTO.getEffectiveDate()));
        partnerContract.setExpiryDate(convertToLocalDateTime(contractDTO.getExpiryDate()));
        partnerContract.setFileUrl(contractDTO.getFileUrl());
        contractPartnerRepository.save(partnerContract);
    }

    @Override
    @Transactional
    public void uploadPaymentBillUrls(Long paymentScheduleId, List<MultipartFile> files) throws DataNotFoundException {
        PaymentSchedule paymentSchedule = paymentScheduleRepository.findById(paymentScheduleId)
                .orElseThrow(() -> new DataNotFoundException("Payment schedule not found"));

//        // Nếu thuộc Contract, kiểm tra điều kiện
//        if (paymentSchedule.getContract() != null) {
//            Contract contract = paymentSchedule.getContract();
//
//            // Kiểm tra status SIGNED + ACTIVE (dựa vào ngày)
//            boolean isActive = contract.getEffectiveDate() != null &&
//                    contract.getExpiryDate() != null &&
//                    !contract.getEffectiveDate().isAfter(LocalDateTime.now()) &&
//                    !contract.getExpiryDate().isBefore(LocalDateTime.now());
//
//            if (!ContractStatus.SIGNED.equals(contract.getStatus()) || !isActive) {
//                throw new InvalidParamException("Chỉ cho upload bằng chứng thanh toán khi hợp đồng đã ký hoặc đang hoạt động");
//            }
//        }

        try {
            // Xóa tất cả các hình ảnh cũ (nếu cần) nếu bạn muốn thay thế hoàn toàn
            paymentSchedule.getBillUrls().clear();  // Nếu bạn muốn thay thế các hình ảnh cũ

            List<String> uploadedUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                // Kiểm tra nếu file là hình ảnh hợp lệ
                MediaType mediaType = MediaType.parseMediaType(Objects.requireNonNull(file.getContentType()));
                if (!mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) &&
                        !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
                    throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_IMAGES_FILE_MUST_BE_IMAGE));
                }

                // Upload file lên Cloudinary
                Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                        ObjectUtils.asMap("folder", "payment_bill/" + paymentScheduleId, "public_id", file.getOriginalFilename()));

                // Lấy URL bảo mật của file đã upload
                String billUrl = uploadResult.get("secure_url").toString();
                uploadedUrls.add(billUrl);
            }

            // Thêm các URL đã upload vào danh sách billUrls
            paymentSchedule.getBillUrls().addAll(uploadedUrls);

            // Cập nhật trạng thái thanh toán nếu cần
            paymentSchedule.setStatus(PaymentStatus.PAID); // Bạn có thể thay đổi trạng thái tùy theo logic của mình

            // Lưu PaymentSchedule đã cập nhật
            paymentScheduleRepository.save(paymentSchedule);
        } catch (IOException e) {
            logger.error("Failed to upload bill urls for payment schedule with ID {}", paymentScheduleId, e);
        }
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
