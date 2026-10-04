# 🐼 PandaStore - Installation & Setup Guide

คู่มือการติดตั้งและเริ่มต้นใช้งานระบบ **PandaStore** (ระบบร้านค้าออนไลน์ E-Commerce แบบ Multi-vendor ครบวงจร) ทั้งในส่วนของ **Backend (Spring Boot + PostgreSQL)** และ **Frontend (Next.js + React 19 + Tailwind CSS)**

---

## 📑 สารบัญ (Table of Contents)
1. [ภาพรวมของระบบและ Tech Stack](#-ภาพรวมของระบบและ-tech-stack)
2. [สิ่งที่ต้องเตรียมล่วงหน้า (Prerequisites)](#-สิ่งที่ต้องเตรียมล่วงหน้า-prerequisites)
3. [โครงสร้างโปรเจกต์ (Project Structure)](#-โครงสร้างโปรเจกต์-project-structure)
4. [ขั้นตอนการติดตั้ง (Step-by-Step Installation)](#-ขั้นตอนการติดตั้ง-step-by-step-installation)
   - [ขั้นตอนที่ 1: Clone Repository](#ขั้นตอนที่-1-clone-repository)
   - [ขั้นตอนที่ 2: ตั้งค่าฐานข้อมูล PostgreSQL](#ขั้นตอนที่-2-ตั้งค่าฐานข้อมูล-postgresql)
   - [ขั้นตอนที่ 3: ตั้งค่าและรัน Backend](#ขั้นตอนที่-3-ตั้งค่าและรัน-backend)
   - [ขั้นตอนที่ 4: ตั้งค่าและรัน Frontend](#ขั้นตอนที่-4-ตั้งค่าและรัน-frontend)
5. [บัญชีผู้ใช้ทดสอบเริ่มต้น (Default Demo Accounts)](#-บัญชีผู้ใช้ทดสอบเริ่มต้น-default-demo-accounts)
6. [สคริปต์เสริมและการทดสอบ (Scripts & Testing)](#-สคริปต์เสริมและการทดสอบ-scripts--testing)
7. [การแก้ไขปัญหาที่พบบ่อย (Troubleshooting)](#-การแก้ไขปัญหาที่พบบ่อย-troubleshooting)

---

## 🚀 ภาพรวมของระบบและ Tech Stack

* **Backend:**
  - Java 21
  - Spring Boot 4.x / Spring Data JPA / Spring Security
  - PostgreSQL Driver & Hibernate
  - JWT Authentication (jjwt 0.13.0)
  - Maven Wrapper (`mvnw` / `mvnw.cmd`)
* **Frontend:**
  - Next.js 16 (App Router)
  - React 19 & TypeScript
  - Tailwind CSS & Radix UI / Shadcn UI
  - Redux Toolkit & React Query (TanStack Query)
  - UploadThing (ระบบอัปโหลดไฟล์รูปภาพสินค้าและเอกสารร้านค้า)
* **Default Ports:**
  - Backend: `http://localhost:8080`
  - Frontend: `http://localhost:3000`

---

## 🛠 สิ่งที่ต้องเตรียมล่วงหน้า (Prerequisites)

ก่อนเริ่มการติดตั้ง กรุณาตรวจสอบว่าเครื่องของคุณได้ติดตั้งโปรแกรมเหล่านี้เรียบร้อยแล้ว:

1. **Java Development Kit (JDK):** เวอร์ชัน **21** ขึ้นไป
   ```bash
   java -version
   ```
2. **Node.js & npm:** Node.js เวอร์ชัน **18.x** หรือ **20.x+** ขึ้นไป
   ```bash
   node -v
   npm -v
   ```
3. **PostgreSQL Database:** เวอร์ชัน **14** ขึ้นไป (กำลังเปิด Service ทำงานอยู่)
   ```bash
   psql --version
   ```
4. **Git:** สำหรับดึง source code

---

## 📁 โครงสร้างโปรเจกต์ (Project Structure)

```text
PandaStore/
├── backend/                # Spring Boot REST API
│   ├── src/                # ซอร์สโค้ด Java (Controller, Service, Repository, Entity)
│   ├── db/                 # สคริปต์ SQL เสริม (auth_sessions.sql, create_demo_admin.sql)
│   ├── scripts/            # สคริปต์ PowerShell สำหรับ Seed สินค้า และ Demo Checkout
│   ├── pom.xml             # การจัดการ Dependency ของ Maven
│   ├── .env.example        # ไฟล์ตัวอย่าง Environment Variables ของ Backend
│   └── mvnw / mvnw.cmd     # Maven Wrapper
├── frontend/               # Next.js Web Application
│   ├── src/                # ซอร์สโค้ด Next.js (App Router, Components, Redux)
│   ├── public/             # ไฟล์ Static Assets และรูปภาพ
│   ├── tests/              # เทสต์สคริปต์สำหรับฟังก์ชัน Checkout, Payment, Middleware
│   ├── package.json        # การจัดการ Dependency ของ Node.js
│   └── .env.example        # ไฟล์ตัวอย่าง Environment Variables ของ Frontend
└── README.md               # คู่มือการติดตั้งและใช้งานโปรเจกต์
```

---

## ⚙️ ขั้นตอนการติดตั้ง (Step-by-Step Installation)

### ขั้นตอนที่ 1: Clone Repository

```bash
git clone <repository-url>
cd PandaStore
```

---

### ขั้นตอนที่ 2: ตั้งค่าฐานข้อมูล PostgreSQL

1. เปิด PostgreSQL Shell (`psql`) หรือเครื่องมือจัดการฐานข้อมูล เช่น **pgAdmin** / **DBeaver**
2. สร้าง Database สำหรับโปรเจกต์:
   ```sql
   CREATE DATABASE pandastore;
   ```
> [!NOTE]
> ระบบเปิดใช้งาน `spring.jpa.hibernate.ddl-auto=update` ไว้ ตารางทั้งหมดจะถูกสร้างขึ้นมาใน Database ให้อัตโนมัติเมื่อ Backend สตาร์ทขึ้นมาเป็นครั้งแรก และ `TestDataInitializer` จะทำการ Seed ข้อมูลเริ่มต้นให้อัตโนมัติ

---

### ขั้นตอนที่ 3: ตั้งค่าและรัน Backend

1. เข้าไปยังโฟลเดอร์ `backend`:
   ```bash
   cd backend
   ```
2. คัดลอกไฟล์ `.env.example` เป็น `.env`:
   - บน Windows (PowerShell):
     ```powershell
     Copy-Item .env.example .env
     ```
   - บน Linux / macOS:
     ```bash
     cp .env.example .env
     ```
3. เปิดไฟล์ `backend/.env` แล้วแก้ไขค่าให้ตรงกับการตั้งค่าเครื่องของคุณ:
   ```env
   # ตัวอย่าง: ปรับ URL, Port, User และ Password ให้ตรงกับ PostgreSQL ของคุณ
   DB_URL=jdbc:postgresql://localhost:5432/pandastore
   DB_USERNAME=postgres
   DB_PASSWORD=your_postgres_password

   # Key สำหรับเข้ารหัส JWT (ต้องมีความยาวอย่างน้อย 64 ตัวอักษรฐาน 16 / Hex String)
   JWT_SECRET=your_64_character_hex_secret_key
   ```

   > [!TIP]
   > สามารถสร้าง `JWT_SECRET` สุ่มความยาว 64 hex characters ได้ทันทีผ่านคำสั่ง:
   > ```bash
   > node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
   > ```

4. **เริ่มรัน Backend:**
   - **บน Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **บน macOS / Linux:**
     ```bash
     ./mvnw spring-boot:run
     ```
   - *หรือเปิดโฟลเดอร์ `backend` ใน IDE เช่น IntelliJ IDEA / Eclipse แล้วรันคลาส `project.project.ProjectApplication`*

5. Backend จะเปิดทำงานที่: **`http://localhost:8080`**
   - เมื่อรันครั้งแรก ระบบจะแสดง log การ seed ข้อมูลเบื้องต้น: `[TestDataInitializer] Complete ordered test data seeded successfully!`

---

### ขั้นตอนที่ 4: ตั้งค่าและรัน Frontend

1. เปิด Terminal ใหม่แล้วเข้าไปยังโฟลเดอร์ `frontend`:
   ```bash
   cd frontend
   ```
2. คัดลอกไฟล์ `.env.example` เป็น `.env.local` หรือ `.env`:
   - บน Windows (PowerShell):
     ```powershell
     Copy-Item .env.example .env.local
     ```
   - บน Linux / macOS:
     ```bash
     cp .env.example .env.local
     ```
3. เปิดไฟล์ `frontend/.env.local` แล้วตั้งค่าตัวแปร:
   ```env
   # ชี้ไปยัง URL ของ Backend API
   BACKEND_API_URL=http://localhost:8080

   # Origin ที่อนุญาตให้ส่งคำขอ (ไม่มี slash ปิดท้าย)
   ALLOWED_REQUEST_ORIGINS=http://localhost:3000

   # UploadThing Token สำหรับอัปโหลดรูปภาพสินค้าและเอกสารร้านค้า
   UPLOADTHING_TOKEN=your_uploadthing_token_here
   ```

   > [!IMPORTANT]
   > **การขอรับ `UPLOADTHING_TOKEN`:**
   > 1. สมัครและเข้าสู่ระบบที่ [https://uploadthing.com/](https://uploadthing.com/)
   > 2. สร้าง App ใหม่
   > 3. ไปที่เมนู **API Keys** แล้วคัดลอก **V7 Token** มาวางในช่อง `UPLOADTHING_TOKEN`
   > 4. แนะนำให้ตั้งค่า Default ACL เป็น **public-read** เพื่อให้เข้าถึงไฟล์รูปภาพได้

4. **ติดตั้ง Dependencies:**
   ```bash
   npm install
   ```
5. **เริ่มรัน Frontend ในโหมด Development:**
   ```bash
   npm run dev
   ```
6. Frontend จะเปิดทำงานที่: **`http://localhost:3000`**

---

## 👥 บัญชีผู้ใช้ทดสอบเริ่มต้น (Default Demo Accounts)

เมื่อระบบสตาร์ทขึ้นมาเป็นครั้งแรก `TestDataInitializer` จะสร้างบัญชีผู้ใช้เริ่มต้นให้ทันที:

| บทบาท (Role) | Username | Password | รายละเอียด |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin1234` | แอดมินหลักของระบบ จัดการร้านค้าและอนุมัติสินค้า |
| **Admin (Checkout Demo)** | `checkout_admin` | `AdminDemo123!` | แอดมินสำหรับทดสอบ Flow การสั่งซื้อและระบบชำระเงิน |
| **Seller** | `seller1` | `password123` | ร้านค้า "Panda Official Shop" (มีสินค้าตัวอย่างในร้าน) |
| **Customer** | `customer1` | `password123` | ลูกค้าทั่วไป (มีที่อยู่จัดส่งและสินค้าตัวอย่างในตะกร้า) |

---

## 📜 สคริปต์เสริมและการทดสอบ (Scripts & Testing)

### 1. สคริปต์เพิ่มสินค้าตัวอย่าง (Seed Demo Products)
หากต้องการเพิ่มสินค้าชุดทดสอบเข้าไปในร้านค้า สามารถรันสคริปต์ PowerShell ได้ดังนี้ (ขณะที่ Backend กำลังทำงาน):
```powershell
cd backend/scripts
.\seed-demo-products.ps1 -Username "seller1" -Password "password123"
```

### 2. สคริปต์ทดสอบ Checkout จำลอง
จำลองการสั่งซื้อสินค้าและสร้าง Order ที่รอชำระเงิน:
```powershell
cd backend/scripts
.\checkout-demo.ps1 -PlaceOrder
```

### 3. การรันตรวจสอบโค้ดและทดสอบฝั่ง Frontend
```bash
cd frontend

# ตรวจสอบ TypeScript Type
node node_modules/typescript/bin/tsc --noEmit --incremental false

# ตรวจสอบ Linting
npm run lint

# รัน Integration / E2E Tests
node --test --test-isolation=none tests/payment.test.cjs tests/middleware-role.test.cjs
```

### 4. การ Build สำหรับ Production
- **Backend:**
  ```bash
  cd backend
  ./mvnw clean package -DskipTests
  # ไฟล์ JAR จะอยู่ที่ target/project-0.0.1-SNAPSHOT.jar
  java -jar target/project-0.0.1-SNAPSHOT.jar
  ```
- **Frontend:**
  ```bash
  cd frontend
  npm run build
  npm run start
  ```

---

## ❓ การแก้ไขปัญหาที่พบบ่อย (Troubleshooting)

### 1. Backend เชื่อมต่อฐานข้อมูลไม่สำเร็จ (`Connection refused` หรือ `database "pandastore" does not exist`)
- ตรวจสอบว่า PostgreSQL service กำลังทำงานอยู่ (เช่น บน Windows เช็คผ่าน `services.msc`)
- ตรวจสอบว่าได้สร้าง database ชื่อ `pandastore` แล้วหรือยัง
- ตรวจสอบ port, username และ password ใน `backend/.env` ว่าถูกต้องตรงกับเครื่องของคุณหรือไม่

### 2. Backend เกิดข้อผิดพลาดเกี่ยวกับ `JWT_SECRET`
- `JWT_SECRET` ต้องมีความยาวอย่างน้อย 256 bits (64 hex characters) หากสั้นเกินไป JJWT จะโยน `WeakKeyException`
- ให้สร้าง key ใหม่ด้วยคำสั่ง:
  ```bash
  node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
  ```

### 3. Frontend แสดงข้อผิดพลาดเกี่ยวกับ `UploadThing` หรืออัปโหลดรูปไม่ผ่าน
- ตรวจสอบว่าใส่ `UPLOADTHING_TOKEN` ใน `frontend/.env.local` หรือ `frontend/.env` แล้วหรือไม่
- ตรวจสอบว่าได้ Restart Next.js dev server หลังแก้ไขไฟล์ `.env` แล้ว

### 4. ปัญหา CORS หรือ Forbidden ในการเรียก Action
- ตรวจสอบค่า `ALLOWED_REQUEST_ORIGINS` ใน `frontend/.env.local`
- หากมีการใช้ ngrok หรือรันบนพอร์ตอื่น ให้เพิ่ม URL คั่นด้วยจุลภาค เช่น:
  `ALLOWED_REQUEST_ORIGINS=http://localhost:3000,https://your-domain.ngrok-free.dev`
create at https://uploadthing.com/