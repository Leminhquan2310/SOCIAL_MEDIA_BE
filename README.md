📱 Social Media Backend API

A scalable backend system for a social media platform, providing user interaction, content sharing, and real-time communication.

🚀 Overview

This project is a backend service built with Spring Boot, designed to support core features of a modern social media platform such as authentication, post management, real-time messaging, and content moderation.

✨ Features
- 🔐 Authentication & Authorization
   - JWT-based authentication with refresh token rotation
   - Role-based access control (RBAC) using Spring Security
   - OAuth2 login (Google, Facebook)
- 👤 User System
  - User profile management
  - Follow / unfollow users
  - Social graph relationships
- 📝 Post & Interaction
  - Create posts (text, image, video)
  - Like, comment, share posts
- 💬 Real-time Communication
  - WebSocket (STOMP) for messaging and notifications
- ☁️ Media Management
  - Upload and store images/videos via Cloudinary
- 🤖 Content Moderation
  - AI-based filtering for inappropriate/spam content (AWS Rekognition)
- ⚙️ API Design
  - RESTful APIs with proper HTTP status codes
  - DTO pattern & validation
  - Centralized exception handling
- 🛠️ Tech Stack
  - Backend: Spring Boot, Spring Security, Spring Data JPA
  - Database: MySQL
  - Real-time: WebSocket (STOMP)
  - Authentication: JWT, OAuth2
  - Cloud Storage: Cloudinary
  - Caching: Caffeine Cache
  - AI Moderation: AWS Rekognition

📂 Project Structure
```
src/main/java/com/yourproject
├── config        # Security, WebSocket, Config classes
├── controller    # REST Controllers
├── service       # Business logic
├── repository    # JPA Repositories
├── entity        # Database entities
├── dto           # Data Transfer Objects
├── exception     # Global exception handling
```
⚙️ Installation & Setup
1. Clone repository
```
git clone https://github.com/Leminhquan2310/SOCIAL_MEDIA_BE.git
cd SOCIAL_MEDIA_BE
```
2. Setup environment variables

Create a file:
```
application.yml
```
or update:
```
application.properties
```
Example configuration:
```
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/social_media
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=your_secret_key
jwt.expiration=86400000

# Cloudinary
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

# OAuth2 (Google example)
spring.security.oauth2.client.registration.google.client-id=your_client_id
spring.security.oauth2.client.registration.google.client-secret=your_client_secret
```
3. Run database

Make sure MySQL is running and create database:
```
CREATE DATABASE social_media;
```
4. Build project
```
mvn clean install
```
5. Run application
```
mvn spring-boot:run
```
App will start at:
```
http://localhost:8080
```
🔌 API Testing

You can test APIs using:

- Postman
- Swagger (if enabled)

Example endpoints:
```
POST   /api/auth/login
POST   /api/auth/register
GET    /api/posts
POST   /api/posts
GET    /api/users/{id}
```
📡 WebSocket

Endpoint:
```
ws://localhost:8080/ws
```
Topics:
```
/topic/messages
/topic/notifications
```
📈 Future Improvements
- Redis caching for scalability
- Microservices architecture
- Docker & CI/CD pipeline
- Advanced recommendation system
👨‍💻 Author
- GitHub: [Leminhquan2310](https://github.com/Leminhquan2310)
📄 License

This project is for educational and portfolio purposes.
