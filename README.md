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

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Git Branch
| :---: | :--- | :---: | :---: | :--- | :--- |
| 1 | นายธนันชัย (Thananchai) | 673380042-8 | 2 | `Thananchai_673380042-8_sec2` |
| 2 | นายภวัต (Pawat) | 673380048-6 | 1 | `Pawat_673380048-6_SEC01` |
| 3 | นายณัฐพงษ์ (Nattapong) | 673380038-9 | 2 | `Nattapong_673380038-9_sec2` 
| 4 | นายธนากร (Thanakon) | 673380040-2 | 2 | `Thanakon_673380040-2_sec2` | 
| 5 | นายปิยพล (Piyapon) | 673380280-2 | 1 | `Piyapon_673380280-2sec1` | 

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
