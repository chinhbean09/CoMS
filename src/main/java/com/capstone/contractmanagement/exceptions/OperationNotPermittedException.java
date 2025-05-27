package com.capstone.contractmanagement.exceptions;

public class OperationNotPermittedException extends Exception {
    public OperationNotPermittedException(String message) {
        super(message);
    }
}