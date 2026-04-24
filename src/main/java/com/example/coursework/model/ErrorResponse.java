package com.example.coursework.model;

public class ErrorResponse {

    private String error;
    private int statusCode;
    private String details;

    public ErrorResponse() {
    }

    public ErrorResponse(String error, int statusCode, String details) {
        this.error = error;
        this.statusCode = statusCode;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
