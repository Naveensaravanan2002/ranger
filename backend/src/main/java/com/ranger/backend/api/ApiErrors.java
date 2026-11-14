package com.ranger.backend.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException error){return ResponseEntity.badRequest().body(Map.of("error",error.getMessage()));}
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String,String>> forbidden(SecurityException error){return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error",error.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> validation(){return ResponseEntity.badRequest().body(Map.of("error","Please check the request size and fields."));}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> unexpected(){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error","The operation could not be completed. No sensitive details are returned."));}
}
