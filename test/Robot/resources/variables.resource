*** Settings ***
Documentation       Global variables, test accounts (Customer, Seller, Admin), and configurations for PandaStore Robot Framework tests.

*** Variables ***
# ==============================================================================
# ENVIRONMENT & BROWSER SETTINGS
# ==============================================================================
${BASE_URL}                 https://panda-store-kappa.vercel.app
${BROWSER}                  chrome
${HEADLESS}                 False
${DELAY}                    0s
${TIMEOUT}                  15s
${IMPLICIT_WAIT}            5s
${SCREENSHOT_DIR}           ${EXECDIR}/img/robot
${TEST_IMAGE_PATH}          ${EXECDIR}/img/products/product-1-sm-1.png

# ==============================================================================
# USER (CUSTOMER) TEST DATA
# ==============================================================================
# Seed Customer
${VALID_USERNAME}           customer1
${VALID_EMAIL}              customer@pandastore.com
${VALID_PASSWORD}           Password123!
${VALID_FULLNAME}           สมชาย รักดี
${VALID_PHONE}              0899998888

# Customer Delivery Address
${CUST_RECEIVER_NAME}       สมชาย รักดี
${CUST_PHONE}               0899998888
${CUST_ADDRESS_LINE}        123 หมู่ 4 ต.ในเมือง
${CUST_DISTRICT}            เมืองขอนแก่น
${CUST_PROVINCE}            ขอนแก่น
${CUST_POSTAL_CODE}         40000

# Invalid Credentials (Negative Tests)
${INVALID_USERNAME}         nonexistent_user_999
${INVALID_PASSWORD}         wrongpassword999
${INVALID_EMAIL}            invalid-format-email

# ==============================================================================
# SELLER TEST DATA
# ==============================================================================
# Seed Seller
${SELLER_USERNAME}          seller1
${SELLER_EMAIL}             seller@pandastore.com
${SELLER_PASSWORD}          Password123!
${SELLER_SHOP_NAME}         Panda Official Shop
${SELLER_SHOP_PHONE}        0812345678
${SELLER_SHOP_EMAIL}        seller@pandastore.com
${SELLER_SHOP_ADDRESS}      99/9 ถ.มิตรภาพ ต.ในเมือง อ.เมือง จ.ขอนแก่น 40000

# Seller Application Data (For User applying to be Seller)
${APPLY_SHOP_NAME}          Panda Gadget & Tech
${APPLY_SHOP_DESC}          ร้านจำหน่ายอุปกรณ์ไอทีและแกดเจ็ตแพนด้าคุณภาพสูง พร้อมส่งทั่วประเทศ
${APPLY_SHOP_PHONE}         0891234567
${APPLY_SHOP_EMAIL}         pandagadget@testmail.com
${APPLY_SHOP_ADDRESS}       888/123 ถนนมิตรภาพ ต.ในเมือง อ.เมือง จ.ขอนแก่น 40000
${APPLY_FIRST_NAME}         สมศักดิ์
${APPLY_LAST_NAME}          ตั้งใจขาย
${APPLY_ID_CARD_NO}         1103701234567
${APPLY_BANK_NAME}          ธนาคารกสิกรไทย (KBANK)
${APPLY_BANK_ACC_NAME}      สมศักดิ์ ตั้งใจขาย
${APPLY_BANK_ACC_NO}        0123456789

# Product To Add For Sale
${NEW_PRODUCT_NAME}         Panda Mechanical Keyboard RGB Pro
${NEW_PRODUCT_DESC}         คีย์บอร์ดแมคคานิคอลคัสตอมลายแพนด้า สวิตช์เงียบ ไฟ RGB สวยงาม รองรับ Bluetooth และ Wireless
${NEW_PRODUCT_PRICE}        1290.00
${NEW_PRODUCT_STOCK}        50
${NEW_PRODUCT_SHIPPING}     จัดส่งผ่านขนส่งเอกชน Standard Delivery ภายใน 1-2 วันทำการ

# Order Fulfillment & Shipping
${COURIER_NAME}             Flash Express
${TRACKING_NUMBER}          TH123456789PANDA

# ==============================================================================
# ADMIN TEST DATA
# ==============================================================================
${ADMIN_USERNAME}           checkout_admin
${ADMIN_EMAIL}              checkout_admin@example.com
${ADMIN_PASSWORD}           AdminDemo123!
${ADMIN_CHECKOUT_USER}      checkout_admin
${ADMIN_CHECKOUT_PASS}      AdminDemo123!

# ==============================================================================
# PAGE URLS
# ==============================================================================
# Customer URLs
${HOME_URL}                 ${BASE_URL}/
${SIGNIN_URL}               ${BASE_URL}/signin
${SIGNUP_URL}               ${BASE_URL}/signup
${ACCOUNT_URL}              ${BASE_URL}/my-account
${SHOP_URL}                 ${BASE_URL}/shop-with-sidebar
${CART_URL}                 ${BASE_URL}/cart
${CHECKOUT_URL}             ${BASE_URL}/checkout
${ORDER_HISTORY_URL}        ${BASE_URL}/order-history

# Seller URLs
${SELLER_APPLY_URL}         ${BASE_URL}/seller-application/apply
${SELLER_STATUS_URL}        ${BASE_URL}/seller-application
${SELLER_DASHBOARD_URL}     ${BASE_URL}/seller-dashboard
${SELLER_ADD_PRODUCT_URL}   ${BASE_URL}/seller/products/add

# Admin URLs
${ADMIN_URL}                ${BASE_URL}/admin
