package com.example.coursework.dao;

import com.example.coursework.model.SensorReading;

import java.util.List;
import java.util.Optional;

public class SensorReadingDAO {

    private final List<SensorReading> store;

    public SensorReadingDAO(String sensorId) {
        this.store = InMemoryDataStore.getInstance().getReadingsForSensor(sensorId);
    }

    public List<SensorReading> findAll() {
        return store;
    }

    public Optional<SensorReading> findById(String readingId) {
        return store.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst();
    }

    public SensorReading create(SensorReading reading) {
        store.add(reading);
        return reading;
    }
}
