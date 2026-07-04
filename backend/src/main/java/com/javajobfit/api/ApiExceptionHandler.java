package com.javajobfit.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String, String> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return Collections.singletonMap("error", "This file is too large. Maximum size is 5MB. Please paste your resume text instead.");
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadMultipart(Exception exception) {
        return Collections.singletonMap("error", "Could not read the uploaded file. Please paste your resume text instead.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Map<String, String> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return Collections.singletonMap("error", "Method not allowed.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Map<String, String> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return Collections.singletonMap("error", "Unsupported content type.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleUnexpected(Exception exception) {
        return Collections.singletonMap("error", "Something went wrong. Please try again.");
    }
}
