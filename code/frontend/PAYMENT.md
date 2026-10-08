# Payment frontend

The route `/payment/[orderGroupId]` loads the customer's real order through the existing `/api/checkout/orders/{id}` API. Checkout success and pending rows in Order History link to it. The supplied `public/images/payment/qr-code.svg` is displayed with `next/image` at 260 × 260; a failed image load shows **QR code image not found**.

## Existing APIs

Confirm payment calls the frontend proxy `/api/payment/{id}`. The proxy forwards the session token and:

1. Reads `/api/checkout/orders/{id}` to verify ownership and pending state.
2. Reads `/api/payments/order-group/{id}` for the persisted payment method and transaction ID.
3. Calls `/api/payments/initiate` with the persisted method and actual shipping-inclusive total if the transaction ID is missing.
4. Calls `/api/payments/simulate` with `isSuccess: true`.
5. Reloads the order from `/api/checkout/orders/{id}` and displays the returned status.

No backend endpoint, service, schema, or configuration is added or changed. The backend controllers and tests are restored to their previous versions. No payment environment flags are needed.

## Current API limitation

The existing `/api/sub-orders/{id}/cancel` accepts only `WAITING_SELLER_CONFIRM` or `PREPARING`. It rejects `PENDING_PAYMENT`. Cancellation on this Payment screen is therefore disabled with an explanation. The frontend does not invent a cancellation result or claim stock was returned.

## Verify

Sign in as a customer, place an order, and choose **Pay now**. Compare products, shipping and total with Checkout, then choose **Confirm payment**. The button disables during the request. Successful mock payment shows **Payment successful (demo)**. Open Order History: the order should show **Awaiting seller confirmation**, and Details should show **PAID**. Reloading the Payment URL displays the persisted state without payment buttons.

TypeScript: `node node_modules/typescript/bin/tsc --noEmit --incremental false`

Lint: `npm run lint`

Tests: `node --test --test-isolation=none tests/payment.test.cjs tests/middleware-role.test.cjs`

Frontend changes: Payment page/components/API proxy, Checkout and Order History links, middleware role routing, and frontend tests. Backend additions and changes from the original Payment commit are removed, along with the example environment flags.
