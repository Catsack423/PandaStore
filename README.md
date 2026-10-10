# PandaStore

> โครงงานรายวิชา **CP353002 Principles of Software Design and Development**
>
> มหาวิทยาลัยขอนแก่น (Khon Kaen University)

## ภาพรวม

PandaStore เป็นเว็บสำหรับซื้อและขายสินค้ามือ 2  ระบบมีผู้ใช้ 3 ประเภท:

1. **ลูกค้า (Customer):** ค้นหาสินค้า เพิ่มสินค้าลงตะกร้า สั่งซื้อ ระบุที่อยู่จัดส่ง ชำระเงินด้วย PromptPay QR Code และดูสถานะคำสั่งซื้อ
2. **ผู้ขาย (Seller):** ส่งคำขอเปิดร้านพร้อมเอกสาร KYC จัดการสินค้าในร้าน ยืนยันคำสั่งซื้อ และเพิ่มเลขพัสดุหลังจัดส่ง
3. **ผู้ดูแลระบบ (Platform Admin):** ตรวจสอบคำขอเปิดร้าน และจัดการหมวดหมู่สินค้า

## สมาชิกกลุ่ม

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Git Branch | หน้าที่รับผิดชอบหลัก |
| :---: | :--- | :---: | :---: | :--- | :--- |
| 1 | นาย ธนันชัย พันธราช (ด้า) | 673380042-8 | 2 | `Thananchai_673380042-8_sec2` | Backend: `SellerShopService`, `ShippingService`, `SubOrderService` |
| 2 | นาย ปวัฒน์ ปัดทุมมา (คิม) | 673380048-6 | 1 | `Pawat_673380048-6_SEC01` | Backend: `AuthService`, `CartService` |
| 3 | นาย ณัฐพงศ์ กรธนกิจ (เพชร) | 673380038-9 | 2 | `Nattapong_673380038-9_sec2` | Backend: `ProductService`, `ReviewService`, `SellerApplicationService` |
| 4 | นาย ธนกร ทองศรี (แซน) | 673380040-2 | 2 | `Thanakon_673380040-2_sec2` | Backend: `NotificationService`, `OrderOrchestrationService` |
| 5 | นาย ปิยะพล ตุ่นป่า | 673380280-2 | 1 | `Piyapon_673380280-2sec1` | Frontend: ออกแบบและพัฒนา Frontend (UI/UX Design & Components) |

### รายละเอียดหน้าที่รับผิดชอบ (Responsibilities Breakdown)

1. **นาย ปวัฒน์ ปัดทุมมา :**
   * **`AuthService.java`**: ระบบยืนยันตัวตน, จัดการสิทธิ์การเข้าใช้งาน (Authentication & Authorization), จัดการโทเคน JWT
   * **`CartService.java`**: ระบบจัดการตะกร้าสินค้า (Cart Management), เพิ่ม/ลบ/แก้ไขจำนวนสินค้าในตะกร้า
2. **นาย ธนกร ทองศรี :**
   * **`NotificationService.java`**: ระบบแจ้งเตือนผู้ใช้งานและผู้ขายเกี่ยวกับการสั่งซื้อและการเปลี่ยนแปลงสถานะ
   * **`OrderOrchestrationService.java`**: ระบบประสานงานและควบคุมวงจรคำสั่งซื้อ (Order Lifecycle & Orchestration)
3. **นาย ณัฐพงศ์ กรธนกิจ :**
   * **`ProductService.java`**: ระบบจัดการข้อมูลสินค้า (Product Management), เพิ่ม แก้ไข ลบ ค้นหา และแสดงรายการสินค้า
   * **`ReviewService.java`**: ระบบรีวิวและให้คะแนนสินค้า (Review & Rating Management)
   * **`SellerApplicationService.java`**: ระบบส่งคำขอเปิดร้านค้าและเอกสารยืนยันตัวตน (KYC) และการตรวจสอบอนุมัติโดยแอดมิน
4. **นาย ธนันชัย พันธราช :**
   * **`SellerShopService.java`**: ระบบจัดการร้านค้าของผู้ขาย (Seller Shop Profile & Configuration)
   * **`ShippingService.java`**: ระบบจัดส่งสินค้า (Shipping Management), คำนวณค่าจัดส่ง และบันทึกเลขติดตามพัสดุ
   * **`SubOrderService.java`**: ระบบจัดการคำสั่งซื้อย่อยแยกตามร้านค้า (Sub-Order Fulfillment)
   * **Automated Testing**: ออกแบบและพัฒนาระบบทดสอบ End-to-End ด้วย Robot Framework
5. **นาย ปิยะพล ตุ่นป่า:**
   * **Frontend Design & Development**: ออกแบบส่วนติดต่อผู้ใช้ (UI/UX Design) และพัฒนาหน้าเว็บ Frontend ด้วย Next.js, React, Tailwind CSS และ Radix UI / Shadcn UI รวมถึงการเชื่อมต่อ REST API

## เทคโนโลยีที่ใช้

### Backend

* **ภาษาและ Framework:** Java 21, Spring Boot 3.x
* **ฐานข้อมูล:** Spring Data JPA, Hibernate, PostgreSQL Driver
* **ระบบความปลอดภัยและเข้าสู่ระบบ:** Spring Security, JWT (jjwt 0.13.0)
* **เครื่องมือ Build:** Maven Wrapper (`./mvnw`)
* **เอกสาร API:** OpenAPI 3 และ Swagger UI

### Frontend

* **Framework:** Next.js 16 (App Router), React 19, TypeScript
* **หน้าตาและส่วนประกอบ:** Tailwind CSS, Radix UI / Shadcn UI, Lucide Icons
* **จัดการข้อมูลในหน้าเว็บ:** Redux Toolkit, TanStack React Query
* **อัปโหลดไฟล์:** UploadThing SDK

### การทดสอบและตรวจสอบโค้ด

* **Unit Test และ Integration Test:** JUnit 5, Mockito, Spring Boot Test
* **ทดสอบการใช้งานระบบอัตโนมัติ:** Robot Framework (SeleniumLibrary)
* **ตรวจสอบโค้ดและความปลอดภัย:** PMD ruleset, Gitleaks

### การ Deploy

* **Frontend:** Vercel
* **Backend และฐานข้อมูล:** Railway (Spring Boot และ Managed PostgreSQL)
* **CI/CD:** GitHub Actions

## โครงสร้างระบบ

Backend แบ่งโค้ดตามหน้าที่เป็นชั้นต่าง ๆ และใช้แนวคิด SOLID กับ Clean Code ในการจัดโครงสร้าง:

```text
Presentation Layer (Controller / RestController)
        ↓
Service Layer (กฎและการทำงานของระบบ)
        ↓
Repository Layer (อ่านและเขียนข้อมูลผ่าน Spring Data JPA)
        ↓
Domain / Entity Layer (ข้อมูลของระบบ)

DTO Layer (ข้อมูลที่รับและส่งผ่าน API) และ Mappers
```

รูปแบบการเขียนโปรแกรมที่ใช้ในโปรเจกต์:

* **Repository Pattern:** ใช้ Spring Data JPA ติดต่อฐานข้อมูล
* **Service Layer Pattern:** รวมการทำงานของระบบไว้ใน Service และใช้ `@Transactional` ควบคุม Transaction
* **DTO และ Mapper:** แปลงข้อมูลก่อนรับหรือส่งผ่าน API
* **Dependency Injection:** ใช้ Constructor Injection เพื่อส่งส่วนที่ต้องใช้ให้แต่ละคลาส
* **Observer Pattern:** ใช้ Spring Application Events ส่งต่อเหตุการณ์หลังบันทึกข้อมูล
* **Factory / Strategy Pattern:** ใช้จัดการวิธีชำระเงินและสถานะคำสั่งซื้อ
* **Builder Pattern:** ใช้สร้าง DTO และ Response Objects

## ฐานข้อมูลและเอกสารออกแบบ

โปรเจกต์ใช้ PostgreSQL และมีตารางมากกว่า 10 ตาราง พร้อม Foreign Key และ Index ตัวอย่างความสัมพันธ์ระหว่างข้อมูล:

* **One-to-One:** `User` กับ `SellerApplication`
* **One-to-Many:** `Customer` กับ `Order` และ `OrderItem`, `Seller` กับ `Product`

แผนภาพ ER, Class, Sequence และรายละเอียด Use Case อยู่ในโฟลเดอร์ [`docs/diagrams/`](docs/diagrams/)

## โครงสร้างโฟลเดอร์

```text
PandaStore/
├── code/
│   ├── backend/                 # Backend ด้วย Spring Boot และ Java 21
│   │   ├── src/main/java/project/project/
│   │   │   ├── Controller/      # รับคำขอจาก API
│   │   │   ├── Service/         # จัดการการทำงานของระบบ
│   │   │   ├── Repository/      # ติดต่อฐานข้อมูล
│   │   │   ├── Entity/          # โครงสร้างข้อมูล
│   │   │   ├── DTO/             # ข้อมูลรับเข้าและส่งออก
│   │   │   ├── Config/          # ตั้งค่าระบบ
│   │   │   ├── Exception/       # จัดการข้อผิดพลาด
│   │   │   └── Observer/        # รับและส่งต่อเหตุการณ์
│   │   └── src/test/            # JUnit 5 และ Mockito Tests
│   └── frontend/                # Frontend ด้วย Next.js, TypeScript และ Tailwind
│       └── src/
│           ├── app/             # หน้าเว็บและ API routes
│           ├── components/      # ส่วนประกอบของหน้าเว็บ
│           └── context/         # AuthContext และ CartContext
├── test/
│   └── Robot/                   # ทดสอบระบบด้วย Robot Framework
│       ├── 00_e2e_full_lifecycle.robot    # ทดสอบการใช้งานตั้งแต่ต้นจนจบ
│       ├── 01_user_register.robot ... 08_order_history.robot # ชุดทดสอบลูกค้า
│       ├── seller_01_*.robot ... seller_04_*.robot           # ชุดทดสอบผู้ขาย
│       ├── admin_01_*.robot ... admin_03_*.robot             # ชุดทดสอบผู้ดูแลระบบ
│       └── resources/           # Locators, Variables และ Keywords ที่ใช้ซ้ำ
├── docs/                        # เอกสารออกแบบระบบและรายงานความปลอดภัย
│   └── diagrams/                # แผนภาพและรายละเอียด Use Case
├── img/                         # รูปภาพและไฟล์ที่ใช้ทดสอบ
├── config/                      # กฎสำหรับ PMD
├── scripts/                     # สคริปต์ตรวจสอบโค้ดและความปลอดภัย
└── README.md                    # ไฟล์นี้
```

## ลิงก์ระบบ

| ระบบ | Platform | URL |
| :--- | :---: | :--- |
| **เว็บ Frontend** | Vercel | https://panda-store-kappa.vercel.app/ |
| **Backend REST API** | Railway | https://forty-nine-shop-production.up.railway.app/ |
| **Swagger UI** | Railway | https://forty-nine-shop-production.up.railway.app/swagger-ui.html |

## วิธีติดตั้งและรัน

### 1. รัน Backend

```bash
cd code/backend
./mvnw clean spring-boot:run
```

เปิด Swagger UI ได้ที่ `http://localhost:8080/swagger-ui.html`

### 2. รัน Frontend

```bash
cd code/frontend
npm install
npm run dev
```

เปิดเว็บได้ที่ `http://localhost:3000`

### 3. ทดสอบระบบด้วย Robot Framework

```bash
# ติดตั้ง Library ที่ต้องใช้
pip install -r test/Robot/requirements.txt

# รันชุดทดสอบ E2E
python -m robot test/Robot/00_e2e_full_lifecycle.robot

# รันแบบ Headless
python -m robot -v HEADLESS:True test/Robot/00_e2e_full_lifecycle.robot
```

## ผลการทดสอบอัตโนมัติ (Robot Framework Test Results)

ระบบผ่านการทดสอบอัตโนมัติแบบครบวงจร (End-to-End Full Lifecycle) ด้วย **Robot Framework** ครบทั้ง 8 Phase สำเร็จ **100% (Pass 8 / Fail 0)**:

| ลำดับ Phase | รายละเอียดการทดสอบ (Test Case) | ผลลัพธ์ | หลักฐานการทดสอบ (Screenshot) |
| :---: | :--- | :---: | :--- |
| **Phase 1** | Customer Account Registration (สมัครสมาชิกใหม่) | **PASS** | [`E2E_01_User_Registered.png`](img/robot/E2E_01_User_Registered.png) |
| **Phase 2** | User Submits Seller Application (ยื่นเอกสารขอเปิดร้านค้า/KYC) | **PASS** | [`E2E_02_Seller_Application_Submitted.png`](img/robot/E2E_02_Seller_Application_Submitted.png) |
| **Phase 3** | Admin Approves Seller Application (แอดมินอนุมัติคำขอเปิดร้าน) | **PASS** | [`E2E_03_Admin_Approved_Application.png`](img/robot/E2E_03_Admin_Approved_Application.png) |
| **Phase 4** | Approved Seller Publishes A New Product (ผู้ขายลงขายสินค้าใหม่) | **PASS** | [`E2E_04_Product_Published.png`](img/robot/E2E_04_Product_Published.png) |
| **Phase 5** | Customer Buys Product And Places Order (ลูกค้าสั่งซื้อสินค้าลงตะกร้า) | **PASS** | [`E2E_05_Order_Placed.png`](img/robot/E2E_05_Order_Placed.png) |
| **Phase 6** | Customer Confirms PromptPay Payment (ชำระเงินผ่าน PromptPay QR) | **PASS** | [`E2E_06_Payment_Confirmed.png`](img/robot/E2E_06_Payment_Confirmed.png) |
| **Phase 7** | Seller Fulfills And Ships The Order (ผู้ขายกดยืนยันออเดอร์และใส่เลขพัสดุ) | **PASS** | [`E2E_07_Seller_Shipped_Order.png`](img/robot/E2E_07_Seller_Shipped_Order.png) |
| **Phase 8** | Customer Verifies Shipped Order In Order History (ลูกค้าตรวจสอบสถานะพัสดุ) | **PASS** | [`E2E_08_Order_History_Complete.png`](img/robot/E2E_08_Order_History_Complete.png) |

### เอกสารและไฟล์สรุปผลการทดสอบ (Test Reports)
* **Report File:** [`test/Robot/report.html`](test/Robot/report.html) / [`results/report.html`](results/report.html)
* **Log File:** [`test/Robot/log.html`](test/Robot/log.html) / [`results/log.html`](results/log.html)
* **Output XML:** [`test/Robot/output.xml`](test/Robot/output.xml) / [`results/output.xml`](results/output.xml)
* **ภาพบันทึกหน้าจอขณะทดสอบ:** โฟลเดอร์ [`img/robot/`](img/robot/)

