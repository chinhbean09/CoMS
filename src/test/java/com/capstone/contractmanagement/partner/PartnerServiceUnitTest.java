package com.capstone.contractmanagement.partner;

import com.capstone.contractmanagement.dtos.bank.CreateBankDTO;
import com.capstone.contractmanagement.dtos.party.CreatePartnerDTO;
import com.capstone.contractmanagement.dtos.party.UpdatePartnerDTO;
import com.capstone.contractmanagement.entities.Bank;
import com.capstone.contractmanagement.entities.Partner;
import com.capstone.contractmanagement.repositories.IBankRepository;
import com.capstone.contractmanagement.repositories.IPartnerRepository;
import com.capstone.contractmanagement.responses.bank.BankResponse;
import com.capstone.contractmanagement.responses.party.CreatePartnerResponse;
import com.capstone.contractmanagement.responses.party.ListPartnerResponse;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.services.party.PartnerService;
import com.capstone.contractmanagement.utils.MessageKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class PartnerServiceUnitTest {
    @Mock
    private IPartnerRepository partnerRepository;
    @Mock private IBankRepository bankRepository;

    @InjectMocks
    private PartnerService partnerService;

    private CreatePartnerDTO createPartnerDTO;
    private UpdatePartnerDTO updatePartnerDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        createPartnerDTO = new CreatePartnerDTO();
        createPartnerDTO.setPartnerName("Partner A");
        createPartnerDTO.setSpokesmanName("John Doe");
        createPartnerDTO.setAddress("123 Street");
        createPartnerDTO.setTaxCode("123456789");
        createPartnerDTO.setPhone("1234567890");
        createPartnerDTO.setEmail("partner@example.com");

        updatePartnerDTO = new UpdatePartnerDTO();
        updatePartnerDTO.setPartnerName("Updated Partner");
        updatePartnerDTO.setSpokesmanName("Jane Doe");
        updatePartnerDTO.setAddress("456 Street");
        updatePartnerDTO.setTaxCode("987654321");
        updatePartnerDTO.setPhone("0987654321");
        updatePartnerDTO.setEmail("updated@example.com");
    }

    @Test
    void createPartner_ShouldReturnPartner_WhenValidInput() {
        // Given
        Partner newPartner = new Partner();
        newPartner.setId(1L);
        newPartner.setPartnerCode("P12345");
        newPartner.setPartnerName("Partner A");

        // Mock the repository behavior
        when(partnerRepository.save(any(Partner.class))).thenReturn(newPartner);

        // Create response for banks
        Bank bank = new Bank();
        bank.setBankName("Bank A");
        bank.setBackAccountNumber("123456");

        when(bankRepository.saveAll(anyList())).thenReturn(List.of(bank));

        // When
        CreatePartnerResponse response = partnerService.createPartner(createPartnerDTO);

        // Then
        assertNotNull(response);
        assertEquals("P12345", response.getPartnerCode());
        assertEquals("Partner A", response.getPartnerName());
        assertEquals(1, response.getBanking().size());
        assertEquals("Bank A", response.getBanking().get(0).getBankName());
    }

    @Test
    void createPartner_ShouldThrowException_WhenPartnerAlreadyExists() {
        // Given
        when(partnerRepository.save(any(Partner.class))).thenThrow(new RuntimeException("Partner already exists"));

        // When & Then
        assertThrows(RuntimeException.class, () -> partnerService.createPartner(createPartnerDTO));
    }

    @Test
    void updatePartner_ShouldUpdateSuccessfully_WhenValidInput() throws DataNotFoundException {
        // Given
        Partner existingPartner = new Partner();
        existingPartner.setId(1L);
        existingPartner.setPartnerName("Old Partner");

        // Mock behavior
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(existingPartner));
        when(partnerRepository.save(any(Partner.class))).thenReturn(existingPartner);

        // Prepare DTO
        updatePartnerDTO.setBanking(null);  // Here we make sure banking is null

        // When
        CreatePartnerResponse response = partnerService.updatePartner(1L, updatePartnerDTO);

        // Then
        assertNotNull(response);
        assertEquals("Updated Partner", response.getPartnerName());
        assertEquals("Jane Doe", response.getSpokesmanName());
        assertNull(response.getBanking());  // Because banking is null
    }

    @Test
    void updatePartner_ShouldThrowException_WhenPartnerNotFound() {
        // Given
        when(partnerRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DataNotFoundException.class, () -> partnerService.updatePartner(1L, updatePartnerDTO));
    }

    @Test
    void deleteParty_ShouldDeletePartner_WhenValidId() throws DataNotFoundException {
        // Given
        Partner partner = new Partner();
        partner.setId(1L);
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));

        // When
        partnerService.deleteParty(1L);

        // Then
        verify(partnerRepository, times(1)).delete(partner);
    }

    @Test
    void deleteParty_ShouldThrowException_WhenPartnerNotFound() {
        // Given
        when(partnerRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DataNotFoundException.class, () -> partnerService.deleteParty(1L));
    }

    @Test
    void getAllPartners_ShouldReturnList_WhenSearchIsValid() {
        // Given
        Partner partner = new Partner();
        partner.setId(1L);
        partner.setPartnerName("Partner A");

        when(partnerRepository.searchByFields(anyString(), any(Pageable.class)))
                .thenReturn(Page.empty());

        // When
        Page<ListPartnerResponse> response = partnerService.getAllPartners("Partner", 0, 10);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getContent().size());
    }

    @Test
    void getAllPartners_ShouldReturnList_WhenNoSearch() {
        // Given
        Partner partner = new Partner();
        partner.setId(1L);
        partner.setPartnerName("Partner A");

        when(partnerRepository.findByIsDeletedFalseAndIdNot(any(Pageable.class), anyLong()))
                .thenReturn(Page.empty());

        // When
        Page<ListPartnerResponse> response = partnerService.getAllPartners(null, 0, 10);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getContent().size());
    }

    @Test
    void getPartnerById_ShouldReturnPartner_WhenValidId() throws DataNotFoundException {
        // Given
        Partner partner = new Partner();
        partner.setId(1L);
        partner.setPartnerName("Partner A");

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));

        // When
        ListPartnerResponse response = partnerService.getPartnerById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Partner A", response.getPartnerName());
    }

    @Test
    void getPartnerById_ShouldThrowException_WhenPartnerNotFound() {
        // Given
        when(partnerRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DataNotFoundException.class, () -> partnerService.getPartnerById(1L));
    }

    @Test
    void updatePartnerStatus_ShouldUpdateStatus_WhenValidId() throws DataNotFoundException {
        // Given
        Partner partner = new Partner();
        partner.setId(1L);
        partner.setIsDeleted(false);

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));

        // When
        partnerService.updatePartnerStatus(1L, true);

        // Then
        assertTrue(partner.getIsDeleted());
        verify(partnerRepository, times(1)).save(partner);
    }

    @Test
    void updatePartnerStatus_ShouldThrowException_WhenPartnerNotFound() {
        // Given
        when(partnerRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DataNotFoundException.class, () -> partnerService.updatePartnerStatus(1L, true));
    }
}
