package com.example.coursework.resource;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.example.coursework.dao.SensorDAO;
import com.example.coursework.dao.SensorReadingDAO;
import com.example.coursework.exception.ResourceNotFoundException;
import com.example.coursework.exception.SensorInMaintenanceException;
import com.example.coursework.model.ErrorResponse;
import com.example.coursework.model.Sensor;
import com.example.coursework.model.SensorReading;

// No class-level @Path — this is mounted via the sub-resource locator in SensorResource
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final SensorReadingDAO readingDAO;
    private final SensorDAO sensorDAO = SensorDAO.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
        this.readingDAO = new SensorReadingDAO(sensorId);
    }

    @GET
    public Response getAllReadings() {
        List<SensorReading> readings = readingDAO.findAll();
        return Response.ok(readings).build();
    }

    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        SensorReading reading = readingDAO.findById(readingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading (on sensor " + sensorId + ")", readingId));
        return Response.ok(reading).build();
    }

    @POST
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        if (reading == null) {
            ErrorResponse err = new ErrorResponse("Bad Request", 400, "Request body must not be empty.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        Sensor sensor = sensorDAO.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));

        // Part 5.3 — reject writes when the sensor is under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorInMaintenanceException(sensorId);
        }

        // Auto-generate ID and default timestamp if not supplied
        reading.setId("rdg-" + UUID.randomUUID().toString().substring(0, 8));
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        SensorReading created = readingDAO.create(reading);

        // Side-effect: update the parent sensor's currentValue (Part 4.2)
        sensor.setCurrentValue(created.getValue());
        sensorDAO.update(sensor);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId())
                .build();

        return Response.created(location).entity(created).build();
    }
}
