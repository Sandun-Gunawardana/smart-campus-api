package com.example.coursework.resource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.example.coursework.dao.InMemoryDataStore;
import com.example.coursework.dao.RoomDAO;
import com.example.coursework.dao.SensorDAO;
import com.example.coursework.exception.DuplicateResourceException;
import com.example.coursework.exception.LinkedResourceNotFoundException;
import com.example.coursework.exception.ResourceNotFoundException;
import com.example.coursework.model.ErrorResponse;
import com.example.coursework.model.Room;
import com.example.coursework.model.Sensor;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final SensorDAO sensorDAO = SensorDAO.getInstance();
    private final RoomDAO roomDAO = RoomDAO.getInstance();

    @Context
    private UriInfo uriInfo;

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result;
        if (type != null && !type.trim().isEmpty()) {
            result = sensorDAO.findByType(type);
        } else {
            result = sensorDAO.findAll();
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorDAO.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));
        return Response.ok(sensor).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            ErrorResponse err = new ErrorResponse("Bad Request", 400, "Request body must not be empty.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        String clientId = sensor.getId();
        if (clientId == null || clientId.trim().isEmpty()) {
            ErrorResponse err = new ErrorResponse("Bad Request", 400, "Field 'id' is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        if (sensorDAO.findById(clientId).isPresent()) {
            throw new DuplicateResourceException("Sensor", clientId);
        }

        // Validate that the referenced room actually exists (Part 5.2 — 422)
        String roomId = sensor.getRoomId();
        Optional<Room> parentRoom = roomDAO.findById(roomId);
        if (!parentRoom.isPresent()) {
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        Sensor created = sensorDAO.create(sensor);

        // Keep the parent room's sensorIds list in sync
        Room room = parentRoom.get();
        room.getSensorIds().add(created.getId());
        roomDAO.update(room);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();

        return Response.created(location).entity(created).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorDAO.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));

        // Detach sensor from its parent room
        roomDAO.findById(sensor.getRoomId()).ifPresent(room -> {
            room.getSensorIds().remove(sensorId);
            roomDAO.update(room);
        });

        // Remove any buffered readings
        InMemoryDataStore.getInstance().getReadingsForSensor(sensorId).clear();

        sensorDAO.delete(sensorId);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", "Sensor deleted successfully.");
        body.put("id", sensorId);
        return Response.ok(body).build();
    }

    // ── Sub-resource locator (Part 4) ────────────────────────────
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
        if (!sensorDAO.findById(sensorId).isPresent()) {
            throw new ResourceNotFoundException("Sensor", sensorId);
        }
        return new SensorReadingResource(sensorId);
    }
}
