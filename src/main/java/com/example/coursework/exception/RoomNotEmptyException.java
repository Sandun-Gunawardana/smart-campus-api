package com.example.coursework.exception;

public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room '" + roomId + "' cannot be deleted because it still has "
                + sensorCount + " sensor(s) assigned. Remove all sensors first.");
    }
}
