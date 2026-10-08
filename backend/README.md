## Swagger UI

Run the backend, then open `http://localhost:8080/swagger-ui.html` to browse and try its API endpoints.
The OpenAPI JSON is at `http://localhost:8080/v3/api-docs` (YAML: `/v3/api-docs.yaml`).
For protected endpoints, sign in through `/api/auth/login`, copy the returned JWT,
and use Swagger UI's **Authorize** button. Requests from the UI go to the same backend origin.
When deployed, replace `localhost:8080` with the backend's public base URL.

## เชื่อม Backend กับ Supabase ก่อน deploy

Backend ใช้ PostgreSQL JDBC และ Spring Data JPA ที่มีอยู่แล้ว โดยใช้ระบบ login/JWT เดิมของ PandaStore

1. เปิด Supabase Dashboard ของโปรเจกต์ แล้วเลือก **Connect** เพื่อคัดลอก host และ username จริง
   - **Direct connection**: ใช้พอร์ต `5432` และ username `postgres` เมื่อเครื่องรองรับ IPv6 หรือโปรเจกต์มี IPv4 add-on
   - **Session pooler**: ใช้พอร์ต `5432` และ username `postgres.PROJECT_REF` สำหรับ backend ที่ deploy บนเครือข่าย IPv4
   - ใช้ host จาก Dashboard เท่านั้น เพราะไม่สามารถระบุ pooler host จาก region ได้
2. ตั้งค่าใน `backend/.env` สำหรับเครื่อง local หรือใน environment variables ของบริการที่ deploy:

   ```env
   DB_URL=jdbc:postgresql://YOUR_HOST:5432/postgres?sslmode=require
   DB_USERNAME=YOUR_USERNAME_FROM_CONNECT
   DB_PASSWORD=YOUR_DATABASE_PASSWORD
   DB_POOL_MAX_SIZE=5
   DB_POOL_MIN_IDLE=1
   DB_SHOW_SQL=false
   DB_DDL_AUTO=validate
   SEED_DATA=false
   ALLOWED_REQUEST_ORIGINS=https://YOUR_FRONTEND_HOST
   JWT_SECRET=YOUR_64_HEX_CHARACTER_SECRET
   ```

   ใช้ database password ของโปรเจกต์ ไม่ใช่ Supabase API key และแยกรหัสผ่านออกจาก JDBC URL
   เก็บ `.env` เฉพาะเครื่อง local; ตั้ง secrets ที่บริการ deploy โดยไม่ commit หรือส่งให้ frontend
   หากบริการกำหนดตัวแปร `PORT` ให้ Backend ใช้ค่านั้นได้อัตโนมัติ ค่าเริ่มต้นคือ `8080`

3. `DB_DDL_AUTO=validate` ตรวจ schema ที่มีอยู่โดยไม่สร้างหรือแก้ตาราง หากฐานข้อมูลใหม่ยังไม่มีตาราง ต้องเตรียม schema ก่อน deploy
   ค่าเริ่มต้น `update` ยังคงพฤติกรรมเดิมสำหรับการเตรียมฐานข้อมูล local ห้ามใช้ `create` หรือ `create-drop` กับฐานข้อมูล Supabase ที่มีข้อมูล
   `SEED_DATA=false` ปิดการสร้างบัญชีและข้อมูล demo
4. กำหนดจำนวน connection รวมจากทุก backend instance ให้ไม่เกินขีดจำกัดของโปรเจกต์ เริ่มต้น instance ละไม่เกิน 5 connections
   หากใช้ **Transaction pooler** พอร์ต `6543` ให้เพิ่ม `&prepareThreshold=0` ใน URL เพราะไม่รองรับ server-side prepared statements; JDBC ยังส่งค่าด้วย parameter binding ตามเดิม
5. `sslmode=require` บังคับเข้ารหัส แต่ยังไม่ยืนยันตัวตนเซิร์ฟเวอร์ สำหรับ production ให้ดาวน์โหลด CA certificate จาก Database settings แล้วตั้ง `sslmode=verify-full&sslrootcert=/ABSOLUTE/PATH/TO/certificate.crt`
   ตรวจสิทธิ์ของตารางใน `public` และเปิด RLS ก่อนเปิด Data API; backend ใช้ Spring Security ควบคุมสิทธิ์ผู้ใช้ หลีกเลี่ยงการเปิดตารางบัญชี/session ให้ `anon` หรือ `authenticated`

อ้างอิง: [การเชื่อม PostgreSQL ของ Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres) และ [SSL ของ PostgreSQL JDBC](https://jdbc.postgresql.org/documentation/ssl/)

### สร้าง schema เริ่มต้นบน Supabase

เตรียมไฟล์ `db/supabase-schema.sql` สำหรับฐานข้อมูลใหม่แล้ว รวม 21 ตารางจาก JPA entities ปัจจุบัน พร้อม foreign keys, unique constraints, enum checks และ indexes ของ foreign keys

1. เปิด **Supabase SQL Editor** โดยใช้ role `postgres` แล้วรันไฟล์ `db/supabase-schema.sql` **ทั้งไฟล์** ครั้งเดียว
   ตารางและสิทธิ์จะ commit พร้อมกัน หากพบตารางหรือ role ชื่อเดียวกัน สคริปต์จะล้มเหลวและ rollback โดยไม่เขียนทับของเดิม
   ไฟล์นี้เป็น initial schema สำหรับฐานข้อมูลใหม่ หากเคยสร้างตารางแล้ว ให้ใช้ migration ที่ตรวจ schema เดิมแทน
2. รัน `db/supabase-schema-check.sql` ทั้งไฟล์เพื่อตรวจสิทธิ์และ constraints สคริปต์ตรวจใช้ transaction และ rollback ข้อมูล probe/สิทธิ์ที่เปลี่ยนไว้
3. สคริปต์สร้าง role `pandastore_backend` แบบ **NOLOGIN** เพื่อไม่ฝังรหัสผ่านใน repository
   ตั้งรหัสผ่านใหม่ด้วยเครื่องมือที่มี password prompt เช่นคำสั่ง `\password pandastore_backend` ภายใน `psql` ที่เชื่อมด้วยบัญชีผู้ดูแล แล้วเปิด login:

   ```sql
   ALTER ROLE pandastore_backend LOGIN;
   ```

4. ใช้ credential ของ role นี้ใน environment ของ backend ที่ deploy:

   ```env
   # Direct connection:
   DB_USERNAME=pandastore_backend
   # Session pooler ใช้ DB_USERNAME=pandastore_backend.YOUR_PROJECT_REF
   DB_PASSWORD=YOUR_BACKEND_ROLE_PASSWORD
   DB_DDL_AUTO=validate
   SEED_DATA=false
   ```

Role ของ backend มีสิทธิ์ SELECT/INSERT/UPDATE/DELETE และใช้ identity sequences เท่านั้น ไม่ใช่เจ้าของตาราง ไม่มี BYPASSRLS และไม่มีสิทธิ์สร้าง/แก้/ลบตาราง
ทุกตารางเปิด RLS พร้อม policy ที่อนุญาตเฉพาะ `pandastore_backend` โดย Spring Security ของแอปยังตรวจสิทธิ์ของ CUSTOMER/SELLER/ADMIN ตามเดิม
`anon`, `authenticated` และ `service_role` ไม่มีสิทธิ์เข้าตารางหรือ sequences ของแอปโดยตรง หน้าเว็บต้องเรียกผ่าน backend
ID ของผู้ใช้ยังเป็น `bigint` ตาม JPA เดิม จึงไม่ได้ใช้ UUID ของ Supabase Auth หรือ policy แบบ `auth.uid()`

การเตรียมไฟล์นี้ตรวจแล้วด้วย PostgreSQL บน Supabase ภายใน transaction ที่ rollback: Hibernate schema validation, backend CRUD, enum/FK constraints, indexes, client CRUD denials 252 กรณี และ RLS เมื่อมี table grants โดยผิดพลาด
ยังไม่ได้ apply schema ถาวร และไม่ได้รัน security gate ตามคำขอในครั้งนี้

### ตั้งค่าและรัน Backend บนเครื่อง local

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
   - ระบบจะ seed ข้อมูล demo เฉพาะเมื่อเปิด `SEED_DATA=true` บนฐานข้อมูล local ที่แยกไว้เท่านั้น

---
