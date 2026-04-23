package com.example.inventoryreservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientInventoryException.class)
    public ProblemDetail handleInsufficientInventory(InsufficientInventoryException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://example.com/problems/insufficient-inventory"));
        pd.setTitle("Insufficient inventory");
        pd.setInstance(URI.create(extractPath(request)));
        return pd;
    }

    @ExceptionHandler(InvalidReservationStateException.class)
    public ProblemDetail handleInvalidState(InvalidReservationStateException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://example.com/problems/invalid-reservation-state"));
        pd.setTitle("Invalid reservation state");
        pd.setInstance(URI.create(extractPath(request)));
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://example.com/problems/resource-not-found"));
        pd.setTitle("Resource not found");
        pd.setInstance(URI.create(extractPath(request)));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(URI.create("https://example.com/problems/validation-error"));
        pd.setTitle("Validation error");
        pd.setInstance(URI.create(extractPath(request)));
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("https://example.com/problems/bad-request"));
        pd.setTitle("Bad request");
        pd.setInstance(URI.create(extractPath(request)));
        return pd;
    }

    private String extractPath(WebRequest request) {
        String desc = request.getDescription(false);
        return desc.startsWith("uri=") ? desc.substring(4) : desc;
    }
}
