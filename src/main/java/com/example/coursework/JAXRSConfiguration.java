package com.example.coursework;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Configures JAX-RS for the application.
 * Sets the base URI for all REST resources.
 */
@ApplicationPath("api/v1")
public class JAXRSConfiguration extends Application {

}
