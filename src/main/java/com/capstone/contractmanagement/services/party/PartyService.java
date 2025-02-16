package com.capstone.contractmanagement.services.party;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // Tự động tạo partnerCode theo định dạng: P + 5 số (vd: P12345)
        String partnerCode = "P" + String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        Party party = Party.builder()
                .partnerCode(partnerCode)
                .partnerType(createPartyDTO.getPartnerType())
                .partnerName(createPartyDTO.getPartnerName())
                .spokesmanName(createPartyDTO.getSpokesmanName())
                .address(createPartyDTO.getAddress())
                .taxCode(createPartyDTO.getTaxCode())
                .phone(createPartyDTO.getPhone())
                .email(createPartyDTO.getEmail())
                .build();

        party = partyRepository.save(party);

        Bank bank = Bank.builder()
                .bankName(createPartyDTO.getBanking().getBankName())
                .backAccountNumber(createPartyDTO.getBanking().getBackAccountNumber())
                .party(party)
                .build();

        bankRepository.save(bank);

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
                .banking(Collections.singletonList(
                        BankResponse.builder()
                                .bankName(bank.getBankName())
                                .backAccountNumber(bank.getBackAccountNumber())
                                .build()
                ))
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
    public List<ListPartyResponse> getAllParties() {
        // get all parties
        List<Party> parties = partyRepository.findAll();
        // Chuyển đổi sang response
        return parties.stream().map(party ->
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
                        .banking(party.getBanking().stream() // Nếu Party có danh sách Banks
                                .map(bank -> BankResponse.builder()
                                        .bankName(bank.getBankName())
                                        .backAccountNumber(bank.getBackAccountNumber())
                                        .build()) // Thiếu .build()
                                .collect(Collectors.toList())) // Đổi từ .toList() thành Collectors.toList()
                        .build() // Thiếu .build()
        ).collect(Collectors.toList()); // Đổi từ .toList() thành Collectors.toList()
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
                .banking(party.getBanking().stream() // Nếu Party có danh sách Banks
                        .map(bank -> BankResponse.builder()
                                .bankName(bank.getBankName())
                                .backAccountNumber(bank.getBackAccountNumber())
                                .build()) // Thiếu .build()
                        .collect(Collectors.toList())) // Đổi từ .toList() thành Collectors.toList()
                .build();
    }
}
