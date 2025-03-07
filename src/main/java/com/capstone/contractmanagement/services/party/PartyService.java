package com.capstone.contractmanagement.services.party;

import com.capstone.contractmanagement.dtos.bank.CreateBankDTO;
import com.capstone.contractmanagement.dtos.party.CreatePartyDTO;
import com.capstone.contractmanagement.dtos.party.UpdatePartyDTO;
import com.capstone.contractmanagement.entities.Bank;
import com.capstone.contractmanagement.entities.Party;
import com.capstone.contractmanagement.enums.PartyType;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IBankRepository;
import com.capstone.contractmanagement.repositories.IPartyRepository;
import com.capstone.contractmanagement.responses.bank.BankResponse;
import com.capstone.contractmanagement.responses.party.CreatePartyResponse;
import com.capstone.contractmanagement.responses.party.ListPartyResponse;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartyService implements IPartyService{
    private final IPartyRepository partyRepository;
    private final IBankRepository bankRepository;

    @Transactional
    @Override
    public CreatePartyResponse createParty(CreatePartyDTO createPartyDTO) {
        // Tự động tạo partnerCode theo định dạng: P + 5 số (ví dụ: P12345)
        String partnerCode = "P" + String.format("%05d", ThreadLocalRandom.current().nextInt(100000));

        // Tạo Party mới với thông tin từ DTO và partnerCode tự tạo
        Party party = Party.builder()
                .partnerCode(partnerCode)
                .partnerType(createPartyDTO.getPartnerType())
                .partnerName(createPartyDTO.getPartnerName())
                .spokesmanName(createPartyDTO.getSpokesmanName())
                .address(createPartyDTO.getAddress())
                .taxCode(createPartyDTO.getTaxCode())
                .phone(createPartyDTO.getPhone())
                .email(createPartyDTO.getEmail())
                .note(createPartyDTO.getNote())
                .position(createPartyDTO.getPosition())
                .isDeleted(false)
                .build();

        // Lưu Party để lấy được ID
        party = partyRepository.save(party);

        // Khởi tạo danh sách Bank từ danh sách DTO
        List<Bank> banks = new ArrayList<>();
        if (createPartyDTO.getBanking() != null && !createPartyDTO.getBanking().isEmpty()) {
            for (CreateBankDTO bankDTO : createPartyDTO.getBanking()) {
                Bank bank = Bank.builder()
                        .bankName(bankDTO.getBankName())
                        .backAccountNumber(bankDTO.getBackAccountNumber())
                        .party(party)
                        .build();
                banks.add(bank);
            }
        }

        // Lưu tất cả các Bank và cập nhật lại danh sách ngân hàng cho Party
        if (!banks.isEmpty()) {
            bankRepository.saveAll(banks);
            party.setBanking(banks);
            // Nếu muốn cập nhật lại Party với danh sách ngân hàng mới (cascade có thể tự động xử lý)
            party = partyRepository.save(party);
        }

        // Chuyển đổi danh sách Bank thành danh sách BankResponse
        List<BankResponse> bankResponses = banks.stream()
                .map(bank -> BankResponse.builder()
                        .bankName(bank.getBankName())
                        .backAccountNumber(bank.getBackAccountNumber())
                        .build())
                .collect(Collectors.toList());

        return CreatePartyResponse.builder()
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
                .banking(bankResponses)
                .build();
    }

    @Override
    @Transactional
    public CreatePartyResponse updateParty(Long id, UpdatePartyDTO updatePartyDTO) throws DataNotFoundException {
        // Lấy party hiện tại, nếu không có thì ném exception
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));

        // Cập nhật thông tin Party
        party.setPartnerType(updatePartyDTO.getPartnerType());
        party.setPartnerName(updatePartyDTO.getPartnerName());
        party.setSpokesmanName(updatePartyDTO.getSpokesmanName());
        party.setAddress(updatePartyDTO.getAddress());
        party.setTaxCode(updatePartyDTO.getTaxCode());
        party.setPhone(updatePartyDTO.getPhone());
        party.setEmail(updatePartyDTO.getEmail());
        party.setPosition(updatePartyDTO.getPosition());
        partyRepository.save(party);

        // Lấy danh sách ngân hàng hiện có của Party
        List<Bank> existingBanks = bankRepository.findByParty(party);

        // Xóa hết các ngân hàng cũ
        if (!existingBanks.isEmpty()) {
            bankRepository.deleteAll(existingBanks);
        }

        // Tạo mới các đối tượng Bank từ danh sách được gửi lên (UpdateBankDTO)
        List<Bank> updatedBanks = updatePartyDTO.getBanking().stream().map(updateBankDTO -> {
            Bank bank = Bank.builder()
                    .bankName(updateBankDTO.getBankName())
                    .backAccountNumber(updateBankDTO.getBackAccountNumber())
                    .party(party)
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

        return CreatePartyResponse.builder()
                .partyId(party.getId())
                .partnerCode(party.getPartnerCode())
                .partnerType(party.getPartnerType())
                .partnerName(party.getPartnerName())
                .spokesmanName(party.getSpokesmanName())
                .address(party.getAddress())
                .taxCode(party.getTaxCode())
                .phone(party.getPhone())
                .email(party.getEmail())
                .banking(bankResponses)
                .note(party.getNote())
                .position(party.getPosition())
                .isDeleted(party.getIsDeleted())
                .build();
    }
    @Override
    public void deleteParty(Long id) throws DataNotFoundException {
        Party party = partyRepository.findById(id).orElseThrow(
                () -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));
        partyRepository.delete(party);
    }

    @Override
    @Transactional
    public Page<ListPartyResponse> getAllParties(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Party> partyPage;

        if (search != null && !search.trim().isEmpty()) {
            search = search.trim(); // Loại bỏ khoảng trắng dư thừa
            // Tìm kiếm với điều kiện isDeleted = false và loại trừ id = 1
            partyPage = partyRepository.searchByFields(search, pageable);
        } else {
            // Lấy tất cả với điều kiện isDeleted = false và loại trừ id = 1
            partyPage = partyRepository.findByIsDeletedFalseAndIdNot(pageable, 1L);
        }

        // Chuyển đổi đối tượng Party sang ListPartyResponse
        return partyPage.map(party ->
                ListPartyResponse.builder()
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
                        .banking(party.getBanking().stream()
                                .map(bank -> BankResponse.builder()
                                        .bankName(bank.getBankName())
                                        .backAccountNumber(bank.getBackAccountNumber())
                                        .build())
                                .collect(Collectors.toList()))
                        .build());
    }

    @Override
    @Transactional
    public ListPartyResponse getPartyById(Long id) throws DataNotFoundException {
        // get party by id
        Party party = partyRepository.findById(id).orElseThrow(() -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));
        // convert to response
        return ListPartyResponse.builder()
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
                .banking(party.getBanking().stream() // Nếu Party có danh sách Banks
                        .map(bank -> BankResponse.builder()
                                .bankName(bank.getBankName())
                                .backAccountNumber(bank.getBackAccountNumber())
                                .build()) // Thiếu .build()
                        .collect(Collectors.toList())) // Đổi từ .toList() thành Collectors.toList()
                .build();
    }

    @Override
    public void updatePartyStatus(Long partyId, Boolean isDeleted) throws DataNotFoundException {
        Party existingParty = partyRepository.findById(partyId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.PARTY_NOT_FOUND));
        existingParty.setIsDeleted(isDeleted);
        partyRepository.save(existingParty);
    }
}
