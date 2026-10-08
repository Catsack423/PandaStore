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





## 🌐 ลิงก์ Deploy

- Frontend (Vercel): https://panda-store-kappa.vercel.app/
- Backend API (Railway): https://forty-nine-shop-production.up.railway.app/

## 📁 โครงสร้างโปรเจกต์ (Project Structure)

```text
PandaStore/
├── code/
│   ├── backend/            # Spring Boot API, Maven, SQL, backend demo scripts
│   └── frontend/           # Next.js app, public assets, tests
├── config/pmd/           # PMD Java ruleset
├── scripts/               # Repository-wide quality and security checks
├── .github/workflows/     # CI checks
└── README.md
```


---

