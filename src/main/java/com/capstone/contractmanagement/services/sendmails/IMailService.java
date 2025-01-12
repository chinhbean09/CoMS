package com.capstone.contractmanagement.services.sendmails;

import com.capstone.contractmanagement.dtos.DataMailDTO;
import jakarta.mail.MessagingException;

public interface IMailService {
    void sendHtmlMail(DataMailDTO dataMail, String templateName) throws MessagingException;

}
