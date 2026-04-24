package com.example.coursework.dao;

import com.example.coursework.model.Sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SensorDAO {

    private static final SensorDAO INSTANCE = new SensorDAO();
    private final Map<String, Sensor> store = InMemoryDataStore.getInstance().getSensors();

    private SensorDAO() {
    }

    public static SensorDAO getInstance() {
        return INSTANCE;
    }

    public List<Sensor> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Sensor> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Sensor> findByType(String type) {
        return store.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public Sensor create(Sensor sensor) {
        store.put(sensor.getId(), sensor);
        return sensor;
    }

    public Sensor update(Sensor sensor) {
        store.put(sensor.getId(), sensor);
        return sensor;
    }

    public void delete(String id) {
        store.remove(id);
    }
}
