package com.capstone.contractmanagement.responses.audit_trail;

import com.capstone.contractmanagement.entities.AuditTrail;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
public class AuditTrailResponse {
    private Long id;
    private String entityName;
    private Long entityId;
    private String action;
    private String changedBy;
    private String fieldName;
    private LocalDateTime changedAt;
    private String changeSummary;
    private String oldValue;
    private String newValue;

    // Constructor nhận AuditTrail và ánh xạ dữ liệu
    public AuditTrailResponse(AuditTrail auditTrail) {
        this.id = auditTrail.getId();
        this.entityName = auditTrail.getEntityName();
        this.entityId = auditTrail.getEntityId();
        this.action = auditTrail.getAction();
        this.changedBy = auditTrail.getChangedBy();
        this.fieldName = auditTrail.getFieldName();
        this.changedAt = auditTrail.getChangedAt();
        this.changeSummary = auditTrail.getChangeSummary();

        // Xử lý oldValue và newValue dựa trên entityName
        if ("ContractTerm".equals(auditTrail.getEntityName())) {
            this.oldValue = extractValue(auditTrail.getOldValue());
            this.newValue = extractValue(auditTrail.getNewValue());
        } else if ("ContractAdditionalTermDetail".equals(auditTrail.getEntityName())) {
            this.oldValue = extractAndLabelValues(auditTrail.getOldValue());
            this.newValue = extractAndLabelValues(auditTrail.getNewValue());
        } else if ("PaymentSchedule".equals(auditTrail.getEntityName())) {
            this.oldValue = formatPaymentSchedule(auditTrail.getOldValue());
            this.newValue = formatPaymentSchedule(auditTrail.getNewValue());
        } else if ("ContractItem".equals(auditTrail.getEntityName())) {
            this.oldValue = formatContractItem(auditTrail.getOldValue());
            this.newValue = formatContractItem(auditTrail.getNewValue());
        } else if ("ContractPartner".equals(auditTrail.getEntityName())) {
            this.oldValue = formatContractPartner(auditTrail.getOldValue());
            this.newValue = formatContractPartner(auditTrail.getNewValue());
        } else {
            this.oldValue = auditTrail.getOldValue();
            this.newValue = auditTrail.getNewValue();
        }
    }

    // Hàm trích xuất phần Value từ chuỗi đầy đủ cho ContractTerm
    private String extractValue(String fullValue) {
        if (fullValue == null) {
            return null;
        }
        String[] parts = fullValue.split(", ");
        for (String part : parts) {
            if (part.startsWith("Value: ")) {
                return part.substring("Value: ".length());
            }
        }
        return null;
    }

    // Hàm trích xuất và gắn nhãn các giá trị cho ContractAdditionalTermDetail
    private String extractAndLabelValues(String fullValue) {
        if (fullValue == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        // Trích xuất và gom commonTerms
        String commonTerms = extractTerms(fullValue, "commonTerms");
        if (commonTerms != null && !commonTerms.isEmpty()) {
            StringBuilder commonValues = new StringBuilder();
            String[] items = commonTerms.split("; ");
            for (String item : items) {
                String label = extractLabel(item);
                if (label != null) {
                    if (!commonValues.isEmpty()) commonValues.append(", ");
                    commonValues.append(label);
                }
            }
            if (!commonValues.isEmpty()) {
                result.append("Chung: ").append(commonValues);
            }
        }

        // Trích xuất aTerms
        String aTerms = extractTerms(fullValue, "aTerms");
        if (aTerms != null && !aTerms.isEmpty()) {
            StringBuilder aValues = new StringBuilder();
            String[] items = aTerms.split("; ");
            for (String item : items) {
                String label = extractLabel(item);
                if (label != null) {
                    if (!aValues.isEmpty()) aValues.append(", ");
                    aValues.append(label);
                }
            }
            if (!aValues.isEmpty()) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Bên A: ").append(aValues);
            }
        }

        // Trích xuất bTerms
        String bTerms = extractTerms(fullValue, "bTerms");
        if (bTerms != null && !bTerms.isEmpty()) {
            StringBuilder bValues = new StringBuilder();
            String[] items = bTerms.split("; ");
            for (String item : items) {
                String label = extractLabel(item);
                if (label != null) {
                    if (!bValues.isEmpty()) bValues.append(", ");
                    bValues.append(label);
                }
            }
            if (!bValues.isEmpty()) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Bên B: ").append(bValues);
            }
        }

        return !result.isEmpty() ? result.toString() : null;
    }

    // Hàm format cho PaymentSchedule
    private String formatPaymentSchedule(String fullValue) {
        if (fullValue == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = fullValue.split(", ");

        for (String part : parts) {
            if (part.startsWith("Order: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Thứ tự: ").append(part.substring("Order: ".length()));
            } else if (part.startsWith("Amount: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Số tiền: ").append(part.substring("Amount: ".length()));
            } else if (part.startsWith("PaymentDate: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Ngày thanh toán: ").append(part.substring("PaymentDate: ".length()));
            } else if (part.startsWith("Status: ")) {
                if (!result.isEmpty()) result.append(", ");
                String status = part.substring("Status: ".length());
                result.append("Trạng thái: ").append(translateStatus(status));
            } else if (part.startsWith("NotifyPaymentContent: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Nội dung: ").append(part.substring("NotifyPaymentContent: ".length()));
            }
        }

        return !result.isEmpty() ? result.toString() : null;
    }

    // Hàm format cho ContractItem
    private String formatContractItem(String fullValue) {
        if (fullValue == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = fullValue.split(", ");

        for (String part : parts) {
            if (part.startsWith("itemOrder: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Thứ tự: ").append(part.substring("itemOrder: ".length()));
            } else if (part.startsWith("amount: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Số tiền: ").append(part.substring("amount: ".length()));
            } else if (part.startsWith("description: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Nội dung: ").append(part.substring("description: ".length()));
            }
        }

        return !result.isEmpty() ? result.toString() : null;
    }

    // Hàm format cho ContractPartner (mới thêm)
    private String formatContractPartner(String fullValue) {
        if (fullValue == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = fullValue.split(", ");

        for (String part : parts) {
            if (part.startsWith("partnerType: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Loại đối tác: ").append(part.substring("partnerType: ".length()));
            } else if (part.startsWith("partnerName: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Tên đối tác: ").append(part.substring("partnerName: ".length()));
            } else if (part.startsWith("partnerAddress: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Địa chỉ: ").append(part.substring("partnerAddress: ".length()));
            } else if (part.startsWith("partnerTaxCode: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Mã số thuế: ").append(part.substring("partnerTaxCode: ".length()));
            } else if (part.startsWith("partnerPhone: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Số điện thoại: ").append(part.substring("partnerPhone: ".length()));
            } else if (part.startsWith("partnerEmail: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Email: ").append(part.substring("partnerEmail: ".length()));
            } else if (part.startsWith("spokesmanName: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Người đại diện: ").append(part.substring("spokesmanName: ".length()));
            } else if (part.startsWith("position: ")) {
                if (!result.isEmpty()) result.append(", ");
                result.append("Chức vụ: ").append(part.substring("position: ".length()));
            }
        }

        return !result.isEmpty() ? result.toString() : null;
    }

    // Hàm dịch trạng thái từ tiếng Anh sang tiếng Việt
    private String translateStatus(String status) {
        switch (status) {
            case "UNPAID":
                return "Chưa thanh toán";
            case "PAID":
                return "Đã thanh toán";
            case "OVERDUE":
                return "Quá hạn";
            default:
                return status; // Giữ nguyên nếu không có bản dịch
        }
    }

    // Hàm trích xuất nội dung sau "Label: " từ một item
    private String extractLabel(String item) {
        String[] parts = item.split(", ");
        for (String part : parts) {
            if (part.startsWith("Label: ")) {
                return part.substring("Label: ".length());
            }
        }
        return null;
    }

    // Hàm hỗ trợ để trích xuất nội dung bên trong của một loại term
    private String extractTerms(String fullValue, String termType) {
        String pattern = termType + "=\\[(.*?)\\]";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(fullValue);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}