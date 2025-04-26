package com.capstone.contractmanagement.services.contract_partner;

import com.capstone.contractmanagement.components.LocalizationUtils;
import com.capstone.contractmanagement.dtos.contract_partner.PartnerContractDTO;
import com.capstone.contractmanagement.entities.Partner;
import com.capstone.contractmanagement.entities.PartnerContract;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.contract.ContractItem;
import com.capstone.contractmanagement.enums.PaymentStatus;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.exceptions.InvalidParamException;
import com.capstone.contractmanagement.repositories.IContractItemRepository;
import com.capstone.contractmanagement.repositories.IPartnerContractRepository;
import com.capstone.contractmanagement.repositories.IPartnerRepository;
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
import java.net.URI;
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
    private final IPartnerRepository partnerRepository;
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
        partnerContract.setFileUrl(String.valueOf(contractDTO.getFileUrl()));
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
    public List<String> uploadPdfToCloudinary(List<MultipartFile> files) throws IOException {
        List<String> resultUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String contentType = file.getContentType();

            // Kiểm tra định dạng file hợp lệ: PDF hoặc Word
            if (!isSupportedFileType(contentType)) {
                throw new IllegalArgumentException("Chỉ cho phép các tập tin PDF hoặc Word.");
            }

            // Upload file lên Cloudinary vào thư mục "contract_partner"
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "raw",      // Cho phép upload file dạng raw
                    "folder", "contract_partner",
                    "use_filename", true,        // Sử dụng tên file gốc làm public_id
                    "unique_filename", true,     // Không thêm ký tự ngẫu nhiên
                    "format", getFileExtension(contentType)  // Đảm bảo file được upload với đúng định dạng
            ));

            // Lấy public ID của file đã upload
            String publicId = (String) uploadResult.get("public_id");

            // Lấy tên file gốc và chuẩn hóa (loại bỏ dấu, ký tự không hợp lệ)
            String originalFilename = file.getOriginalFilename();

            // Normalize the filename (remove diacritics and replace spaces with underscores)
            String customFilename = normalizeFilename(originalFilename);

            // Ensure there’s only one extension (e.g., "file_okl9cf.pdf" instead of "file_okl9cf.pdf.pdf")
            String fileExtension = getFileExtension(contentType);
//        if (!customFilename.endsWith("." + fileExtension)) {
//            customFilename += "." + fileExtension;
//        }

            // URL-encode tên file (một lần encoding là đủ khi tên đã là ASCII)
            String encodedFilename = URLEncoder.encode(customFilename, "UTF-8");

            // Tạo URL bảo mật với transformation flag attachment:<encoded_filename>
            // Khi tải file về, trình duyệt sẽ đặt tên file theo custom filename
            String secureUrl = cloudinary.url()
                    .resourceType("raw")
                    .publicId(publicId)
                    .secure(true)
                    .transformation(new Transformation().flags("attachment:" + customFilename))
                    .generate();

            resultUrls.add(secureUrl);
        }
        return resultUrls;
    }

    // Hàm kiểm tra định dạng file hỗ trợ (PDF, Word 2003, Word 2007+)
    private boolean isSupportedFileType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("application/msword") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }

    // Hàm lấy phần mở rộng (extension) của file dựa trên MIME type
    private String getFileExtension(String contentType) {
        if (contentType == null) {
            return "pdf";  // Default extension if MIME type is not recognized
        }

        switch (contentType) {
            case "application/pdf":
                return "pdf";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            default:
                return "pdf";  // Default to PDF if MIME type doesn't match
        }
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
        partnerContract.setFileUrl(String.valueOf(contractDTO.getFileUrl()));
        contractPartnerRepository.save(partnerContract);
    }

    @Override
    @Transactional
    public void uploadPaymentBillUrls(Long paymentScheduleId, List<MultipartFile> files) throws DataNotFoundException {
        PaymentSchedule paymentSchedule = paymentScheduleRepository.findById(paymentScheduleId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy lịch thanh toán"));

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
            // 🔥 Xoá các file cũ trên Cloudinary nếu có
            for (String oldUrl : paymentSchedule.getBillUrls()) {
                String publicId = extractPublicIdFromUrl(oldUrl);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                }
            }

            // Xoá danh sách URL cũ trong DB
            paymentSchedule.getBillUrls().clear();

            List<String> uploadedUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                // Kiểm tra định dạng hình ảnh
                MediaType mediaType = MediaType.parseMediaType(Objects.requireNonNull(file.getContentType()));
                if (!mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) &&
                        !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
                    throw new InvalidParamException(MessageKeys.UPLOAD_IMAGES_FILE_MUST_BE_IMAGE);
                }

                // Upload lên Cloudinary
                Map uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "payment_bill/" + paymentScheduleId,
                                "use_filename", true,
                                "unique_filename", true,
                                "resource_type", "image"
                        )
                );

                // Lấy URL ảnh đã upload
                String billUrl = uploadResult.get("secure_url").toString();
                uploadedUrls.add(billUrl);
            }

            // Lưu danh sách URL mới
            paymentSchedule.getBillUrls().addAll(uploadedUrls);
            paymentSchedule.setStatus(PaymentStatus.PAID);
            paymentScheduleRepository.save(paymentSchedule);

        } catch (IOException e) {
            logger.error("Không tải được url hóa đơn cho lịch thanh toán. Lỗi:", e);
        }
    }

    @Override
    @Transactional
    public void setPartnerContractToPartner(Long contractPartnerId, Long partnerId) throws DataNotFoundException {
        PartnerContract partnerContract = contractPartnerRepository.findById(contractPartnerId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.CONTRACT_PARTNER_NOT_FOUND));
        Partner partner = partnerRepository.findById(partnerId).orElseThrow(() -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));
        partnerContract.setPartner(partner);
        contractPartnerRepository.save(partnerContract);
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // Ví dụ URL:
            // https://res.cloudinary.com/your_cloud_name/image/upload/v1234567890/payment_bill/12/filename_xyz.png
            // Cần tách phần sau: payment_bill/12/filename_xyz

            URI uri = new URI(url);
            String path = uri.getPath(); // /your_cloud_name/image/upload/v1234567890/payment_bill/12/file.png
            int versionIndex = path.indexOf("/v"); // tìm vị trí bắt đầu version

            if (versionIndex != -1) {
                String publicPath = path.substring(versionIndex + 2); // bỏ "/v" và version
                int slashIndex = publicPath.indexOf('/');
                if (slashIndex != -1) {
                    return publicPath.substring(slashIndex + 1, publicPath.lastIndexOf('.')); // bỏ phần mở rộng .jpg/.png
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract publicId from URL: {}", url);
        }
        return null;
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
