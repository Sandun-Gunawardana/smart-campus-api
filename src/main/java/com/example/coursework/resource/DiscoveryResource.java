package com.example.coursework.resource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @Context
    private UriInfo uriInfo;

    @GET
    public Response discover() {
        URI base = uriInfo.getBaseUri();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", "Smart Campus Sensor & Room Management API");
        metadata.put("version", "1.0.0");
        metadata.put("description",
                "RESTful API for managing rooms, sensors, and historical readings across the university campus.");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name", "Campus IT Department");
        contact.put("email", "it-support@campus.ac.uk");
        metadata.put("contact", contact);

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", base + "rooms");
        resources.put("sensors", base + "sensors");
        metadata.put("resources", resources);

        return Response.ok(metadata).build();
    }
}
