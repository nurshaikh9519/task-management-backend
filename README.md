# task-management-backend
Task Management Application built using Java, Spring Boot, PostgreSQL and JWT authentication.

# Task Management Backend

A RESTful Task Management application developed using Java 17 and Spring Boot. The application provides secure user authentication, user-specific task management, dynamic task searching, pagination, dashboard analytics, and profile management.

## Technology Stack

| Category | Technologies |
|---|---|
| Programming Language | Java 17 |
| Backend Framework | Spring Boot |
| Web Layer | Spring MVC, REST APIs |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Validation | Jakarta Bean Validation |
| API Documentation | Swagger / OpenAPI |
| Build Tool | Gradle |
| Testing | JUnit 5, Mockito, MockMvc |
| ORM Querying | JPA Specifications |

## Architecture

The application follows a layered architecture:

- **Controller Layer:** Exposes REST endpoints and handles HTTP requests.
- **Service Layer:** Implements business logic and application workflows.
- **Repository Layer:** Handles database operations using Spring Data JPA.
- **Entity Layer:** Defines database entities and relationships.
- **DTO Layer:** Separates API request/response models from persistence entities.
- **Security Layer:** Implements JWT authentication and request authorization.
- **Exception Layer:** Provides centralized exception handling.

## Key Features

### Authentication and Security
- User registration and login.
- Stateless JWT-based authentication.
- Password hashing using BCrypt.
- Spring Security filter chain and custom JWT authentication filter.
- Protected REST APIs.
- User-specific task access and ownership validation.

### Task Management
- Create, retrieve, update, and delete tasks.
- Search tasks by title.
- Filter tasks by status and due-date range.
- Identify overdue tasks.
- Pagination and sorting.
- Dynamic queries using Spring Data JPA Specifications.

### Dashboard and User Management
- Dashboard statistics for total, completed, pending, in-progress, and overdue tasks.
- Retrieve and update user profile.
- Change password with current-password verification.

### Exception Handling and Validation
- Centralized global exception handling.
- Request validation using Jakarta Bean Validation.
- Meaningful HTTP status codes and error responses.

### Testing
- Service-layer unit tests using JUnit 5 and Mockito.
- Controller tests using MockMvc.
- JWT service and authentication filter tests.
- Security integration tests.
- PostgreSQL repository tests.

## REST API Endpoints

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/users/register` | Register a new user |
| POST | `/users/login` | Authenticate user and generate JWT |

### User Management

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users/me` | Retrieve logged-in user's profile |
| PUT | `/users/me` | Update user profile |
| PATCH | `/users/me/password` | Change password |

### Task Management

| Method | Endpoint | Description |
|---|---|---|
| POST | `/tasks` | Create a task |
| GET | `/tasks` | Retrieve tasks with search, filters and pagination |
| GET | `/tasks/{id}` | Retrieve task by ID |
| PUT | `/tasks/{id}` | Update task |
| DELETE | `/tasks/{id}` | Delete task |
| GET | `/tasks/dashboard` | Retrieve task dashboard statistics |

## Database

**PostgreSQL**

The application uses Spring Data JPA and Hibernate for ORM and database interactions.

Configure the database connection through environment variables rather than committing credentials.

## Running the Application

### Prerequisites
- Java 17
- PostgreSQL
- Gradle (or use the Gradle wrapper)

### Database Setup
Create a PostgreSQL database:

`task_management_db`

### Environment Variables

Configure the following environment variables:

- `DB_PASSWORD` – PostgreSQL password
- `JWT_SECRET` – Base64-encoded secret key for JWT signing

Use a strong, securely generated JWT key. Never commit real credentials or secret keys to GitHub.

### Run the Application

On Windows:

`gradlew.bat bootRun`

On Linux/macOS:

`./gradlew bootRun`

The application runs on:

`http://localhost:8080`

### API Documentation

Swagger UI:

`http://localhost:8080/swagger-ui.html`

## Testing

Run the test suite using:

Windows:

`gradlew.bat test`

Linux/macOS:

`./gradlew test`

The project includes unit tests, controller tests, security tests, and PostgreSQL repository tests.
