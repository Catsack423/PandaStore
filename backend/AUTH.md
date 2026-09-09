# Authentication service

`AuthServiceImp` implements the service layer. HTTP authentication endpoints and
request authorization filters are not part of this change.

## Registration

- `registerCustomer(...)` validates the request and matching passwords, calls
  `CustomerService`, and creates an empty cart. All writes share one transaction.
- `registerSeller(RegisterSellerRequest)` requires the shop, identity-document,
  and banking details required by the existing entities. It delegates to
  `SellerRegistrationService` and creates a seller and application in `PENDING`
  status in one transaction. It does not approve the shop or verify documents.
- Customer creation, including the existing customer endpoint, now stores BCrypt
  hashes. Passwords must be at least 6 characters and no more than 72 UTF-8 bytes.
  Existing plaintext passwords are not accepted by login; existing accounts need
  a separately authorized migration or recovery process.

## Login and password changes

- `login(usernameOrEmail, password)` returns a random, opaque bearer token valid
  for 24 hours. This is not a JWT. Only a SHA-256 digest of the token is stored in
  `auth_sessions`. A username/email collision between different users is rejected.
- `validateToken(token)` checks the persisted session, expiry, active user status,
  and whether the password has changed. Expired sessions are removed on login.
- `resetPassword(token, currentPassword, password, confirmPassword)` is an
  authenticated password change. It requires the current password and invalidates
  all existing sessions. Invalid input returns `false`. Forgotten-password email
  or OTP recovery is not implemented.
- The old seller registration signature and ID-only password reset signature were
  replaced. There were no callers in the repository. Future callers must use the
  new signatures and must not expose entity password hashes or identity documents
  in HTTP responses. Token validation alone does not authorize seller operations;
  those operations must also check shop approval and resource ownership.

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
