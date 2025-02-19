package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.CreateTermDTO;
import com.capstone.contractmanagement.dtos.term.CreateTypeTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTermDTO;
import com.capstone.contractmanagement.dtos.term.UpdateTypeTermDTO;
import com.capstone.contractmanagement.entities.Term;
import com.capstone.contractmanagement.entities.TypeTerm;
import com.capstone.contractmanagement.enums.TermStatus;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.ITermRepository;
import com.capstone.contractmanagement.repositories.ITypeTermRepository;
import com.capstone.contractmanagement.responses.term.CreateTermResponse;
import com.capstone.contractmanagement.responses.term.GetAllTermsResponse;
import com.capstone.contractmanagement.responses.term.TypeTermResponse;
import com.capstone.contractmanagement.services.translation.TranslationService;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermService implements ITermService{

    private final ITermRepository termRepository;
    private final ITypeTermRepository typeTermRepository;


    @Override
    public CreateTermResponse createTerm(Long typeTermId, CreateTermDTO request) {
        // Lấy TypeTerm theo typeTermId
        TypeTerm typeTerm = typeTermRepository.findById(typeTermId)
                .orElseThrow(() -> new IllegalArgumentException("TypeTerm not found"));

        // Sinh clauseCode tự động dựa trên tên của typeTerm và số thứ tự (logic của bạn)
        String clauseCode = generateClauseCode(typeTerm);

        // Tạo Term mới với status = NEW và version = 1
        Term term = Term.builder()
                .clauseCode(clauseCode)
                .label(request.getLabel())
                .value(request.getValue())
                .createdAt(LocalDateTime.now())
                .typeTerm(typeTerm)
                .status(TermStatus.NEW)
                .version(1)
                .build();
        termRepository.save(term);

        return CreateTermResponse.builder()
                .id(term.getId())
                .clauseCode(term.getClauseCode())
                .label(term.getLabel())
                .value(term.getValue())
                .createdAt(term.getCreatedAt())
                .type(term.getTypeTerm().getName())
                .status(TermStatus.NEW)
                .identifier(String.valueOf(term.getTypeTerm().getIdentifier()))
                .build();
    }

    /**
     * Phương thức sinh clauseCode dựa trên tên của TypeTerm và số thứ tự.
     * Ví dụ: nếu typeTerm.getName() = "Điều khoản thêm" và chưa có term nào => clauseCode = "ĐKT001"
     */
    private String generateClauseCode(TypeTerm typeTerm) {
        // Lấy tiền tố từ tên của typeTerm (ví dụ: "Điều khoản thêm" -> "ĐKT")
        String prefix = getPrefixFromName(typeTerm.getName());

        // Đếm số lượng term hiện có của typeTerm
        int count = termRepository.countByTypeTermId(typeTerm.getId());
        int sequence = count + 1; // Số thứ tự cho term mới

        // Định dạng số thứ tự thành chuỗi 3 chữ số, ví dụ: 001, 002, ...
        String sequenceStr = String.format("%03d", sequence);

        return prefix.toUpperCase() + sequenceStr;
    }

    /**
     * Phương thức lấy tiền tố từ tên của TypeTerm bằng cách lấy ký tự đầu của mỗi từ.
     * Ví dụ: "Điều khoản thêm" -> "ĐKT"
     */
    private String getPrefixFromName(String name) {
        String[] words = name.split("\\s+");
        StringBuilder prefix = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                prefix.append(word.charAt(0));
            }
        }
        return prefix.toString();
    }


    @Override
    public TypeTerm createTypeTerm(CreateTypeTermDTO request) {
        TypeTermIdentifier typeTermIdentifier = TypeTermIdentifier.valueOf(request.getIdentifier().toUpperCase());
        TypeTerm typeTerm = TypeTerm.builder()
                .identifier(typeTermIdentifier)
                .name(request.getName())
                .build();
        typeTermRepository.save(typeTerm);
        return typeTerm;
    }

    @Override
    @Transactional
    public CreateTermResponse updateTerm(Long termId, UpdateTermDTO termRequest) throws DataNotFoundException {

        Term oldTerm = termRepository.findById(termId)
                .orElseThrow(() -> new DataNotFoundException("Term not found"));

        // cũ thành OLD (phiên bản cũ)
        oldTerm.setStatus(TermStatus.OLD);
        termRepository.save(oldTerm);

        // Tạo Term mới với dữ liệu cập nhật, version tăng thêm 1 và status là NEW

        Term newTerm = Term.builder()
                .clauseCode(oldTerm.getClauseCode())
                .label(termRequest.getLabel())
                .value(termRequest.getValue())
                .createdAt(LocalDateTime.now())
                .typeTerm(oldTerm.getTypeTerm())
                .status(TermStatus.NEW)
                .version(oldTerm.getVersion() + 1)
                .build();
        termRepository.save(newTerm);

        return CreateTermResponse.builder()
                .id(newTerm.getId())
                .clauseCode(newTerm.getClauseCode())
                .label(newTerm.getLabel())
                .value(newTerm.getValue())
                .createdAt(newTerm.getCreatedAt())
                .type(newTerm.getTypeTerm().getName())
                .identifier(String.valueOf(newTerm.getTypeTerm().getIdentifier()))
                .status(TermStatus.NEW)
                .build();
    }

//    @Override
//    public Page<GetAllTermsResponse> getAllTerms(List<TypeTermIdentifier> identifiers, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size); // Không cần ép kiểu
//
//        Page<Term> termPage;
//
//
//        if (identifiers != null && !identifiers.isEmpty()) {
//            if (identifiers.contains(TypeTermIdentifier.LEGAL_BASIS) && identifiers.size() == 1) {
//
//                termPage = termRepository.findByTypeTermIdentifier(TypeTermIdentifier.LEGAL_BASIS, pageable);
//            } else {
//                // Lọc theo danh sách identifier, có thể chứa nhiều loại
//                termPage = termRepository.findByTypeTermIdentifierInExcludingLegalBasic(identifiers, pageable);
//            }
//        } else {
//            // Nếu không có filter identifier, trả về tất cả ngoại trừ "LEGAL_BASIS"
//            termPage = termRepository.findAllExcludingLegalBasic(pageable);
//        }
//
//
//        return termPage.map(term -> GetAllTermsResponse.builder()
//                .id(term.getId())
//                .clauseCode(term.getClauseCode())
//                .label(term.getLabel())
//                .value(term.getValue())
//                .type(term.getTypeTerm().getName())
//                .identifier(term.getTypeTerm().getIdentifier().name())
//                .isDelete(term.getIsDeleted())
//                .createdAt(term.getCreatedAt())
//                .build());
//    }

    @Override
    public Page<GetAllTermsResponse> getAllTerms(List<Long> typeTermIds, boolean includeLegalBasis, String search, Pageable pageable) {
        Page<Term> termPage;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (typeTermIds != null && !typeTermIds.isEmpty()) {
            if (hasSearch) {
                if (includeLegalBasis) {
                    termPage = termRepository.findByLegalBasisOrTypeTermIdInWithSearch(typeTermIds, search, pageable);
                } else {
                    termPage = termRepository.findByTypeTermIdInWithSearch(typeTermIds, search, pageable);
                }
            } else {
                if (includeLegalBasis) {
                    termPage = termRepository.findByLegalBasisOrTypeTermIdIn(typeTermIds, pageable);
                } else {
                    termPage = termRepository.findByTypeTermIdIn(typeTermIds, pageable);
                }
            }
        } else {
            if (hasSearch) {
                termPage = termRepository.findAllExcludingLegalBasicWithSearch(search, pageable);
            } else {
                termPage = termRepository.findAllExcludingLegalBasic(pageable);
            }
        }

        return termPage.map(term -> GetAllTermsResponse.builder()
                .id(term.getId())
                .clauseCode(term.getClauseCode())
                .label(term.getLabel())
                .value(term.getValue())
                .type(term.getTypeTerm().getName())
                .identifier(term.getTypeTerm().getIdentifier().name())
                .status(term.getStatus())
                .createdAt(term.getCreatedAt())
                .build());
    }

    @Override
    public CreateTermResponse getTermById(Long id) throws DataNotFoundException {

        Term term = termRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.TERM_NOT_FOUND));
        return CreateTermResponse.builder()
                .id(term.getId())
                .clauseCode(term.getClauseCode())
                .label(term.getLabel())
                .value(term.getValue())
                .type(term.getTypeTerm().getName())
                .identifier(String.valueOf(term.getTypeTerm().getIdentifier()))
                .build();
    }

    @Override
    public void deleteTerm(Long termId) throws DataNotFoundException {

        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.TERM_NOT_FOUND));
        termRepository.delete(term);
    }

    @Override
    public String updateTypeTerm(Long typeTermId, UpdateTypeTermDTO request) {
        // check if type term exists
        TypeTerm typeTerm = typeTermRepository.findById(typeTermId)
                .orElseThrow(() -> new IllegalArgumentException(MessageKeys.TYPE_TERM_NOT_FOUND));


        typeTerm.setName(request.getName());
        TypeTermIdentifier typeTermIdentifier = TypeTermIdentifier.valueOf(request.getIdentifier().toUpperCase());

        typeTerm.setIdentifier(typeTermIdentifier);
        typeTermRepository.save(typeTerm);
        return "Updated type term successfully";
    }

    @Override
    public void deleteTypeTerm(Long typeTermId) {
        // check if type term exists
        TypeTerm typeTerm = typeTermRepository.findById(typeTermId)
                .orElseThrow(() -> new IllegalArgumentException(MessageKeys.TYPE_TERM_NOT_FOUND));
        typeTermRepository.delete(typeTerm);
    }

    @Override
    public TypeTermResponse getTypeTermById(Long typeTermId) {
        // check if type term exists
        TypeTerm typeTerm = typeTermRepository.findById(typeTermId)
                .orElseThrow(() -> new IllegalArgumentException(MessageKeys.TYPE_TERM_NOT_FOUND));
        return TypeTermResponse.builder()
                .id(typeTerm.getId())
                .name(typeTerm.getName())
                .identifier(typeTerm.getIdentifier())
                .build();
    }

    @Override
    public List<TypeTermResponse> getAllTypeTerms() {
        return typeTermRepository.findAll().stream()
                // Loại bỏ các TypeTerm có identifier là LEGAL_BASIS
                .filter(typeTerm -> !typeTerm.getIdentifier().equals(TypeTermIdentifier.LEGAL_BASIS))
                .map(typeTerm -> TypeTermResponse.builder()
                        .id(typeTerm.getId())
                        .name(typeTerm.getName())
                        .identifier(typeTerm.getIdentifier())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Page<GetAllTermsResponse> getAllTermsByTypeTermId(Long typeTermId, String search, int page, int size) {
        // Kiểm tra sự tồn tại của TypeTerm
        TypeTerm typeTerm = typeTermRepository.findById(typeTermId)
                .orElseThrow(() -> new IllegalArgumentException(MessageKeys.TYPE_TERM_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        // Nếu search null, gán chuỗi rỗng để trả về tất cả dữ liệu
        if (search == null) {
            search = "";
        }

        // Tìm kiếm theo label hoặc clauseCode dựa trên một trường search
        Page<Term> termPage = termRepository.searchByTypeTermAndLabelOrClauseCode(typeTerm, search, pageable);

        // Map sang DTO trả về
        return termPage.map(term -> GetAllTermsResponse.builder()
                .id(term.getId())
                .clauseCode(term.getClauseCode())
                .label(term.getLabel())
                .value(term.getValue())
                .type(term.getTypeTerm().getName())
                .identifier(String.valueOf(term.getTypeTerm().getIdentifier()))
                .build());
    }

    @Override
    @Transactional
    public void updateTermStatus(Long termId, Boolean isDeleted) throws DataNotFoundException {
        Term existingTerm = termRepository.findById(termId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy điều khoản với id: " + termId));
        existingTerm.setStatus(TermStatus.SOFT_DELETED);
        termRepository.save(existingTerm);
    }


}