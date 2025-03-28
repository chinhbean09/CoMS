package com.capstone.contractmanagement.services.sendmails;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import com.capstone.contractmanagement.entities.Addendum;
import com.capstone.contractmanagement.entities.PaymentSchedule;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalStage;
import com.capstone.contractmanagement.entities.contract.Contract;
import jakarta.mail.MessagingException;

public interface IMailService {
    void sendHtmlMail(DataMailDTO dataMail, String templateName) throws MessagingException;
    void sendEmailReminder(Contract contract, User user, ApprovalStage stage);
    void sendUpdateContractReminder(Contract contract, User user);
    void sendAccountPassword(String email, String password);
    void sendEmailReminder(PaymentSchedule payment);
    void sendEmailExpired(PaymentSchedule payment);

    void sendUpdateAddendumReminder(Addendum addendum, User user);

    // addendum
    void sendEmailAddendumReminder(Addendum addendum, User user, ApprovalStage approvalStage);

    void sendEmailApprovalSuccessForContract(Contract contract, User user);

    void sendEmailApprovalSuccessForAddendum(Addendum addendum, User user);
    void sendEmailContractOverdue(Contract contract);
    void sendEmailContractEffectiveDate(Contract contract);
    void sendEmailContractExpiryDate(Contract contract);
}
