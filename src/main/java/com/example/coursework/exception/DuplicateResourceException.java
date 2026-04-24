package com.example.coursework.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceType, String id) {
        super(resourceType + " with id '" + id + "' already exists.");
    }
}
