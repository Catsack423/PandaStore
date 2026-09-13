# Authentication service

`AuthServiceImp` implements the service layer. `AuthController` exposes the routes
below. The new Auth and Cart controllers validate bearer tokens explicitly; this
is not a global authorization filter for the application's other controllers.

## HTTP endpoints

| Method | Route | Request |
| --- | --- | --- |
| POST | `/api/auth/register/customer` | username, email, password, confirmPassword, fullName, phoneNumber |
| POST | `/api/auth/register/seller` | username, email, password, confirmPassword; currently returns 501 |
| POST | `/api/auth/login` | usernameOrEmail, password |
| GET | `/api/auth/token` | Authorization: Bearer token; returns valid |
| POST | `/api/auth/reset-password` | Authorization header plus currentPassword, password, confirmPassword |

Registration returns 201 with a customer DTO, never the User entity or password
hash. Login returns `token` and `tokenType: Bearer`. Wrong credentials return 401;
duplicate registration returns 409. Password reset derives the user ID from the
session and verifies the current password before calling the service. Do not send
a user ID in the reset request. The token endpoint returns `valid: false` for an
expired/unknown well-formed token; missing or malformed headers return 401.

## Registration

- `registerCustomer(...)` validates the request and matching passwords, calls
  `CustomerService`, and creates an empty cart. All writes share one transaction.
- `registerSeller(username, email, password, confirmPassword)` retains the original
  interface. It currently throws `UnsupportedOperationException` without writing
  data: `SellerService.createSeller()` accepts no account details, and the Auth
  interface does not supply the required application details. Agree on these
  contracts before connecting `SellerService` and `SellerApplicationService`.
  There is no separate seller registration implementation in this change.
- Registration through AuthService stores BCrypt hashes in the same transaction
  as customer creation. CustomerService's implementation remains unchanged.
  Passwords must be at least 6 characters and no more than 72 UTF-8 bytes.
  Existing plaintext passwords are not accepted by login; existing accounts need
  a separately authorized migration or recovery process.
- The existing customer endpoint still calls CustomerService directly. It does
  not create a cart or hash passwords through AuthService. Route authentication
  registration through AuthService when connecting the API; this change does not
  modify that endpoint.

## Login and password changes

- `login(usernameOrEmail, password)` returns a random, opaque bearer token valid
  for 24 hours. This is not a JWT. Only a SHA-256 digest of the token is stored in
  `auth_sessions`. A username/email collision between different users is rejected.
- `validateToken(token)` checks the persisted session, expiry, active user status,
  and whether the password has changed. Expired sessions are removed on login.
- `resetPassword(id, password, confirmPassword)` retains the original interface
  and invalidates all sessions for that user. The caller must verify reset
  authorization for this user ID first; this method does not verify the caller's
  identity, current password, or OTP. Do not expose it as an unauthenticated
  endpoint accepting arbitrary user IDs. Invalid input or a missing user returns
  `false`. Forgotten-password email/OTP recovery is not implemented.
- Both Auth and Cart retain their original service signatures. Auth uses
  repositories for database access and AuthSession for persistent sessions.
  Supporting repositories and PasswordService are retained; CustomerService's
  implementation and its tests are restored to their original versions.

## Database and checks

The application needs the new `auth_sessions` table mapped by `AuthSession`.
`db/auth_sessions.sql` supplies the PostgreSQL schema addition; it has not been
applied to an external database. Execute it once against the existing schema.
Configure the database using Spring Boot's `spring.datasource.*` properties and
apply the schema before using this service. The existing application properties
use unprefixed database/JPA keys; those are not Spring Boot datasource settings.
Do not rely on them to provision a production database.

Run `mvn test` from the backend directory with Java 25. The authentication
integration tests explicitly use an isolated H2 database and verify registration,
rollback, password hashing, token validity/expiry, suspended accounts, and password
changes. They do not require a running PostgreSQL server.

Commit the authentication change after the tests pass, before starting CartService:

    feat(auth): implement registration and token authentication

Authentication now delegates creation of the initial empty cart to `CartService`.
The customer and cart still share the registration transaction. See `CART.md` for
cart operations.
