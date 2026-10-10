*** Settings ***
Documentation       Admin Authentication and Access Control Test Suite for PandaStore.
...                 Covers admin security and portal access:
...                 - Guest and Customer access prevention to /admin
...                 - Successful Admin login (admin / admin1234)
...                 - Admin header navigation and layout verification
Resource            ../resources/variables.resource
Resource            ../resources/locators.resource
Resource            ../resources/common_keywords.resource

Test Setup          Open PandaStore Browser    ${HOME_URL}
Test Teardown       Teardown With Screenshot

*** Test Cases ***
TC_ADMIN_AUTH_01: Unauthenticated Guest Is Blocked From Admin Portal
    [Documentation]    Verify an unauthenticated visitor is redirected to signin when attempting to visit /admin.
    [Tags]             admin    auth    security
    Logout Customer
    Go To    ${ADMIN_URL}
    Wait Until Location Contains    /signin    timeout=${TIMEOUT}
    Location Should Contain         callbackUrl

TC_ADMIN_AUTH_02: Customer User Is Denied Admin Portal Access
    [Documentation]    Verify a regular customer user is denied access to /admin and redirected away.
    [Tags]             admin    auth    role_check
    Login As Customer    ${VALID_USERNAME}    ${VALID_PASSWORD}
    Go To                ${ADMIN_URL}
    Wait Until Location Does Not Contain    /admin    timeout=${TIMEOUT}
    Page Should Not Contain Element         ${ADMIN_HEADING}

TC_ADMIN_AUTH_03: Admin User Successfully Logs In And Accesses Admin Portal
    [Documentation]    Verify admin account logs in and accesses /admin with management options.
    [Tags]             admin    auth    access
    Login As Admin    ${ADMIN_USERNAME}    ${ADMIN_PASSWORD}
    Go To             ${ADMIN_URL}
    Wait Until Location Contains        /admin    timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${ADMIN_HEADING}    timeout=${TIMEOUT}
    Page Should Contain                 All applications

TC_ADMIN_AUTH_04: Verify Admin Navigation Menu Links
    [Documentation]    Verify admin layout includes links to Seller Applications and Categories.
    [Tags]             admin    navigation
    Login As Admin    ${ADMIN_USERNAME}    ${ADMIN_PASSWORD}
    Go To             ${ADMIN_URL}
    Wait Until Page Contains Element    ${ADMIN_NAV_APPLICATIONS}    timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${ADMIN_NAV_CATEGORIES}      timeout=${TIMEOUT}
    Page Should Contain                 Seller applications
    Page Should Contain                 Categories
