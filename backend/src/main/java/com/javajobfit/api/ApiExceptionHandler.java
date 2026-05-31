package com.javajobfit.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.javajobfit.service.ReportNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ReportNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleReportNotFound(ReportNotFoundException exception) {
        return Collections.singletonMap("error", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            String field = error.getField();
            String message = error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage();
            if (!fieldErrors.containsKey(field) || isRequiredFieldError(error)) {
                fieldErrors.put(field, message);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Please provide valid resume text, job description, and experience level.");
        body.put("fields", fieldErrors);
        return body;
    }

    private boolean isRequiredFieldError(FieldError error) {
        String[] codes = error.getCodes();
        if (codes == null) {
            return false;
        }
        for (String code : codes) {
            if (code != null && code.startsWith("NotBlank")) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleUnreadable(HttpMessageNotReadableException exception) {
        return Collections.singletonMap("error", "Request body could not be parsed. Expected valid JSON.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpected(Exception exception) {
        return Collections.singletonMap("error", "Something went wrong. Please try again.");
    }
}
