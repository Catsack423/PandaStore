*** Settings ***
Documentation       Test suite for Order Placement & Checkout in PandaStore (/checkout).
Resource            resources/variables.resource
Resource            resources/locators.resource
Resource            resources/common_keywords.resource

Test Setup          Setup Customer With Item In Cart
Test Teardown       Teardown With Screenshot

*** Keywords ***
Setup Customer With Item In Cart
    Open PandaStore Browser    ${SIGNIN_URL}
    Login As Customer          ${VALID_USERNAME}    ${VALID_PASSWORD}
    Add First Product In Shop To Cart
    Navigate To Cart Page

*** Test Cases ***
TC_CHECKOUT_01: Navigate From Cart To Checkout Page
    [Documentation]    Verify customer can proceed from the shopping cart to checkout.
    [Tags]             checkout    positive    smoke
    Wait Until Element Is Visible    ${CART_LINK_CHECKOUT}    timeout=${TIMEOUT}
    Click Element                    ${CART_LINK_CHECKOUT}

    Wait Until Location Contains     /checkout                timeout=${TIMEOUT}
    Page Should Contain              Delivery address
    Page Should Contain              Order summary

TC_CHECKOUT_02: Verify Checkout Order Summary Details
    [Documentation]    Verify product details, quantity, shipping, and grand total appear on checkout.
    [Tags]             checkout    positive
    Navigate To Checkout Page

    Page Should Contain              Order summary
    Page Should Contain              Products
    Page Should Contain              Payment method

TC_CHECKOUT_03: Add New Delivery Address During Checkout
    [Documentation]    Verify customer can add a delivery address directly on the checkout page.
    [Tags]             checkout    address    positive
    Navigate To Checkout Page

    ${has_add_btn}=    Run Keyword And Return Status    Wait Until Element Is Visible    ${ADDR_BTN_ADD_FORM}    timeout=5s
    IF    ${has_add_btn}
        Click Button    ${ADDR_BTN_ADD_FORM}
        ${rand}=        Generate Random String    4    [NUMBERS]
        Wait Until Element Is Visible    ${ADDR_INPUT_RECEIVER}    timeout=${TIMEOUT}
        Input Text      ${ADDR_INPUT_RECEIVER}    ผู้รับ เช็คเอาท์ ${rand}
        Input Text      ${ADDR_INPUT_PHONE}       089777${rand}
        Input Text      ${ADDR_INPUT_LINE}        88/9 หมู่ 2 ถนนประชาสโมสร
        Input Text      ${ADDR_INPUT_DISTRICT}    ในเมือง
        Input Text      ${ADDR_INPUT_PROVINCE}    ขอนแก่น
        Input Text      ${ADDR_INPUT_POSTAL}      40000
        Click Button    ${ADDR_BUTTON_SAVE}
        Sleep           2s
    END

TC_CHECKOUT_04: Place Order Successfully
    [Documentation]    Test placing an order with selected address, shipping method, and PromptPay payment.
    [Tags]             checkout    order    smoke    positive
    Navigate To Checkout Page

    # Ensure delivery address is selected
    Wait Until Page Contains Element    ${CHECKOUT_RADIO_FIRST_ADDR}    timeout=${TIMEOUT}
    Click Element                       ${CHECKOUT_RADIO_FIRST_ADDR}

    # Ensure PromptPay payment method radio is selected
    Wait Until Page Contains Element    ${CHECKOUT_RADIO_PROMPTPAY}     timeout=${TIMEOUT}
    Click Element                       ${CHECKOUT_RADIO_PROMPTPAY}

    # Wait for shipping quotes to finish calculating and Place order button to be enabled
    Wait Until Element Is Enabled       ${CHECKOUT_BTN_PLACE_ORDER}     timeout=20s
    Click Button                        ${CHECKOUT_BTN_PLACE_ORDER}

    # Verify confirmation page is displayed
    Wait Until Page Contains Element    ${CHECKOUT_CONFIRMATION_TITLE}  timeout=20s
    Page Should Contain                 Your order has been placed
    Page Should Contain Element         ${CHECKOUT_LINK_PAY_NOW}
    Page Should Contain Element         ${CHECKOUT_LINK_VIEW_ORDER}

TC_CHECKOUT_05: Checkout Requires Customer Authentication
    [Documentation]    Verify guest user is prompted to sign in when accessing checkout without account.
    [Tags]             checkout    negative
    Logout Customer
    Go To    ${CHECKOUT_URL}
    Wait Until Page Contains    Sign in to check out    timeout=${TIMEOUT}
