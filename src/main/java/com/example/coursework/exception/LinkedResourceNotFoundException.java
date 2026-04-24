package com.example.coursework.exception;

public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String parentType, String parentId) {
        super("The referenced " + parentType + " with id '" + parentId
                + "' does not exist. The sensor must be linked to a valid, existing room.");
    }
}
