package com.example.coursework.exception;

public class SensorInMaintenanceException extends RuntimeException {

    public SensorInMaintenanceException(String sensorId) {
        super("Sensor '" + sensorId
                + "' is currently in MAINTENANCE mode and cannot accept new readings. "
                + "Change the sensor status to ACTIVE before posting data.");
    }
}
