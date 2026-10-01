# Demo payment flow

The Payment route is `/payment/[orderGroupId]`. Checkout success and pending orders in Order History link to it. It loads the authenticated customer's actual order total, including shipping, without caching.

## Enable locally

In `frontend/.env.local`, set:

```dotenv
NEXT_PUBLIC_MOCK_PAYMENT=true
MOCK_PAYMENT_ENABLED=true
```

In `backend/.env`, set:

```dotenv
MOCK_PAYMENT_ENABLED=true
```

Restart both servers after changing flags. All flags default to disabled; `.env.example` intentionally uses `false`. Keep them disabled in production. The backend independently enforces its private flag even if a browser bypasses the disabled button or the Next.js proxy.

The supplied image is `public/images/payment/qr-code.svg`, displayed with `next/image` at 260 × 260. No QR is generated. A failed image load displays **QR code image not found**. To use a PNG later, put it in `public/images/payment/payment-qr.png` and change `qrSource` in the Payment route.

## Verify the flow

1. Sign in as a customer, add products, and place an order. The original checkout success screen remains; it now has **Pay now**, **Continue shopping**, and **View order**.
2. Click **Pay now**. Compare the products, shipping, and total with Checkout. Click **Confirm payment**. Both buttons disable during the request, followed by **Payment successful (demo)**.
3. Open **Order History**. Every sub-order in that group must show **Awaiting seller confirmation**; payment is **PAID** in Details. **Pay now** disappears. Reloading the Payment URL shows its current status without payment/cancellation buttons.
4. Place another order and open **Pay now**. Click **Cancel order**, then confirm **Cancel this order?**. Order History opens with a toast, and every sub-order shows **Cancelled**. The stock reserved at checkout is returned once.
5. With the flags off, the Payment screen shows **Payment is not available**, confirmation is disabled, and server requests to confirm are rejected. Unpaid cancellation still works.
6. Try another customer's group, an absent group, or an invalid identifier: the page returns 404/access denial and cannot mutate it. Test slow/offline requests, retry, missing QR, keyboard dialog controls, and mobile button stacking.

Cancellation uses the existing unpaid-failure transition: sub-orders become `CANCELLED`; the payment/group payment status becomes `FAILED` because no money was collected. Confirmation uses the existing transition `PENDING_PAYMENT` → `WAITING_SELLER_CONFIRM`, with payment `SUCCESS` and group payment `PAID`. No database fields or migrations were added. A shared group lock serializes transitions. The legacy `/api/payments/simulate` endpoint uses the same guards.

The central backend integration point is `CustomerPaymentService.confirmPayment`; its TODO marks the replacement with a verified gateway/webhook. The old unsigned `/api/payments/callback` is disabled (501) until a real provider's webhook signature and amount verification are implemented, so it cannot bypass the demo flag or ownership checks.

## Checks

From `frontend`:

```powershell
node node_modules/typescript/bin/tsc --noEmit --incremental false
npm run lint
node --test --test-isolation=none tests/*.test.cjs
```

From `backend` (JDK 25):

```powershell
mvn -Dtest=CustomerCheckoutTest,PaymentServiceTest,SubOrderServiceTest,OrderOrchestrationControllerTest test
```

Integration tests use an isolated H2 database and cover multi-shop payment totals, owner/non-owner/anonymous requests, missing groups, private flag enforcement, the legacy simulation endpoint, repeated transitions, cancellation and stock restoration. Frontend tests cover the proxy, flags, uncached server loading, status display rules, errors and role restrictions. Browser UI checks use a temporary fixture; it is removed before commit and never touches customer orders.

Verified: TypeScript passes; full frontend lint has zero errors and nine existing warnings; the focused frontend suite passes 32/32; the backend command above passes 65/65. The full frontend suite passes 64/65: the unchanged `seller-product.test.cjs` Skeleton test expects loading to stop immediately on the original image error, while the existing `ProductImage` keeps its Skeleton until the fallback image settles. Both files are outside this payment change.

## Files changed or added

| Area | Files |
| --- | --- |
| Payment UI and route | `src/components/Payment/index.tsx`, `src/components/Payment/api.ts`, `src/app/(site)/(pages)/payment/[orderGroupId]/page.tsx` |
| Entry points | `src/components/Checkout/index.tsx`, `src/components/OrderHistory/index.tsx` |
| Session proxy and routing | `src/app/api/checkout/[[...path]]/route.ts`, `src/middleware.ts` |
| Backend | `backend/src/main/java/project/project/Service/implement/CustomerPaymentService.java`, `backend/src/main/java/project/project/Controller/CustomerCheckoutController.java`, `backend/src/main/java/project/project/Controller/PaymentController.java` |
| Tests | `tests/payment.test.cjs`, `tests/middleware-role.test.cjs`, `backend/src/test/java/project/project/Controller/CustomerCheckoutTest.java` |
| Configuration and asset | `frontend/.env.example`, `backend/.env.example`, `public/images/payment/qr-code.svg` |
| Instructions | `frontend/PAYMENT.md` |

Paths without a `backend/` or `frontend/` prefix are relative to `frontend/`.
