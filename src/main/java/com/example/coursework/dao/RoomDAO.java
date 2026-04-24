package com.example.coursework.dao;

import com.example.coursework.model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RoomDAO {

    private static final RoomDAO INSTANCE = new RoomDAO();
    private final Map<String, Room> store = InMemoryDataStore.getInstance().getRooms();

    private RoomDAO() {
    }

    public static RoomDAO getInstance() {
        return INSTANCE;
    }

    public List<Room> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Room> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Room create(Room room) {
        store.put(room.getId(), room);
        return room;
    }

    public Room update(Room room) {
        store.put(room.getId(), room);
        return room;
    }

    public void delete(String id) {
        store.remove(id);
    }
}
