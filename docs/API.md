# StayNest API

Auth/User:
- `POST /api/auth/register` — create a CUSTOMER account; body: `name`, `email`, `password`.
- `POST /api/auth/login` — authenticate; returns `token`, `email`, `name`, and `roles`.
- `POST /api/auth/logout` — client-side token invalidation; the caller discards its JWT.
- `GET /api/auth/me` or `GET /api/users/me` — current authenticated profile.
- `PUT /api/users/me` — update current profile (`name`, `email`).
- `GET /api/users/{id}` — own user or ADMIN.
- `GET /api/users` — ADMIN only.
- `PATCH /api/users/{id}/roles` — ADMIN only; body is an array of role names (`CUSTOMER`, `HOST`, `ADMIN`).

Protected endpoints require `Authorization: Bearer <JWT>`. The same externally configured `JWT_SECRET` is required by the gateway-facing services. Passwords and JWT signing secrets are never returned by the API. Docker Compose requires `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET` (at least 32 bytes), and `PAYMENT_SERVICE_API_KEY`; use the placeholder values in `.env.example` as a template and replace passwords/secrets before running. `POSTGRES_PORT` selects the host port for PostgreSQL (default `5432`). Database credentials have no source-config fallback.

Properties:
- `GET /api/properties` — list properties as a JSON array (keeps the existing frontend contract).
- `GET /api/properties/{id}` — retrieve one property.
- `GET /api/properties/search` — paginated search. Optional parameters: `location` (matches city, state, or country), `propertyType`, `minPrice`, `maxPrice`, `guests`, and `amenity`. Pagination/sorting parameters: `page` (zero-based), `size` (1–100), `sortBy` (`id`, `title`, `city`, `state`, `country`, `propertyType`, `pricePerNight`, or `maxGuests`), and `direction` (`asc` or `desc`). Returns a Spring `Page` JSON response.
- `POST /api/properties` — create a property; requires a HOST or ADMIN bearer token. HOST ownership is assigned from the token; ADMIN may optionally set `hostId`.
- `PUT /api/properties/{id}` — update a property; HOST may update only their own properties, ADMIN may update any.
- `DELETE /api/properties/{id}` — delete a property; HOST may delete only their own properties, ADMIN may delete any.

Write requests accept property details and an optional `amenities` string array. Responses use property DTOs and do not expose persistence entities.
Protected writes require `Authorization: Bearer <access-token>` issued by the User/Auth Service. The User/Auth and Property services must be configured with the same external `JWT_SECRET` (at least 32 characters).

Bookings:
- `GET /api/bookings/availability` — public date check and price estimate. Query parameters: `propertyId`, `checkInDate`, `checkOutDate`, and `numberOfGuests`. Booking creation rechecks availability atomically to prevent double booking.
- `POST /api/bookings` — CUSTOMER creates a booking; price is calculated by Booking Service from Property Service nightly price.
- `GET /api/bookings/{id}` — customer who owns it, the property's HOST, or ADMIN.
- `GET /api/bookings/my` — customer's own bookings.
- `GET /api/bookings/host` — bookings for properties owned by the HOST; ADMIN sees all bookings.
- `GET /api/bookings/property/{propertyId}` — property's HOST or ADMIN.
- `PUT /api/bookings/{id}/cancel` — booking owner or ADMIN; pending/confirmed only.
- `PUT /api/bookings/{id}/status` — property's HOST or ADMIN; valid transitions are PENDING → CONFIRMED → COMPLETED. Use cancel endpoint for cancellation.

Booking requests require `propertyId`, `checkInDate`, `checkOutDate`, and `numberOfGuests`; client-supplied totals are ignored. Dates use ISO-8601 (`YYYY-MM-DD`). Payments initially use UNPAID status.

Payments (MOCK/DEMO ONLY — no real payment gateway or payment credentials are used):
- `POST /api/payments` — CUSTOMER pays their own booking; amount and customer are taken from Booking Service. Request fields: `bookingId`, ISO 4217 `currency`, `paymentMethod` (`CARD`, `UPI`, `NET_BANKING`, or `MOCK`), optional positive `amount` (ignored for charging), and optional `simulateFailure` (mock-only deterministic failure flag).
- `GET /api/payments/{id}` — payment owner or ADMIN.
- `GET /api/payments/booking/{bookingId}` — booking customer or ADMIN.
- `GET /api/payments/my` — customer's payment history; ADMIN receives all payments.
- `POST /api/payments/{id}/refund` — payment owner or ADMIN; SUCCESS payments only. Refunds are mock-only.

Successful mock payments set the payment to SUCCESS and booking payment status to PAID. A request with `simulateFailure: true` returns a FAILED payment with `DEMO_FAILURE_REQUESTED` and does not update the booking. The service never accepts or stores card numbers, CVVs, UPI PINs, passwords, or bank credentials. Payment Service and Booking Service require the same external `JWT_SECRET`.

Booking internal payment status API:
- `PUT /api/bookings/{id}/payment-status` — payment status updates only; requires both the booking owner's/ADMIN's bearer token and the `X-Payment-Service-Key` credential configured as `PAYMENT_SERVICE_API_KEY`. This prevents a customer from marking a booking paid without completing the mock-payment flow. Accepts `{"paymentStatus":"PAID"}` or `{"paymentStatus":"REFUNDED"}` and validates the status transition. Used by Payment Service; no cross-service database foreign keys.

Reviews:
- `GET /api/reviews` — ADMIN only; paginated system-wide reviews (`page`, `size`).
- `POST /api/reviews` — CUSTOMER only. Body: `propertyId`, `bookingId`, `rating` (1–5), `title`, and optional `comment`. Booking Service validates that the authenticated customer owns the booking, the property matches, and the booking is COMPLETED.
- `GET /api/reviews/{id}` — authenticated review owner or ADMIN.
- `GET /api/reviews/property/{propertyId}` — public paginated reviews. Optional `page` (zero-based, default 0), `size` (1–50, default 10), and `sort` (`newest` or `rating`, default `newest`). Response includes page metadata, `averageRating`, and `reviewCount`.
- `GET /api/reviews/my` — authenticated CUSTOMER or ADMIN's own review history.
- `PUT /api/reviews/{id}` — owner or ADMIN; body accepts only `rating`, `title`, and `comment`, so property, booking, and customer ownership cannot be changed.
- `DELETE /api/reviews/{id}` — owner or ADMIN.

Review ownership is derived from the JWT, not the request body. A database uniqueness constraint prevents duplicate reviews for the same booking/property/customer. Review Service calls Booking Service using the forwarded bearer token; it stores no cross-service foreign keys. Review Service and Booking Service must use the same external `JWT_SECRET`.

Notifications (internal demo inbox; no email/SMS delivery):
- `POST /api/notifications` — CUSTOMER, HOST, or ADMIN. Body: `type` (`BOOKING_CREATED`, `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `REFUND_SUCCESS`, or `REVIEW_CREATED`), `message`, and optional `userId`. Recipient is always the authenticated user except ADMIN may specify another recipient.
- `GET /api/notifications/my` — authenticated user's paginated notifications. Optional `page` (zero-based, default 0), `size` (1–100, default 20), and `unreadOnly` (default false). Response includes `unreadCount`.
- `GET /api/notifications` — ADMIN only; paginated system-wide notification list with optional `unreadOnly`.
- `GET /api/notifications/{id}` — notification owner or ADMIN.
- `PUT /api/notifications/{id}/read` — marks the caller's notification read; ADMIN may mark any notification read.
- `PUT /api/notifications/read-all` — marks all of the authenticated user's notifications read; returns `updatedCount`.

Notification records include `createdAt` and nullable `readAt`. All notifications are stored locally in the Notification Service database; no email/SMS integrations or cross-service database references are used. Notification Service uses the shared external `JWT_SECRET`.

The current UI explicitly calls POST /api/notifications for supported booking, payment, refund, and review actions. Services do not yet publish lifecycle events to Notification Service, so API calls made outside those UI actions do not automatically create notifications.

Property imagery is illustrative and frontend-provided. The Property Service does not currently persist or serve uploaded property images.
