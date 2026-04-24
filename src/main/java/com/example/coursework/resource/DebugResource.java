package com.example.coursework.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    // Deliberately throws a RuntimeException so the GlobalExceptionMapper can be demonstrated
    @GET
    @Path("/error")
    public Response triggerError() {
        throw new RuntimeException("Deliberate failure to test global safety-net mapper.");
    }
}
