*** Settings ***
Documentation       Seller Dashboard Test Suite for PandaStore.
...                 Covers seller shop dashboard:
...                 - Authentication as Seller
...                 - Dashboard metrics (Paid sales, Active orders, Awaiting acceptance, Completed orders)
...                 - Order views tab filtering (Active vs History)
...                 - Storefront link navigation
Resource            resources/variables.resource
Resource            resources/locators.resource
Resource            resources/common_keywords.resource

Suite Setup         Open PandaStore Browser And Login As Seller
Suite Teardown      Close PandaStore Browser
Test Teardown       Capture Screenshot On Test Complete

*** Keywords ***
Open PandaStore Browser And Login As Seller
    Open PandaStore Browser    ${HOME_URL}
    Login As Seller            ${SELLER_USERNAME}    ${SELLER_PASSWORD}

*** Test Cases ***
TC_SELLER_DASH_01: Seller Authenticates And Accesses Shop Dashboard
    [Documentation]    Verify seller can log in and view the shop dashboard with shop heading.
    [Tags]             seller    dashboard    auth
    Go To    ${SELLER_DASHBOARD_URL}
    Wait Until Location Contains    /seller-dashboard    timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${SELLER_LINK_STOREFRONT}    timeout=${TIMEOUT}
    Page Should Contain                 Sell on PandaStore

TC_SELLER_DASH_02: Verify Metrics Cards Display On Dashboard
    [Documentation]    Verify all 4 operational metrics cards are rendered on the seller dashboard.
    [Tags]             seller    dashboard    metrics
    Go To    ${SELLER_DASHBOARD_URL}
    Wait Until Page Contains Element    ${SELLER_METRIC_CARDS}    timeout=${TIMEOUT}

    Page Should Contain    Paid sales
    Page Should Contain    Active orders
    Page Should Contain    Awaiting acceptance
    Page Should Contain    Completed orders

TC_SELLER_DASH_03: Toggle Order Tabs Between Active And History
    [Documentation]    Verify seller can toggle order views between Active and History.
    [Tags]             seller    dashboard    tabs
    Go To    ${SELLER_DASHBOARD_URL}
    Wait Until Page Contains Element    ${SELLER_TAB_ACTIVE}    timeout=${TIMEOUT}

    # Click History tab
    Scroll Element Into View    ${SELLER_TAB_HISTORY}
    Click Button                ${SELLER_TAB_HISTORY}
    Sleep                       1s
    Element Attribute Value Should Be    ${SELLER_TAB_HISTORY}    aria-pressed    true

    # Click Active tab
    Click Button                ${SELLER_TAB_ACTIVE}
    Sleep                       1s
    Element Attribute Value Should Be    ${SELLER_TAB_ACTIVE}     aria-pressed    true

TC_SELLER_DASH_04: Navigate To Public Storefront From Dashboard
    [Documentation]    Verify seller can click 'View storefront' and land on their public store page.
    [Tags]             seller    dashboard    storefront
    Go To    ${SELLER_DASHBOARD_URL}
    Wait Until Page Contains Element    xpath://a[contains(., 'View storefront')]    timeout=${TIMEOUT}
    ${store_url}=    Get Element Attribute    xpath://a[contains(., 'View storefront')]    href
    Go To            ${store_url}
    Wait Until Location Contains        /shop/    timeout=${TIMEOUT}
    Page Should Contain                 Storefront
