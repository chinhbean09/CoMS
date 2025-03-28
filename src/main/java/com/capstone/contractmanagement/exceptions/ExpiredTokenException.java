package com.capstone.contractmanagement.exceptions;

public class ExpiredTokenException extends Exception {
    public ExpiredTokenException(String message) {
        super(message);
    }
}