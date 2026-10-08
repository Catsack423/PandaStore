# 🐼 PandaStore - Robot Framework Test Automation Suite

ชุดทดสอบอัตโนมัติด้วย **Robot Framework** และ **SeleniumLibrary** ครอบคลุมการทำงานทุกมิติของระบบ **PandaStore**:
- 👤 **User (Customer / ลูกค้า):** สมัครสมาชิก, ล็อกอิน, จัดการโปรไฟล์/ที่อยู่, ค้นหาสินค้า, ตะกร้าสินค้า, สั่งซื้อ, ชำระเงิน PromptPay, ประวัติคำสั่งซื้อ
- 🏪 **Seller (ผู้ขาย):** สมัครเปิดร้านค้า (KYC & Bookbank), หน้าแดชบอร์ดสรุปยอด, วางขายสินค้าใหม่พร้อมรูปภาพ, จัดการออเดอร์และการจัดส่งพัสดุ
- 🛡️ **Admin (ผู้ดูแลระบบ):** ควบคุมสิทธิ์เข้าถึง (/admin), ตรวจสอบและอนุมัติร้านค้าผู้ขาย, จัดการหมวดหมู่สินค้า (Categories CRUD)
- 🔄 **Full Lifecycle (End-to-End E2E):** รันวงจรระบบเต็มรูปแบบตั้งแต่ สมัครผู้ใช้ -> สมัครร้านค้า -> แอดมินอนุมัติ -> ผู้ขายลงสินค้า -> ลูกค้าสั่งซื้อ -> จ่ายเงิน -> ผู้ขายจัดส่ง -> ลูกค้าตรวจสอบสินค้าที่จัดส่ง

---

## 📁 โครงสร้างไฟล์ในโฟลเดอร์ `test/Robot`

```text
test/Robot/
├── requirements.txt                   # รายการไลบรารี Python สำหรับทดสอบ
├── README.md                          # เอกสารแนะนำและคู่มือการใช้งานชุดทดสอบ
├── resources/
│   ├── variables.resource             # Test Data กลางของ User, Seller, Admin, URLs และคอนฟิก
│   ├── locators.resource              # UI Locators ครบทุกหน้า (User, Seller, Admin)
│   └── common_keywords.resource       # คีย์เวิร์ดสำเร็จรูป (Login, Apply, Approve, Ship, ฯลฯ)
│
├── 00_e2e_full_lifecycle.robot        # ★ E2E Full Lifecycle ทดสอบทั้งระบบในไฟล์เดียว
│
├── [USER SUITES]
│   ├── 01_user_register.robot         # สมัครสมาชิกผู้ใช้ (Customer Sign Up + Validation)
│   ├── 02_user_login.robot            # เข้าสู่ระบบและออกจากระบบ (Sign In / Sign Out)
│   ├── 03_user_profile.robot          # จัดการข้อมูลส่วนตัวและที่อยู่จัดส่ง (/my-account)
│   ├── 04_browse_and_search.robot     # ค้นหาและดูรายละเอียดสินค้าในร้าน (Shop Catalog)
│   ├── 05_cart_management.robot       # ระบบตะกร้าสินค้า (เพิ่ม/ลด/ลบ/เคลียร์ตะกร้า /cart)
│   ├── 06_checkout_order.robot        # สั่งซื้อสินค้าและการเช็คเอาท์ (/checkout)
│   ├── 07_payment.robot               # สแกนจ่าย QR PromptPay & ยกเลิกออเดอร์ (/payment)
│   └── 08_order_history.robot         # ตรวจสอบประวัติคำสั่งซื้อและสถานะ (/order-history)
│
├── [SELLER SUITES]
│   ├── seller_01_application.robot    # สมัครเป็นผู้ขาย (Step 1 ข้อมูลร้าน + Step 2 KYC & บัญชีธนาคาร)
│   ├── seller_02_dashboard.robot      # แดชบอร์ดร้านค้า ยอดขาย 4 การ์ด และแท็บออเดอร์ (/seller-dashboard)
│   ├── seller_03_manage_products.robot# ลงขายสินค้าใหม่ อัปโหลดรูปภาพ สต็อก หมวดหมู่ (/seller/products/add)
│   └── seller_04_orders_fulfillment.robot # รับออเดอร์ ใส่เลข Tracking ขนส่ง และกดจัดส่งพัสดุ (/seller/{orderId})
│
└── [ADMIN SUITES]
    ├── admin_01_access.robot          # ป้องกันผู้ใช้ทั่วไปเข้าถึง และล็อกอินผู้ดูแลระบบ (/admin)
    ├── admin_02_seller_applications.robot # ตรวจสอบรายชื่อคำขอเปิดร้าน กรองสถานะ และกดอนุมัติ/ปฏิเสธ
    └── admin_03_categories.robot      # จัดการหมวดหมู่สินค้า เพิ่ม/ลบหมวดหมู่พร้อมกล่องยืนยัน (/admin/categories)
```

---

## 🛠️ รายละเอียดชุดการทดสอบ (Test Suites Breakdown)

### 1. 🔄 Full Lifecycle E2E Suite (`00_e2e_full_lifecycle.robot`)
รันระบบต่อเนื่องตั้งแต่ต้นจนจบแบบสมบูรณ์ในไฟล์เดียว:
- **Phase 1:** สมัครบัญชีผู้ใช้ใหม่แบบไดนามิก (Customer)
- **Phase 2:** ยื่นใบสมัครขอเปิดร้านค้า (Seller Application - กรอกข้อมูลร้าน, บัตร ปชช., สมุดบัญชี)
- **Phase 3:** แอดมินเข้าสู่ระบบเพื่อตรวจสอบเอกสารและอนุมัติร้านค้า (Admin Approve)
- **Phase 4:** ผู้ขายล็อกอินเข้าแดชบอร์ดและลงขายสินค้าใหม่ (Seller Add Product พร้อมรูปภาพ)
- **Phase 5:** ลูกค้าเข้าเลือกซื้อสินค้า ใส่ตะกร้า และทำรายการสั่งซื้อ (Checkout Order)
- **Phase 6:** ลูกค้าชำระเงินผ่าน PromptPay QR Code สำเร็จ
- **Phase 7:** ผู้ขายกดยอมรับคำสั่งซื้อ ใส่ชื่อขนส่ง + เลขพัสดุ (Tracking No.) และกดส่งสินค้า (Ship)
- **Phase 8:** ลูกค้าตรวจสอบในประวัติคำสั่งซื้อพบสถานะการจัดส่งสำเร็จ (SHIPPED)

---

### 2. 👤 User / Customer Suites
| ไฟล์ทดสอบ | ฟังก์ชันที่ทดสอบ | จุดสำคัญที่ตรวจสอบ |
| :--- | :--- | :--- |
| **`01_user_register.robot`** | สมัครสมาชิก | Validation ฟิลด์บังคับ, รหัสผ่านสั้น, อีเมลผิดรูปแบบ, สมัครสำเร็จ |
| **`02_user_login.robot`** | ล็อกอิน / ล็อกเอาท์ | ล็อกอินด้วย Username, Email, กรณีรหัสผ่านผิด, ออกจากระบบ |
| **`03_user_profile.robot`** | โปรไฟล์และที่อยู่ | เปลี่ยนชื่อ, เปลี่ยนรหัสผ่าน, เพิ่มที่อยู่จัดส่งใหม่, รายการที่อยู่ |
| **`04_browse_and_search.robot`**| ค้นหาสินค้า | ค้นหาด้วยคีย์เวิร์ด, ดูรายการสินค้าใน Catalog, เข้าหน้าสินค้า |
| **`05_cart_management.robot`** | จัดการตะกร้าสินค้า | เพิ่มสินค้าลงตะกร้า, เพิ่ม/ลดจำนวน, ลบรายการ, เคลียร์ตะกร้า |
| **`06_checkout_order.robot`** | เช็คเอาท์สั่งซื้อ | เลือกที่อยู่, เลือกวิธีจัดส่ง, เลือก PromptPay, วางออเดอร์สำเร็จ |
| **`07_payment.robot`** | ชำระเงิน PromptPay | ตรวจสอบ QR Code, กดชำระเงินสำเร็จ, กรณีกดยกเลิกออเดอร์ |
| **`08_order_history.robot`** | ประวัติคำสั่งซื้อ | รายการออเดอร์ย้อนหลัง, กดดู Details สินค้า, สถานะออเดอร์ |

---

### 3. 🏪 Seller Suites
| ไฟล์ทดสอบ | ฟังก์ชันที่ทดสอบ | จุดสำคัญที่ตรวจสอบ |
| :--- | :--- | :--- |
| **`seller_01_application.robot`** | สมัครเปิดร้านค้า | กรอกรายละเอียดร้าน (Step 1) -> ยืนยันตัวตน KYC & บัญชีธนาคาร (Step 2) -> ตรวจสอบสถานะ "Under review" |
| **`seller_02_dashboard.robot`** | แดชบอร์ดร้านค้า | สรุปยอดขาย (Paid sales, Active orders, Awaiting acceptance, Completed orders) และสลับแท็บ Active/History |
| **`seller_03_manage_products.robot`**| ลงขายสินค้า | กรอกชื่อ, ราคา, จำนวนสต็อก, ข้อมูลจัดส่ง, อัปโหลดภาพสินค้า, เลือกหมวดหมู่, บันทึกสำเร็จ |
| **`seller_04_orders_fulfillment.robot`** | จัดการและส่งออเดอร์ | ดูรายการคำสั่งซื้อของร้าน, กดรับออเดอร์ (Accept), ใส่ชื่อขนส่งและเลข Tracking, กดบันทึกส่งพัสดุ (Shipped) |

---

### 4. 🛡️ Admin Suites
| ไฟล์ทดสอบ | ฟังก์ชันที่ทดสอบ | จุดสำคัญที่ตรวจสอบ |
| :--- | :--- | :--- |
| **`admin_01_access.robot`** | สิทธิ์เข้าถึงระบบ Admin | ป้องกันผู้ใช้ทั่วไปหรือ Guest ไม่ให้เข้า `/admin`, ตรวจสอบเมนูนำทางของผู้ดูแลระบบ |
| **`admin_02_seller_applications.robot`** | ตรวจสอบคำขอร้านค้า | กรองสถานะคำขอ (All, Under review, Approved, Rejected), เปิดดูเอกสาร KYC และกดอนุมัติ (Approve) |
| **`admin_03_categories.robot`** | จัดการหมวดหมู่สินค้า | ดูรายการหมวดหมู่, ตรวจสอบเงื่อนไขชื่อหมวดหมู่ (ไม่เกิน 20 ตัวอักษร), เพิ่มหมวดหมู่ใหม่, ลบหมวดหมู่พร้อมยืนยัน |

---

## 📷 การบันทึกรูปภาพหน้าจอ (Screenshots)

รูปภาพการทดสอบทุกรูปจะถูกบันทึกไว้ในโฟลเดอร์:
```text
img/robot/
```
- ทุก Test Case มี Teardown จับภาพหน้าจออัตโนมัติ
- ชื่อไฟล์ถูกจัดรูปแบบอย่างปลอดภัย (Sanitized safe filename) ป้องกันอักขระพิเศษบนระบบปฏิบัติการ Windows

---

## 🚀 คำสั่งในการรันเทส (Execution)

> 💡 **ข้อแนะนำ:** หากใช้งานบน Windows และคำสั่ง `robot` หรือ `pip` ยังไม่ได้ถูกเซ็ตไว้ใน PATH สามารถรันผ่าน `python -m robot` ได้ทันที

### 1. ติดตั้งไลบรารี
```powershell
python -m pip install -r test/Robot/requirements.txt
```

### 2. รัน Full Lifecycle (E2E)
```powershell
python -m robot test/Robot/00_e2e_full_lifecycle.robot
```

### 3. รันกลุ่ม Seller ทั้งหมด
```powershell
python -m robot test/Robot/seller_*.robot
```

### 4. รันกลุ่ม Admin ทั้งหมด
```powershell
python -m robot test/Robot/admin_*.robot
```

### 5. รันกลุ่ม User ทั้งหมด
```powershell
python -m robot test/Robot/0*.robot
```

### 6. รันแบบ Headless (ไม่เปิดหน้าต่างเบราว์เซอร์)
```powershell
python -m robot -v HEADLESS:True test/Robot/
```

### 7. ระบุปลายทางระบบ (Vercel Production หรือ Localhost)
```powershell
python -m robot -v BASE_URL:https://panda-store-kappa.vercel.app test/Robot/
```

---

## 📊 รายงานผลการทดสอบ (Reports)
หลังจากการรันเสร็จสิ้น สามารถเปิดดูไฟล์รายงานได้ด้วยเบราว์เซอร์:
- **`report.html`**: รายงานสรุปผลภาพรวม (กราฟวงกลม, สถิติ Pass/Fail และเวลาที่ใช้)
- **`log.html`**: รายละเอียดเชิงลึก บันทึกทุกคำสั่งที่ทำงานและลิงก์รูปภาพบันทึกหน้าจอ
- **`output.xml`**: ข้อมูลผลลัพธ์ในรูปแบบ XML สำหรับระบบ CI/CD
