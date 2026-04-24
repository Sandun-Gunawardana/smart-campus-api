# Smart Campus Sensor & Room Management API

**Student:** Sandun Arjuna Gunawardana  
**Student ID:** 20240627 / w2152954  
**Module:** 5COSC022W — Client-Server Architectures  
**Coursework:** Smart Campus REST API using JAX-RS

---

## API Overview

This project implements a RESTful Smart Campus API using Java, JAX-RS (Jersey), Maven, and Apache Tomcat with in-memory data structures (`LinkedHashMap`, `ArrayList`) only — no database is used.

The API models a campus monitoring system where rooms can be created, listed, retrieved, and safely removed, while sensors are assigned to rooms and can record historical readings.

The API follows a resource-oriented design:

- `GET /api/v1` is a discovery endpoint that returns API metadata, version information, contact details, and links to the main resource collections.
- `/api/v1/rooms` manages campus rooms. Clients can create rooms, list all rooms, fetch a specific room by ID, and delete a room only when it has no sensors assigned.
- `/api/v1/sensors` manages sensors linked to existing rooms. Clients can create sensors, list all sensors, fetch a sensor by ID, delete sensors, and filter sensors by type using a query parameter such as `?type=CO2`.
- `/api/v1/sensors/{sensorId}/readings` is a nested sub-resource for sensor reading history. Clients can fetch previous readings or append a new reading. When a new reading is added, the parent sensor's `currentValue` is automatically updated.

The implementation uses JSON request and response bodies, meaningful HTTP status codes (201, 400, 403, 404, 409, 422, 500), custom exception mappers for consistent error responses, and a JAX-RS logging filter for request and response observability.

**Base URL:** `http://localhost:8080/api/v1`

---

## Build and Run

### Using NetBeans

1. Install JDK 8 (or later), Apache NetBeans, and Apache Tomcat 9.
2. Open NetBeans and go to `File > Open Project`.
3. Select the project folder.
4. Ensure Tomcat 9 is configured under `Tools > Servers`.
5. Right-click the project and select `Clean and Build`.
6. Right-click the project and select `Run`.
7. Open `http://localhost:8080/api/v1` in a browser or Postman.

### Using Maven and Tomcat manually

1. Install JDK 8+, Maven, and Apache Tomcat 9.
2. Run `mvn clean package` in the project root.
3. Copy `target/Coursework-1.0-SNAPSHOT.war` into the Tomcat `webapps` folder.
4. Start Tomcat.
5. Open `http://localhost:8080/api/v1`.

---

## Sample curl Commands

```bash
# 1. Discovery endpoint
curl -i -X GET http://localhost:8080/api/v1

# 2. List all rooms
curl -i -X GET http://localhost:8080/api/v1/rooms

# 3. Create a new room
curl -i -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"RM-D401","name":"Design Studio","capacity":35}'

# 4. Get a specific room
curl -i -X GET http://localhost:8080/api/v1/rooms/RM-A101

# 5. Create a sensor linked to an existing room
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"SNR-HUM-01","type":"Humidity","status":"ACTIVE","currentValue":0,"roomId":"RM-D401"}'

# 6. Filter sensors by type
curl -i -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"

# 7. Post a reading to a sensor
curl -i -X POST http://localhost:8080/api/v1/sensors/SNR-TMP-01/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'

# 8. Get reading history for a sensor
curl -i -X GET http://localhost:8080/api/v1/sensors/SNR-TMP-01/readings

# 9. Delete a room (will fail with 409 if sensors are assigned)
curl -i -X DELETE http://localhost:8080/api/v1/rooms/RM-A101

# 10. Trigger global 500 error (safety net demo)
curl -i -X GET http://localhost:8080/api/v1/debug/error
```

---

## Conceptual Report

### Part 1 — Service Architecture & Discovery

#### Q1: Default lifecycle of a JAX-RS resource class and its impact on in-memory data

By default, a JAX-RS resource class annotated with `@Path` follows a **per-request lifecycle**: the JAX-RS runtime (in this case Jersey) creates a **new instance of the resource class for every incoming HTTP request** and discards it once the response has been sent. This design ensures that request-scoped state such as `@Context`-injected objects like `UriInfo` or `HttpHeaders` cannot leak between concurrent callers, which would be both a correctness and a security problem.

The alternative is to annotate the resource with `@Singleton`, which instructs the container to create a single shared instance that serves every request for the lifetime of the deployment. While this reduces object allocation overhead, it introduces a strict requirement: every mutable field must be thread-safe, and per-request injected objects cannot be stored as instance fields unless they are proxied.

In this project, the per-request lifecycle is the correct choice because the resource classes themselves hold no mutable state. They delegate all data access to DAO singletons (`RoomDAO`, `SensorDAO`, `SensorReadingDAO`), which in turn point at the single `InMemoryDataStore` instance. Since multiple request-scoped resource instances access the same shared data store concurrently, the data structures must be chosen carefully:

- `LinkedHashMap` is used for rooms, sensors, and the outer readings map. It preserves insertion order for predictable JSON output when listing collections.
- `ArrayList` backs each per-sensor readings list, which is appropriate given the low write frequency relative to reads.
- Compound operations that span a read-then-write sequence (for example, creating a sensor and then adding its ID to the parent room's `sensorIds` list) are sensitive to race conditions in a high-concurrency environment. In a production system, these would be wrapped in `ConcurrentHashMap.compute()` or protected by explicit synchronisation. The current implementation accepts this trade-off as a documented limitation of the mock persistence layer.

In summary, **per-request resources combined with thread-aware storage singletons** is the standard JAX-RS architecture. It isolates per-request concerns in the resource class and centralises concurrency concerns in the data tier.

#### Q2: Why HATEOAS is a hallmark of advanced RESTful design

HATEOAS (Hypermedia as the Engine of Application State) occupies the highest tier of the Richardson Maturity Model and is the defining characteristic that separates a genuine REST API from a simple "JSON-over-HTTP" service. In a HATEOAS-compliant API, every response is self-describing: it includes typed links that advertise the valid next-step transitions, allowing the client to navigate the API dynamically rather than relying on hard-coded URLs.

The `GET /api/v1` discovery endpoint in this project demonstrates this principle by returning a `resources` map that links to the rooms and sensors collections.

The advantages over static documentation are significant:

- **Decoupled evolution.** The server can restructure URLs (for example, moving from `/api/v1` to `/api/v2` or deploying behind an API gateway) without breaking clients, because clients follow links rather than embedding paths.
- **Discoverability.** A developer can point Postman at the root endpoint and walk the entire API graph without consulting any external documentation, which aligns with Roy Fielding's original argument that REST APIs should be consumable without out-of-band knowledge.
- **State-driven navigation.** If a resource enters a state that forbids certain operations (for example, a sensor in MAINTENANCE mode), the server can omit the relevant action link from the response. The client does not need to know the business rule; the absence of the link communicates the constraint.
- **Reduced client fragility.** Clients that follow links rather than constructing URLs are inherently more resilient to server-side changes, reducing maintenance costs on both sides.

Static documentation is fundamentally a snapshot of yesterday's API; HATEOAS provides a live, machine-readable contract that evolves with the service.

---

### Part 2 — Room Management

#### Q1: Returning full objects vs. IDs only when listing rooms

When `GET /rooms` returns the **full Room object** for every entry — as implemented here — the client receives all necessary data to render a dashboard in a single round-trip. This minimises total latency, which is especially important on high-latency mobile networks where each additional HTTP request can add 100–300ms. It also simplifies the client code: one call, one parse, one render.

The cost is **bandwidth and payload size**. Every room ships with every field, including `sensorIds` and `capacity`, even if the client only needs `id` and `name`. For a campus with thousands of rooms, this overhead becomes measurable.

The **ID-only approach** (returning just an array of ID strings) inverts the trade-off. The collection payload shrinks dramatically, but it forces an **N+1 request pattern**: the client must issue a separate `GET /rooms/{id}` for every room it wants to display, multiplying both server load and perceived latency.

The industry-standard middle ground is to return a **summary representation** with minimal fields plus a `self` link for each entry, letting the client fetch full detail on demand. An alternative is field selection via query parameters (e.g., `?fields=id,name`).

For this project's small, seed-sized data set, returning full objects is the pragmatic choice: it trades negligible extra bandwidth for a much simpler client and fewer requests.

#### Q2: Is DELETE idempotent in this implementation?

**Yes.** `DELETE /rooms/{roomId}` is idempotent in the strict HTTP sense. Idempotent does *not* mean "returns the same status code every time"; it means "the observable server state after N identical requests is indistinguishable from the state after one" (RFC 9110 §9.2.2).

Consider two consecutive calls to `DELETE /rooms/RM-D401`:

| Call | Precondition | Action | Post-state | Response |
|------|-------------|--------|------------|----------|
| 1st | Room exists, no sensors | `roomDAO.delete("RM-D401")` | Room absent | 200 OK |
| 2nd | Room already absent | `findById` returns empty, throws `ResourceNotFoundException` | Room still absent | 404 Not Found |

After both calls, the server holds exactly the same state: `RM-D401` does not exist. The differing status codes are orthogonal — idempotency is a property of state, not of response symmetry.

The business-rule branch reinforces this guarantee: if a room has linked sensors, every `DELETE` attempt throws `RoomNotEmptyException` and returns 409 Conflict with zero state change. The request can be repeated indefinitely with the same result until the sensors are removed. This is a form of *safe failure*, which is itself idempotent.

---

### Part 3 — Sensor Operations & Integrity

#### Q1: Consequences of a Content-Type mismatch with @Consumes(APPLICATION_JSON)

Every `POST` endpoint in this project is annotated `@Consumes(MediaType.APPLICATION_JSON)`. This annotation participates directly in JAX-RS's content-negotiation algorithm (JAX-RS 2.1 spec §3.7).

When a client sends a request with a `Content-Type` that does not match the declared `@Consumes` type — for example `text/plain` or `application/xml` — the JAX-RS runtime takes the following path:

1. Jersey inspects the incoming `Content-Type` header.
2. It searches the resource method table for a method at the matching path whose `@Consumes` covers that media type.
3. If no match exists, the request is short-circuited **before any user code runs**.
4. The client receives **HTTP 415 Unsupported Media Type**.

This is valuable for three reasons:

- **Early rejection.** Malformed or wrong-format payloads never reach the `MessageBodyReader` pipeline, so there is zero risk of a partially-parsed object being persisted.
- **Correct error semantics.** The client receives the precise status code that RFC 9110 §15.5.16 designates for this condition, making the error diagnostically clear.
- **Security.** Attackers cannot smuggle non-JSON payloads into JSON-only endpoints to exploit downstream parsers, because the request is rejected before any parser is selected.

A symmetric mechanism governs responses: `@Produces` is matched against the client's `Accept` header, returning 406 Not Acceptable if no suitable representation exists.

#### Q2: @QueryParam filtering vs. path-based filtering

Both approaches are technically workable, but `@QueryParam` is the semantically correct design for filtering a collection. The reasoning is grounded in the REST resource model:

- **`/sensors` is a collection resource.** The canonical URL identifies the set of all sensors. A query string is a filter applied to that set — it does not create a new resource, it narrows the view. Bookmarking `/sensors?type=CO2` and `/sensors?type=Temperature` correctly represents two different views of the same collection.
- **`/sensors/type/CO2` invents a fictitious sub-resource.** There is no individual "type CO2" entity in the domain to `GET`, `PUT`, or `DELETE`. URL paths should mirror the ownership graph of the domain, not its filterable attributes.
- **Composability.** Query strings support multiple orthogonal filters without combinatorial explosion: `/sensors?type=CO2&status=ACTIVE&page=2`. A path-based design would require `/sensors/type/CO2/status/ACTIVE/page/2`, which is fragile, order-sensitive, and verbose.
- **Caching.** HTTP caches treat the query string as part of the cache key by default, so different filter combinations are cached independently — exactly the intended behaviour.
- **Server-side simplicity.** A single `@GET` method with optional `@QueryParam` bindings handles every filter permutation; the path-based equivalent requires either regex-heavy `@Path` expressions or multiple methods.

This is why every major REST API (GitHub, Stripe, AWS) reserves the URL path for identity and the query string for selection.

---

### Part 4 — Sub-Resources

#### Q1: Architectural benefits of the Sub-Resource Locator pattern

A sub-resource locator is a method on a parent resource annotated with `@Path` but no HTTP-method annotation, whose return value is an instance that JAX-RS dispatches the remainder of the request against. In this project, `SensorResource.getReadingsSubResource(sensorId)` returns a `SensorReadingResource` instance.

The pattern delivers several engineering benefits over placing all endpoints in a single controller:

- **Separation of concerns.** The class responsible for `/sensors/{id}/readings/*` is physically separate from the class responsible for `/sensors/*`. This aligns with the Single Responsibility Principle: `SensorReadingResource` knows only how to manage readings, and its lifecycle is entirely scoped to a specific sensor context.
- **Contextual parameter capture.** The `sensorId` path parameter is captured once by the locator and stored as a final field in the sub-resource. Every method handler inside `SensorReadingResource` has access to it without re-declaring `@PathParam("sensorId")`, eliminating a common source of copy-paste bugs.
- **Business-rule gating at the boundary.** The locator is the natural place to verify that the parent sensor exists. Our implementation throws `ResourceNotFoundException` before Jersey even dispatches to the sub-resource, giving every nested URL a uniform 404 for unknown sensors with a single line of code.
- **Pluggable sub-graphs.** Adding a new nested resource (for example, `/sensors/{id}/alerts`) requires only a new locator method and a new class — zero changes to the existing readings code. Monolithic controllers tend to grow into large, difficult-to-maintain classes; sub-resource locators scale horizontally by composition.
- **Testability.** `SensorReadingResource` can be unit-tested in isolation by instantiating it with a test `sensorId`, without requiring a full JAX-RS container or path-parameter injection mocking.

---

### Part 5 — Error Handling & Observability

#### Q1: Why HTTP 422 is more semantically accurate than 404 for payload-reference failures

Both 404 and 422 communicate that something requested is unavailable, but they differ fundamentally in where the problem lies:

- **404 Not Found** signals that the target URI does not identify a resource. It is a property of the request line (method + URL). The client should interpret this as "this address does not exist."
- **422 Unprocessable Entity** (RFC 4918 §11.2) signals that the request line is correct, the Content-Type is accepted, the body is syntactically valid JSON, but the server cannot process it because of a **semantic error in the payload**. The client should interpret this as "fix your request body and retry — do not change the URL."

In the `POST /sensors` case with an unknown `roomId`:

- The endpoint `/sensors` exists.
- The `Content-Type: application/json` is accepted.
- The JSON body deserialises into a valid `Sensor` POJO.

The only failure is a referential integrity check: the `roomId` field references a room that does not exist. This is a semantic problem with the payload, not with the URL, which is precisely the condition 422 was designed for. Returning 404 would be misleading because the client's developer would spend time inspecting the URL and routing configuration before realising the actual error is one field in the request body.

#### Q2: Cybersecurity risks of exposing Java stack traces to external consumers

Exposing raw Java stack traces in HTTP response bodies is a widely recognised information-disclosure vulnerability, listed under OWASP Top Ten A05:2021 (Security Misconfiguration). Each element of a stack trace leaks intelligence that an attacker can exploit:

| Leaked artefact | What an attacker learns |
|----------------|------------------------|
| Exception class name (e.g., `NullPointerException`) | Which input field is unvalidated and crash-prone; enables targeted fuzzing |
| Fully-qualified class names (e.g., `com.example.coursework.dao.RoomDAO`) | Internal package structure and naming conventions — valuable for reconnaissance |
| File paths in frames (e.g., `RoomDAO.java:37`) | Filesystem layout hints that aid path-traversal or local-file-inclusion attacks |
| Library and framework versions (e.g., `org.glassfish.jersey.server.*`) | Specific CVE lookup targets for known vulnerabilities in those exact versions |
| SQL fragments or ORM messages | Schema names, column names, and partial query templates — fuel for SQL injection |
| Internal hostnames or IP addresses | Network topology information supporting lateral movement |

The `GlobalExceptionMapper<Throwable>` in this project mitigates this by splitting the communication channel: the **client** receives only a generic error message with no internal details, while the **server log** receives the full stack trace via `Logger.log(Level.SEVERE, ...)` for operator debugging. This is the standard defence-in-depth pattern: full diagnostics for the people who own the server, zero leakage to external consumers.

#### Q3: Why JAX-RS filters are superior to manual Logger.info() calls

Placing `LOGGER.info("entering method X")` at the top of every resource method is the naive approach to request logging. It is also architecturally wrong for several reasons:

- **Cross-cutting concerns deserve cross-cutting abstractions.** Logging is orthogonal to business logic — it is the textbook case of scattering and tangling that filter chains and AOP were designed to solve. A `ContainerRequestFilter` + `ContainerResponseFilter` is the JAX-RS-native mechanism for this.
- **Uniformity by construction.** With a single `RequestResponseLoggingFilter`, it is impossible to forget to log a request, impossible to log the wrong URI, and impossible for different developers to format log lines inconsistently. A codebase with 30 manual `LOGGER.info()` calls has 30 opportunities for drift and omission.
- **Access to the full HTTP envelope.** The filter receives `ContainerRequestContext` (method, URI, headers) and `ContainerResponseContext` (status code, response headers). A manual logger inside a resource method has no access to the final status code because it does not know what the response will be at the start of the method.
- **Elapsed time measurement.** The filter stores `System.currentTimeMillis()` as a request property on entry and computes elapsed time on exit, producing per-request latency figures in a single implementation — the foundation for observability metrics. This is impossible with method-top-only logging.
- **Pipeline composition.** Multiple `@Provider` filters can be chained and ordered with `@Priority`. Adding authentication, CORS, or rate-limiting filters requires zero changes to any resource method.
- **Testability.** Business-logic methods remain pure (input to output) and can be unit-tested without mocking a logger. The logging concern can be toggled or reconfigured independently.

In summary, the filter embodies the Open/Closed Principle at the HTTP boundary: new cross-cutting behaviour is added by extension (registering another `@Provider`), never by modification of existing resource code.
