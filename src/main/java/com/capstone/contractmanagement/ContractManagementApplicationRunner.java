package com.capstone.contractmanagement;

import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.enums.PartyType;
import com.capstone.contractmanagement.enums.TypeTermIdentifier;
import com.capstone.contractmanagement.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ContractManagementApplicationRunner implements ApplicationRunner {
    @Autowired
    private IUserRepository IUserRepository;

    @Autowired
    private IRoleRepository IRoleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IPartyRepository partyRepository;

    @Autowired
    private ITypeTermRepository typeTermRepository;

    @Autowired
    private IApprovalWorkflowRepository approvalWorkflowRepository;

    private IApprovalStageRepository approvalStageRepository;

    @Value("${contract.admin.email}")
    private String email;

    @Value("${contract.manager.email}")
    private String managerEmail;

    @Value("${contract.staff.email}")
    private String staffEmail;

    @Value("${contract.admin.fullName}")
    private String fullName;

    @Value("${contract.manager.fullName}")
    private String managerFullName;

    @Value("${contract.staff.fullName}")
    private String staffFullName;

    @Value("${contract.admin.address}")
    private String address;

    @Value("${contract.manager.address}")
    private String managerAddress;

    @Value("${contract.manager.address}")
    private String staffAddress;

    @Value("${contract.admin.phoneNumber}")
    private String phoneNumber;

    @Value("${contract.manager.phoneNumber}")
    private String managerPhoneNumber;

    @Value("${contract.staff.phoneNumber}")
    private String staffPhoneNumber;

    @Value("${contract.admin.password}")
    private String password;

    @Value("${contract.admin.active}")
    private Boolean active;

    public void initializeParty() {

        if (partyRepository.count() > 0) {
            System.out.println("Party already initialized!");
            return;
        }
        Party party = Party.builder()
                .address("Khu công nghệ cao")
                .email("fsoftd1@gmail.com")
                .isDeleted(false)
                .note(null)
                .partnerCode("P40076")
                .partnerName("FPT software HCM")
                .partnerType(PartyType.COMPANY)
                .phone("0922343454")
                .spokesmanName("Đặng Nam Tiến")
                .taxCode("93245244534467")
                .build();
        partyRepository.save(party);
    }
    public void initializeTypeTerms() {
        if (typeTermRepository.count() > 0) {
            System.out.println("Type terms already initialized!");
            return;
        }
        //generalTerms, additionalTerms, otherTerms, legalbasis
        List<TypeTerm> typeTerms = List.of(
                // Additional Terms
                TypeTerm.builder().name("Điều khoản bổ sung").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản Quyền và nghĩa vụ các bên").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản Bảo hành và bảo trì").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản vi phạm và thiệt hại").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản chấm dứt hợp đồng").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản giải quyết tranh chấp").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản bảo mật").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),

                // Legal basis
                TypeTerm.builder().name("Căn cứ pháp lí").identifier(TypeTermIdentifier.LEGAL_BASIS).build(),

                // General terms
                TypeTerm.builder().name("Điều khoản chung").identifier(TypeTermIdentifier.GENERAL_TERMS).build(),

                // Other terms
                TypeTerm.builder().name("Các điều khoản khác").identifier(TypeTermIdentifier.OTHER_TERMS).build()
        );

        typeTermRepository.saveAll(typeTerms);
        System.out.println("Type terms initialized successfully!");
    }

    private void initializeApprovalWorkflow() {
        // Nếu đã có quy trình duyệt, không cần tạo lại
        if (approvalWorkflowRepository.count() > 0) {
            System.out.println("Approval workflows already initialized!");
            return;
        }
        try {
            User approver1 = IUserRepository.findById(1L).orElse(null);
            User approver2 = IUserRepository.findById(2L).orElse(null);
            User approver3 = IUserRepository.findById(3L).orElse(null);

            // Tạo quy trình duyệt mới
            ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                    .name("Standard Approval Workflow")
                    .createdAt(LocalDateTime.now())
                    .build();

            // Tạo đợt duyệt 1: Manager
            ApprovalStage stage1 = ApprovalStage.builder()
                    .stageOrder(1)
                    .approver(approver1)
                    .status(ApprovalStatus.PENDING)
                    .approvalWorkflow(workflow)
                    .build();
            // Tạo đợt duyệt 2: Admin (đóng vai CEO)
            ApprovalStage stage2 = ApprovalStage.builder()
                    .stageOrder(2)
                    .approver(approver2)
                    .status(ApprovalStatus.PENDING)
                    .approvalWorkflow(workflow)
                    .build();

            ApprovalStage stage3 = ApprovalStage.builder()
                    .stageOrder(3)
                    .approver(approver3)
                    .status(ApprovalStatus.PENDING)
                    .approvalWorkflow(workflow)
                    .build();

            // Thêm các stage vào quy trình
            List<ApprovalStage> stages = new ArrayList<>();
            stages.add(stage1);
            stages.add(stage2);
            stages.add(stage3);
            workflow.setStages(stages);
            workflow.setCustomStagesCount(stages.size());

            approvalWorkflowRepository.save(workflow);
            System.out.println("Predefined approval workflow created successfully!");
        } catch (Exception e) {
            System.err.println("Error while initializing approval workflow: " + e.getMessage());
        }
    }

    @Override
    public void run(ApplicationArguments args) {


        Optional<User> findAccountResult = IUserRepository.findByPhoneNumber(phoneNumber);
        Optional<Role> existRolePermission = IRoleRepository.findById((long) 1);
        Optional<User> findAccountManager = IUserRepository.findByPhoneNumber(managerPhoneNumber);
        Optional<User> findAccountStaff = IUserRepository.findByPhoneNumber(staffPhoneNumber);

        // Initialize type terms
        initializeTypeTerms();
        initializeParty();


        Role AdminRole = Role.builder()
                .id(1L)
                .roleName("ADMIN")
                .build();
        Role ManagerRole = Role.builder()
                .id(2L)
                .roleName("MANAGER")
                .build();
        Role StaffRole = Role.builder()
                .id(3L)
                .roleName("STAFF")
                .build();

        if (existRolePermission.isEmpty()) {
            System.out.println("There is no role Initialing...!");
        }

        IRoleRepository.save(AdminRole);
        IRoleRepository.save(ManagerRole);
        IRoleRepository.save(StaffRole);


        if (findAccountResult.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(password);

            User user = new User();
            user.setEmail(email);
            user.setAddress(address);
            user.setPassword(encodedPassword);
            user.setActive(active);
            user.setFullName(fullName);
            user.setPhoneNumber(phoneNumber);
            user.setRole(AdminRole);
            user.setActive(true);
            IUserRepository.save(user);
            System.out.println("Admin initialized!");
        }

        if (findAccountManager.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(password);

            User user = new User();
            user.setEmail(managerEmail);
            user.setAddress(managerAddress);
            user.setPassword(encodedPassword);
            user.setActive(active);
            user.setFullName(managerFullName);
            user.setPhoneNumber(managerPhoneNumber);
            user.setRole(ManagerRole);
            user.setActive(true);
            IUserRepository.save(user);
            System.out.println("Manager initialized!");
        }

        if (findAccountStaff.isEmpty()) {
            String encodedPassword = passwordEncoder.encode(password);

            User user = new User();
            user.setEmail(staffEmail);
            user.setAddress(staffAddress);
            user.setPassword(encodedPassword);
            user.setActive(active);
            user.setFullName(staffFullName);
            user.setPhoneNumber(staffPhoneNumber);
            user.setRole(StaffRole);
            user.setActive(true);
            IUserRepository.save(user);
            System.out.println("Staff initialized!");
        }
        initializeApprovalWorkflow();

        System.out.println("Hello, I'm System Manager!");
    }
}
