package org.superwindcloud.shortlink.exception;

import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
      DataIntegrityViolationException e) {
    Map<String, String> error = new HashMap<>();
    error.put(
        "error", "Invalid input or constraint violation: " + e.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "An error occurred: " + e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Map<String, String>> handleResourceNotFoundException(
      NoResourceFoundException e) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "Resource not found: " + e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }
}
