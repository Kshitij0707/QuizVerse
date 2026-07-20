Project: QuizVerse

Description:
QuizVerse is a production-ready full-stack quiz platform built using Spring Boot and React. It allows users to take quizzes from multiple sources: built-in quizzes stored in the database, quizzes fetched from external Quiz APIs, and custom quizzes created by users. The application follows a layered architecture (Controller → Service → Repository) and emphasizes clean code, security, scalability, and RESTful design.

Tech Stack:
- Java 21
- Spring Boot
- Spring Security
- JWT Authentication
- Spring Data JPA (Hibernate)
- MySQL
- React
- Axios
- Swagger/OpenAPI
- Maven
- Git

Architecture:
- Controller → Service → Repository
- DTO Pattern
- Global Exception Handling
- Validation
- REST APIs
- Layered Architecture

Roles:
- Guest
- User
- Admin

Core Features:
- User registration and login
- JWT authentication with refresh tokens
- Role-based authorization
- Create/Edit/Delete custom quizzes
- Built-in quizzes managed by admin
- Import quizzes from external APIs
- Attempt quizzes with timer and scoring
- Quiz history
- Dashboard with analytics
- Leaderboards
- Search, filtering, pagination
- Bookmarks
- Ratings and comments
- Admin dashboard
- Swagger documentation

Entities:
User
Role
Quiz
Question
Option
Category
QuizAttempt
UserAnswer
Bookmark
Comment
Rating

Coding Standards:
- Follow SOLID principles
- Use constructor injection
- Return DTOs instead of entities
- Keep controllers thin
- Business logic belongs in services
- Use meaningful package structure
- Use ResponseEntity for APIs
- Validate all request DTOs
- Handle exceptions globally
- Write clean, maintainable code

Future Enhancements:
- Redis caching
- Docker
- WebSocket multiplayer quizzes
- Email notifications
- AI-generated quizzes
- CI/CD pipeline