package com.capstone.contractmanagement.services.term;

import com.capstone.contractmanagement.dtos.term.*;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.term.Term;
import com.capstone.contractmanagement.entities.term.TypeTerm;
import com.capstone.contractmanagement.enums.TermStatus;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.exceptions.DataNotFoundException;
import com.capstone.contractmanagement.repositories.IContractTemplateAdditionalTermDetailRepository;
import com.capstone.contractmanagement.repositories.ITermRepository;
import com.capstone.contractmanagement.repositories.ITypeTermRepository;
import com.capstone.contractmanagement.responses.term.*;
import com.capstone.contractmanagement.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermService implements ITermService{

    private final ITermRepository termRepository;
    private final ITypeTermRepository typeTermRepository;
    private final IContractTemplateAdditionalTermDetailRepository contractTemplateAdditionalTermDetailRepository;

    @Override
    @Transactional
    public CreateTermResponse createTerm(Long typeTermId, CreateTermDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        TypeTerm typeTerm = typeTermRepository.findById(typeTermId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại điều khoản"));

        String newLabel = request.getLabel().trim();
        if (termRepository.existsByLabelAndTypeTermAndStatus(newLabel, typeTerm, TermStatus.NEW)) {
            throw new IllegalArgumentException(
                    "Tên điều khoản '" + newLabel + "' đã tồn tại trong loại '"
                            + typeTerm.getName() + "'. Vui lòng chọn tên khác!"
            );
        }

        String clauseCode = generateClauseCode(typeTerm);
        Term term = Term.builder()
                .clauseCode(clauseCode)
                .label(newLabel)
                .value(request.getValue())
                .createdAt(LocalDateTime.now())
                .typeTerm(typeTerm)
                .status(TermStatus.NEW)
                .user(currentUser)
                .version(1)
                .build();
        termRepository.save(term);

        return CreateTermResponse.builder()
                .id(term.getId())
                .clauseCode(term.getClauseCode())
                .label(term.getLabel())
                .value(term.getValue())
                .createdAt(term.getCreatedAt())
                .createdBy(TermCreatorResponse.builder()
                        .id(term.getUser().getId())
                        .name(term.getUser().getFullName()).build())
                .type(typeTerm.getName())
                .status(term.getStatus())
                .identifier(String.valueOf(typeTerm.getIdentifier()))
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Term oldTerm = termRepository.findById(termId)
                .orElseThrow(() -> new DataNotFoundException("Điều khoản không tìm thấy"));

        // Chỉ creator mới được update
        if (!oldTerm.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException(
                    "Bạn không có quyền cập nhật điều khoản này"
            );
        }


        String newLabel = termRequest.getLabel().trim();
        if (!oldTerm.getLabel().equals(newLabel)
                && termRepository.existsByLabelAndTypeTermAndStatus(newLabel, oldTerm.getTypeTerm(), TermStatus.NEW)) {
            throw new IllegalArgumentException(
                    "Tên điều khoản '" + newLabel + "' đã tồn tại trong loại '"
                            + oldTerm.getTypeTerm().getName() + "'."
            );
        }

        oldTerm.setLabel(newLabel);
        oldTerm.setValue(termRequest.getValue());
        oldTerm.setStatus(TermStatus.NEW);
        oldTerm.setVersion(oldTerm.getVersion() + 1);
        termRepository.save(oldTerm);

        return CreateTermResponse.builder()
                .id(oldTerm.getId())
                .clauseCode(oldTerm.getClauseCode())
                .label(oldTerm.getLabel())
                .value(oldTerm.getValue())
                .createdAt(oldTerm.getCreatedAt())
                .createdBy(TermCreatorResponse.builder()
                        .id(oldTerm.getUser().getId())
                        .name(oldTerm.getUser().getFullName()).build())
                .type(oldTerm.getTypeTerm().getName())
                .identifier(String.valueOf(oldTerm.getTypeTerm().getIdentifier()))
                .status(oldTerm.getStatus())
                .build();
    }

    @Override
    public Page<GetAllTermsResponse> getAllTerms(List<Long> typeTermIds, boolean includeLegalBasis, String search, Pageable pageable) {
        Page<Term> termPage;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasSearch) {
            search = search.trim();
        }

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

        return termPage.map(term -> {
            int contractTemplateCount = termRepository.countContractTemplateUsage(term.getId());
            int contractCount = termRepository.countContractUsage(term.getId());
            return GetAllTermsResponse.builder()
                    .id(term.getId())
                    .clauseCode(term.getClauseCode())
                    .label(term.getLabel())
                    .value(term.getValue())
                    .type(term.getTypeTerm().getName())
                    .identifier(term.getTypeTerm().getIdentifier().name())
                    .status(term.getStatus())
                    .createdBy(TermCreatorResponse.builder()
                            .id(term.getUser().getId())
                            .name(term.getUser().getFullName()).build())
                    .createdAt(term.getCreatedAt())
                    .version(term.getVersion())
                    .contractTemplateCount(contractTemplateCount)
                    .contractCount(contractCount)
                    .build();
        });
    }

    @Override
    public Page<GetAllTermsResponseLessField> getAllTermsLessField(List<Long> typeTermIds, boolean includeLegalBasis, String search, Pageable pageable) {
        Page<Term> termPage;
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasSearch) {
            search = search.trim(); // Loại bỏ khoảng trắng dư thừa
        }

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



        return termPage.map(term -> GetAllTermsResponseLessField.builder()
                .id(term.getId())
                .label(term.getLabel())
                .value(term.getValue())
                .build());
    }

        @Override
        @Transactional
        public List<CreateTermResponse> batchCreateTerms(List<BatchCreateTermDTO> dtos) throws DataNotFoundException {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();
            // Validate type term tồn tại
            Set<Long> typeTermIds = dtos.stream()
                    .map(BatchCreateTermDTO::getTypeTermId)
                    .collect(Collectors.toSet());

            Map<Long, TypeTerm> typeTermMap = typeTermRepository.findAllById(typeTermIds)
                    .stream()
                    .collect(Collectors.toMap(TypeTerm::getId, Function.identity()));

            // Tạo danh sách terms
            List<Term> terms = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (BatchCreateTermDTO dto : dtos) {
                TypeTerm typeTerm = typeTermMap.get(dto.getTypeTermId());

                if (typeTerm == null) {
                    throw new DataNotFoundException("Không tìm thấy loại điều khoản");
                }

                Term term = Term.builder()
                        .label(dto.getLabel())
                        .value(dto.getValue())
                        .typeTerm(typeTerm)
                        .clauseCode(generateClauseCode(typeTerm))
                        .createdAt(now)
                        .status(TermStatus.NEW)
                        .user(currentUser)
                        .version(1)
                        .build();

                terms.add(term);
            }

            // Lưu batch và generate response
            List<Term> savedTerms = termRepository.saveAll(terms);

            return savedTerms.stream()
                    .map(term -> CreateTermResponse.builder()
                            .id(term.getId())
                            .label(term.getLabel())
                            .value(term.getValue())
                            .createdAt(term.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        }

    @Override
    @Transactional
    public List<CreateTermResponse> importTermsFromExcel(MultipartFile file, Long typeTermId) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        // Kiểm tra sự tồn tại của TypeTerm
        TypeTerm typeTerm = typeTermRepository.findById(typeTermId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại điều khoản"));

        // Kiểm tra xem file có phải là file Excel không
        if (!file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") &&
                !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("File bạn gửi lên không phải là file Excel (XLSX).");
        }

        // Đọc dữ liệu từ file Excel
        List<CreateTermResponse> termResponses = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(inputStream); // Tạo workbook từ file Excel
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

            // Duyệt qua các dòng trong sheet
            for (int i = 1; i <= sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || row.getCell(0) == null || row.getCell(1) == null) {
                    continue; // Bỏ qua các dòng không hợp lệ
                }

                String label = row.getCell(0).getStringCellValue();
                String value = row.getCell(1).getStringCellValue();

                // Kiểm tra nếu label đã tồn tại
                if (termRepository.existsByLabelAndTypeTermAndStatus(label, typeTerm, TermStatus.NEW)) {
                    continue; // Bỏ qua nếu label đã tồn tại
                }

                // Tạo mới Term
                Term term = Term.builder()
                        .label(label)
                        .value(value)
                        .typeTerm(typeTerm)
                        .clauseCode(generateClauseCode(typeTerm)) // Gọi phương thức tạo clauseCode
                        .createdAt(LocalDateTime.now())
                        .user(currentUser)
                        .status(TermStatus.NEW)
                        .version(1)
                        .build();

                term = termRepository.save(term);

                // Thêm vào danh sách phản hồi
                termResponses.add(CreateTermResponse.builder()
                        .id(term.getId())
                        .label(term.getLabel())
                        .value(term.getValue())
                        .createdAt(term.getCreatedAt())
                        .identifier(String.valueOf(typeTerm.getIdentifier()))
                        .type(typeTerm.getName())
                        .createdBy(TermCreatorResponse.builder()
                                        .id(currentUser.getId())
                                        .name(currentUser.getFullName()).build())
                        .clauseCode(term.getClauseCode())
                        .status(term.getStatus())
                        .build());
            }
        }

        return termResponses;
    }

    @Override
    @Transactional
    public Page<CreateTermResponse> searchTerm(String keyword, int page, int size) {
        List<Term> allTerms = termRepository.findAll();
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        double threshold = 0.7;

        List<CreateTermResponse> matchedTerms = allTerms.stream()
                .filter(term -> {
                    double labelSim = similarity.apply(keyword.toLowerCase(), term.getLabel().toLowerCase());
                    double valueSim = similarity.apply(keyword.toLowerCase(), term.getValue().toLowerCase());
                    return labelSim >= threshold || valueSim >= threshold;
                })
                .filter(term -> term.getStatus() == TermStatus.NEW)
                .map(term -> CreateTermResponse.builder()
                        .id(term.getId())
                        .label(term.getLabel())
                        .value(term.getValue())
                        .clauseCode(term.getClauseCode())
                        .createdAt(term.getCreatedAt())
                        .status(term.getStatus())
                        .createdBy(TermCreatorResponse.builder()
                                .id(term.getUser().getId())
                                .name(term.getUser().getFullName()).build())
                        .identifier(String.valueOf(term.getTypeTerm().getIdentifier()))
                        .type(term.getTypeTerm().getName())
                        .build())
                .collect(Collectors.toList());

        int start = Math.min(page * size, matchedTerms.size());
        int end = Math.min(start + size, matchedTerms.size());

        List<CreateTermResponse> pageContent = matchedTerms.subList(start, end);
        return new PageImpl<>(pageContent, PageRequest.of(page, size), matchedTerms.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetAllTermsResponse> getAllTermsByUser(List<Long> typeTermIds, boolean includeLegalBasis, String search, Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        boolean hasSearch         = StringUtils.hasText(search);
        boolean hasTypeTermFilter = typeTermIds != null && !typeTermIds.isEmpty();

        Page<Term> termPage;
        if (hasTypeTermFilter) {
            if (includeLegalBasis) {
                termPage = hasSearch
                        ? termRepository.findByUserLegalBasisOrTypeTermIdInWithSearch(
                        currentUser, typeTermIds, search.trim(), pageable)
                        : termRepository.findByUserLegalBasisOrTypeTermIdIn(
                        currentUser, typeTermIds,          pageable);
            } else {
                termPage = hasSearch
                        ? termRepository.findByUserAndTypeTermIdInWithSearch(
                        currentUser, typeTermIds, search.trim(), pageable)
                        : termRepository.findByUserAndTypeTermIdIn(
                        currentUser, typeTermIds,          pageable);
            }
        } else {
            if (includeLegalBasis) {
                termPage = hasSearch
                        ? termRepository.findByUserLegalBasisWithSearch(currentUser, search.trim(), pageable)
                        : termRepository.findByUserLegalBasis(currentUser, pageable);
            } else {
                termPage = hasSearch
                        ? termRepository.findByUserExcludingLegalBasicWithSearch(currentUser, search.trim(), pageable)
                        : termRepository.findByUserExcludingLegalBasic(currentUser,          pageable);
            }
        }

        return termPage.map(term -> {
            int contractTemplateCount = termRepository.countContractTemplateUsage(term.getId());
            int contractCount         = termRepository.countContractUsage(term.getId());
            return GetAllTermsResponse.builder()
                    .id(term.getId())
                    .clauseCode(term.getClauseCode())
                    .label(term.getLabel())
                    .value(term.getValue())
                    .type(term.getTypeTerm().getName())
                    .identifier(term.getTypeTerm().getIdentifier().name())
                    .status(term.getStatus())
                    .createdBy(TermCreatorResponse.builder()
                            .id(term.getUser().getId())
                            .name(term.getUser().getFullName())
                            .build())
                    .createdAt(term.getCreatedAt())
                    .version(term.getVersion())
                    .contractTemplateCount(contractTemplateCount)
                    .contractCount(contractCount)
                    .build();
        });
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
                .createdBy(TermCreatorResponse.builder()
                        .id(term.getUser().getId())
                        .name(term.getUser().getFullName()).build())
                .type(term.getTypeTerm().getName())
                .identifier(String.valueOf(term.getTypeTerm().getIdentifier()))
                .build();
    }

    @Override
    public void deleteTerm(Long termId) throws DataNotFoundException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new DataNotFoundException(MessageKeys.TERM_NOT_FOUND));
        if (!term.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không thể xóa điều khoản do người khác tạo");
        }
        // Kiểm tra trong các mối quan hệ ManyToMany của ContractTemplate
        long countInTemplates = termRepository.countTemplatesUsingTerm(term);
        if (countInTemplates > 0) {
            throw new IllegalStateException("Không thể xóa điều khoản vì nó đang được sử dụng trong " + countInTemplates + " template");
        }

        // Kiểm tra trong ContractTemplateAdditionalTermDetail
        long countInAdditional = contractTemplateAdditionalTermDetailRepository.countByTermIdInLists(termId);
        if (countInAdditional > 0) {
            throw new IllegalStateException("Không thể xóa điều khoản vì nó đang được sử dụng trong " + countInAdditional + " template");
        }

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
                .createdBy(TermCreatorResponse.builder()
                        .id(term.getUser().getId())
                        .name(term.getUser().getFullName()).build())
                .identifier(String.valueOf(term.getTypeTerm().getIdentifier()))
                .build());
    }

    @Override
    @Transactional
    public void updateTermStatus(Long termId, Boolean isDeleted) throws DataNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        Term existingTerm = termRepository.findById(termId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy điều khoản với id: " + termId));

        if (!existingTerm.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không thể xóa điều khoản do người khác tạo");
        }

        // Kiểm tra trong các mối quan hệ ManyToMany của ContractTemplate
        long countInTemplates = termRepository.countTemplatesUsingTerm(existingTerm);
        if (countInTemplates > 0) {
            throw new IllegalStateException("Không thể xóa điều khoản vì nó đang được sử dụng trong " + countInTemplates + " template");
        }

        // Kiểm tra trong ContractTemplateAdditionalTermDetail
        long countInAdditional = contractTemplateAdditionalTermDetailRepository.countByTermIdInLists(termId);
        if (countInAdditional > 0) {
            throw new IllegalStateException("Không thể xóa điều khoản vì nó đang được sử dụng trong " + countInAdditional + " template");
        }

        // Nếu không có tham chiếu, tiến hành xóa mềm
        existingTerm.setStatus(TermStatus.SOFT_DELETED);
        termRepository.save(existingTerm);
    }

}