# StayNest
Full-stack homestay booking platform starter.

Stack: React + Vite, Spring Boot microservices, Spring Cloud Gateway, Eureka, PostgreSQL, JWT-ready security, Docker, GitHub Actions.

Architecture:
React -> API Gateway -> Eureka -> User/Auth, Property, Booking, Payment, Review, Notification services.
Each business service owns a PostgreSQL database.
