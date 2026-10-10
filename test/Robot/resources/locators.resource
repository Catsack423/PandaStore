*** Settings ***
Documentation       UI element locators for PandaStore Robot Framework test automation.

*** Variables ***
# ==============================================================================
# HEADER LOCATORS
# ==============================================================================
${HEADER_LOGO}                  css:header a[href="/"]
${HEADER_SEARCH_INPUT}          id:search
${HEADER_SEARCH_BUTTON}         id:search-btn
${HEADER_ACCOUNT_LINK}          css:header a[aria-label="Sign in"], header a[aria-label="Open my account"]
${HEADER_CART_BUTTON}           css:header button:has(svg)
${HEADER_CART_COUNT}            css:header span.rounded-full

# ==============================================================================
# AUTH LOCATORS (SIGN IN & SIGN UP)
# ==============================================================================
# Sign In
${SIGNIN_INPUT_USERNAME}        id:usernameOrEmail
${SIGNIN_INPUT_PASSWORD}        id:signin-password
${SIGNIN_BUTTON_SUBMIT}         xpath://button[@type='submit' and (contains(., 'Sign in') or contains(., 'Signing in'))]
${SIGNIN_ALERT_ERROR}           xpath://*[@role='alert']
${SIGNIN_LINK_CREATE_ACCOUNT}   xpath://a[@href='/signup']

# Sign Up (Customer)
${SIGNUP_INPUT_USERNAME}        id:username
${SIGNUP_INPUT_FULLNAME}        id:fullName
${SIGNUP_INPUT_EMAIL}           id:email
${SIGNUP_INPUT_PHONE}           id:phoneNumber
${SIGNUP_INPUT_PASSWORD}        id:password
${SIGNUP_INPUT_CONFIRM_PW}      id:confirmPassword
${SIGNUP_BUTTON_SUBMIT}         xpath://button[@type='submit' and (contains(., 'Sign up') or contains(., 'Creating'))]
${SIGNUP_ALERT_ERROR}           xpath://div[contains(@class, 'bg-red-light-6')]
${SIGNUP_TEXT_SUCCESS}          xpath://*[contains(text(), 'Customer account created successfully!')]
${SIGNUP_LINK_SIGNIN}           xpath://a[@href='/signin']

# ==============================================================================
# MY ACCOUNT LOCATORS (/my-account)
# ==============================================================================
# Navigation Tabs
${ACCOUNT_TAB_PROFILE}          xpath://button[contains(., 'Profile')]
${ACCOUNT_TAB_ADDRESSES}        xpath://button[contains(., 'Addresses')]
${ACCOUNT_TAB_PASSWORD}         xpath://button[contains(., 'Change password')]
${ACCOUNT_TAB_ORDERS}           xpath://a[@href='/order-history?from=my-account' or contains(., 'Order history')]
${ACCOUNT_BUTTON_SIGNOUT}       xpath://button[contains(., 'Sign out')]

# Profile Details Form
${PROFILE_INPUT_NAME}           id:profile-name
${PROFILE_INPUT_EMAIL}          id:profile-email
${PROFILE_INPUT_PHONE}          id:profile-phone
${PROFILE_BUTTON_SAVE}          xpath://button[@type='submit' and contains(., 'Save profile')]

# Change Password Form
${PW_INPUT_CURRENT}             id:current-password
${PW_INPUT_NEW}                 id:new-password
${PW_INPUT_CONFIRM}             id:confirm-password
${PW_BUTTON_UPDATE}             xpath://button[@type='submit' and contains(., 'Update password')]

# Address Management in My Account & Checkout
${ADDR_BTN_ADD_FORM}            xpath://button[contains(., 'Add delivery address')]
${ADDR_BTN_CLOSE_FORM}          xpath://button[contains(., 'Close address form') or contains(., 'Cancel')]
${ADDR_INPUT_RECEIVER}          name:receiverName
${ADDR_INPUT_PHONE}             name:phoneNumber
${ADDR_INPUT_LINE}              name:addressLine
${ADDR_INPUT_DISTRICT}          name:district
${ADDR_INPUT_PROVINCE}          name:province
${ADDR_INPUT_POSTAL}            name:postalCode
${ADDR_BUTTON_SAVE}             xpath://button[@type='submit' and (contains(., 'Save address') or contains(., 'Update address'))]
${ADDR_BUTTON_DELETE}           xpath://button[contains(., 'Delete')]
${ADDR_BUTTON_SET_DEFAULT}      xpath://button[contains(., 'Set Default')]
${ADDR_DEFAULT_BADGE}           xpath://span[contains(text(), 'Default')]

# ==============================================================================
# SHOP & PRODUCT DETAILS LOCATORS
# ==============================================================================
${SHOP_PRODUCT_CARD}            xpath://div[contains(@class, 'product-item') or contains(@class, 'group')]
${SHOP_FIRST_PRODUCT_LINK}      xpath:(//a[contains(@href, '/prouduct') or contains(@href, '/shop-details') or contains(@aria-label, 'View ')])[1]
${PRODUCT_DETAIL_TITLE}         id:product-title
${PRODUCT_DETAIL_PRICE}         xpath://span[contains(@class, 'text-blue') and contains(text(), '฿')]
${PRODUCT_BTN_INCREASE_QTY}     xpath://button[@aria-label='Increase quantity']
${PRODUCT_BTN_DECREASE_QTY}     xpath://button[@aria-label='Decrease quantity']
${PRODUCT_BTN_ADD_TO_CART}      xpath://button[contains(., 'Add to cart')]

# ==============================================================================
# CART LOCATORS (/cart)
# ==============================================================================
${CART_HEADING}                 xpath://h2[contains(., 'Your cart')]
${CART_EMPTY_TITLE}             xpath://h3[contains(., 'Your cart is empty')]
${CART_BTN_CLEAR}               xpath://button[contains(., 'Clear cart')]
${CART_BTN_INCREASE}            xpath:(//button[contains(@aria-label, 'Increase')])[1]
${CART_BTN_DECREASE}            xpath:(//button[contains(@aria-label, 'Decrease')])[1]
${CART_BTN_REMOVE_ITEM}         xpath:(//button[contains(@aria-label, 'Remove')])[1]
${CART_LINK_CHECKOUT}           xpath://a[@href='/checkout' and contains(., 'Checkout all')]
${CART_LINK_CONTINUE_SHOP}      xpath://a[contains(@href, '/shop') and contains(., 'Continue shopping')]

# ==============================================================================
# CHECKOUT LOCATORS (/checkout)
# ==============================================================================
${CHECKOUT_RADIO_FIRST_ADDR}    xpath:(//input[@name='delivery-address'])[1]
${CHECKOUT_RADIO_SHIPPING_STD}  xpath:(//input[contains(@name, 'shipping-')])[1]
${CHECKOUT_RADIO_PROMPTPAY}     xpath://input[@name='payment' and @type='radio']
${CHECKOUT_BTN_PLACE_ORDER}     xpath://button[contains(., 'Place order')]
${CHECKOUT_CONFIRMATION_TITLE}  xpath://h2[contains(., 'Your order has been placed')]
${CHECKOUT_LINK_PAY_NOW}        xpath://a[contains(@href, '/payment/') and contains(., 'Pay now')]
${CHECKOUT_LINK_VIEW_ORDER}     xpath://a[@href='/order-history' and contains(., 'View order')]

# ==============================================================================
# PAYMENT LOCATORS (/payment/{id})
# ==============================================================================
${PAYMENT_PAGE_TITLE}           xpath://h2[contains(., 'Scan to pay') or contains(., 'Payment successful')]
${PAYMENT_QR_IMAGE}             xpath://img[@alt='Payment QR code']
${PAYMENT_BTN_CONFIRM}          xpath://button[contains(@aria-label, 'Confirm payment') or contains(., 'Confirm payment')]
${PAYMENT_BTN_CANCEL_ORDER}     xpath://button[contains(@aria-label, 'Cancel order') or contains(., 'Cancel order')]
${PAYMENT_SUCCESS_MESSAGE}      xpath://*[contains(text(), 'Payment successful') or contains(text(), 'Your payment is recorded')]
${PAYMENT_LINK_ORDER_HISTORY}   xpath://a[@href='/order-history' and contains(., 'View Order History')]

# ==============================================================================
# ORDER HISTORY LOCATORS (/order-history)
# ==============================================================================
${ORDER_HISTORY_HEADING}        xpath://h1[contains(., 'Order') or contains(., 'History')] | //main
${ORDER_ROW_FIRST}              xpath:(//div[contains(@class, 'grid') and contains(., '#')])[1]
${ORDER_BTN_DETAILS}            xpath:(//button[contains(., 'Details')])[1]
${ORDER_ITEMS_CONTAINER}        xpath://*[contains(text(), 'Items in this order')]
${ORDER_BTN_CANCEL}             xpath:(//button[contains(., 'Cancel')])[1]

# ==============================================================================
# SELLER APPLICATION LOCATORS (/seller-application/apply)
# ==============================================================================
${APPLY_LINK_APPLY_NOW}         xpath://a[contains(@href, '/seller-application/apply') or contains(., 'Apply to sell') or contains(., 'Update application') or contains(., 'Revise and reapply')]
${APPLY_STEP1_NAV}              xpath://button[contains(., '1. Shop details')]
${APPLY_STEP2_NAV}              xpath://button[contains(., '2. Identity')]
${APPLY_INPUT_SHOP_NAME}        id:shopName
${APPLY_INPUT_SHOP_DESC}        id:shopDescription
${APPLY_INPUT_SHOP_PHONE}       id:shopPhone
${APPLY_INPUT_SHOP_EMAIL}       id:shopEmail
${APPLY_INPUT_SHOP_ADDR}        id:shopAddress
${APPLY_BTN_CONTINUE}           xpath://button[@type='submit' and contains(., 'Continue')]

${APPLY_INPUT_FIRST_NAME}       id:sellerFirstName
${APPLY_INPUT_LAST_NAME}        id:sellerLastName
${APPLY_INPUT_ID_CARD}          id:idCardNumber
${APPLY_FILE_ID_CARD}           id:idCardImageUrl
${APPLY_INPUT_BANK_NAME}        id:bankName
${APPLY_INPUT_BANK_ACC_NAME}    id:bankAccountName
${APPLY_INPUT_BANK_ACC_NO}      id:bankAccountNumber
${APPLY_FILE_BANK_BOOK}         id:bankBookImageUrl
${APPLY_BTN_SUBMIT}             xpath://button[@type='submit' and (contains(., 'Submit') or contains(., 'Uploading') or contains(., 'Submitting'))]
${APPLY_STATUS_BADGE}           xpath://*[contains(text(), 'Under review') or contains(text(), 'Approved') or contains(text(), 'Action needed') or contains(text(), 'Rejected')]

# ==============================================================================
# ADMIN LOCATORS (/admin)
# ==============================================================================
${ADMIN_HEADING}                xpath://h2[contains(., 'All applications')]
${ADMIN_FILTER_STATUS}          id:application-status-filter
${ADMIN_BTN_REFRESH}            xpath://button[contains(., 'Refresh')]
${ADMIN_ROW_FIRST_APP}          xpath:(//table//tr | //div[contains(@class, 'divide-y')]/div)[1]
${ADMIN_BTN_REVIEW}             xpath:(//table//button[contains(., 'Review application') or contains(., 'View application')])[1]
${ADMIN_DIALOG_TITLE}           xpath://*[@role='dialog']//h2
${ADMIN_DECISION_APPROVE}       xpath://button[contains(., 'Approve')]
${ADMIN_DECISION_REJECT}        xpath://button[contains(., 'Reject')]
${ADMIN_DECISION_REQUEST_DOCS}  xpath://button[contains(., 'Request documents')]
${ADMIN_INPUT_DECISION_NOTE}    xpath://textarea[@aria-label='Rejection reason' or @aria-label='Document request']
${ADMIN_BTN_CONFIRM_DECISION}   xpath://button[contains(., 'Confirm decision')]
${ADMIN_BTN_CANCEL_DECISION}    xpath://button[contains(., 'Cancel')]

# Admin Navigation & Categories (/admin/categories)
${ADMIN_NAV_APPLICATIONS}       xpath://a[@href='/admin' or contains(., 'Seller applications')]
${ADMIN_NAV_CATEGORIES}         xpath://a[@href='/admin/categories' or contains(., 'Categories')]
${ADMIN_CAT_INPUT_NAME}         id:category-name
${ADMIN_CAT_INPUT_DESC}         id:category-description
${ADMIN_CAT_BTN_ADD}            xpath://button[@type='submit' and contains(., 'Add category')]
${ADMIN_CAT_FIRST_DELETE}       xpath:(//button[contains(@aria-label, 'Delete') or contains(., 'Delete')])[1]
${ADMIN_CAT_CONFIRM_DELETE}     xpath://button[contains(., 'Delete category')]

# ==============================================================================
# SELLER DASHBOARD & ADD PRODUCT LOCATORS
# ==============================================================================
${SELLER_NAV_DASHBOARD}         xpath://a[@href='/seller-dashboard' or contains(., 'Shop dashboard')]
${SELLER_NAV_ADD_PRODUCT}       xpath://a[@href='/seller/products/add' or contains(., 'Add Product')]
${SELLER_LINK_STOREFRONT}       xpath://a[contains(., 'View storefront')]
${SELLER_TAB_ACTIVE}            xpath://button[contains(., 'Active')]
${SELLER_TAB_HISTORY}           xpath://button[contains(., 'History')]
${SELLER_METRIC_CARDS}          xpath://div[contains(@class, 'grid')]//p[contains(text(), 'orders') or contains(text(), 'sales')]

# Add Product Form
${PRODUCT_ADD_FILE_INPUT}       xpath://input[@type='file' and @aria-label='Product images']
${PRODUCT_ADD_INPUT_NAME}       id:product-name
${PRODUCT_ADD_INPUT_DESC}       id:product-description
${PRODUCT_ADD_INPUT_PRICE}      id:product-price
${PRODUCT_ADD_INPUT_STOCK}      id:product-stock
${PRODUCT_ADD_INPUT_SHIPPING}   id:product-shipping
${PRODUCT_ADD_INPUT_CAT_SEARCH}    id:category-search
${PRODUCT_ADD_CHECKBOX_FIRST}       xpath:(//div[@role='group' and @aria-label='Product categories']//input[@type='checkbox'])[1]
${PRODUCT_ADD_BTN_SUBMIT}           xpath://button[@type='submit' and contains(., 'Add product')]

# Seller Orders & Fulfillment
${SELLER_ORDER_FIRST_LINK}      xpath:(//a[contains(@href, '/seller/') and contains(., '#')])[1]
${SELLER_ORDER_BTN_ACCEPT}      xpath://button[contains(., 'Accept and confirm')]
${SELLER_ORDER_BTN_REJECT}      xpath://button[contains(., 'Reject order')]
${SELLER_ORDER_INPUT_REJECT}    id:reject-reason
${SELLER_ORDER_INPUT_COURIER}   id:courier
${SELLER_ORDER_INPUT_TRACKING}  id:tracking
${SELLER_ORDER_BTN_SHIP}        xpath://button[@type='submit' and contains(., 'Mark as shipped')]

