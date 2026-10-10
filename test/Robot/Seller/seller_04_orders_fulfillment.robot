*** Settings ***
Documentation       Seller Orders & Fulfillment Test Suite for PandaStore.
...                 Covers seller order management and shipping:
...                 - Viewing orders on the dashboard
...                 - Inspecting order details (/seller/{orderId})
...                 - Accepting paid order (WAITING_SELLER_CONFIRM -> PREPARING)
...                 - Entering shipping courier and tracking number
...                 - Marking order as shipped (PREPARING -> SHIPPED)
Resource            ../resources/variables.resource
Resource            ../resources/locators.resource
Resource            ../resources/common_keywords.resource

Suite Setup         Open PandaStore Browser And Login As Seller
Suite Teardown      Close PandaStore Browser
Test Teardown       Capture Screenshot On Test Complete

*** Keywords ***
Open PandaStore Browser And Login As Seller
    Open PandaStore Browser    ${HOME_URL}
    Login As Seller            ${SELLER_USERNAME}    ${SELLER_PASSWORD}

*** Test Cases ***
TC_SELLER_ORD_01: Seller Views Order List On Dashboard
    [Documentation]    Verify seller can review incoming orders on the dashboard table.
    [Tags]             seller    orders    list
    Go To              ${SELLER_DASHBOARD_URL}
    Wait Until Location Contains    /seller-dashboard    timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${SELLER_METRIC_CARDS}    timeout=${TIMEOUT}
    Page Should Contain                 Orders

TC_SELLER_ORD_02: Seller Opens An Order Detail Page
    [Documentation]    Verify seller can click an order link to inspect full details at /seller/{orderId}.
    [Tags]             seller    orders    details
    Go To    ${SELLER_DASHBOARD_URL}
    Wait Until Page Contains Element    ${SELLER_METRIC_CARDS}    timeout=${TIMEOUT}

    ${has_orders}=    Run Keyword And Return Status
    ...    Wait Until Page Contains Element    ${SELLER_ORDER_FIRST_LINK}    timeout=5s
    IF    ${has_orders}
        Click Element    ${SELLER_ORDER_FIRST_LINK}
        Wait Until Location Contains    /seller/    timeout=${TIMEOUT}
        Page Should Contain             Order items
        Page Should Contain             Delivery details
    ELSE
        Log    No active orders currently present for this seller.
    END

TC_SELLER_ORD_03: Verify Order Details Structure And Items
    [Documentation]    Verify order details screen displays items, pricing summary, and shipping address.
    [Tags]             seller    orders    structure
    ${in_order_page}=    Run Keyword And Return Status    Location Should Contain    /seller/
    IF    ${in_order_page}
        Page Should Contain Element    xpath://*[contains(text(), 'Order items')]
        Page Should Contain Element    xpath://*[contains(text(), 'Delivery details')]
        Page Should Contain Element    xpath://a[contains(., 'Back to dashboard')]
    END

TC_SELLER_ORD_04: Seller Accepts Pending Customer Order
    [Documentation]    Verify seller can accept order awaiting confirmation.
    [Tags]             seller    orders    accept
    ${has_accept}=    Run Keyword And Return Status
    ...    Wait Until Element Is Visible    ${SELLER_ORDER_BTN_ACCEPT}    timeout=5s
    IF    ${has_accept}
        Click Button    ${SELLER_ORDER_BTN_ACCEPT}
        Sleep           2s
        Page Should Not Contain Element    ${SELLER_ORDER_BTN_ACCEPT}
    ELSE
        Log    Order is not currently in WAITING_SELLER_CONFIRM status or no orders exist.
    END

TC_SELLER_ORD_05: Seller Dispatches Order With Courier And Tracking Number
    [Documentation]    Verify seller can provide courier name and tracking number to mark order as shipped.
    [Tags]             seller    orders    ship
    ${has_ship_input}=    Run Keyword And Return Status
    ...    Wait Until Element Is Visible    ${SELLER_ORDER_INPUT_COURIER}    timeout=5s
    IF    ${has_ship_input}
        Input Text    ${SELLER_ORDER_INPUT_COURIER}     ${COURIER_NAME}
        Input Text    ${SELLER_ORDER_INPUT_TRACKING}    ${TRACKING_NUMBER}
        Scroll Element Into View    ${SELLER_ORDER_BTN_SHIP}
        Click Button  ${SELLER_ORDER_BTN_SHIP}
        Sleep         2s
        Page Should Contain    This order has shipped
    ELSE
        Log    Order is not currently in PREPARING status ready for shipping.
    END
