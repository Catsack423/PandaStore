# Defect fixes and demo data

Each defect is handled in its own frontend-only commit with the demo data needed
to review it. Backend code is outside the scope of these fixes.

| Defect | Work |
| --- | --- |
| Def-37 | Consistent Home product image frames and spacing; mixed image ratios |
| Def-39 | Navigation usability |
| Def-40 | Account menu click targets |
| Def-41 | Consistent Thai baht prices |
| Def-42 | Consistent order status presentation |
| Def-43 | Review buttons before and after reviewing |
| Def-44 | Explain why an address used in an order cannot be deleted |
| Def-45 | Reduce unused cart drawer space |
| Def-46 | Seller Applications usability and explanations |
| Def-47 | Explicit shipping fee labels in checkout |

## Def-37 review

The local `pandastore` database already has 12 additional demo products in the
`seller1` shop, including portrait (600 × 900), landscape (1000 × 500), square
(600 × 600), and missing-image examples. They are real database records and
support product details, cart, and checkout. Open the frontend Home page with the
backend running to review them; no manual product entry or reset is required.

These records persist in the current local database. This commit does not change
backend startup or automatically populate a new database.

Check Home at desktop and mobile widths: image frames should be square, keep
their size while loading, show the complete image without stretching or cropping,
and use the placeholder for the product without an image.

Demo customer: `customer1` / `password123`.
Demo seller: `seller1` / `password123`.
