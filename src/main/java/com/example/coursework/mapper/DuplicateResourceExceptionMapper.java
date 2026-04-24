package com.example.coursework.mapper;

import com.example.coursework.exception.DuplicateResourceException;
import com.example.coursework.model.ErrorResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DuplicateResourceExceptionMapper implements ExceptionMapper<DuplicateResourceException> {

    @Override
    public Response toResponse(DuplicateResourceException ex) {
        ErrorResponse error = new ErrorResponse(
                "Conflict",
                409,
                ex.getMessage()
        );
        return Response.status(Response.Status.CONFLICT)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
