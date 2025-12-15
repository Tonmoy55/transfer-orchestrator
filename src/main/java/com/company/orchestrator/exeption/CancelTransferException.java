package com.company.orchestrator.exeption;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Author: Tonmoy Sikder Date:12/13/2025
 */

@ResponseStatus(HttpStatus.EXPECTATION_FAILED)
public class CancelTransferException extends RuntimeException{
    public CancelTransferException(String message) {
        super(message);
    }
}
