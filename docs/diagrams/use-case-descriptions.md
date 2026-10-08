# PandaStore Use Case Descriptions

The IDs match [the use case source](use-cases.puml) and [rendered diagram](use-cases.svg). The diagram uses separate capability panels and repeats actor names to keep associations local; repeated names represent the same role. This document describes the current implementation. **UI** means a connected frontend workflow; **API only** means a backend route exists without a complete current UI workflow. Guest is an unauthenticated visitor; Customer, Seller and Admin are the distinct `UserRole` values. An approved seller retains the existing customer record, but customer endpoints still require the `CUSTOMER` role.

Authenticated browser requests use the Next.js proxy and its `auth_token` HttpOnly cookie. The proxy forwards a Bearer token to Spring Boot. Backend authentication requires a valid token, a persisted unexpired session and an active user. Role and resource ownership checks are applied where described below; a diagram association does not imply that every controller enforces ownership.

Sources: [SecurityConfig](../../backend/src/main/java/project/project/Config/SecurityConfig.java), [SessionAuthenticator](../../backend/src/main/java/project/project/Security/SessionAuthenticator.java), [CurrentUser](../../backend/src/main/java/project/project/Security/CurrentUser.java), [auth proxy](../../frontend/src/app/api/auth/%5Baction%5D/route.ts).

## Catalogue and account

### UC01 — Browse products, categories, shops and product reviews

- **Actor:** Guest, Customer, Seller or Admin. **Availability:** UI.
- **Trigger:** Open the storefront, search/filter products, select a product or shop, or open its reviews.
- **Preconditions:** Catalogue backend is reachable; no sign-in is required for the public routes listed below.
- **Main flow:** Retrieve active catalogue products or a paged search, apply supported keyword/category/price/sort filters, view product details, category products and shop details, then read product reviews and seller replies.
- **Alternative/error flows:** Empty results show an empty state; missing products/shops or a failed upstream request produce the current error/fallback UI. Sample template products/shops are previews rather than proof of persisted catalogue records. The alternate backend `GET /api/products/category/{categoryId}` requires authentication; use the public category route for anonymous browsing.
- **Postconditions:** No business data changes.
- **Sources:** [ProductController](../../backend/src/main/java/project/project/Controller/ProductController.java), [CategoryController](../../backend/src/main/java/project/project/Controller/CategoryController.java), [SellerShopController](../../backend/src/main/java/project/project/Controller/SellerShopController.java), [ReviewController](../../backend/src/main/java/project/project/Controller/ReviewController.java), [ProductDetail](../../frontend/src/components/Shop/ProductDetail.tsx), [PublicShop](../../frontend/src/components/Seller/PublicShop.tsx).

### UC02 — Register a customer account

- **Actor:** Guest. **Availability:** UI at `/signup`.
- **Trigger:** Submit the customer registration form.
- **Preconditions:** Valid username, email, password/confirmation, full name and phone fields; identifiers are available.
- **Main flow:** `POST /api/auth/register` proxies `POST /api/auth/register/customer`; validate the request and matching password, create the active customer user and customer record, hash the password, create the customer's cart, and return registration success. The signup UI then sends a separate login request and redirects home on success.
- **Alternative/error flows:** Invalid fields, mismatched passwords, duplicate identifiers or persistence failure reject registration. If automatic sign-in fails, the account remains created and the UI redirects to sign-in with an explanatory message.
- **Postconditions:** Customer account and cart exist after successful transaction; successful follow-up login establishes the browser session.
- **Sources:** [AuthController](../../backend/src/main/java/project/project/Controller/AuthController.java), [AuthServiceImp](../../backend/src/main/java/project/project/Service/implement/AuthServiceImp.java), [CustomerServiceImp](../../backend/src/main/java/project/project/Service/implement/CustomerServiceImp.java), [Signup](../../frontend/src/components/Auth/Signup/customer/index.tsx).

### UC03 — Sign in and sign out

- **Actor:** Guest signs in; Customer, Seller or Admin signs out. **Availability:** UI.
- **Trigger:** Submit credentials at `/signin`, restore the current session, or select logout.
- **Preconditions:** Sign-in requires an active user and matching password. Backend logout requires the current valid Bearer session.
- **Main flow:** Resolve username or email, verify the password, issue a JWT expiring after 24 hours and store its SHA-256 hash in `AuthSession`. The auth proxy sets the HttpOnly cookie and returns user data. Session restoration uses `GET /api/auth/me`. Logout revokes that token's session and clears browser cookies.
- **Alternative/error flows:** Invalid/inactive accounts or ambiguous username/email identity return 401. With default limits, the sixth backend login request from one source in a 60-second window returns 429 with `Retry-After`. Expired sessions require sign-in. Browser logout clears its cookie even when backend revocation is unavailable.
- **Postconditions:** Successful sign-in establishes one session; successful backend logout revokes that session.
- **Sources:** [AuthServiceImp](../../backend/src/main/java/project/project/Service/implement/AuthServiceImp.java), [LoginRateLimitFilter](../../backend/src/main/java/project/project/Filter/LoginRateLimitFilter.java), [auth proxy](../../frontend/src/app/api/auth/%5Baction%5D/route.ts), [AuthContext](../../frontend/src/app/context/AuthContext.tsx).

### UC04 — Edit own profile and change password

- **Actor:** Customer, Seller or Admin. **Availability:** UI at `/my-account`.
- **Trigger:** Save account details or submit current/new/confirmed password.
- **Preconditions:** Valid active session; password change requires the correct current password.
- **Main flow:** `PUT /api/auth/me` updates the current user's email and role-specific profile fields (customer name/phone, seller shop name/phone, or admin username). `POST /api/auth/reset-password` verifies the current password, hashes the new password, revokes all existing sessions, creates a replacement session, and rotates the browser cookie.
- **Alternative/error flows:** Incorrect current password returns 401; invalid/mismatched new password or invalid profile fields are rejected. The route is an authenticated password change, not anonymous forgotten-password recovery.
- **Postconditions:** Profile is saved, or password is changed and prior sessions revoked.
- **Sources:** [AuthController](../../backend/src/main/java/project/project/Controller/AuthController.java), [AuthServiceImp](../../backend/src/main/java/project/project/Service/implement/AuthServiceImp.java), [MyAccount](../../frontend/src/components/MyAccount/index.tsx).

### UC14 — Read own notifications and mark them as read

- **Actor:** Customer, Seller or Admin. **Availability:** UI and API.
- **Trigger:** Open notifications or select one to mark as read.
- **Preconditions:** Active session; requested notifications belong to the current user.
- **Main flow:** Frontend proxy resolves the user and retrieves `GET /api/notifications/users/{userId}`. `PATCH /api/notifications/{notificationId}/read` checks ownership and records the read flag.
- **Alternative/error flows:** Another user's list or notification is denied with 403; missing session is denied with 401; missing data/upstream failures are reported. These are stored in-app notifications, not an implemented email/SMS service.
- **Postconditions:** Own notifications are displayed; selected notification becomes read.
- **Sources:** [NotificationController](../../backend/src/main/java/project/project/Controller/NotificationController.java), [NotificationServiceImp](../../backend/src/main/java/project/project/Service/implement/NotificationServiceImp.java), [notification proxy](../../frontend/src/app/api/notifications/route.ts), [read proxy](../../frontend/src/app/api/notifications/%5BnotificationId%5D/read/route.ts).

## Customer shopping

### UC05 — Manage a cart

- **Actor:** Guest or Customer. **Availability:** UI at `/cart`.
- **Trigger:** Add a product, change quantity, remove an item, clear the cart or refresh stock.
- **Preconditions:** Product and positive quantity are available; a persisted cart requires a `CUSTOMER` session.
- **Main flow:** Guests edit a browser Redux cart. Customers use `/api/cart` to read/create the owned cart, add items, set quantities and remove/clear items. Stock synchronization reconciles unavailable items and returns current data. The UI groups items by shop and checks out every visible item.
- **Alternative/error flows:** Insufficient/unknown stock, unavailable product or stale item fails the mutation and refreshes current data. Seller/Admin accounts cannot use the customer cart UI. The backend also supports selection, stock validation and seller grouping APIs; selection is not a partial-item checkout option in the current UI. Guest cart is not automatically merged into the customer's server cart by `CartContext` on sign-in.
- **Postconditions:** Guest state or the customer's persisted cart reflects successful operations; no stock is reserved until checkout.
- **Sources:** [CartController](../../backend/src/main/java/project/project/Controller/CartController.java), [CartServiceImp](../../backend/src/main/java/project/project/Service/implement/CartServiceImp.java), [CartContext](../../frontend/src/app/context/CartContext.tsx), [Cart](../../frontend/src/components/Cart/index.tsx).

### UC06 — Manage own shipping addresses

- **Actor:** Customer. **Availability:** UI at `/my-account` and `/checkout`.
- **Trigger:** Add/edit an address, select its default flag or delete an address.
- **Preconditions:** `CUSTOMER` session, valid address fields and ownership of the target address.
- **Main flow:** Read owned addresses; add/update through `/api/checkout/addresses`, or use `/api/customers/{customerId}/addresses`. The first address becomes default. Selecting a default clears other defaults under a customer lock; deletion of a default promotes a remaining address.
- **Alternative/error flows:** Foreign address/customer IDs are rejected or return not found. An address referenced by an order cannot be deleted. Existing referenced addresses remain editable in the service; there is no immutable shipping-address snapshot implemented here.
- **Postconditions:** Address list and default choice reflect the successful mutation.
- **Sources:** [AddressController](../../backend/src/main/java/project/project/Controller/AddressController.java), [AddressServiceImp](../../backend/src/main/java/project/project/Service/implement/AddressServiceImp.java), [CustomerCheckoutService](../../backend/src/main/java/project/project/Service/implement/CustomerCheckoutService.java), [Addresses UI](../../frontend/src/components/MyAccount/Addresses.tsx).

### UC07 — Checkout across shops

- **Actor:** Customer. **Availability:** UI at `/checkout`.
- **Trigger:** Confirm all cart items, shipping address, a shipping method per shop and payment method; place the order.
- **Preconditions:** `CUSTOMER` session, nonempty valid cart, owned shipping address and supported shipping method for exactly every shop in the cart.
- **Main flow:** Load checkout context, obtain per-shop quotes, mark all cart items selected, and call `POST /api/checkout/orders`. Orchestration validates stock, creates an `OrderGroup`, groups items into one `Order` per seller, saves item price/name snapshots, calculates shipping and grand total, creates a pending payment, deducts inventory and removes checked-out items from the cart. The UI moves to the payment page.
- **Alternative/error flows:** Empty cart, missing/extra shop shipping selection, unsupported method, address owned by another customer or insufficient stock rejects checkout. Transaction failure rolls back the order creation. Totals come from backend product prices and shipping strategies; coupon UI templates do not implement discounts.
- **Postconditions:** Pending payment group, seller sub-orders and order items exist; stock is deducted and cart is cleared for the included items.
- **Sources:** [CustomerCheckoutController](../../backend/src/main/java/project/project/Controller/CustomerCheckoutController.java), [CustomerCheckoutService](../../backend/src/main/java/project/project/Service/implement/CustomerCheckoutService.java), [OrderOrchestrationServiceImp](../../backend/src/main/java/project/project/Service/implement/OrderOrchestrationServiceImp.java), [Checkout](../../frontend/src/components/Checkout/index.tsx).

### UC08 — Simulate payment

- **Actor:** Customer for their own order; Admin can use the backend simulation API. **Availability:** UI at `/payment/{orderGroupId}`.
- **Trigger:** Click the payment simulation action.
- **Preconditions:** Owned pending group with pending payment; valid payment method and exact persisted grand total when initialization is needed.
- **Main flow:** The payment proxy verifies the owned order and pending states. If the pending payment has no transaction ID, call `/api/payments/initiate`. Call `/api/payments/simulate` with `isSuccess=true`, then reload the order. The service sets `Payment.SUCCESS`, `OrderGroup.PAID`, moves eligible sub-orders to `WAITING_SELLER_CONFIRM`, and directly notifies the customer and sellers.
- **Alternative/error flows:** Unauthorized order/transaction access is denied; mismatched transaction/group IDs are rejected. Backend failure simulation sets payment/group to `FAILED`. A repeated successful backend callback is idempotent; attempting failure after success or any callback after refund is rejected. The frontend rejects a second payment action when the order is no longer pending and asks for a refresh after uncertain timeout.
- **Postconditions:** Successful simulation produces paid order state. No money is charged externally; method labels, transaction IDs and callbacks are local demonstration behavior.
- **Sources:** [PaymentController](../../backend/src/main/java/project/project/Controller/PaymentController.java), [PaymentServiceImp](../../backend/src/main/java/project/project/Service/implement/PaymentServiceImp.java), [payment proxy](../../frontend/src/app/api/payment/%5BorderGroupId%5D/route.ts), [Payment UI](../../frontend/src/components/Payment/index.tsx).

### UC09 — View own order history, details and tracking

- **Actor:** Customer. **Availability:** UI at `/order-history` and the payment/order views.
- **Trigger:** Open history, filter by status or expand an order.
- **Preconditions:** `CUSTOMER` session and order ownership.
- **Main flow:** `GET /api/checkout/orders` returns owned groups newest first; `/api/checkout/orders/{id}` returns a selected group. Show seller sub-orders, totals, order/payment state and available shipment details; offer actions according to current state.
- **Alternative/error flows:** Another customer's group is unavailable. No history produces an empty state; failure supports retry. Authorized sellers/admins have separate sub-order read APIs, not access to another customer's checkout history endpoint.
- **Postconditions:** No business data changes.
- **Sources:** [CustomerCheckoutService](../../backend/src/main/java/project/project/Service/implement/CustomerCheckoutService.java), [SubOrderController](../../backend/src/main/java/project/project/Controller/SubOrderController.java), [ShippingController](../../backend/src/main/java/project/project/Controller/ShippingController.java), [OrderHistory](../../frontend/src/components/OrderHistory/index.tsx).

### UC10 — Cancel own sub-order

- **Actor:** Customer. **Availability:** UI in order history and pending payment cancellation.
- **Trigger:** Enter a reason and request cancellation.
- **Preconditions:** Customer owns the order; state is `PENDING_PAYMENT`, `WAITING_SELLER_CONFIRM` or `PREPARING`.
- **Main flow:** `POST /api/sub-orders/{orderId}/cancel` checks customer ownership and locks state, stores the reason, sets `CANCELLED` and restores item stock. For a paid/partially refunded group, refund this shop's total; otherwise mark an all-cancelled group `FAILED`. Notify the seller.
- **Alternative/error flows:** Missing reason, foreign order or shipped/completed order is rejected. Already-cancelled order is idempotent. Cancellation of one shop leaves other sub-orders active. The payment-page DELETE proxy issues cancellation calls per eligible sub-order but currently does not inspect each upstream cancellation response; refresh the backend order state to confirm its result.
- **Postconditions:** Successfully cancelled order is cancelled once, inventory restored and applicable simulated refund recorded.
- **Sources:** [SubOrderController](../../backend/src/main/java/project/project/Controller/SubOrderController.java), [SubOrderServiceImp](../../backend/src/main/java/project/project/Service/implement/SubOrderServiceImp.java), [sub-order proxy](../../frontend/src/app/api/sub-orders/%5B%5B...path%5D%5D/route.ts), [payment proxy](../../frontend/src/app/api/payment/%5BorderGroupId%5D/route.ts).

### UC11 — Confirm delivery receipt

- **Actor:** Customer. **Availability:** UI in order history.
- **Trigger:** Confirm receipt of a shipped order.
- **Preconditions:** Owned sub-order is `SHIPPED` (or already `COMPLETED` for a repeat).
- **Main flow:** `POST /api/sub-orders/{orderId}/confirm-delivered` resolves the current customer, checks ownership, locks the state and changes the order to `COMPLETED` with `completedAt`.
- **Alternative/error flows:** Foreign order or another order state is denied. Repeated confirmation after completion returns without another mutation. The scheduler also auto-confirms eligible shipped orders after seven days; customer confirmation does not change the separate shipment's status.
- **Postconditions:** Order is completed and purchased items can become review-eligible.
- **Sources:** [SubOrderController](../../backend/src/main/java/project/project/Controller/SubOrderController.java), [SubOrderServiceImp](../../backend/src/main/java/project/project/Service/implement/SubOrderServiceImp.java), [OrderSchedulerService](../../backend/src/main/java/project/project/Service/implement/OrderSchedulerService.java), [OrderHistory](../../frontend/src/components/OrderHistory/index.tsx).

### UC12 — Create or edit an own product review

- **Actor:** Customer. **Availability:** UI for completed order items.
- **Trigger:** Submit or edit a rating and comment.
- **Preconditions:** For creation, the customer owns the purchased item, its order is `COMPLETED`, and no review exists. For editing, the customer owns the review.
- **Main flow:** Check eligibility, call `POST /api/reviews` with the order item and rating 1–5, save the review, mark the item reviewed, update product rating/count and notify its seller. `PUT /api/reviews/{reviewId}` updates an owned review and recalculates the product average.
- **Alternative/error flows:** Foreign item/review, incomplete order, duplicate review or invalid rating is rejected. `generateAutoFiveStarReview` exists as an internal service method for unreviewed completed items and marks reviews `isAutoReview`, but no scheduler or controller invokes it in the current code; automatic review timing is not an implemented workflow.
- **Postconditions:** One review is associated with the purchased item; product rating data is updated.
- **Sources:** [ReviewController](../../backend/src/main/java/project/project/Controller/ReviewController.java), [ReviewServiceImp](../../backend/src/main/java/project/project/Service/implement/ReviewServiceImp.java), [OrderSchedulerService](../../backend/src/main/java/project/project/Service/implement/OrderSchedulerService.java), [OrderHistory](../../frontend/src/components/OrderHistory/index.tsx).

### UC13 — Apply to sell and view application history

- **Actor:** Customer; an approved Seller may read their existing applications. **Availability:** UI at `/seller-application` and `/seller-application/apply`.
- **Trigger:** Submit shop, identity and bank details/documents or open application history.
- **Preconditions:** Submission requires `CUSTOMER` role, no existing seller/shop and no `PENDING` or `APPROVED` application.
- **Main flow:** Upload supporting images using UploadThing, submit `POST /api/seller-applications`, save a new `PENDING` application and notify admins. `GET /api/seller-applications/mine` shows the user's history and review notes.
- **Alternative/error flows:** Invalid documents/fields, duplicate pending/approved application or existing seller record is rejected. After `REJECTED` or `NEED_MORE_DOC`, the form can prefill previous details and submit a new application record; the old application is retained. Other users' application detail is denied.
- **Postconditions:** A new pending application awaits UC21; the customer role remains until approval.
- **Sources:** [SellerApplicationController](../../backend/src/main/java/project/project/Controller/SellerApplicationController.java), [SellerApplicationServiceImp](../../backend/src/main/java/project/project/Service/implement/SellerApplicationServiceImp.java), [SellerApplicationForm](../../frontend/src/components/Seller/SellerApplicationForm.tsx), [upload endpoints](../../frontend/src/app/api/uploadthing/core.ts).

## Seller operations

### UC15 — View shop dashboard and shop orders

- **Actor:** Seller. **Availability:** UI at `/seller-dashboard` and `/seller/{orderId}`.
- **Trigger:** Open the dashboard/order, refresh, or focus the page.
- **Preconditions:** Active `SELLER` user with their own active shop.
- **Main flow:** The seller-order proxy verifies identity, resolves the owned shop, loads shop sub-orders and shipment data, and returns validated data. Display active/history tabs, paid sales and counts derived from those orders. Refresh on focus and periodically while visible.
- **Alternative/error flows:** Unauthenticated users go to sign-in; non-sellers/inactive shops are denied. Foreign seller/order IDs are denied. Invalid/inconsistent state blocks order actions; failed refresh may show previously loaded data with an outdated-data message.
- **Postconditions:** No business data changes.
- **Sources:** [seller-order proxy](../../frontend/src/app/api/seller-orders/%5B%5B...path%5D%5D/route.ts), [OrderAccess](../../backend/src/main/java/project/project/Security/OrderAccess.java), [SellerDashboard](../../frontend/src/components/Seller/SellerDashboard.tsx), [useSellerOrders](../../frontend/src/components/Seller/useSellerOrders.ts).

### UC16 — Create or update own products

- **Actor:** Seller. **Availability:** UI for creation at `/seller/products/add`; update API exists.
- **Trigger:** Submit a new product with images/categories, or call the product update API.
- **Preconditions:** Creation requires `SELLER` role and their own active shop; valid name, price, stock, image URLs and category IDs.
- **Main flow:** Upload product images with UploadThing, then `POST /api/products?sellerId=...`; service creates the active product and related image/category records. `PUT /api/products/{id}` updates supplied fields for the specified shop; seller identity can supply the shop ID automatically.
- **Alternative/error flows:** Another seller's shop, invalid category/image fields or invalid price/stock rejects creation. Abandoned uploaded product images have cleanup support. Product update is API-only in the current pages and its controller does not enforce a universal `SELLER`-only policy for all authenticated roles; service validates the supplied seller/product relationship.
- **Postconditions:** Successful product data is persisted and returned; no new shopping features are implied by template pages.
- **Sources:** [ProductController](../../backend/src/main/java/project/project/Controller/ProductController.java), [ProductServiceImp](../../backend/src/main/java/project/project/Service/implement/ProductServiceImp.java), [SellerAddProduct](../../frontend/src/components/Seller/SellerAddProduct.tsx), [product image cleanup](../../frontend/src/app/api/seller-product-images/cleanup/route.ts).

### UC17 — Accept or reject a paid shop order

- **Actor:** Seller. **Availability:** UI at `/seller/{orderId}`.
- **Trigger:** Accept the order or submit a rejection reason.
- **Preconditions:** Seller owns the order, the shop is active, group is `PAID` or `PARTIALLY_REFUNDED`, and order is `WAITING_SELLER_CONFIRM`.
- **Main flow:** Accept calls `POST /api/sub-orders/{orderId}/accept` and changes the order to `PREPARING`. Reject calls `/reject`, stores the reason, cancels that sub-order, restores its item stock and records its total as a partial/full simulated refund. Customer notifications describe the result.
- **Alternative/error flows:** Unpaid, foreign or wrong-state order is rejected; blank/overlong rejection reason is invalid. Rejection affects this shop only. Frontend reconciles server data after uncertain requests instead of automatically replaying a mutation.
- **Postconditions:** Order is preparing, or cancelled with inventory/refund updates and the group's applicable refund status.
- **Sources:** [SubOrderController](../../backend/src/main/java/project/project/Controller/SubOrderController.java), [SubOrderServiceImp](../../backend/src/main/java/project/project/Service/implement/SubOrderServiceImp.java), [SellerOrder](../../frontend/src/components/Seller/SellerOrder.tsx), [useSellerOrders](../../frontend/src/components/Seller/useSellerOrders.ts).

### UC18 — Assign courier and tracking number

- **Actor:** Seller. **Availability:** UI at `/seller/{orderId}`.
- **Trigger:** Submit courier and tracking fields.
- **Preconditions:** Owned order is `PREPARING`; group is paid/partially refunded; both fields are nonblank and no more than 100 characters.
- **Main flow:** Seller proxy forwards to `POST /api/shipping/orders/{orderId}/assign-tracking`. Lock and validate the order, create/update its shipment, set shipment and order to `SHIPPED` with timestamps, and notify the customer.
- **Alternative/error flows:** Foreign/unpaid/wrong-state order or invalid tracking fields is rejected. Subsequent calls after the order is shipped fail the preparation-state guard. Courier names/tracking numbers are saved manually; no courier tracking integration is implemented.
- **Postconditions:** Shipment data is persisted and the order can be confirmed received in UC11.
- **Sources:** [ShippingController](../../backend/src/main/java/project/project/Controller/ShippingController.java), [ShippingServiceImp](../../backend/src/main/java/project/project/Service/implement/ShippingServiceImp.java), [seller-order proxy](../../frontend/src/app/api/seller-orders/%5B%5B...path%5D%5D/route.ts), [SellerOrder](../../frontend/src/components/Seller/SellerOrder.tsx).

### UC19 — Manage shop profile and bank account

- **Actor:** Seller; Admin may read a shop bank account. **Availability:** API only for these management operations.
- **Trigger:** Read/update shop profile or add/update bank-account details.
- **Preconditions:** Profile/bank writes require the seller's own active shop. Bank reads require that seller or Admin.
- **Main flow:** `PUT /api/seller/shops/{sellerId}` saves profile details. `POST` or `PUT /api/seller/shops/{sellerId}/bank-account` adds/updates the bank account; authenticated authorized GET reads it.
- **Alternative/error flows:** Foreign/inactive shop, non-seller write, invalid request fields or missing bank account is rejected. Public shop GET exposes `SellerShopResponse`, not the bank-account endpoint. Saved bank details do not implement payouts.
- **Postconditions:** Shop/bank details persist after a successful write.
- **Sources:** [SellerShopController](../../backend/src/main/java/project/project/Controller/SellerShopController.java), [SellerShopServiceImp](../../backend/src/main/java/project/project/Service/implement/SellerShopServiceImp.java), [OrderAccess](../../backend/src/main/java/project/project/Security/OrderAccess.java).

### UC20 — Reply to a product review

- **Actor:** Seller. **Availability:** API only for reply management.
- **Trigger:** Submit a reply for a review on the seller's product.
- **Preconditions:** Own active seller shop; review exists on a product belonging to that shop; nonblank reply.
- **Main flow:** `POST /api/reviews/{reviewId}/reply?sellerId=...` checks identity/shop ownership, creates or updates the review's single reply, records reply time and notifies the customer.
- **Alternative/error flows:** Foreign product review, inactive/non-seller account, missing review or blank message is rejected.
- **Postconditions:** Reply is saved and can be displayed with public product reviews.
- **Sources:** [ReviewController](../../backend/src/main/java/project/project/Controller/ReviewController.java), [ReviewServiceImp](../../backend/src/main/java/project/project/Service/implement/ReviewServiceImp.java), [OrderAccess](../../backend/src/main/java/project/project/Security/OrderAccess.java).

## Administration

### UC21 — Review seller applications

- **Actor:** Admin. **Availability:** UI at `/admin`.
- **Trigger:** Open pending/history application details, approve, reject or request documents.
- **Preconditions:** Admin session; application and reviewing user exist. Approval additionally rejects an already-approved application or an applicant already holding a seller role/shop.
- **Main flow:** Admin proxy verifies the role, retrieves application data and forwards `/approve`, `/reject` or `/request-docs`. Approval saves reviewer/time, changes the user to `SELLER`, creates an `ACTIVE` seller and bank account, and notifies the applicant. Rejection stores `REJECTED` with reason; requesting documents stores `NEED_MORE_DOC` with the message.
- **Alternative/error flows:** Anonymous users return 401; non-admins return 403 through `SellerApplicationAdminFilter`. Duplicate approval/existing seller fails. Reject/request-docs service methods have no current-state transition guard; they must not be described as strictly pending-only. Applicant resubmission creates a new record (UC13).
- **Postconditions:** Decision and audit fields are saved; successful normal approval enables the seller shop workflow.
- **Sources:** [SellerApplicationController](../../backend/src/main/java/project/project/Controller/SellerApplicationController.java), [SellerApplicationAdminFilter](../../backend/src/main/java/project/project/Filter/SellerApplicationAdminFilter.java), [SellerApplicationServiceImp](../../backend/src/main/java/project/project/Service/implement/SellerApplicationServiceImp.java), [AdminApplications](../../frontend/src/components/Admin/AdminApplications.tsx).

### UC22 — Create and delete categories

- **Actor:** Admin. **Availability:** UI at `/admin/categories`.
- **Trigger:** Submit a category or delete a selected category.
- **Preconditions:** Admin session; valid category name on creation; selected category exists for deletion.
- **Main flow:** Proxy forwards `POST /api/categories` or `DELETE /api/categories/{categoryId}`. Controller checks Admin; service validates category uniqueness and persists or deletes. Public readers can retrieve the updated category list.
- **Alternative/error flows:** Non-admin mutations are denied; duplicate/invalid name, missing category or a category still used by products is rejected. No category edit endpoint is implemented.
- **Postconditions:** Category is added or an unused category is removed.
- **Sources:** [CategoryController](../../backend/src/main/java/project/project/Controller/CategoryController.java), [CategoryServiceImp](../../backend/src/main/java/project/project/Service/implement/CategoryServiceImp.java), [AdminCategories](../../frontend/src/components/Admin/AdminCategories.tsx), [admin proxy](../../frontend/src/app/api/admin/%5B...path%5D/route.ts).

### UC23 — Process payment callbacks and refunds

- **Actor:** Admin. **Availability:** API only; no external gateway integration.
- **Trigger:** Submit `POST /api/payments/callback`, `/refund/partial` or `/refund/full`.
- **Preconditions:** Admin session; existing transaction/group/order. Refund requires positive amount (partial), reason, successful/partially refunded payment and sufficient remaining refundable amount.
- **Main flow:** Callback delegates to the same local payment-state handler as UC08. Partial refund records the amount, sets the sub-order cancelled and updates payment/group refund state. Full refund records the full amount and cancels non-completed sub-orders while preserving completed sub-orders.
- **Alternative/error flows:** Non-admin requests are denied. Amount exceeding sub-order total or remaining payment balance, invalid state, absent transaction or blank reason fails. Duplicate success callback is idempotent; failure-after-success and callbacks-after-refund fail. Direct refund methods do not restore inventory; UC10/UC17 perform restoration before their refund calls.
- **Postconditions:** Payment/refund accounting and relevant order states are updated locally; no funds transfer occurs externally.
- **Sources:** [SecurityConfig](../../backend/src/main/java/project/project/Config/SecurityConfig.java), [PaymentController](../../backend/src/main/java/project/project/Controller/PaymentController.java), [PaymentServiceImp](../../backend/src/main/java/project/project/Service/implement/PaymentServiceImp.java).

### UC24 — Update shipment status

- **Actor:** Admin. **Availability:** API only.
- **Trigger:** `PATCH /api/shipping/shipments/{shipmentId}/status`.
- **Preconditions:** Admin session, existing shipment and valid `ShippingStatus` value.
- **Main flow:** Lock the associated order, parse the requested status, save it and set `shippedAt` when first shipped or `deliveredAt` when delivered.
- **Alternative/error flows:** Non-admin, missing shipment or invalid enum fails. Current implementation accepts any enum value without a progression guard, including regressions; it does not update the order to `COMPLETED`.
- **Postconditions:** Shipment status/timestamps change; customer receipt confirmation remains UC11.
- **Sources:** [ShippingController](../../backend/src/main/java/project/project/Controller/ShippingController.java), [ShippingServiceImp](../../backend/src/main/java/project/project/Service/implement/ShippingServiceImp.java), [ShippingStatus](../../backend/src/main/java/project/project/Entity/order/ShippingStatus.java).

### UC25 — Manage customer records

- **Actor:** Admin; Customer may read/update/delete their own record. **Availability:** API only for record administration; account-profile UI uses UC04.
- **Trigger:** Call customer create/list/detail/update/delete routes.
- **Preconditions:** `/api/customers` root operations require Admin through Spring Security. Detail/update/delete require Admin or the owning customer.
- **Main flow:** `POST /api/customers` creates a customer account; `GET /api/customers` lists customer responses. `GET`, `PUT` or `DELETE /api/customers/{id}` checks ownership or Admin and reads, updates or deletes the record.
- **Alternative/error flows:** Anonymous/non-admin collection access is denied; foreign customer access is denied. Missing records return 404; validation, duplicate account data or persistence constraints can fail mutations.
- **Postconditions:** Read operations leave data unchanged; successful create/update/delete modifies the requested customer data.
- **Sources:** [CustomerController](../../backend/src/main/java/project/project/Controller/CustomerController.java), [CustomerServiceImp](../../backend/src/main/java/project/project/Service/implement/CustomerServiceImp.java), [SecurityConfig](../../backend/src/main/java/project/project/Config/SecurityConfig.java).

## Additional exposed APIs and current limitations

### UC26 — Legacy direct seller registration

- **Actor:** Guest. **Availability:** API only at `POST /api/auth/register/seller`.
- **Trigger:** Submit the legacy seller registration payload.
- **Preconditions:** Valid credentials/shop/identity/bank fields; unique username, email and shop name; matching confirmation when supplied.
- **Main flow:** Register a user, submit a `PENDING` application before a shop exists, then change the user's role to `SELLER` and create a `PENDING` seller/shop and bank account in the same transaction.
- **Alternative/error flows:** Invalid/duplicate input or database failure rolls back. The created role/shop conflict with normal `approveApplication` guards, which reject existing sellers. This path therefore does not provide the working customer-application-to-active-shop approval workflow; it is not the frontend `/signup` flow.
- **Postconditions:** Legacy pending seller records exist; an active shop is not established by this call.
- **Sources:** [AuthController](../../backend/src/main/java/project/project/Controller/AuthController.java), [AuthServiceImp](../../backend/src/main/java/project/project/Service/implement/AuthServiceImp.java), [SellerServiceImp](../../backend/src/main/java/project/project/Service/implement/SellerServiceImp.java), [SellerApplicationServiceImp](../../backend/src/main/java/project/project/Service/implement/SellerApplicationServiceImp.java).

### UC27 — Deduct or restore product stock directly

- **Actor:** Any authenticated role under the current controller security. **Availability:** API only.
- **Trigger:** `POST /api/products/{id}/deduct-stock?quantity=...` or `/restore-stock?quantity=...`.
- **Preconditions:** Valid session, existing product and positive quantity; deduction requires sufficient stock.
- **Main flow:** Product service validates quantity and deducts or restores product stock.
- **Alternative/error flows:** Invalid quantity, missing product or insufficient stock fails. The controller currently adds no role or product ownership check beyond default authentication; this is a documented access-control limitation, not an intended administrator-only policy.
- **Postconditions:** Stock changes for the requested product after success.
- **Sources:** [ProductController](../../backend/src/main/java/project/project/Controller/ProductController.java), [ProductServiceImp](../../backend/src/main/java/project/project/Service/implement/ProductServiceImp.java), [SecurityConfig](../../backend/src/main/java/project/project/Config/SecurityConfig.java).

## Exact public backend route inventory

This inventory distinguishes configured matcher **patterns** from implemented controller **route templates**. `{id}` denotes a path variable; `*` is the actual Spring Security single-segment pattern, not a concrete URL. Unlisted methods/routes require authentication by default and may have additional role/ownership checks. Public permission does not guarantee successful input validation, a matching handler or an existing resource.

| Method | Exact implemented route template | Public security rule | Controller / behavior |
| --- | --- | --- | --- |
| GET | `/status` | exact `/status` | `StatusController`: health status |
| POST | `/api/auth/login` | exact route | `AuthController`: credentials + rate limit |
| POST | `/api/auth/register/customer` | exact route | `AuthController`: customer registration |
| POST | `/api/auth/register/seller` | exact route | `AuthController`: legacy seller registration, UC26 |
| GET | `/api/auth/token` | exact route | `AuthController`: explicit Bearer argument still required for token validation |
| GET, HEAD | `/api/products` | exact `/api/products` | `ProductController`: active products; implicit HEAD mapping |
| GET, HEAD | `/api/products/{id}` | `/api/products/*` | `ProductController`: product detail; implicit HEAD mapping |
| GET, HEAD | `/api/products/search` | `/api/products/*` | `ProductController`: paged search; implicit HEAD mapping |
| GET, HEAD | `/api/products/catalog-summary` | `/api/products/*` | `ProductController`: summary; implicit HEAD mapping |
| GET | `/api/products/seller/{sellerId}` | `/api/products/seller/*` | `ProductController`: seller products; HEAD is not public in the configured rules |
| GET, HEAD | `/api/categories` | exact `/api/categories` | `CategoryController`: categories; implicit HEAD mapping |
| GET | `/api/categories/{categoryId}/products` | `/api/categories/*/products` | `CategoryController`: category products; HEAD is not public in the configured rules |
| GET, HEAD | `/api/reviews/product/{productId}` | `/api/reviews/product/*` | `ReviewController`: product reviews; implicit HEAD mapping |
| GET | `/api/seller/shops/{sellerId}` | `/api/seller/shops/*` | `SellerShopController`: public shop profile; HEAD is not public |
| GET | `/api/seller/shops/user/{userId}` | `/api/seller/shops/user/*` | `SellerShopController`: public shop by user; HEAD is not public |

Additional explicitly public configured paths without a REST-controller mapping found in this repository:

| Method | Configured path | Interpretation |
| --- | --- | --- |
| GET | `/favicon.ico` | Permitted static-resource path; no controller handler found |
| POST | `/api/sellers/register` | Permitted legacy alias; no controller handler found, not a second implemented registration API |

Configured anonymous patterns are only the ones shown above. In particular, `/api/products/category/{categoryId}`, bank-account routes, checkout/cart/address/order/notification operations, logout/password change and administrative actions are not anonymous public APIs. CORS processes eligible `OPTIONS` preflight for exact configured origins and allowed methods/headers; that is separate from the controller route inventory. Backend Bearer endpoints do not authenticate using browser cookies.

Sources: [SecurityConfig](../../backend/src/main/java/project/project/Config/SecurityConfig.java), [StatusController](../../backend/src/main/java/project/project/Controller/StatusController.java), [AuthController](../../backend/src/main/java/project/project/Controller/AuthController.java), [ProductController](../../backend/src/main/java/project/project/Controller/ProductController.java), [CategoryController](../../backend/src/main/java/project/project/Controller/CategoryController.java), [ReviewController](../../backend/src/main/java/project/project/Controller/ReviewController.java), [SellerShopController](../../backend/src/main/java/project/project/Controller/SellerShopController.java).

## Features excluded from the implemented use cases

Template components for coupons/discounts, wishlist-style interactions, contact/newsletter/social-login controls, sample catalogue/shops and other static template pages are not treated as implemented backend business workflows. There is no external payment-gateway charge, automatic courier tracking, payout integration, anonymous forgotten-password email flow, or complete UI for administrative refunds/customer management/shipment status/shop bank management/review replies. Scheduler jobs are internal automated behavior, not user-initiated use cases.
