package com.example.attendance.dto;

public record ErrorResponse(
    String error,
    String message
) {}
