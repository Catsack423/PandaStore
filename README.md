# 🐼 PandaStore - Installation & Setup Guide



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

