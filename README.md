# Booking API - Complete Documentation

**Version:** 1.0.0  
**Base URL:** `http://localhost:8080/api`  
**Documentation:** Swagger UI, OpenAPI JSON  

---

## Table of Contents

1. [Overview](#1-overview)
2. [Quick Start](#2-quick-start)
3. [Architecture](#3-architecture)
4. [Authentication & Authorization](#4-authentication--authorization)
5. [API Reference - Resources](#5-api-reference---resources)
6. [API Reference - Bookings](#6-api-reference---bookings)
7. [API Reference - Availability](#7-api-reference---availability)
8. [Error Handling](#8-error-handling)
9. [Data Models](#9-data-models)
10. [Business Rules](#10-business-rules)
11. [Concurrency Handling](#11-concurrency-handling)
12. [Configuration](#12-configuration)
13. [Project Structure](#13-project-structure)
14. [Testing](#14-testing)
15. [Deployment](#15-deployment)
16. [Troubleshooting](#16-troubleshooting)
17. [FAQ](#17-faq)
18. [Examples](#18-examples)

---

## 1. Overview

### 1.1 Description

Booking API is a RESTful service for scheduling limited resources at specific times. The system prevents time slot conflicts, handles concurrent requests, and supports asynchronous notifications.

### 1.2 Use Cases

| Use Case | Description |
|----------|-------------|
| Meeting Room Booking | Reserve conference rooms with capacity limits |
| Medical Appointments | Schedule doctor consultations |
| Equipment Rental | Rent projectors, cameras, vehicles |
| Sports Facilities | Book courts, fields, courts |
| Parking Slots | Reserve parking spaces |
| Vehicle Rental | Car/bike rental scheduling |

### 1.3 Key Features

| Feature | Implementation |
|---------|----------------|
| Time Slot Validation | Prevents overlapping bookings |
| Conflict Detection | Pessimistic + Optimistic locking |
| Async Notifications | Spring Events with @Async |
| Role-based Access | Basic Auth with USER/ADMIN roles |
| OpenAPI Documentation | Swagger UI integration |
| In-memory Database | H2 for development |
| Health Monitoring | Spring Actuator endpoints |

### 1.4 Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.2.5 |
| Language | Java | 17 |
| Database | H2 (dev) | - |
| ORM | Spring Data JPA | - |
| Security | Spring Security | - |
| Documentation | springdoc-openapi | 2.5.0 |
| Build Tool | Maven | - |

### 1.5 System Requirements

| Requirement | Specification |
|-------------|---------------|
| JDK | 17 or higher |
| Memory | 512MB minimum |
| Disk | 100MB for application |
| Network | Port 8080 available |

---

## 2. Quick Start

### 2.1 Prerequisites

```bash
# Check Java version
java -version
# Should show: java version "17.x.x"

# Check Maven (optional, can use mvnw wrapper)
mvn -version
```

### 2.2 Running the Application

```bash
# Option 1: Using Maven wrapper
cd booking-api
./mvnw spring-boot:run

# Option 2: Using Maven
mvn spring-boot:run

# Option 3: Running packaged JAR
./mvnw clean package
java -jar target/booking-api-1.0.0.jar
```

### 2.3 Verify Installation

```bash
# Health check
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

### 2.4 First API Call

```bash
# Login as admin and create a resource
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Conference Room A",
    "capacity": 10,
    "location": "Floor 1"
  }'

# Expected response: 201 Created with resource details
```

---

## 3. Architecture

### 3.1 High-Level Architecture

```
                    ┌─────────────────────────────────────┐
                    │           API Gateway               │
                    │      (Spring Security Filter)       │
                    └──────────────────┬──────────────────┘
                                       │
                    ┌──────────────────▼──────────────────┐
                    │         REST Controllers            │
                    │  (Resource, Booking, Availability)  │
                    └──────────────────┬──────────────────┘
                                       │
                    ┌──────────────────▼──────────────────┐
                    │          Service Layer              │
                    │    (Business Logic + Validation)     │
                    └──────────────────┬──────────────────┘
                                       │
          ┌────────────────────────────┼────────────────────────────┐
          │                            │                            │
┌─────────▼─────────┐    ┌────────────▼────────────┐    ┌─────────▼─────────┐
│   Event Publisher │    │      Repository        │    │   Async Tasks      │
│   (Booking Events)│    │   (Spring Data JPA)     │    │ (Notifications)    │
└───────────────────┘    └────────────┬────────────┘    └───────────────────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │      H2 Database               │
                    │   (In-Memory, DDL Auto)         │
                    └────────────────────────────────┘
```

### 3.2 Request Flow

```
1. Client sends request with Basic Auth header
2. SecurityFilterChain validates credentials
3. Controller receives request, validates input
4. Service layer applies business rules
5. Repository persists data (with locking)
6. Event published for async notification
7. Response returned to client
```

### 3.3 Package Structure

```
com.template.booking
├── config/           # Configuration classes
├── controller/       # REST endpoints
├── dto/              # Data transfer objects
├── event/            # Application events
├── exception/        # Custom exceptions + handler
├── model/            # JPA entities
├── repository/       # Data access layer
└── service/          # Business logic
```

### 3.4 Database Schema

```
┌─────────────────┐       ┌─────────────────┐
│    resources    │       │    bookings     │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)         │
│ name            │       │ user_id         │
│ description     │       │ resource_id(FK) │
│ capacity        │◄──────│ start_time      │
│ location        │       │ end_time       │
│ status          │       │ status         │
│ created_at      │       │ reason         │
└─────────────────┘       │ created_at     │
                          │ updated_at     │
                          │ version        │
                          └─────────────────┘
```

---

## 4. Authentication & Authorization

### 4.1 Authentication Method

The API uses HTTP Basic Authentication with in-memory user credentials.

```
Authorization: Basic <base64(username:password)>
```

### 4.2 Default Users

| Username | Password | Roles | Permissions |
|----------|----------|-------|-------------|
| user | password | ROLE_USER | View resources, Create/Cancel bookings, View own bookings |
| admin | admin123 | ROLE_USER, ROLE_ADMIN | All USER permissions + Create resources, Update status |

### 4.3 Generating Auth Header

```bash
# Linux/Mac
echo -n "user:password" | base64
# Output: dXNlcjpwYXNzd29yZA==

# Windows PowerShell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("user:password"))

# Python
import base64
base64.b64encode(b"user:password").decode()
```

### 4.4 Auth Header Examples

```bash
# USER access
-H "Authorization: Basic dXNlcjpwYXNzd29yZA=="

# ADMIN access
-H "Authorization: Basic YWRtaW46YWRtaW4xMjM="
```

### 4.5 Endpoint Authorization Matrix

| Endpoint | Method | USER | ADMIN |
|----------|--------|------|-------|
| /api/resources | POST | NO | YES |
| /api/resources | GET | YES | YES |
| /api/resources/{id} | GET | YES | YES |
| /api/resources/{id}/status | PATCH | NO | YES |
| /api/bookings | POST | YES | YES |
| /api/bookings/{id} | GET | YES | YES |
| /api/bookings/my | GET | YES | YES |
| /api/bookings/{id} | DELETE | YES* | YES |
| /api/availability/** | GET | YES | YES |

*USER can only cancel their own bookings

---

## 5. API Reference - Resources

### 5.1 Endpoints Summary

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /api/resources | Create resource | ADMIN |
| GET | /api/resources | List all resources | USER |
| GET | /api/resources/active | List active resources | USER |
| GET | /api/resources/{id} | Get resource by ID | USER |
| PATCH | /api/resources/{id}/status | Update status | ADMIN |

---

### POST /api/resources

**Description:** Creates a new resource in the system.

**Authorization:** ADMIN only

**Request:**
```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Meeting Room Alpha",
    "description": "Modern meeting room with video conferencing",
    "capacity": 12,
    "location": "Building A, Floor 3, Room 301"
  }'
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "name": "Meeting Room Alpha",
  "description": "Modern meeting room with video conferencing",
  "capacity": 12,
  "location": "Building A, Floor 3, Room 301",
  "status": "ACTIVE",
  "createdAt": "2026-05-27T10:00:00"
}
```

**Response Headers:**
```
HTTP/1.1 201 Created
Location: /api/resources/1
Content-Type: application/json
```

**Error Responses:**

`400 Bad Request` - Validation error
```json
{
  "timestamp": "2026-05-27T10:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/resources",
  "fieldErrors": [
    {"field": "name", "message": "Name is required"}
  ]
}
```

`401 Unauthorized` - Missing credentials
```json
{"status": 401, "error": "Unauthorized"}
```

`403 Forbidden` - Not ADMIN
```json
{"status": 403, "error": "Forbidden"}
```

---

### GET /api/resources

**Description:** Returns all resources in the system.

**Authorization:** USER, ADMIN

**Request:**
```bash
curl http://localhost:8080/api/resources \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "name": "Meeting Room Alpha",
    "description": "Modern meeting room",
    "capacity": 12,
    "location": "Floor 3",
    "status": "ACTIVE"
  },
  {
    "id": 2,
    "name": "Auditorium",
    "description": "Main auditorium with 200 seats",
    "capacity": 200,
    "location": "Ground Floor",
    "status": "ACTIVE"
  },
  {
    "id": 3,
    "name": "Training Room",
    "description": "Room for training sessions",
    "capacity": 30,
    "location": "Floor 2",
    "status": "INACTIVE"
  }
]
```

**Empty Response `200 OK`:**
```json
[]
```

---

### GET /api/resources/active

**Description:** Returns only active resources available for booking.

**Authorization:** USER, ADMIN

**Request:**
```bash
curl http://localhost:8080/api/resources/active \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "name": "Meeting Room Alpha",
    "status": "ACTIVE"
  },
  {
    "id": 2,
    "name": "Auditorium",
    "status": "ACTIVE"
  }
]
```

---

### GET /api/resources/{id}

**Description:** Returns details of a specific resource.

**Authorization:** USER, ADMIN

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | Resource ID |

**Request:**
```bash
curl http://localhost:8080/api/resources/1 \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "name": "Meeting Room Alpha",
  "description": "Modern meeting room with video conferencing",
  "capacity": 12,
  "location": "Building A, Floor 3, Room 301",
  "status": "ACTIVE",
  "createdAt": "2026-05-27T10:00:00"
}
```

**Response `404 Not Found`:**
```json
{
  "timestamp": "2026-05-27T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found: 999",
  "path": "/api/resources/999"
}
```

---

### PATCH /api/resources/{id}/status

**Description:** Updates the status of a resource (ACTIVE/INACTIVE).

**Authorization:** ADMIN only

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | Resource ID |

**Query Parameters:**

| Parameter | Type | Required | Values |
|-----------|------|----------|--------|
| status | string | Yes | ACTIVE, INACTIVE |

**Request:**
```bash
curl -X PATCH "http://localhost:8080/api/resources/1/status?status=INACTIVE" \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM="
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "name": "Meeting Room Alpha",
  "description": "Modern meeting room",
  "capacity": 12,
  "location": "Floor 3",
  "status": "INACTIVE",
  "createdAt": "2026-05-27T10:00:00"
}
```

**Response `404 Not Found`:**
```json
{
  "status": 404,
  "message": "Resource not found: 999"
}
```

**Response `400 Bad Request`:**
```json
{
  "status": 400,
  "message": "Invalid status value. Use ACTIVE or INACTIVE"
}
```

---

## 6. API Reference - Bookings

### 6.1 Endpoints Summary

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /api/bookings | Create booking | USER |
| GET | /api/bookings/{id} | Get booking by ID | USER |
| GET | /api/bookings/my | Get user's bookings | USER |
| DELETE | /api/bookings/{id} | Cancel booking | USER/ADMIN |

---

### POST /api/bookings

**Description:** Creates a new booking for a resource. The system validates time conflicts and business rules.

**Authorization:** USER, ADMIN

**Business Rules:**
- Start time must be in the future
- Start time must be before end time
- Minimum duration: 30 minutes
- No overlap with existing CONFIRMED bookings
- Resource must be ACTIVE

**Request:**
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-05-28T10:00:00",
    "endTime": "2026-05-28T11:00:00",
    "reason": "Weekly team standup meeting"
  }'
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "userId": "user",
  "resourceId": 1,
  "resourceName": "Meeting Room Alpha",
  "startTime": "2026-05-28T10:00:00",
  "endTime": "2026-05-28T11:00:00",
  "status": "CONFIRMED",
  "reason": "Weekly team standup meeting",
  "createdAt": "2026-05-27T15:30:00"
}
```

**Response Headers:**
```
HTTP/1.1 201 Created
Location: /api/bookings/1
Content-Type: application/json
```

**Error Response `409 Conflict` - Time Slot Overlap:**
```json
{
  "timestamp": "2026-05-27T15:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Time slot conflict: resource 1 is already booked from 2026-05-28T10:00 to 2026-05-28T12:00",
  "path": "/api/bookings"
}
```

**Error Response `422 Unprocessable Entity` - Validation Failed:**
```json
{
  "timestamp": "2026-05-27T15:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Start time must be before end time",
  "path": "/api/bookings"
}
```

**Error Response `404 Not Found` - Resource Not Found:**
```json
{
  "status": 404,
  "message": "Resource not found: 999"
}
```

**Error Response `422 Unprocessable Entity` - Resource Inactive:**
```json
{
  "status": 422,
  "message": "Resource is not active: 1"
}
```

**Request Body Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| resourceId | long | Yes | ID of the resource to book |
| startTime | datetime | Yes | Booking start time (ISO 8601) |
| endTime | datetime | Yes | Booking end time (ISO 8601) |
| reason | string | No | Reason/purpose of booking |

---

### GET /api/bookings/{id}

**Description:** Returns details of a specific booking.

**Authorization:** USER, ADMIN

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | Booking ID |

**Request:**
```bash
curl http://localhost:8080/api/bookings/1 \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "userId": "user",
  "resourceId": 1,
  "resourceName": "Meeting Room Alpha",
  "startTime": "2026-05-28T10:00:00",
  "endTime": "2026-05-28T11:00:00",
  "status": "CONFIRMED",
  "reason": "Weekly team standup meeting",
  "createdAt": "2026-05-27T15:30:00"
}
```

**Response `404 Not Found`:**
```json
{
  "status": 404,
  "message": "Booking not found: 999"
}
```

---

### GET /api/bookings/my

**Description:** Returns all bookings for the authenticated user with CONFIRMED status.

**Authorization:** USER, ADMIN

**Request:**
```bash
curl http://localhost:8080/api/bookings/my \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "userId": "user",
    "resourceId": 1,
    "resourceName": "Meeting Room Alpha",
    "startTime": "2026-05-28T10:00:00",
    "endTime": "2026-05-28T11:00:00",
    "status": "CONFIRMED",
    "reason": "Weekly team standup",
    "createdAt": "2026-05-27T15:30:00"
  },
  {
    "id": 3,
    "userId": "user",
    "resourceId": 2,
    "resourceName": "Auditorium",
    "startTime": "2026-05-29T14:00:00",
    "endTime": "2026-05-29T16:00:00",
    "status": "CONFIRMED",
    "reason": "All-hands meeting",
    "createdAt": "2026-05-27T16:00:00"
  }
]
```

**Empty Response `200 OK`:**
```json
[]
```

---

### DELETE /api/bookings/{id}

**Description:** Cancels an existing booking.

**Authorization:** USER (owner only) or ADMIN

**Business Rules:**
- USER can only cancel their own bookings
- ADMIN can cancel any booking
- Cannot cancel if start time has passed
- Cannot cancel within 1 hour of start time

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | Booking ID |

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/bookings/1 \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "userId": "user",
  "resourceId": 1,
  "resourceName": "Meeting Room Alpha",
  "startTime": "2026-05-28T10:00:00",
  "endTime": "2026-05-28T11:00:00",
  "status": "CANCELLED",
  "reason": "Weekly team standup meeting",
  "createdAt": "2026-05-27T15:30:00"
}
```

**Response `404 Not Found`:**
```json
{
  "status": 404,
  "message": "Booking not found: 999"
}
```

**Response `403 Forbidden` - Not Owner:**
```json
{
  "status": 403,
  "message": "You are not authorized to cancel this booking"
}
```

**Response `422 Unprocessable Entity` - Too Late:**
```json
{
  "status": 422,
  "message": "Cannot cancel booking within 1 hour(s) of start time"
}
```

**Response `422 Unprocessable Entity` - Already Started:**
```json
{
  "status": 422,
  "message": "Cannot cancel a booking that has already started"
}
```

---

## 7. API Reference - Availability

### 7.1 Endpoint Summary

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /api/availability/resource/{id} | Check availability | USER |

---

### GET /api/availability/resource/{id}

**Description:** Returns hourly time slots showing availability for a resource on a specific date.

**Authorization:** USER, ADMIN

**Default Business Hours:** 08:00 - 18:00 (10 slots of 1 hour each)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | long | Resource ID |

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| date | string | Yes | Date in YYYY-MM-DD format |

**Request:**
```bash
curl "http://localhost:8080/api/availability/resource/1?date=2026-05-28" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```

**Response `200 OK`:**
```json
[
  {"startTime": "2026-05-28T08:00:00", "endTime": "2026-05-28T09:00:00", "available": true, "reason": null},
  {"startTime": "2026-05-28T09:00:00", "endTime": "2026-05-28T10:00:00", "available": true, "reason": null},
  {"startTime": "2026-05-28T10:00:00", "endTime": "2026-05-28T11:00:00", "available": false, "reason": "Already booked"},
  {"startTime": "2026-05-28T11:00:00", "endTime": "2026-05-28T12:00:00", "available": true, "reason": null},
  {"startTime": "2026-05-28T12:00:00", "endTime": "2026-05-28T13:00:00", "available": true, "reason": null},
  {"startTime": "2026-05-28T13:00:00", "endTime": "2026-05-28T14:00:00", "available": true, "reason": null},
  {"startTime": "2026-05-28T14:00:00", "endTime": "2026-05-28T15:00:00", "available": false, "reason": "Already booked"},
  {"startTime": "2026-05-28T15:00:00", "endTime": "2026-05-28T16:00:00", "available": true, "reason": null},
  {"startTime": "2026-05-28T16:00:00", "endTime": "2026-05-28T17:00:00", "available": true, "reason": null},
  {"startTime": "2026-05-28T17:00:00", "endTime": "2026-05-28T18:00:00", "available": true, "reason": null}
]
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| startTime | datetime | Slot start time |
| endTime | datetime | Slot end time |
| available | boolean | true if slot is free, false if booked |
| reason | string | null if available, "Already booked" if not |

---

## 8. Error Handling

### 8.1 HTTP Status Codes

| Code | Status | When Used |
|------|--------|-----------|
| 200 | OK | Successful GET, PATCH, DELETE |
| 201 | Created | Successful POST (resource created) |
| 400 | Bad Request | Input validation failure |
| 401 | Unauthorized | Missing or invalid credentials |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Time slot overlap detected |
| 422 | Unprocessable Entity | Business rule violation |
| 500 | Internal Server Error | Unexpected server error |

### 8.2 Error Response Structure

All error responses follow a consistent format:

```json
{
  "timestamp": "2026-05-27T15:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Time slot conflict: resource 1 is already booked...",
  "path": "/api/bookings"
}
```

| Field | Type | Description |
|-------|------|-------------|
| timestamp | datetime | When the error occurred |
| status | int | HTTP status code |
| error | string | HTTP status text |
| message | string | Human-readable error description |
| path | string | Request path that caused the error |
| fieldErrors | array | (Optional) List of validation errors |

### 8.3 Validation Error Response (400)

```json
{
  "timestamp": "2026-05-27T15:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/bookings",
  "fieldErrors": [
    {"field": "resourceId", "message": "Resource ID is required"},
    {"field": "startTime", "message": "Start time is required"},
    {"field": "endTime", "message": "End time is required"}
  ]
}
```

### 8.4 Common Error Messages

| Error Message | HTTP Code | Cause |
|---------------|-----------|-------|
| Resource not found: {id} | 404 | Resource ID doesn't exist |
| Booking not found: {id} | 404 | Booking ID doesn't exist |
| Time slot conflict: resource {id} is already booked | 409 | Overlapping booking exists |
| Start time must be before end time | 422 | Invalid time range |
| Cannot create a booking in the past | 422 | Start time is in the past |
| Booking must be at least 30 minutes long | 422 | Duration less than 30 minutes |
| Resource is not active: {id} | 422 | Resource status is INACTIVE |
| Cannot cancel a booking that has already started | 422 | Booking start time has passed |
| Cannot cancel booking within 1 hour(s) of start time | 422 | Less than 1 hour before start |
| You are not authorized to cancel this booking | 403 | Not owner and not admin |

---

## 9. Data Models

### 9.1 Resource Entity

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, Auto-generated | Unique identifier |
| name | String | NOT NULL, max 255 | Resource name |
| description | String | max 500 | Detailed description |
| capacity | Integer | nullable | Capacity (people/items) |
| location | String | nullable | Physical location |
| status | Enum | NOT NULL, default ACTIVE | ACTIVE or INACTIVE |
| createdAt | LocalDateTime | Auto-set on create | Creation timestamp |

**Status Values:**
- `ACTIVE`: Available for booking
- `INACTIVE`: Not available for booking

### 9.2 Booking Entity

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, Auto-generated | Unique identifier |
| userId | String | NOT NULL | User who made the booking |
| resource | Resource | NOT NULL, FK | Booked resource |
| startTime | LocalDateTime | NOT NULL | Booking start |
| endTime | LocalDateTime | NOT NULL | Booking end |
| status | Enum | NOT NULL, default CONFIRMED | PENDING, CONFIRMED, CANCELLED |
| reason | String | max 500 | Reason for booking |
| createdAt | LocalDateTime | Auto-set on create | Creation timestamp |
| updatedAt | LocalDateTime | Auto-set on update | Last update timestamp |
| version | Long | Auto-managed | Optimistic lock version |

**Status Values:**
- `PENDING`: Awaiting confirmation
- `CONFIRMED`: Booking confirmed
- `CANCELLED`: Booking cancelled

### 9.3 Enums Reference

**ResourceStatus:**
```java
public enum ResourceStatus {
    ACTIVE,   // Available for booking
    INACTIVE  // Not available for booking
}
```

**BookingStatus:**
```java
public enum BookingStatus {
    PENDING,    // Awaiting confirmation
    CONFIRMED,   // Booking confirmed
    CANCELLED    // Booking cancelled
}
```

### 9.4 DTOs Reference

**CreateBookingRequest:**
```json
{
  "resourceId": 1,
  "startTime": "2026-05-28T10:00:00",
  "endTime": "2026-05-28T11:00:00",
  "reason": "Team meeting"
}
```

**BookingResponse:**
```json
{
  "id": 1,
  "userId": "user",
  "resourceId": 1,
  "resourceName": "Meeting Room A",
  "startTime": "2026-05-28T10:00:00",
  "endTime": "2026-05-28T11:00:00",
  "status": "CONFIRMED",
  "reason": "Team meeting",
  "createdAt": "2026-05-27T15:30:00"
}
```

---

## 10. Business Rules

### 10.1 Booking Creation Rules

| Rule | Description | Error Code |
|------|-------------|------------|
| Future Time | Start time must be after current time | 422 |
| Valid Range | Start time must be before end time | 422 |
| Minimum Duration | Booking must be at least 30 minutes | 422 |
| Resource Active | Resource must have ACTIVE status | 422 |
| No Overlap | Cannot overlap with CONFIRMED bookings | 409 |

### 10.2 Booking Cancellation Rules

| Rule | Description | Error Code |
|------|-------------|------------|
| Ownership | USER can only cancel own bookings | 403 |
| Admin Override | ADMIN can cancel any booking | - |
| Not Started | Cannot cancel if booking already started | 422 |
| Advance Notice | Cannot cancel within 1 hour of start | 422 |

### 10.3 Time Slot Conflict Logic

A conflict occurs when:
```
(startTime < existingEndTime) AND (endTime > existingStartTime)
```

**Example Conflicts:**
```
Existing: 10:00 - 12:00

Conflicting:
  - 09:00 - 11:00  (YES, overlaps 10-11)
  - 11:00 - 13:00  (YES, overlaps 11-12)
  - 09:30 - 10:30  (YES, overlaps 10-10:30)
  - 13:00 - 15:00  (YES, overlaps 12-13)

Not Conflicting:
  - 08:00 - 10:00  (NO, ends exactly when starts)
  - 12:00 - 14:00  (NO, starts exactly when ends)
```

---

## 11. Concurrency Handling

### 11.1 Overview

The system uses two mechanisms to handle concurrent booking requests:

1. **Pessimistic Locking** - Prevents concurrent writes to same resource
2. **Optimistic Locking** - Detects and handles conflicts via version field

### 11.2 Pessimistic Locking

**Implementation:** Uses `SELECT ... FOR UPDATE` via JPA `@Lock` annotation.

**Location:** `BookingRepository.findConflictingBooking()`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
       "AND b.status != 'CANCELLED' " +
       "AND b.startTime < :endTime AND b.endTime > :startTime")
Optional<Booking> findConflictingBooking(...);
```

**Effect:** When multiple threads try to book the same slot:
1. First thread acquires lock, finds no conflict, creates booking
2. Other threads wait for lock
3. Once first transaction commits, others see the conflict and get 409

### 11.3 Optimistic Locking

**Implementation:** Uses `@Version` annotation on Booking entity.

```java
@Version
private Long version;
```

**Effect:** If two transactions somehow both create a booking before seeing each other, the `@Version` field will cause one to fail with `OptimisticLockException`.

### 11.4 Transaction Boundaries

All booking operations are wrapped in `@Transactional`:

```java
@Transactional
public BookingResponse createBooking(String userId, CreateBookingRequest request) {
    // All validation and persistence in same transaction
    // If any step fails, entire operation rolls back
}
```

---

## 12. Configuration

### 12.1 Application Configuration (application.yml)

```yaml
spring:
  application:
    name: booking-api
  datasource:
    url: jdbc:h2:mem:bookingdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        format_sql: true

server:
  port: 8080

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

logging:
  level:
    com.template.booking: DEBUG
    org.springframework.security: INFO
```

### 12.2 Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| server.port | 8080 | Server port |
| spring.datasource.url | jdbc:h2:mem:bookingdb | Database URL |
| spring.jpa.hibernate.ddl-auto | create-drop | DDL strategy |
| springdoc.api-docs.path | /api-docs | OpenAPI docs path |
| springdoc.swagger-ui.path | /swagger-ui.html | Swagger UI path |

### 12.3 Environment Variables

| Variable | Description |
|----------|-------------|
| SERVER_PORT | Override server port |
| SPRING_DATASOURCE_URL | Override database URL |

### 12.4 Customizable Business Rules

All business rules are configurable via environment variables. No code changes needed.

#### 12.4.1 Business Hours

| Variable | Default | Description |
|----------|---------|-------------|
| BOOKING_HOUR_START | 08:00 | Start of business day (24h format) |
| BOOKING_HOUR_END | 18:00 | End of business day (24h format) |

#### 12.4.2 Booking Constraints

| Variable | Default | Description |
|----------|---------|-------------|
| BOOKING_MIN_ADVANCE_HOURS | 1 | Minimum hours in advance to book |
| BOOKING_MIN_DURATION_MINUTES | 30 | Minimum booking duration |
| BOOKING_MAX_DURATION_HOURS | 8 | Maximum booking duration |
| BOOKING_ALLOW_WEEKENDS | false | Allow bookings on Saturday/Sunday |

#### 12.4.3 Cancellation Policy

| Variable | Default | Description |
|----------|---------|-------------|
| CANCELLATION_MIN_HOURS | 1 | Hours before start to allow cancellation |

### 12.5 Changing Business Hours Without Code

Create a file `application-prod.yml` with your custom settings:

```yaml
booking:
  business:
    hour-start: "09:00"
    hour-end: "20:00"
  booking:
    min-advance-hours: 2
    min-duration-minutes: 60
    allow-weekends: true
  cancellation:
    min-hours: 2

server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:bookingdb
    username: sa
    password:
```

Then run with: `java -jar app.jar --spring.profiles.active=prod`

### 12.6 Quick Reference - What Can Be Configured

| Feature | Location | Default |
|---------|----------|---------|
| Business hours | Environment/Properties | 08:00 - 18:00 |
| Minimum advance booking | BookingService.java | 1 hour |
| Minimum booking duration | BookingService.java | 30 minutes |
| Cancellation deadline | BookingService.java | 1 hour before |
| Weekends allowed | BookingService.java | No |
| API port | application.yml | 8080 |
| Database | application.yml | H2 in-memory |
| API documentation | application.yml | /swagger-ui.html |

---

## 13. Project Structure

### 13.1 Directory Layout

```
booking-api/
├── pom.xml                              # Maven configuration
├── README.md                            # This documentation
├── mvnw                                 # Maven wrapper (Unix)
├── mvnw.cmd                             # Maven wrapper (Windows)
└── src/
    ├── main/
    │   ├── java/com/template/booking/
    │   │   ├── BookingApplication.java  # Main class
    │   │   ├── config/
    │   │   │   ├── AsyncConfig.java     # Async thread pool
    │   │   │   ├── OpenApiConfig.java   # Swagger configuration
    │   │   │   └── SecurityConfig.java  # Security configuration
    │   │   ├── controller/
    │   │   │   ├── AvailabilityController.java
    │   │   │   ├── BookingController.java
    │   │   │   └── ResourceController.java
    │   │   ├── dto/
    │   │   │   ├── AvailabilityResponse.java
    │   │   │   ├── BookingResponse.java
    │   │   │   ├── CreateBookingRequest.java
    │   │   │   ├── ErrorResponse.java
    │   │   │   ├── ResourceRequest.java
    │   │   │   └── ResourceResponse.java
    │   │   ├── event/
    │   │   │   ├── BookingCancelledEvent.java
    │   │   │   ├── BookingCreatedEvent.java
    │   │   │   ├── BookingEvent.java
    │   │   │   └── BookingEventListener.java
    │   │   ├── exception/
    │   │   │   ├── BookingNotFoundException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── InvalidBookingException.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── TimeSlotConflictException.java
    │   │   ├── model/
    │   │   │   ├── Booking.java
    │   │   │   └── Resource.java
    │   │   ├── repository/
    │   │   │   ├── BookingRepository.java
    │   │   │   └── ResourceRepository.java
    │   │   └── service/
    │   │       ├── BookingService.java
    │   │       ├── NotificationService.java
    │   │       └── ResourceService.java
    │   └── resources/
    │       └── application.yml           # Application config
    └── test/
        └── java/com/template/booking/
            └── service/
                ├── BookingConcurrencyTest.java
                └── BookingServiceTest.java
```

### 13.2 Key Files

| File | Purpose |
|------|---------|
| pom.xml | Maven dependencies and build config |
| BookingApplication.java | Spring Boot entry point |
| SecurityConfig.java | Authentication setup |
| BookingService.java | Core booking logic |
| BookingRepository.java | Database queries with locking |
| GlobalExceptionHandler.java | Centralized error handling |

---

## 14. Testing

### 14.1 Test Categories

| Test Type | Description | Location |
|-----------|-------------|----------|
| Unit Tests | Service layer business logic | service/*.java |
| Concurrency Tests | Thread-safety verification | service/BookingConcurrencyTest.java |

### 14.2 Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=BookingServiceTest

# Run specific test method
./mvnw test -Dtest=BookingServiceTest#createBooking_Success

# Run with coverage
./mvnw test jacoco:report
```

### 14.3 Test Coverage

**BookingServiceTest:**
- createBooking_Success
- createBooking_Conflict
- createBooking_PastTime
- createBooking_InvalidTimes
- createBooking_ResourceNotFound
- createBooking_InactiveResource
- cancelBooking_Success
- cancelBooking_TooLate
- cancelBooking_AdminCanCancelOthers
- getAvailability_Success

**BookingConcurrencyTest:**
- concurrentBooking_OnlyOneShouldSucceed

### 14.4 Manual API Testing

```bash
# Health check
curl http://localhost:8080/actuator/health

# List resources (as user)
curl -s http://localhost:8080/api/resources \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" | jq

# Create booking (as user)
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -H "Content-Type: application/json" \
  -d '{"resourceId":1,"startTime":"2026-06-01T10:00:00","endTime":"2026-06-01T11:00:00"}' | jq

# Check availability
curl "http://localhost:8080/api/availability/resource/1?date=2026-06-01" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" | jq
```

---

## 15. Deployment

### 15.1 Building

```bash
# Clean and package
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# Build with specific profile
./mvnw clean package -Pprod
```

### 15.2 Running JAR

```bash
# Run with default settings
java -jar target/booking-api-1.0.0.jar

# Run on custom port
java -jar target/booking-api-1.0.0.jar --server.port=9090

# Run with custom config
java -jar target/booking-api-1.0.0.jar --spring.config.location=file:/path/to/config.yml
```

### 15.3 Docker (Optional)

```dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/booking-api-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build and run
docker build -t booking-api .
docker run -p 8080:8080 booking-api
```

### 15.4 Environment-Specific Config

**Development:** Uses H2 in-memory database

**Production:** Configure external database in application.yml:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking
    username: booking_user
    password: ${DB_PASSWORD}
```

---

## 16. Troubleshooting

### 16.1 Common Issues

**Issue: "Connection refused" when accessing API**

Cause: Application not running
Solution: Start application with `./mvnw spring-boot:run`

---

**Issue: "401 Unauthorized" on all requests**

Cause: Missing or incorrect Authorization header
Solution: Ensure header is `Authorization: Basic <base64>`

---

**Issue: "403 Forbidden" when creating resources**

Cause: Using USER role instead of ADMIN
Solution: Use admin credentials: `admin:admin123`

---

**Issue: "409 Conflict" on every booking**

Cause: Time slot already booked
Solution: Check availability first with `/api/availability/resource/{id}?date=YYYY-MM-DD`

---

**Issue: "422 Unprocessable Entity" - past time**

Cause: Start time is in the past
Solution: Use future date/time for booking

---

**Issue: Swagger UI not loading**

Cause: Port conflict or network issue
Solution: Verify port 8080 is available, check `http://localhost:8080/swagger-ui.html`

---

### 16.2 Debug Mode

Enable debug logging in application.yml:
```yaml
logging:
  level:
    com.template.booking: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

### 16.3 H2 Console Access

URL: http://localhost:8080/h2-console

Settings:
- JDBC URL: `jdbc:h2:mem:bookingdb`
- Username: `sa`
- Password: (leave empty)

---

## 17. FAQ

### Q: How do I create a resource?
A: Use POST /api/resources with ADMIN credentials. See section 5.1.

### Q: How do I find available times?
A: Use GET /api/availability/resource/{id}?date=YYYY-MM-DD.

### Q: Why am I getting 409 Conflict?
A: The requested time slot overlaps with an existing booking. Choose a different time.

### Q: Can I modify a booking?
A: No, bookings cannot be modified. Cancel and create a new one.

### Q: How long can a booking be?
A: Minimum 30 minutes. No maximum duration.

### Q: Can I book for past dates?
A: No, start time must be in the future.

### Q: What happens if I don't cancel and just don't show up?
A: The booking remains as CONFIRMED. There's no auto-cancellation.

### Q: How do I change business hours?
A: Create `application-prod.yml` with custom values:
```yaml
booking:
  business:
    hour-start: "09:00"
    hour-end: "20:00"
```
Then run: `java -jar app.jar --spring.profiles.active=prod`

### Q: What rules can I customize without changing code?
A: All business rules are configurable:
- Business hours (08:00-18:00)
- Minimum advance booking (1 hour default)
- Minimum duration (30 minutes)
- Cancellation deadline (1 hour)
- Weekends allowed (no by default)

### Q: Can I use a real database instead of H2?
A: Yes, update spring.datasource.url in application.yml for PostgreSQL, MySQL, etc.

### Q: How does notification work?
A: Notifications are simulated via logging. Implement NotificationService for real email/SMS.

---

## 18. Examples

### 18.1 Complete Workflow

**Step 1: Admin creates a room**
```bash
curl -X POST http://localhost:8080/api/resources \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Innovation Lab",
    "description": "Modern lab with whiteboards and screens",
    "capacity": 15,
    "location": "Building B, Floor 4"
  }'
```
Response:
```json
{
  "id": 10,
  "name": "Innovation Lab",
  "status": "ACTIVE"
}
```

---

**Step 2: User checks availability for a date**
```bash
curl "http://localhost:8080/api/availability/resource/10?date=2026-06-15" \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```
Response: All slots available (all show `"available": true`)

---

**Step 3: User books the room**
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 10,
    "startTime": "2026-06-15T14:00:00",
    "endTime": "2026-06-15T15:30:00",
    "reason": "Brainstorm session for new product"
  }'
```
Response:
```json
{
  "id": 50,
  "userId": "user",
  "resourceId": 10,
  "resourceName": "Innovation Lab",
  "startTime": "2026-06-15T14:00:00",
  "endTime": "2026-06-15T15:30:00",
  "status": "CONFIRMED"
}
```

---

**Step 4: Another user tries conflicting time**
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA==" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 10,
    "startTime": "2026-06-15T14:30:00",
    "endTime": "2026-06-15T16:00:00"
  }'
```
Response: `409 Conflict` - overlaps with 14:00-15:30

---

**Step 5: User views their bookings**
```bash
curl http://localhost:8080/api/bookings/my \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```
Response: List includes the Innovation Lab booking

---

**Step 6: User cancels the booking (more than 1 hour in advance)**
```bash
curl -X DELETE http://localhost:8080/api/bookings/50 \
  -H "Authorization: Basic dXNlcjpwYXNzd29yZA=="
```
Response: Status changed to `CANCELLED`

---

### 18.2 Bash Script for Testing

```bash
#!/bin/bash
# booking-api-test.sh

BASE_URL="http://localhost:8080/api"
USER_AUTH="Basic dXNlcjpwYXNzd29yZA=="
ADMIN_AUTH="Basic YWRtaW46YWRtaW4xMjM="

echo "=== Create Resource ==="
curl -s -X POST $BASE_URL/resources \
  -H "Authorization: $ADMIN_AUTH" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Room","capacity":5,"location":"Test Floor"}' | jq .

echo -e "\n=== Check Availability ==="
curl -s "$BASE_URL/availability/resource/1?date=2026-06-20" \
  -H "Authorization: $USER_AUTH" | jq .

echo -e "\n=== Create Booking ==="
curl -s -X POST $BASE_URL/bookings \
  -H "Authorization: $USER_AUTH" \
  -H "Content-Type: application/json" \
  -d '{"resourceId":1,"startTime":"2026-06-20T09:00:00","endTime":"2026-06-20T10:00:00","reason":"Test meeting"}' | jq .

echo -e "\n=== Get My Bookings ==="
curl -s $BASE_URL/bookings/my -H "Authorization: $USER_AUTH" | jq .

echo -e "\n=== List All Resources ==="
curl -s $BASE_URL/resources -H "Authorization: $USER_AUTH" | jq .

echo -e "\n=== Try Conflicting Booking (expect 409) ==="
curl -s -X POST $BASE_URL/bookings \
  -H "Authorization: $USER_AUTH" \
  -H "Content-Type: application/json" \
  -d '{"resourceId":1,"startTime":"2026-06-20T09:30:00","endTime":"2026-06-20T10:30:00"}' | jq .
```

Run with: `chmod +x booking-api-test.sh && ./booking-api-test.sh`

---

### 18.3 Java Client Example

```java
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class BookingClient {
    private static final String BASE_URL = "http://localhost:8080/api";
    
    public static void main(String[] args) {
        RestTemplate rest = new RestTemplate();
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth("user", "password");
        
        // Create booking request
        String json = """
            {
                "resourceId": 1,
                "startTime": "2026-06-01T10:00:00",
                "endTime": "2026-06-01T11:00:00",
                "reason": "Client meeting"
            }
            """;
        
        HttpEntity<String> request = new HttpEntity<>(json, headers);
        
        // Make request
        ResponseEntity<String> response = rest.postForEntity(
            BASE_URL + "/bookings", request, String.class);
        
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
    }
}
```

---

## Appendix: Quick Reference Card

### Authentication Headers
| User | Base64 Encoded |
|------|----------------|
| user:password | dXNlcjpwYXNzd29yZA== |
| admin:admin123 | YWRtaW46YWRtaW4xMjM= |

### API Endpoints Summary
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/resources | ADMIN | Create resource |
| GET | /api/resources | USER | List all |
| GET | /api/resources/active | USER | List active |
| GET | /api/resources/{id} | USER | Get one |
| PATCH | /api/resources/{id}/status | ADMIN | Update status |
| POST | /api/bookings | USER | Create booking |
| GET | /api/bookings/{id} | USER | Get one |
| GET | /api/bookings/my | USER | My bookings |
| DELETE | /api/bookings/{id} | USER/ADMIN | Cancel |
| GET | /api/availability/resource/{id} | USER | Check availability |

### URL Reference
| Resource | URL |
|----------|-----|
| API Base | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| H2 Console | http://localhost:8080/h2-console |
| Health | http://localhost:8080/actuator/health |

---

**Document Version:** 1.0.0  
**Last Updated:** May 2026