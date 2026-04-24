package com.example.coursework.resource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.example.coursework.dao.RoomDAO;
import com.example.coursework.exception.DuplicateResourceException;
import com.example.coursework.exception.ResourceNotFoundException;
import com.example.coursework.exception.RoomNotEmptyException;
import com.example.coursework.model.ErrorResponse;
import com.example.coursework.model.Room;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final RoomDAO roomDAO = RoomDAO.getInstance();

    @Context
    private UriInfo uriInfo;

    @GET
    public Response getAllRooms() {
        List<Room> rooms = roomDAO.findAll();
        return Response.ok(rooms).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        return Response.ok(room).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null) {
            ErrorResponse err = new ErrorResponse("Bad Request", 400, "Request body must not be empty.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        String clientId = room.getId();
        if (clientId == null || clientId.trim().isEmpty()) {
            ErrorResponse err = new ErrorResponse("Bad Request", 400, "Field 'id' is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        if (roomDAO.findById(clientId).isPresent()) {
            throw new DuplicateResourceException("Room", clientId);
        }

        Room created = roomDAO.create(room);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();

        return Response.created(location).entity(created).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = roomDAO.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        // Business rule: block deletion if sensors are still assigned
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        roomDAO.delete(roomId);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", "Room deleted successfully.");
        body.put("id", roomId);
        return Response.ok(body).build();
    }
}
