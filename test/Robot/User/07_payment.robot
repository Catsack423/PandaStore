*** Settings ***
Documentation       Test suite for Payment Processing in PandaStore (/payment/{orderGroupId}).
Resource            ../resources/variables.resource
Resource            ../resources/locators.resource
Resource            ../resources/common_keywords.resource

Test Setup          Setup Placed Order For Payment
Test Teardown       Teardown With Screenshot

*** Keywords ***
Setup Placed Order For Payment
    Open PandaStore Browser    ${SIGNIN_URL}
    Login As Customer          ${VALID_USERNAME}    ${VALID_PASSWORD}
    Add First Product In Shop To Cart
    Navigate To Checkout Page
    Wait Until Page Contains Element    ${CHECKOUT_RADIO_FIRST_ADDR}    timeout=${TIMEOUT}
    Click Element                       ${CHECKOUT_RADIO_FIRST_ADDR}
    Wait Until Element Is Enabled       ${CHECKOUT_BTN_PLACE_ORDER}     timeout=20s
    Click Button                        ${CHECKOUT_BTN_PLACE_ORDER}
    Wait Until Element Is Visible       ${CHECKOUT_LINK_PAY_NOW}        timeout=20s
    Click Element                       ${CHECKOUT_LINK_PAY_NOW}
    Wait Until Location Contains        /payment/                       timeout=${TIMEOUT}

*** Test Cases ***
TC_PAY_01: Display Payment QR Code And Order Total
    [Documentation]    Verify the payment page displays the QR code, products amount, and grand total.
    [Tags]             payment    positive    smoke
    Wait Until Page Contains Element    ${PAYMENT_PAGE_TITLE}     timeout=${TIMEOUT}
    Page Should Contain                 Scan to pay
    Page Should Contain                 Total to pay
    Page Should Contain Element         ${PAYMENT_QR_IMAGE}

TC_PAY_02: Confirm Demo Payment Successfully
    [Documentation]    Verify customer can confirm payment and see the success status.
    [Tags]             payment    positive    smoke
    Wait Until Element Is Visible       ${PAYMENT_BTN_CONFIRM}          timeout=${TIMEOUT}
    Click Button                        ${PAYMENT_BTN_CONFIRM}

    # Verify confirmation success message and link to order history
    Wait Until Page Contains Element    ${PAYMENT_SUCCESS_MESSAGE}      timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${PAYMENT_LINK_ORDER_HISTORY}   timeout=${TIMEOUT}

TC_PAY_03: Cancel Order From Payment Screen
    [Documentation]    Verify customer can cancel pending payment and order from payment page.
    [Tags]             payment    cancel    positive
    Wait Until Element Is Visible    ${PAYMENT_BTN_CANCEL_ORDER}    timeout=${TIMEOUT}
    Click Button                     ${PAYMENT_BTN_CANCEL_ORDER}

    # Confirm Dialog appears
    Wait Until Page Contains         Are you sure you want to cancel this order    timeout=5s
    Click Button                     xpath://button[contains(., 'Yes, Cancel Order')]

    # Verifies redirect to order-history
    Wait Until Location Contains     /order-history    timeout=${TIMEOUT}
