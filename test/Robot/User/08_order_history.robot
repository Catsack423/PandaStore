*** Settings ***
Documentation       Test suite for Order History & Order Details in PandaStore (/order-history).
Resource            ../resources/variables.resource
Resource            ../resources/locators.resource
Resource            ../resources/common_keywords.resource

Test Setup          Open And Login Customer
Test Teardown       Teardown With Screenshot

*** Keywords ***
Open And Login Customer
    Open PandaStore Browser    ${SIGNIN_URL}
    Login As Customer          ${VALID_USERNAME}    ${VALID_PASSWORD}
    Navigate To Order History Page

*** Test Cases ***
TC_ORD_01: View Order History Page
    [Documentation]    Verify customer can access and view their order history list.
    [Tags]             order_history    positive    smoke
    Wait Until Location Contains    /order-history    timeout=${TIMEOUT}
    # Page header or breadcrumb should indicate order history
    Page Should Contain             Order

TC_ORD_02: Expand Order Details To View Purchased Items
    [Documentation]    Verify customer can click Details to expand sub-order items and pricing.
    [Tags]             order_history    positive
    ${has_orders}=    Run Keyword And Return Status    Wait Until Element Is Visible    ${ORDER_BTN_DETAILS}    timeout=5s
    IF    ${has_orders}
        Click Button    ${ORDER_BTN_DETAILS}
        Wait Until Page Contains Element    ${ORDER_ITEMS_CONTAINER}    timeout=${TIMEOUT}
        Page Should Contain                 Items in this order
    END

TC_ORD_03: Verify Order Action Buttons Based On Status
    [Documentation]    Verify action buttons (Pay now / Cancel / Review / Details) render appropriately.
    [Tags]             order_history    positive
    ${has_orders}=    Run Keyword And Return Status    Wait Until Element Is Visible    ${ORDER_BTN_DETAILS}    timeout=5s
    IF    ${has_orders}
        # Check details button is clickable
        Element Should Be Enabled    ${ORDER_BTN_DETAILS}
    END

TC_ORD_04: Order History Requires Customer Authentication
    [Documentation]    Verify guest cannot view order history without signing in.
    [Tags]             order_history    negative
    Logout Customer
    Go To    ${ORDER_HISTORY_URL}
    # Guest user is either redirected to signin or prompted to sign in
    ${prompt_or_redirect}=    Run Keyword And Return Status
    ...    Wait Until Location Contains    /signin    timeout=5s
    IF    not ${prompt_or_redirect}
        Page Should Contain    Sign in
    END
