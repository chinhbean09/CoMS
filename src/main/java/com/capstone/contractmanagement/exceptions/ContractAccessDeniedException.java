package com.capstone.contractmanagement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ContractAccessDeniedException extends RuntimeException{
    public ContractAccessDeniedException(String msg) { super(msg); }
}
