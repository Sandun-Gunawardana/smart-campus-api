package com.example.coursework.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, String id) {
        super(resourceType + " with id '" + id + "' was not found.");
    }
}
