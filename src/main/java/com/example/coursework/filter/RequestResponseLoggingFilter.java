package com.example.coursework.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class RequestResponseLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(RequestResponseLoggingFilter.class.getName());

    private static final String START_TIME = "request-start-time";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Store start time so we can compute elapsed time in the response filter
        requestContext.setProperty(START_TIME, System.currentTimeMillis());

        LOGGER.info(String.format("[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        Object startTime = requestContext.getProperty(START_TIME);
        String elapsed = (startTime instanceof Long)
                ? (System.currentTimeMillis() - (Long) startTime) + " ms"
                : "unknown";

        LOGGER.info(String.format("[RESPONSE] %s %s -> %d (%s)",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus(),
                elapsed));
    }
}
