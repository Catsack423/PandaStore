# วิเคราะห์ Domain Model และ Service Layer
### จาก Use Case: ระบบ E-Commerce (Marketplace)

อ้างอิงจาก 6 Use Case: (1) ซื้อสินค้า/ชำระเงิน (2) คืนสินค้า (3) Seller สมัครร้านค้า (4) Seller ลงสินค้า (5) รีวิวสินค้า (6) อัปเดตโปรไฟล์/ร้านค้า

---

## ส่วนที่ 1: Domain Model (Entity/Class)

### 1.1 กลุ่ม User & Identity

| Entity | Attribute หลัก | ความสัมพันธ์ |
|---|---|---|
| **User** (base) | userId, username, email, passwordHash, phone, role {CUSTOMER, SELLER, ADMIN}, status {ACTIVE, SUSPENDED}, createdAt | 1 User → 1 Customer หรือ 1 Seller (role-based) |
| **Customer** | customerId, userId (FK), avatarUrl | 1 Customer → N Address, 1 Customer → N Order, 1 Customer → N Review, 1 Customer → N ReturnRequest |
| **Seller** | sellerId, userId (FK), shopName, shopDescription, shopLogoUrl, shopBannerUrl, shopEmail, shopPhone, shopAddress, status {PENDING, ACTIVE, REJECTED, SUSPENDED} | 1 Seller → 1 SellerApplication, 1 Seller → N Product, 1 Seller → N BankAccount, 1 Seller → N WarehouseAddress |
| **Admin** | adminId, userId (FK), permissionLevel | ตรวจสอบ/อนุมัติ SellerApplication, Dispute, Return, Review |
| **Address** | addressId, customerId (FK), receiverName, addressLine, district, province, postalCode, phone, isDefault | ใช้ใน Order (shipping address) |
| **BankAccount** | bankAccountId, sellerId (FK), accountName, bankName, accountNumber, proofImageUrl, verifyStatus {PENDING, VERIFIED, REJECTED} | ใช้ตอน Payout |
| **SellerApplication** | applicationId, sellerId (FK), idCardNumber, idCardImageUrl, status {PENDING, APPROVED, REJECTED, NEED_MORE_DOC}, rejectReason, reviewedBy (Admin) | สร้างจาก Seller Registration (UC3) |

### 1.2 กลุ่ม Product & Catalog

| Entity | Attribute หลัก | ความสัมพันธ์ |
|---|---|---|
| **Category** | categoryId, name, parentCategoryId (สำหรับหมวดหมู่ย่อย) | N Category ↔ M Product (Many-to-Many ผ่าน product_categories) |
| **Product** | productId, sellerId (FK), name, description, categories (Many-to-Many), price, weight, status {DRAFT, ACTIVE, OUT_OF_STOCK, HIDDEN}, coverImageUrl, avgRating, reviewCount, createdAt | 1 Product → N ProductImage, 1 Product → N ProductVariant, 1 Product → N Review, N Product ↔ M Category |
| **ProductImage** | imageId, productId (FK), imageUrl, isCover | — |
| **ProductVariant** | variantId, productId (FK), optionName (สี/ไซซ์/รุ่น), optionValue, stock, priceAdjustment | ตัด stock ตอน checkout (UC1) |
| **ShippingOption** (ของสินค้า) | productId (FK), shippingMethod, dimensionInfo | ใช้คำนวณค่าส่ง |

### 1.3 กลุ่ม Cart & Order

| Entity | Attribute หลัก | ความสัมพันธ์ |
|---|---|---|
| **Cart** | cartId, customerId (FK) | 1 Cart → N CartItem |
| **CartItem** | cartItemId, cartId (FK), productId (FK), variantId (FK), quantity | ตรวจสอบ stock ตอนเพิ่ม/checkout |
| **OrderGroup** | orderGroupId, customerId (FK), addressId (FK), paymentId (FK), totalAmount, createdAt | เกิดจาก 1 ครั้งของการกด "ชำระเงิน" — 1 OrderGroup → N Order (แตกตาม Seller), 1 OrderGroup → 1 Payment |
| **Order** *(sub-order ต่อร้าน)* | orderId, orderGroupId (FK), sellerId (FK), subTotal, shippingFee, discount, status {WAITING_SELLER_CONFIRM, PREPARING, SHIPPED, COMPLETED, CANCELLED}, createdAt | 1 Order → N OrderItem, 1 Order → 1 Shipping, N Order → 1 OrderGroup |
| **OrderItem** | orderItemId, orderId (FK), productId (FK), variantId (FK), priceAtPurchase, quantity | Snapshot ราคา ณ ตอนซื้อ |
| **Payment** | paymentId, orderGroupId (FK), method, amount, status {PENDING, SUCCESS, FAILED}, gatewayTransactionId, paidAt | ชำระครั้งเดียวที่ระดับ OrderGroup ไม่ผูกกับ Order ย่อยรายร้าน |
| **Shipping** | shippingId, orderId (FK), carrierName, trackingNumber, status {NOT_SHIPPED, SHIPPED, DELIVERED}, shippedAt, deliveredAt | แยกพัสดุ/tracking ต่อร้าน (ต่อ Order ย่อย) อัปเดตโดย Seller (UC1 step 41+) |

### 1.4 กลุ่ม Return / Dispute

| Entity | Attribute หลัก | ความสัมพันธ์ |
|---|---|---|
| **ReturnRequest** | returnId, orderId (FK), customerId (FK), reason, description, status {WAITING_SELLER, APPROVED, REJECTED, RETURNING, RETURNED, REFUND_PENDING, REFUNDED}, createdAt | 1 ReturnRequest → N ReturnItem, 1 ReturnRequest → N Evidence, 1 ReturnRequest → 0..1 Dispute |
| **ReturnItem** | returnItemId, returnId (FK), orderItemId (FK), quantity | ระบุว่าคืนสินค้าชิ้นไหน/จำนวนเท่าไร |
| **Evidence** | evidenceId, returnId (FK), imageUrl/videoUrl | หลักฐานแนบ |
| **Refund** | refundId, returnId (FK), amount, status {PENDING, SUCCESS, FAILED}, refundedAt | ผูกกับ Payment เดิม |
| **Dispute** | disputeId, returnId (FK), raisedBy, status {OPEN, RESOLVED}, adminDecision, decidedBy (Admin) | เกิดเมื่อ Seller/Customer ไม่เห็นด้วย |

### 1.5 กลุ่ม Review & Notification

| Entity | Attribute หลัก | ความสัมพันธ์ |
|---|---|---|
| **Review** | reviewId, customerId (FK), productId (FK), orderId (FK), rating (1-5), comment, imageUrls, videoUrl, isAnonymous, sellerReply, isReported, isHidden, createdAt | 1 Product → N Review (avgRating คำนวณจาก Review ทั้งหมด) |
| **Notification** | notificationId, userId (FK), type {ORDER_UPDATE, PAYMENT, RETURN, REVIEW, SELLER_APPROVAL}, message, isRead, createdAt | ส่งให้ Customer/Seller/Admin |

---

## ส่วนที่ 2: Service Layer (Business Logic)

| Service | หน้าที่หลัก | Method สำคัญ | ใช้ใน UC |
|---|---|---|---|
| **AuthService** | สมัครสมาชิก, ล็อกอิน, ตรวจสอบสิทธิ์, ยืนยัน OTP | `register()`, `login()`, `sendOtp()`, `verifyOtp()` | UC1 (1A), UC6 (OTP) |
| **UserProfileService** | จัดการข้อมูลโปรไฟล์ลูกค้า/ที่อยู่จัดส่ง | `updateProfile()`, `addAddress()`, `updateAddress()`, `deleteAddress()`, `setDefaultAddress()` | UC6 |
| **ProductService** | CRUD สินค้า, ค้นหา/แสดงรายการ, จัดการ stock | `createProduct()`, `updateProduct()`, `getProductDetail()`, `searchProducts()`, `checkStock()`, `deductStock()`, `restoreStock()` | UC1, UC4 |
| **CartService** | จัดการตะกร้าสินค้า | `addToCart()`, `updateQuantity()`, `removeItem()`, `getCartSummary()` | UC1 |
| **OrderService** | สร้าง/จัดการคำสั่งซื้อ, orchestrate checkout, **แบ่ง Cart ตาม Seller แล้ว fan-out สร้าง OrderGroup + Order ย่อย** | `checkout()`, `splitCartBySeller()`, `createOrderGroup()`, `createOrder()`, `updateOrderStatus()`, `getOrderHistory()` | UC1, UC2, UC5 |
| **PaymentService** | เชื่อมต่อ Payment Gateway, ประมวลผลชำระเงิน **ที่ระดับ OrderGroup เท่านั้น** (ไม่รู้จัก Order ย่อยรายร้าน) | `initiatePayment(orderGroup)`, `handlePaymentCallback()`, `processRefund()`, `processPartialRefund(order)` | UC1 (27A), UC2 (32A) |
| **ShippingService** | คำนวณค่าจัดส่ง, จัดการ tracking | `calculateShippingFee()`, `assignTracking()`, `updateShippingStatus()` | UC1 |
| **SellerApplicationService** | รับสมัคร/อนุมัติร้านค้า | `submitApplication()`, `requestMoreDocuments()`, `approveApplication()`, `rejectApplication()` | UC3 |
| **SellerShopService** | จัดการข้อมูลร้านค้า, บัญชีธนาคาร (payout) | `updateShopInfo()`, `updateBankAccount()`, `verifyBankAccount()` | UC6 |
| **ReturnService** | จัดการคำขอคืนสินค้าทั้ง flow | `createReturnRequest()`, `checkReturnEligibility()`, `sellerApprove()`, `sellerReject()`, `confirmReceived()`, `completeReturn()` | UC2 |
| **DisputeService** | จัดการข้อพิพาทระหว่าง Customer-Seller | `openDispute()`, `submitEvidence()`, `adminResolve()` | UC2 (18B, 28A, 28B) |
| **ReviewService** | สร้างรีวิว, ตอบกลับ, รายงาน, คำนวณคะแนนเฉลี่ย | `createReview()`, `checkPurchaseVerified()`, `sellerReply()`, `reportReview()`, `recalculateAverageRating()` | UC5 |
| **NotificationService** | แจ้งเตือนทุกฝ่าย (cross-cutting) | `notifyCustomer()`, `notifySeller()`, `notifyAdmin()` | ใช้ในทุก UC |
| **AdminModerationService** | งานฝั่ง Admin: อนุมัติร้าน, ตัดสิน dispute, ซ่อนรีวิว | `reviewSellerDocs()`, `resolveDispute()`, `hideReview()` | UC2, UC3, UC5 |

---

## ส่วนที่ 3: Mapping Service ↔ Entity (สรุปความสัมพันธ์)

- **OrderService** เป็น orchestrator หลักของ UC1 — เรียกใช้ `ProductService.checkStock()` → `PaymentService.initiatePayment()` → `ShippingService` → `NotificationService` (ตรงกับหลักการ Single Responsibility: OrderService ไม่คำนวณค่าส่งหรือตัดสต็อกเอง แต่เรียก Service อื่น)
- **ReturnService** และ **DisputeService** แยกกันเพราะ Return คือ flow ปกติ (happy path) ส่วน Dispute คือ exception เมื่อสองฝ่ายขัดแย้งกัน — ควรแยกเพื่อไม่ให้ ReturnService รับผิดชอบ logic การตัดสินของ Admin
- **SellerApplicationService** กับ **SellerShopService** แยกกันเพราะคนละ lifecycle: Application ใช้ครั้งเดียวตอนสมัคร (UC3), ShopService ใช้ตลอดเวลา (UC6)
- **NotificationService** เป็น cross-cutting service ที่ทุก Service อื่นเรียกใช้ (ไม่ผูกกับ UC ใดโดยเฉพาะ)
- **OrderGroup vs Order**: `PaymentService` ทำงานเฉพาะระดับ `OrderGroup` (จ่ายครั้งเดียว) ส่วน `ShippingService` และ `ReturnService` ทำงานเฉพาะระดับ `Order` ย่อยรายร้าน (จัดส่ง/คืนสินค้าเป็นเรื่องต่อร้าน) — แยก concern การเงินออกจากการปฏิบัติการของแต่ละร้านอย่างชัดเจน

### Checkout flow แบบ multi-seller (ขยายจาก UC1 ขั้นตอน 23-27)

1. ลูกค้ากด "ชำระเงิน" → `OrderService.splitCartBySeller()` แบ่ง `CartItem` ตาม `sellerId`
2. สร้าง `OrderGroup` 1 รายการ (ผูก customer + address)
3. Loop ต่อร้าน → สร้าง `Order` ย่อย 1 รายการ/ร้าน พร้อม `OrderItem` และคำนวณ `shippingFee` แยกตามร้าน (ต่างคลัง/ต่างวิธีจัดส่ง ค่าส่งไม่เท่ากัน)
4. รวมยอดทุก sub-order → เรียก `PaymentService.initiatePayment(orderGroup)` ครั้งเดียว
5. จ่ายสำเร็จ → เปลี่ยนสถานะทุก `Order` ย่อยเป็น `WAITING_SELLER_CONFIRM` พร้อมกัน แล้ว `NotificationService` แจ้งแต่ละร้านแยกกัน
6. กรณี UC1-37A (Seller ปฏิเสธ) กระทบเฉพาะ `Order` ย่อยของร้านนั้น — ร้านอื่นใน `OrderGroup` เดียวกันดำเนินการต่อได้ปกติ, คืนเงินเป็น partial refund ผ่าน `PaymentService.processPartialRefund(order)`

---

## ข้อสังเกตเชิงออกแบบ (Design Notes)

1. **Order เป็นระดับร้านค้าเดียว หรือหลายร้าน? (แก้ไขแล้ว)** — ใช้ `OrderGroup` (parent, ผูก Payment ระดับเดียว) + `Order` ย่อยต่อ Seller (ผูก Shipping/สถานะของร้านตัวเอง) ตามรายละเอียดในหัวข้อ "Checkout flow แบบ multi-seller" ด้านบน ยังมีจุดที่ต้องตัดสินใจเพิ่มเติม:
   - ยกเลิก 1 ร้านใน group: คืนเงินเฉพาะส่วนนั้น (partial refund) หรือยกเลิกทั้ง group แล้วให้ลูกค้าสั่งใหม่?
   - ค่าส่งรวม: คำนวณแยกต่อร้านแล้วบวกรวม หรือมีโปรโมชั่นส่งฟรีรวมเมื่อยอดถึงเกณฑ์ (ต้อง allocate ค่าส่งคืนกลับหากมีการคืนสินค้าบางส่วน)?
2. **State Machine ของ Order/Return** ควรทำเป็น enum ที่มี transition rule ชัดเจน เพราะมี status เปลี่ยนตามหลายเงื่อนไข (เช่น UC1 37A ที่ seller ปฏิเสธต้องคืน stock + คืนเงินอัตโนมัติ)
3. **Stock ต้องมี Lock/Transaction** ตอน checkout เพื่อป้องกัน race condition (สินค้าหมดระหว่างสองคนซื้อพร้อมกัน — ตรงกับ UC1 17A)
4. **Review ผูกกับ Order ไม่ใช่ Product อย่างเดียว** เพื่อยืนยันว่าซื้อจริง (verified purchase) ตาม UC5 pre-condition
