    package com.capstone.contractmanagement.services.party;

    import com.capstone.contractmanagement.dtos.bank.CreateBankDTO;
    import com.capstone.contractmanagement.dtos.party.CreatePartnerDTO;
    import com.capstone.contractmanagement.dtos.party.UpdatePartnerDTO;
    import com.capstone.contractmanagement.entities.Bank;
    import com.capstone.contractmanagement.entities.Partner;
    import com.capstone.contractmanagement.entities.Role;
    import com.capstone.contractmanagement.entities.User;
    import com.capstone.contractmanagement.enums.ContractStatus;
    import com.capstone.contractmanagement.enums.PartnerType;
    import com.capstone.contractmanagement.exceptions.DataNotFoundException;
    import com.capstone.contractmanagement.exceptions.OperationNotPermittedException;
    import com.capstone.contractmanagement.repositories.IBankRepository;
    import com.capstone.contractmanagement.repositories.IContractRepository;
    import com.capstone.contractmanagement.repositories.IPartnerRepository;
    import com.capstone.contractmanagement.responses.bank.BankResponse;
    import com.capstone.contractmanagement.responses.party.CreatePartnerResponse;
    import com.capstone.contractmanagement.responses.party.CreatedByResponse;
    import com.capstone.contractmanagement.responses.party.ListPartnerResponse;
    import com.capstone.contractmanagement.utils.MessageKeys;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.ArrayList;
    import java.util.List;
    import java.util.Optional;
    import java.util.concurrent.ThreadLocalRandom;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class PartnerService implements IPartnerService {
        private final IPartnerRepository partyRepository;
        private final IBankRepository bankRepository;
        private final IContractRepository contractRepository;

        @Transactional
        @Override
        public CreatePartnerResponse createPartner(CreatePartnerDTO createPartnerDTO) {
            // Kiểm tra trùng mã số thuế
//            if (createPartnerDTO.getTaxCode() != null &&
//                    partyRepository.existsByTaxCodeAndPartnerType(
//                            createPartnerDTO.getTaxCode(),
//                            createPartnerDTO.getPartnerType())
//            ) {
//                throw new RuntimeException("Mã số thuế đã tồn tại cho cùng loại đối tác: "
//                        + createPartnerDTO.getTaxCode() + " - " + createPartnerDTO.getPartnerType());
//            }
//
//            // Kiểm tra trùng số tài khoản ngân hàng
//            if (createPartnerDTO.getBanking() != null && !createPartnerDTO.getBanking().isEmpty()) {
//                for (CreateBankDTO bankDTO : createPartnerDTO.getBanking()) {
//                    if (bankRepository.existsByBackAccountNumber(bankDTO.getBackAccountNumber())) {
//                        throw new RuntimeException("Số tài khoản ngân hàng đã tồn tại: " + bankDTO.getBackAccountNumber());
//                    }
//                }
//            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();
            // Tự động tạo partnerCode theo định dạng: P + 5 số (ví dụ: P12345)
            String partnerCode = "P" + String.format("%05d", ThreadLocalRandom.current().nextInt(100000));

            // Tạo Partner mới với thông tin từ DTO và partnerCode tự tạo
            Partner partner = Partner.builder()
                    .partnerCode(partnerCode)
                    .partnerType(createPartnerDTO.getPartnerType())
                    .partnerName(createPartnerDTO.getPartnerName())
                    .spokesmanName(createPartnerDTO.getSpokesmanName())
                    .address(createPartnerDTO.getAddress())
                    .taxCode(createPartnerDTO.getTaxCode())
                    .phone(createPartnerDTO.getPhone())
                    .email(createPartnerDTO.getEmail())
                    .note(createPartnerDTO.getNote())
                    .position(createPartnerDTO.getPosition())
                    .user(currentUser)
                    .isDeleted(false)
                    .abbreviation(createPartnerDTO.getAbbreviation())
                    .build();

            // Lưu Partner để lấy được ID
            partner = partyRepository.save(partner);

            // Khởi tạo danh sách Bank từ danh sách DTO
            List<Bank> banks = new ArrayList<>();
            if (createPartnerDTO.getBanking() != null && !createPartnerDTO.getBanking().isEmpty()) {
                for (CreateBankDTO bankDTO : createPartnerDTO.getBanking()) {
                    Bank bank = Bank.builder()
                            .bankName(bankDTO.getBankName())
                            .backAccountNumber(bankDTO.getBackAccountNumber())
                            .partner(partner)
                            .build();
                    banks.add(bank);
                }
            }

            // Lưu tất cả các Bank và cập nhật lại danh sách ngân hàng cho Partner
            if (!banks.isEmpty()) {
                bankRepository.saveAll(banks);
                partner.setBanking(banks);
                // Nếu muốn cập nhật lại Partner với danh sách ngân hàng mới (cascade có thể tự động xử lý)
                partner = partyRepository.save(partner);
            }

            // Chuyển đổi danh sách Bank thành danh sách BankResponse
            List<BankResponse> bankResponses = banks.stream()
                    .map(bank -> BankResponse.builder()
                            .bankName(bank.getBankName())
                            .backAccountNumber(bank.getBackAccountNumber())
                            .build())
                    .collect(Collectors.toList());

            return CreatePartnerResponse.builder()
                    .partyId(partner.getId())
                    .partnerCode(partner.getPartnerCode())
                    .partnerType(partner.getPartnerType())
                    .partnerName(partner.getPartnerName())
                    .spokesmanName(partner.getSpokesmanName())
                    .address(partner.getAddress())
                    .taxCode(partner.getTaxCode())
                    .phone(partner.getPhone())
                    .email(partner.getEmail())
                    .note(partner.getNote())
                    .createdBy(CreatedByResponse.builder()
                            .userId(partner.getUser().getId())
                            .username(partner.getUser().getUsername())
                            .build())
                    .position(partner.getPosition())
                    .isDeleted(partner.getIsDeleted())
                    .abbreviation(partner.getAbbreviation())
                    .banking(bankResponses)
                    .build();
        }

        @Override
        @Transactional
        public CreatePartnerResponse updatePartner(Long id, UpdatePartnerDTO updatePartnerDTO)
                throws DataNotFoundException, OperationNotPermittedException {            // Lấy partner hiện tại, nếu không có thì ném exception
            Partner existingPartner = partyRepository.findById(id)
                    .orElseThrow(() -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));

//            if (isPartnerInActiveContract(id)) {
//                throw new OperationNotPermittedException(
//                        "Không thể xóa đối tác vì đang trong hợp đồng đang hoạt động. Vui lòng tạo phụ lục thay thế."
//                );
//            }

            // Cập nhật thông tin Partner
            existingPartner.setPartnerType(updatePartnerDTO.getPartnerType());
            existingPartner.setPartnerName(updatePartnerDTO.getPartnerName());
            existingPartner.setSpokesmanName(updatePartnerDTO.getSpokesmanName());
            existingPartner.setAddress(updatePartnerDTO.getAddress());

            //không cho update taxCode
            //partner.setTaxCode(updatePartnerDTO.getTaxCode());
            existingPartner.setPhone(updatePartnerDTO.getPhone());
            existingPartner.setEmail(updatePartnerDTO.getEmail());
            existingPartner.setPosition(updatePartnerDTO.getPosition());
            existingPartner.setAbbreviation(updatePartnerDTO.getAbbreviation());
            partyRepository.save(existingPartner);

            // Lấy danh sách ngân hàng hiện có của Partner
            List<Bank> existingBanks = bankRepository.findByPartner(existingPartner);

            // Xóa hết các ngân hàng cũ
            if (!existingBanks.isEmpty()) {
                bankRepository.deleteAll(existingBanks);
            }

            // Tạo mới các đối tượng Bank từ danh sách được gửi lên (UpdateBankDTO)
            List<Bank> updatedBanks = updatePartnerDTO.getBanking().stream().map(updateBankDTO -> {
                Bank bank = Bank.builder()
                        .bankName(updateBankDTO.getBankName())
                        .backAccountNumber(updateBankDTO.getBackAccountNumber())
                        .partner(existingPartner)
                        .build();
                return bankRepository.save(bank);
            }).collect(Collectors.toList());

            // Chuyển đổi danh sách Bank sang danh sách BankResponse
            List<BankResponse> bankResponses = updatedBanks.stream()
                    .map(bank -> BankResponse.builder()
                            .bankName(bank.getBankName())
                            .backAccountNumber(bank.getBackAccountNumber())
                            .build())
                    .collect(Collectors.toList());

            return CreatePartnerResponse.builder()
                    .partyId(existingPartner.getId())
                    .partnerCode(existingPartner.getPartnerCode())
                    .partnerType(existingPartner.getPartnerType())
                    .partnerName(existingPartner.getPartnerName())
                    .spokesmanName(existingPartner.getSpokesmanName())
                    .address(existingPartner.getAddress())
                    .taxCode(existingPartner.getTaxCode())
                    .phone(existingPartner.getPhone())
                    .email(existingPartner.getEmail())
                    .banking(bankResponses)
                    .note(existingPartner.getNote())
                    .createdBy(CreatedByResponse.builder()
                            .userId(existingPartner.getUser().getId())
                            .username(existingPartner.getUser().getUsername())
                            .build())
                    .position(existingPartner.getPosition())
                    .isDeleted(existingPartner.getIsDeleted())
                    .abbreviation(existingPartner.getAbbreviation())
                    .build();
        }
        @Override
        public void deleteParty(Long id) throws DataNotFoundException {
            Partner partner = partyRepository.findById(id).orElseThrow(
                    () -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));
            partyRepository.delete(partner);
        }

        @Override
        @Transactional
        public Page<ListPartnerResponse> getAllPartners(String search, int page, int size, PartnerType partnerType) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();
            Pageable pageable = PageRequest.of(page, size);
            Page<Partner> partyPage;

            boolean hasSearch = search != null && !search.trim().isEmpty();
            boolean hasPartnerType = partnerType != null;
            boolean isDirector = currentUser.getRole() != null && Role.DIRECTOR.equalsIgnoreCase(currentUser.getRole().getRoleName());

            if (isDirector) {
                // Nếu là Director thì không lọc theo user
                if (hasSearch && hasPartnerType) {
                    partyPage = partyRepository.searchByFieldsAndPartnerType(search.trim(), partnerType, pageable);
                } else if (hasSearch) {
                    partyPage = partyRepository.searchByFields(search.trim(), pageable);
                } else if (hasPartnerType) {
                    partyPage = partyRepository.findByIsDeletedFalseAndPartnerTypeAndIdNot(partnerType, 1L, pageable);
                } else {
                    partyPage = partyRepository.findByIsDeletedFalseAndIdNot(pageable, 1L);
                }
            } else {
                // Logic cũ cho các role khác
                if (hasSearch && hasPartnerType) {
                    partyPage = partyRepository.searchByFieldsAndPartnerTypeAndUser(search.trim(), partnerType, currentUser, pageable);
                } else if (hasSearch) {
                    partyPage = partyRepository.searchByFieldsAndUser(search.trim(), currentUser, pageable);
                } else if (hasPartnerType) {
                    partyPage = partyRepository.findByIsDeletedFalseAndPartnerTypeAndUserAndIdNot(partnerType, currentUser, 1L, pageable);
                } else {
                    partyPage = partyRepository.findByIsDeletedFalseAndUserAndIdNot(currentUser, 1L, pageable);
                }
            }

            return partyPage.map(party ->
                    ListPartnerResponse.builder()
                            .partyId(party.getId())
                            .partnerCode(party.getPartnerCode())
                            .partnerType(party.getPartnerType())
                            .partnerName(party.getPartnerName())
                            .spokesmanName(party.getSpokesmanName())
                            .address(party.getAddress())
                            .taxCode(party.getTaxCode())
                            .phone(party.getPhone())
                            .email(party.getEmail())
                            .note(party.getNote())
                            .position(party.getPosition())
                            .isDeleted(party.getIsDeleted())
                            .createdBy(Optional.ofNullable(party.getUser())
                                    .map(user -> CreatedByResponse.builder()
                                            .userId(user.getId())
                                            .username(user.getUsername())
                                            .build())
                                    .orElse(null))
                            .banking(party.getBanking().stream()
                                    .map(bank -> BankResponse.builder()
                                            .bankName(bank.getBankName())
                                            .backAccountNumber(bank.getBackAccountNumber())
                                            .build())
                                    .collect(Collectors.toList()))
                            .abbreviation(party.getAbbreviation())
                            .build());
        }

        @Override
        @Transactional
        public ListPartnerResponse getPartnerById(Long id) throws DataNotFoundException {
            // get partner by id
            Partner partner = partyRepository.findById(id).orElseThrow(() -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));
            // convert to response
            return ListPartnerResponse.builder()
                    .partyId(partner.getId())
                    .partnerCode(partner.getPartnerCode())
                    .partnerType(partner.getPartnerType())
                    .partnerName(partner.getPartnerName())
                    .spokesmanName(partner.getSpokesmanName())
                    .address(partner.getAddress())
                    .taxCode(partner.getTaxCode())
                    .phone(partner.getPhone())
                    .email(partner.getEmail())
                    .note(partner.getNote())
                    .position(partner.getPosition())
                    .createdBy(CreatedByResponse.builder()
                            .userId(partner.getUser().getId())
                            .username(partner.getUser().getUsername())
                            .build())
                    .isDeleted(partner.getIsDeleted())
                    .banking(partner.getBanking().stream() // Nếu Partner có danh sách Banks
                            .map(bank -> BankResponse.builder()
                                    .bankName(bank.getBankName())
                                    .backAccountNumber(bank.getBackAccountNumber())
                                    .build()) // Thiếu .build()
                            .collect(Collectors.toList())) // Đổi từ .toList() thành Collectors.toList()
                    .abbreviation(partner.getAbbreviation())
                    .build();
        }


        private boolean isPartnerInActiveContract(Long partnerId) {
            return contractRepository.existsByPartnerIdAndStatus(partnerId, ContractStatus.ACTIVE);
        }

        @Transactional
        @Override
        public void updatePartnerStatus(Long partyId, Boolean isDeleted) throws DataNotFoundException, OperationNotPermittedException {
            Partner existingPartner = partyRepository.findById(partyId)
                    .orElseThrow(() -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));

            // Nếu đặt isDeleted = true và partner đang trong hợp đồng Active, ngăn chặn thao tác
            if (isDeleted && isPartnerInActiveContract(partyId)) {
                throw new OperationNotPermittedException(
                        "Không thể xóa đối tác vì đang trong hợp đồng đang hoạt động. Vui lòng tạo phụ lục thay thế."
                );
            }

            existingPartner.setIsDeleted(isDeleted);
            partyRepository.save(existingPartner);
        }

        @Override
        public boolean existsByTaxCodeAndPartnerType(String taxCode, PartnerType partnerType) {
            return partyRepository.existsByTaxCodeAndPartnerType(taxCode, PartnerType.PARTNER_A);
        }
    }
