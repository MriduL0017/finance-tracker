package com.finance.tracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// This annotation tells Spring: "Use this class to handle errors across ALL controllers"
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Tell Spring to run this method when a Validation error happens
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // 2. Force the HTTP status to be 400 Bad Request instead of 500 Internal Error
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        
        // Create an empty dictionary to hold our clean error messages
        Map<String, String> errors = new HashMap<>();
        
        // Loop through all the messy errors Spring generated...
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            // Extract the specific field name (like "amount" or "description")
            String fieldName = ((FieldError) error).getField();
            
            // Extract your custom message ("Amount must be greater than zero")
            String errorMessage = error.getDefaultMessage();
            
            // Put them in the dictionary
            errors.put(fieldName, errorMessage);
        });
        
        // Return the clean dictionary. Spring automatically converts this to JSON!
        return errors;
    }
}