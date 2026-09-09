# รายงานการวิเคราะห์การปฏิบัติตามหลักการ SOLID (SOLID Principles Analysis)
### วิชา: CP353002 Principles of Software Design and Development
### โปรเจกต์: PandaStore (E-Commerce Multi-Seller Marketplace)
**ผู้พัฒนา:** นายณัฐพงษ์ คนธนกิจ (Nattapong Gontanagit) รหัสนักศึกษา: 673380038-9 Section: 2  
**โมดูลที่รับผิดชอบ:** `ProductService`, `ReviewService`, `SellerApplicationService`

เอกสารนี้ระบุการปฏิบัติตามหลักการ SOLID ทั้ง 5 ข้อใน Service Layer ของระบบส่วน Product, Review และ Seller Application

---

## 1. S — Single Responsibility Principle (SRP)
> **หลักการ:** แต่ละคลาสต้องมีความรับผิดชอบเพียงอย่างเดียว ไม่รวมหน้าที่ด้าน Business Logic, Validation และ Database Persistence ไว้ในคลาสเดียวกัน

| คลาส / ไฟล์ | หน้าที่ความรับผิดชอบเพียงอย่างเดียว | สิ่งที่แยกออกไปให้คลาสอื่น |
|---|---|---|
| [`ProductServiceImp.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/ProductServiceImp.java) | จัดการกฎทางธุรกิจ (Business Rules) เช่น ตรวจสอบราคา > 0, ตัดสต็อก, คืนสต็อก, คำนวณ Rating | Database Access มอบหมายให้ `ProductRepository`, `ProductImageRepository` |
| [`ReviewServiceImp.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/ReviewServiceImp.java) | ตรวจสอบสิทธิ์การรีวิว (Verified Purchase), จัดการคำนวณคะแนนรีวิว, การตอบกลับรีวิว | การส่งแจ้งเตือนมอบหมายให้ `NotificationService`, การบันทึกมอบหมายให้ `ReviewRepository` |
| [`SellerApplicationServiceImp.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/SellerApplicationServiceImp.java) | ควบคุม Workflow การสมัครร้านค้า (Validation ข้อมูล, สถานะ PENDING, การอนุมัติและปฏิเสธ) | การบันทึกร้านค้ามอบหมายให้ `SellerRepository`, ผู้ใช้งานมอบหมายให้ `UserRepository` |

---

## 2. O — Open/Closed Principle (OCP)
> **หลักการ:** ซอฟต์แวร์ควรเปิดให้ต่อขยายได้ (Open for extension) แต่ปิดต่อการแก้ไขโค้ดเดิม (Closed for modification)

| ตัวอย่างในโปรเจกต์ | คำอธิบายการปฏิบัติตาม OCP |
|---|---|
| **Service Interfaces** ([`ProductService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/ProductService.java), [`ReviewService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/ReviewService.java), [`SellerApplicationService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/SellerApplicationService.java)) | ระบบถูกออกแบบผ่าน Interface สัญญาการทำงาน หากในอนาคตต้องการเปลี่ยนกลยุทธ์การตัดสต็อก เช่น ใช้ Redis Lock หรือ Distributed Transaction สามารถสร้าง Implementation ใหม่มาสวมแทนได้ทันทีโดยไม่ต้องแก้ไข Caller |
| **Notification Integration ใน Service** | ใน [`SellerApplicationServiceImp.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/SellerApplicationServiceImp.java) และ [`ReviewServiceImp.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/ReviewServiceImp.java) เรียกใช้ `NotificationService` (Interface) หากต้องการเพิ่มช่องทางแจ้งเตือน เช่น Line Notify, SMS, หรือ WebSocket สามารถต่อขยายที่ Notification Module ได้โดยไม่ต้องแก้โค้ดใน Service เดิม |
| **Spring Data JPA Custom Query** | [`ProductRepository.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Repository/ProductRepository.java) ใช้ JPQL Query ที่รองรับ Dynamic Parameters (Keyword และ Category) โดยไม่ต้องเขียน if-else สร้าง SQL String ซ้ำซ้อน |

---

## 3. L — Liskov Substitution Principle (LSP)
> **หลักการ:** คลาสลูก (Subclass หรือ Implementation) ต้องสามารถถูกนำมาใช้แทนคลาสแม่หรือ Interface ได้อย่างสมบูรณ์ โดยไม่ทำให้พฤติกรรมหรือตรรกะของโปรแกรมเสียหาย และต้องไม่โยน `UnsupportedOperationException`

| คลาส Implementation | Interface ที่สวมแทน | การตรวจสอบความถูกต้องตาม LSP |
|---|---|---|
| [`ProductServiceImp`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/ProductServiceImp.java) | [`ProductService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/ProductService.java) | ทุกเมธอดทั้ง 9 เมธอดใน Interface ถูก Implement ครบถ้วน ไม่มีเมธอดใด throw `UnsupportedOperationException` หรือปฏิเสธการทำงานตามสัญญา |
| [`ReviewServiceImp`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/ReviewServiceImp.java) | [`ReviewService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/ReviewService.java) | เมธอดทั้งหมด 7 เมธอดปฏิบัติตามสัญญาการคืนค่าและประเภทข้อมูลครบถ้วน |
| [`SellerApplicationServiceImp`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/SellerApplicationServiceImp.java) | [`SellerApplicationService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/SellerApplicationService.java) | ปฏิบัติตามสัญญาทั้ง 6 เมธอด โดยสามารถนำไปสวมแทนใน Controller และ Unit Test ได้อย่างไร้รอยต่อ |

---

## 4. I — Interface Segregation Principle (ISP)
> **หลักการ:** ไม่ควรบังคับให้ Client ต้องพึ่งพา Interface ที่ตนเองไม่ได้ใช้งาน (หลีกเลี่ยง Fat / God Interface)

| การออกแบบ | คำอธิบาย |
|---|---|
| **แยกตาม Domain แทนการรวมศูนย์** | ระบบแยก Interface ออกเป็นส่วนย่อยชัดเจน: <br>• [`ProductService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/ProductService.java) ดูแลเฉพาะ Catalog/Stock<br>• [`ReviewService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/ReviewService.java) ดูแลเฉพาะ Feedback/Rating<br>• [`SellerApplicationService`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/api/SellerApplicationService.java) ดูแลเฉพาะ Onboarding |
| **ผลลัพธ์ของ ISP** | Client หรือคลาสภายนอกที่ต้องการจัดการสินค้า เรียกใช้เฉพาะ `ProductService` โดยไม่ต้องเห็นเมธอดของ `Review` หรือ `SellerApplication` แต่อย่างใด |

---

## 5. D — Dependency Inversion Principle (DIP)
> **หลักการ:** คลาสระดับสูง (High-level Modules) ต้องไม่ขึ้นอยู่กับคลาสระดับต่ำ (Low-level Modules) ทั้งคู่ต้องขึ้นอยู่กับนามธรรม (Abstractions/Interfaces) และต้องใช้ **Constructor Injection** เท่านั้น

| ตำแหน่งในโค้ด | คลาสที่เรียกใช้ | Dependency (Interface เท่านั้น) | รูปแบบการ Inject |
|---|---|---|---|
| [`ProductServiceImp.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/ProductServiceImp.java) | `ProductServiceImp` | `ProductRepository`, `ProductImageRepository`, `SellerRepository`, `CategoryRepository` | Constructor Injection (`final`) |
| [`ReviewServiceImp.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/ReviewServiceImp.java) | `ReviewServiceImp` | `ReviewRepository`, `OrderItemRepository`, `CustomerRepository`, `ProductService`, `NotificationService` | Constructor Injection (`final`) |
| [`SellerApplicationServiceImp.java`](file:///C:/Users/Petchy_STB/Desktop/Lab_673380038-9/Project/PandaStore/backend/src/main/java/project/project/Service/implement/SellerApplicationServiceImp.java) | `SellerApplicationServiceImp` | `SellerApplicationRepository`, `UserRepository`, `SellerRepository`, `NotificationService` | Constructor Injection (`final`) |

> **สรุป:** ไม่มีส่วนใดในโค้ดที่ใช้ Field Injection ด้วย `@Autowired` บนตัวแปรโดยตรง ทุกคลาสประกาศเป็น `private final` และฉีดผ่าน Constructor ทั้งหมด 100% ตามข้อกำหนด
