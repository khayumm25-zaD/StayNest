# StayNest Architecture

Frontend calls only the API Gateway.
Gateway routes to Eureka-discovered microservices.

Services:
- Eureka Server: discovery
- API Gateway: routing, CORS, future JWT filter
- User Service: registration, login, profiles, roles
- Property Service: properties, search, amenities
- Booking Service: availability and bookings
- Payment Service: mock payment lifecycle
- Review Service: ratings/reviews
- Notification Service: booking/payment notifications

Rule: database-per-service; never use cross-service foreign keys.
