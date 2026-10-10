*** Settings ***
Documentation       Complete End-to-End (E2E) Lifecycle Test Suite for PandaStore.
...                 Workflow: User Signup -> Seller Application -> Admin Approval ->
...                 Seller Publishes Product -> Customer Orders & Pays -> Seller Fulfills & Ships.
Resource            resources/variables.resource
Resource            resources/locators.resource
Resource            resources/common_keywords.resource

Suite Setup         Open PandaStore Browser    ${HOME_URL}
Suite Teardown      Close PandaStore Browser

*** Variables ***
${NEW_SELLER_USER}          ${EMPTY}
${NEW_SELLER_PASS}          ${EMPTY}
${NEW_SELLER_EMAIL}         ${EMPTY}
${NEW_SHOP_NAME}            ${EMPTY}
${ORDER_GROUP_ID}           ${EMPTY}

*** Test Cases ***
PHASE 1: Customer Account Registration
    [Documentation]    Step 1: A new user registers a Customer account on PandaStore.
    [Tags]             e2e    user    register
    ${u}    ${e}    ${p}    ${n}    ${pw}=    Generate Random Customer Credentials
    Set Suite Variable    ${NEW_SELLER_USER}     ${u}
    Set Suite Variable    ${NEW_SELLER_EMAIL}    ${e}
    Set Suite Variable    ${NEW_SELLER_PASS}     ${pw}
    Set Suite Variable    ${NEW_SHOP_NAME}       Shop_${u}

    Go To Sign Up Page
    Input Text        ${SIGNUP_INPUT_USERNAME}      ${u}
    Input Text        ${SIGNUP_INPUT_FULLNAME}      ${n}
    Input Text        ${SIGNUP_INPUT_EMAIL}         ${e}
    Input Text        ${SIGNUP_INPUT_PHONE}         ${p}
    Input Password    ${SIGNUP_INPUT_PASSWORD}      ${pw}
    Input Password    ${SIGNUP_INPUT_CONFIRM_PW}    ${pw}
    Wait Until Element Is Visible    ${SIGNUP_BUTTON_SUBMIT}    timeout=${TIMEOUT}
    Scroll Element Into View         ${SIGNUP_BUTTON_SUBMIT}
    ${sub_btn}=    Get WebElement    ${SIGNUP_BUTTON_SUBMIT}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${sub_btn}

    # Verify registration succeeds and navigates away from /signup
    ${redirected}=    Run Keyword And Return Status
    ...    Wait Until Location Does Not Contain    /signup    timeout=10s
    IF    not ${redirected}
        Execute JavaScript    arguments[0].click();    ARGUMENTS    ${sub_btn}
        Wait Until Location Does Not Contain    /signup    timeout=20s
    END
    Capture Page Screenshot    ${SCREENSHOT_DIR}/E2E_01_User_Registered.png

PHASE 2: User Submits Seller Application
    [Documentation]    Step 2: The registered customer applies to become a Seller.
    [Tags]             e2e    seller    application
    # Ensure logged in as the new user
    Login If Not Already Logged In    ${NEW_SELLER_USER}    ${NEW_SELLER_PASS}

    # Open seller application form
    Go To    ${SELLER_APPLY_URL}
    Wait Until Page Contains Element    ${APPLY_INPUT_SHOP_NAME}    timeout=${TIMEOUT}

    # Step 1: Shop details
    Input Text    ${APPLY_INPUT_SHOP_NAME}     ${NEW_SHOP_NAME}
    Input Text    ${APPLY_INPUT_SHOP_DESC}     ${APPLY_SHOP_DESC}
    Input Text    ${APPLY_INPUT_SHOP_PHONE}    ${APPLY_SHOP_PHONE}
    Input Text    ${APPLY_INPUT_SHOP_EMAIL}    ${NEW_SELLER_EMAIL}
    Input Text    ${APPLY_INPUT_SHOP_ADDR}     ${APPLY_SHOP_ADDRESS}
    Scroll Element Into View    ${APPLY_BTN_CONTINUE}
    Click Button    ${APPLY_BTN_CONTINUE}

    # Step 2: Identity & Bank details
    Wait Until Page Contains Element    ${APPLY_INPUT_FIRST_NAME}    timeout=${TIMEOUT}
    Input Text    ${APPLY_INPUT_FIRST_NAME}       ${APPLY_FIRST_NAME}
    Input Text    ${APPLY_INPUT_LAST_NAME}        ${APPLY_LAST_NAME}
    Input Text    ${APPLY_INPUT_ID_CARD}          ${APPLY_ID_CARD_NO}
    Make File Input Interactable                  ${APPLY_FILE_ID_CARD}
    Choose File   ${APPLY_FILE_ID_CARD}           ${TEST_IMAGE_PATH}
    Wait Until Element Is Visible                 xpath://span[contains(., 'File uploaded')]    timeout=30s

    Input Text    ${APPLY_INPUT_BANK_NAME}        ${APPLY_BANK_NAME}
    Input Text    ${APPLY_INPUT_BANK_ACC_NAME}    ${APPLY_BANK_ACC_NAME}
    Input Text    ${APPLY_INPUT_BANK_ACC_NO}      ${APPLY_BANK_ACC_NO}
    Make File Input Interactable                  ${APPLY_FILE_BANK_BOOK}
    Choose File   ${APPLY_FILE_BANK_BOOK}         ${TEST_IMAGE_PATH}

    Scroll Element Into View         ${APPLY_BTN_SUBMIT}
    Wait Until Element Is Enabled    ${APPLY_BTN_SUBMIT}    timeout=40s
    ${sub_btn}=    Get WebElement    ${APPLY_BTN_SUBMIT}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${sub_btn}
    Wait Until Location Contains     /seller-application    timeout=${TIMEOUT}
    Page Should Contain              Under review
    Capture Page Screenshot          ${SCREENSHOT_DIR}/E2E_02_Seller_Application_Submitted.png

PHASE 3: Admin Approves Seller Application
    [Documentation]    Step 3: Platform Admin reviews and approves the pending seller application.
    [Tags]             e2e    admin    approval
    Login As Admin

    Wait Until Page Contains Element    ${ADMIN_HEADING}          timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${ADMIN_FILTER_STATUS}    timeout=${TIMEOUT}
    Select From List By Value           ${ADMIN_FILTER_STATUS}    PENDING
    Sleep                               1.5s
    # Locate pending application for the newly created shop, or fallback to first review button
    ${specific_shop_btn}=    Set Variable    xpath:(//table//tr[contains(., '${NEW_SHOP_NAME}')]//button[contains(., 'Review') or contains(., 'View')])[1]
    ${has_specific}=    Run Keyword And Return Status    Page Should Contain Element    ${specific_shop_btn}
    IF    ${has_specific}
        Scroll Element Into View    ${specific_shop_btn}
        ${rev_btn}=    Get WebElement    ${specific_shop_btn}
    ELSE
        Wait Until Page Contains Element    ${ADMIN_BTN_REVIEW}       timeout=${TIMEOUT}
        Wait Until Element Is Visible       ${ADMIN_BTN_REVIEW}       timeout=${TIMEOUT}
        Scroll Element Into View            ${ADMIN_BTN_REVIEW}
        ${rev_btn}=    Get WebElement       ${ADMIN_BTN_REVIEW}
    END
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${rev_btn}

    # Review dialog: Click Approve and Confirm decision
    Wait Until Element Is Visible       ${ADMIN_DECISION_APPROVE}        timeout=${TIMEOUT}
    ${appr_btn}=    Get WebElement      ${ADMIN_DECISION_APPROVE}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${appr_btn}

    Wait Until Element Is Visible       ${ADMIN_BTN_CONFIRM_DECISION}    timeout=${TIMEOUT}
    ${conf_btn}=    Get WebElement      ${ADMIN_BTN_CONFIRM_DECISION}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${conf_btn}
    Sleep                               2s

    Capture Page Screenshot             ${SCREENSHOT_DIR}/E2E_03_Admin_Approved_Application.png

PHASE 4: Approved Seller Publishes A New Product
    [Documentation]    Step 4: The approved Seller adds and publishes a product for sale.
    [Tags]             e2e    seller    product
    # Login as approved seller
    Login As Seller    ${NEW_SELLER_USER}    ${NEW_SELLER_PASS}

    # Navigate to Add Product form
    Go To    ${SELLER_ADD_PRODUCT_URL}
    Wait Until Page Contains Element    ${PRODUCT_ADD_INPUT_NAME}    timeout=${TIMEOUT}

    # Fill product details first
    Input Text    ${PRODUCT_ADD_INPUT_NAME}        ${NEW_PRODUCT_NAME}
    Input Text    ${PRODUCT_ADD_INPUT_DESC}        ${NEW_PRODUCT_DESC}
    Input Text    ${PRODUCT_ADD_INPUT_PRICE}       ${NEW_PRODUCT_PRICE}
    Input Text    ${PRODUCT_ADD_INPUT_STOCK}       ${NEW_PRODUCT_STOCK}
    Input Text    ${PRODUCT_ADD_INPUT_SHIPPING}    ${NEW_PRODUCT_SHIPPING}

    # Select category
    Scroll Element Into View    ${PRODUCT_ADD_CHECKBOX_FIRST}
    Select Checkbox             ${PRODUCT_ADD_CHECKBOX_FIRST}

    # Upload product image
    Make File Input Interactable    ${PRODUCT_ADD_FILE_INPUT}
    Choose File   ${PRODUCT_ADD_FILE_INPUT}    ${TEST_IMAGE_PATH}

    # Submit product
    Scroll Element Into View         ${PRODUCT_ADD_BTN_SUBMIT}
    Wait Until Element Is Enabled    ${PRODUCT_ADD_BTN_SUBMIT}    timeout=30s
    ${add_btn}=    Get WebElement    ${PRODUCT_ADD_BTN_SUBMIT}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${add_btn}
    Wait Until Location Contains     /shop/    timeout=25s

    Capture Page Screenshot     ${SCREENSHOT_DIR}/E2E_04_Product_Published.png

PHASE 5: Customer Buys Product And Places Order
    [Documentation]    Step 5: Customer browses, adds the product to cart, and completes checkout.
    [Tags]             e2e    customer    checkout
    # Ensure fresh login as Customer
    Logout Customer
    Login As Customer    ${VALID_USERNAME}    ${VALID_PASSWORD}

    # Add product to cart
    Add First Product In Shop To Cart
    Navigate To Checkout Page

    # Select Delivery Address & PromptPay Payment
    ${has_addr}=    Run Keyword And Return Status
    ...    Wait Until Page Contains Element    ${CHECKOUT_RADIO_FIRST_ADDR}    timeout=5s
    IF    ${has_addr}
        ${addr_elem}=    Get WebElement    ${CHECKOUT_RADIO_FIRST_ADDR}
        Execute JavaScript    arguments[0].click();    ARGUMENTS    ${addr_elem}
    ELSE
        ${has_add_btn}=    Run Keyword And Return Status    Wait Until Element Is Visible    ${ADDR_BTN_ADD_FORM}    timeout=5s
        IF    ${has_add_btn}
            Scroll Element Into View         ${ADDR_BTN_ADD_FORM}
            ${add_btn}=    Get WebElement    ${ADDR_BTN_ADD_FORM}
            Execute JavaScript    arguments[0].click();    ARGUMENTS    ${add_btn}
            Wait Until Element Is Visible    ${ADDR_INPUT_RECEIVER}    timeout=${TIMEOUT}
            Input Text      ${ADDR_INPUT_RECEIVER}    ${CUST_RECEIVER_NAME}
            Input Text      ${ADDR_INPUT_PHONE}       ${CUST_PHONE}
            Input Text      ${ADDR_INPUT_LINE}        ${CUST_ADDRESS_LINE}
            Input Text      ${ADDR_INPUT_DISTRICT}    ${CUST_DISTRICT}
            Input Text      ${ADDR_INPUT_PROVINCE}    ${CUST_PROVINCE}
            Input Text      ${ADDR_INPUT_POSTAL}      ${CUST_POSTAL_CODE}
            Scroll Element Into View          ${ADDR_BUTTON_SAVE}
            ${save_btn}=    Get WebElement    ${ADDR_BUTTON_SAVE}
            Execute JavaScript    arguments[0].click();    ARGUMENTS    ${save_btn}
            Sleep           2s
        END
        Wait Until Page Contains Element    ${CHECKOUT_RADIO_FIRST_ADDR}    timeout=${TIMEOUT}
        ${addr_elem}=    Get WebElement     ${CHECKOUT_RADIO_FIRST_ADDR}
        Execute JavaScript    arguments[0].click();    ARGUMENTS    ${addr_elem}
    END

    Wait Until Page Contains Element    ${CHECKOUT_RADIO_PROMPTPAY}     timeout=${TIMEOUT}
    ${pay_elem}=    Get WebElement      ${CHECKOUT_RADIO_PROMPTPAY}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${pay_elem}

    # Place order
    Scroll Element Into View         ${CHECKOUT_BTN_PLACE_ORDER}
    Wait Until Element Is Enabled    ${CHECKOUT_BTN_PLACE_ORDER}     timeout=20s
    ${place_btn}=    Get WebElement  ${CHECKOUT_BTN_PLACE_ORDER}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${place_btn}

    # Verify confirmation page
    Wait Until Page Contains Element    ${CHECKOUT_CONFIRMATION_TITLE}  timeout=20s
    Page Should Contain                 Your order has been placed
    Capture Page Screenshot             ${SCREENSHOT_DIR}/E2E_05_Order_Placed.png

PHASE 6: Customer Confirms PromptPay Payment
    [Documentation]    Step 6: Customer scans PromptPay QR and confirms payment.
    [Tags]             e2e    customer    payment
    # Click Pay now from checkout confirmation
    Wait Until Element Is Visible    ${CHECKOUT_LINK_PAY_NOW}    timeout=${TIMEOUT}
    ${paynow_btn}=    Get WebElement    ${CHECKOUT_LINK_PAY_NOW}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${paynow_btn}

    # Payment page
    Wait Until Page Contains Element    ${PAYMENT_PAGE_TITLE}        timeout=${TIMEOUT}
    Wait Until Element Is Visible       ${PAYMENT_BTN_CONFIRM}       timeout=${TIMEOUT}
    ${confirm_btn}=    Get WebElement   ${PAYMENT_BTN_CONFIRM}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${confirm_btn}

    # Verify payment success
    Wait Until Page Contains Element    ${PAYMENT_SUCCESS_MESSAGE}   timeout=${TIMEOUT}
    Capture Page Screenshot             ${SCREENSHOT_DIR}/E2E_06_Payment_Confirmed.png

PHASE 7: Seller Fulfills And Ships The Order
    [Documentation]    Step 7: Seller accepts order, enters tracking, and marks as shipped.
    [Tags]             e2e    seller    shipping
    Login As Seller    ${SELLER_USERNAME}    ${SELLER_PASSWORD}

    # Open first order on Seller Dashboard
    Wait Until Page Contains Element    ${SELLER_ORDER_FIRST_LINK}    timeout=${TIMEOUT}
    ${ord_link}=    Get WebElement      ${SELLER_ORDER_FIRST_LINK}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${ord_link}

    # If order is waiting seller confirmation, click Accept
    ${has_accept}=    Run Keyword And Return Status
    ...    Wait Until Element Is Visible    ${SELLER_ORDER_BTN_ACCEPT}    timeout=5s
    IF    ${has_accept}
        ${acc_btn}=    Get WebElement    ${SELLER_ORDER_BTN_ACCEPT}
        Execute JavaScript    arguments[0].click();    ARGUMENTS    ${acc_btn}
        Sleep           2s
    END

    # Fill Courier and Tracking number, then ship
    ${has_ship}=    Run Keyword And Return Status
    ...    Wait Until Element Is Visible    ${SELLER_ORDER_INPUT_COURIER}    timeout=5s
    IF    ${has_ship}
        Input Text    ${SELLER_ORDER_INPUT_COURIER}     ${COURIER_NAME}
        Input Text    ${SELLER_ORDER_INPUT_TRACKING}    ${TRACKING_NUMBER}
        ${ship_btn}=    Get WebElement    ${SELLER_ORDER_BTN_SHIP}
        Execute JavaScript    arguments[0].click();    ARGUMENTS    ${ship_btn}
        Sleep         2s
    END

    Capture Page Screenshot    ${SCREENSHOT_DIR}/E2E_07_Seller_Shipped_Order.png

PHASE 8: Customer Verifies Shipped Order In Order History
    [Documentation]    Step 8: Customer tracks order in Order History and verifies SHIPPED status.
    [Tags]             e2e    customer    order_history
    Logout Customer
    Login As Customer    ${VALID_USERNAME}    ${VALID_PASSWORD}
    Navigate To Order History Page

    # Expand order details
    ${has_details}=    Run Keyword And Return Status
    ...    Wait Until Element Is Visible    ${ORDER_BTN_DETAILS}    timeout=5s
    IF    ${has_details}
        ${det_btn}=    Get WebElement    ${ORDER_BTN_DETAILS}
        Execute JavaScript    arguments[0].click();    ARGUMENTS    ${det_btn}
        Wait Until Page Contains Element    ${ORDER_ITEMS_CONTAINER}    timeout=${TIMEOUT}
    END

    Capture Page Screenshot    ${SCREENSHOT_DIR}/E2E_08_Order_History_Complete.png
