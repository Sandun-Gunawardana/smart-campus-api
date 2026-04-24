package com.example.coursework.mapper;

import com.example.coursework.exception.SensorInMaintenanceException;
import com.example.coursework.model.ErrorResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SensorInMaintenanceExceptionMapper implements ExceptionMapper<SensorInMaintenanceException> {

    @Override
    public Response toResponse(SensorInMaintenanceException ex) {
        ErrorResponse error = new ErrorResponse(
                "Forbidden",
                403,
                ex.getMessage()
        );
        return Response.status(Response.Status.FORBIDDEN)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
