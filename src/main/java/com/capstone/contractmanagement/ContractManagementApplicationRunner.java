package com.capstone.contractmanagement;

import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.enums.ApprovalStatus;
import com.capstone.contractmanagement.entities.term.TypeTerm;
import com.capstone.contractmanagement.enums.PartnerType;
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
import java.util.Random;

@Component
public class ContractManagementApplicationRunner implements ApplicationRunner {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IPartnerRepository partyRepository;

    @Autowired
    private ITypeTermRepository typeTermRepository;

    @Autowired
    private IApprovalWorkflowRepository approvalWorkflowRepository;

    @Autowired
    private IApprovalStageRepository approvalStageRepository;

    @Autowired
    private IAppConfigRepository appConfigRepository;

    @Autowired
    private IDepartmentRepository departmentRepository;

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

    @Value("${contract.staff.address}") // Sửa từ manager.address thành staff.address
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

    private String generateStaffCode(Department department) {
        String departmentCode = "XX"; // default nếu department null hoặc không trích được

        if (department != null && department.getDepartmentName() != null) {
            // 1. Lấy tên phòng, loại bỏ tiền tố "Phòng "
            String name = department.getDepartmentName().replaceAll("(?i)^Phòng\\s+", "").trim();

            // 2. Split theo dấu cách, lấy ký tự đầu của hai từ đầu
            String[] parts = name.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    sb.append(part.charAt(0));
                    if (sb.length() == 2) break;
                }
            }
            // 3. Nếu không đủ 2 ký tự, bổ sung X
            while (sb.length() < 2) {
                sb.append('X');
            }
            departmentCode = sb.toString().toUpperCase();
        }

        // 4. Sinh ngẫu nhiên 6 chữ số (từ 000000 đến 999999)
        String randomDigits = String.format("%06d", new Random().nextInt(1_000_000));

        // 5. Kết hợp
        return departmentCode + randomDigits;
    }

    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.saveAll(List.of(
                    Role.builder().id(1L).roleName("ADMIN").build(),
                    Role.builder().id(2L).roleName("DIRECTOR").build(),
                    Role.builder().id(3L).roleName("MANAGER").build(),
                    Role.builder().id(4L).roleName("STAFF").build()
            ));
            System.out.println("Roles initialized!");
        } else {
            System.out.println("Roles already initialized!");
        }
    }

    private void initializeDepartments() {
        if (departmentRepository.count() == 0) {
            departmentRepository.saveAll(List.of(
                    Department.builder()
                            .departmentName("Phòng Công nghệ Thông tin")
                            .build(),
                    Department.builder()
                            .departmentName("Phòng Kinh doanh")
                            .build(),
                    Department.builder()
                            .departmentName("Phòng Nhân sự")
                            .build(),
                    Department.builder()
                            .departmentName("Phòng Tiếp thị")
                            .build(),
                    Department.builder()
                            .departmentName("Phòng Giám đốc")
                            .build()
            ));
            System.out.println("Departments initialized!");
        } else {
            System.out.println("Departments already initialized!");
        }
    }

    private void initializeTypeTerms() {
        if (typeTermRepository.count() > 0) {
            System.out.println("Type terms already initialized!");
            return;
        }
        List<TypeTerm> typeTerms = List.of(
                TypeTerm.builder().name("Điều khoản bổ sung").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản Quyền và nghĩa vụ các bên").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản Bảo hành và bảo trì").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản vi phạm và thiệt hại").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản chấm dứt hợp đồng").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản giải quyết tranh chấp").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Điều khoản bảo mật").identifier(TypeTermIdentifier.ADDITIONAL_TERMS).build(),
                TypeTerm.builder().name("Căn cứ pháp lí").identifier(TypeTermIdentifier.LEGAL_BASIS).build(),
                TypeTerm.builder().name("Điều khoản chung").identifier(TypeTermIdentifier.GENERAL_TERMS).build(),
                TypeTerm.builder().name("Các điều khoản khác").identifier(TypeTermIdentifier.OTHER_TERMS).build()
        );
        typeTermRepository.saveAll(typeTerms);
        System.out.println("Type terms initialized successfully!");
    }

    private void initializeUser(String email, String phoneNumber, String fullName, String address,
                                String password, String roleName, Long roleId, Long departmentId) {
        if (userRepository.findByPhoneNumber(phoneNumber).isEmpty()) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            Department department = departmentId != null ? departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found: " + departmentId)) : null;
            String staffCode = generateStaffCode(department);
            String encodedPassword = passwordEncoder.encode(password);

            User user = User.builder()
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .fullName(fullName)
                    .address(address)
                    .password(encodedPassword)
                    .role(role)
                    .staffCode(staffCode)
                    .department(department)
                    .active(true)
                    .build();

            userRepository.save(user);
            System.out.println(roleName + " initialized with staff code: " + staffCode);
        } else {
            System.out.println(roleName + " with phone number " + phoneNumber + " already exists!");
        }
    }

    private void initializeParty() {
        if (partyRepository.count() > 0) {
            System.out.println("Partner already initialized!");
            return;
        }
        Partner partner = Partner.builder()
                .address("26 Nguyễn Đình Khơi, Phường 4, Quận Tân Bình, HCM")
                .email("hisoft@gmail.com")
                .isDeleted(false)
                .note(null)
                .partnerCode("P40076")
                .partnerName("Hisoft Company HCM")
                .partnerType(PartnerType.PARTNER_A)
                .phone("0922343454")
                .abbreviation("HISOFT")
                .spokesmanName("Ngô Đăng Hà An")
                .taxCode("93245244534467")
                .position("Giám đốc")
                .user(userRepository.findById(1L).orElse(null))
                .build();
        partyRepository.save(partner);
        System.out.println("Partner initialized!");
    }

    private void initializeApprovalWorkflow() {
        if (approvalWorkflowRepository.count() > 0) {
            System.out.println("Approval workflows already initialized!");
            return;
        }
        try {
            User approver1 = userRepository.findById(2L).orElse(null); // Manager
            User approver2 = userRepository.findById(3L).orElse(null);
            User approver3 = userRepository.findById(4L).orElse(null); // Staff (có thể sửa thành Admin nếu cần)

            if (approver1 == null || approver2 == null) {
                System.err.println("Approvers not found, skipping approval workflow initialization.");
                return;
            }

            ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                    .name("Standard Approval Workflow")
                    .createdAt(LocalDateTime.now())
                    .build();

            ApprovalStage stage1 = ApprovalStage.builder()
                    .stageOrder(1)
                    .approver(approver1)
                    .status(ApprovalStatus.NOT_STARTED)
                    .approvalWorkflow(workflow)
                    .build();

            ApprovalStage stage2 = ApprovalStage.builder()
                    .stageOrder(2)
                    .approver(approver2)
                    .status(ApprovalStatus.NOT_STARTED)
                    .approvalWorkflow(workflow)
                    .build();

            ApprovalStage stage3 = ApprovalStage.builder()
                    .stageOrder(2)
                    .approver(approver3)
                    .status(ApprovalStatus.NOT_STARTED)
                    .approvalWorkflow(workflow)
                    .build();

            List<ApprovalStage> stages = new ArrayList<>();
            stages.add(stage1);
            stages.add(stage2);
            workflow.setStages(stages);
            workflow.setCustomStagesCount(stages.size());

            approvalWorkflowRepository.save(workflow);
            System.out.println("Predefined approval workflow created successfully!");
        } catch (Exception e) {
            System.err.println("Error while initializing approval workflow: " + e.getMessage());
        }
    }

    private void initializeAppConfig() {
        if (appConfigRepository.count() > 0) {
            System.out.println("App config already initialized!");
            return;
        }
        AppConfig appConfig1 = AppConfig.builder()
                .key("APPROVAL_DEADLINE")
                .value("2")
                .description("Hạn phê duyệt cho hợp đồng trong một đợt")
                .build();
        appConfigRepository.save(appConfig1);


        AppConfig appConfig2 = AppConfig.builder()
                .key("PAYMENT_DEADLINE")
                .value("5")
                .description("Thông báo thanh toán cho hợp đồng trước:")
                .build();
        appConfigRepository.save(appConfig2);

        System.out.println("App config initialized!");
    }

    @Override
    public void run(ApplicationArguments args) {
        // Khởi tạo Roles trước
        initializeRoles();

        // Khởi tạo Departments trước Users
        initializeDepartments();

        // Khởi tạo Type Terms
        initializeTypeTerms();


        // Khởi tạo các tài khoản
        initializeUser(email, phoneNumber, "Đỗ Minh Chính", "Hồ Chí Minh", password, "ADMIN", 1L, null);
        initializeUser(managerEmail, managerPhoneNumber, "Lâm Quốc Vinh", "Hồ Chí Minh", password, "MANAGER", 3L, 1L);
        initializeUser("AnNDH22@fe.edu.vn", "0874534458", "Ngô Đăng Hà An", "Hồ Chí Minh", password, "MANAGER", 3L, 2L);
        initializeUser(staffEmail, staffPhoneNumber, "Hoàng Tuấn Khang", "Hồ Chí Minh", password, "STAFF", 4L, 4L);
        initializeUser("nguyenthiencammc@gmail.com", "0974534458", "Ngô Đăng Hà An", "Hồ Chí Minh", password, "DIRECTOR", 2L, 5L);

        // Khởi tạo các dữ liệu khác
        initializeParty();
        // Khởi tạo Approval Workflow
        initializeApprovalWorkflow();
        initializeAppConfig();

        System.out.println("Hello, I'm System Manager!");
    }
}