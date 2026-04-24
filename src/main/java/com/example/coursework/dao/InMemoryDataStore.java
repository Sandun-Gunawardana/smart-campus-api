package com.example.coursework.dao;

import com.example.coursework.model.Room;
import com.example.coursework.model.Sensor;
import com.example.coursework.model.SensorReading;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton in-memory data store.
 * Pre-populated with sample campus data for immediate testing.
 */
public class InMemoryDataStore {

    private static final InMemoryDataStore INSTANCE = new InMemoryDataStore();

    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Map<String, Sensor> sensors = new LinkedHashMap<>();
    private final Map<String, List<SensorReading>> readings = new LinkedHashMap<>();

    private InMemoryDataStore() {
        loadSampleData();
    }

    public static InMemoryDataStore getInstance() {
        return INSTANCE;
    }

    private void loadSampleData() {
        // Rooms
        Room r1 = new Room("RM-A101", "Main Lecture Hall", 200);
        Room r2 = new Room("RM-B205", "Computer Lab B", 40);
        Room r3 = new Room("RM-C310", "Research Office", 10);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        // Sensors linked to rooms
        Sensor s1 = new Sensor("SNR-TMP-01", "Temperature", "ACTIVE", 21.3, "RM-A101");
        Sensor s2 = new Sensor("SNR-CO2-01", "CO2", "ACTIVE", 480.0, "RM-A101");
        Sensor s3 = new Sensor("SNR-OCC-01", "Occupancy", "ACTIVE", 25.0, "RM-B205");
        Sensor s4 = new Sensor("SNR-LGT-01", "Light", "MAINTENANCE", 350.0, "RM-C310");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);
        sensors.put(s4.getId(), s4);

        // Link sensors to their rooms
        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());
        r3.getSensorIds().add(s4.getId());

        // Initialise empty reading lists for each sensor
        for (String sensorId : sensors.keySet()) {
            readings.put(sensorId, new ArrayList<>());
        }
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }

    public Map<String, List<SensorReading>> getAllReadings() {
        return readings;
    }
}
