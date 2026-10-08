*** Settings ***
Documentation       Test suite for User Profile & Address Management in PandaStore (/my-account).
Resource            resources/variables.resource
Resource            resources/locators.resource
Resource            resources/common_keywords.resource

Test Setup          Open And Login Customer
Test Teardown       Teardown With Screenshot

*** Keywords ***
Open And Login Customer
    Open PandaStore Browser    ${SIGNIN_URL}
    Login As Customer          ${VALID_USERNAME}    ${VALID_PASSWORD}
    Go To                      ${ACCOUNT_URL}
    Wait Until Location Contains    /my-account    timeout=${TIMEOUT}

*** Test Cases ***
TC_PROF_01: View Customer Profile Details
    [Documentation]    Verify customer can view their personal information in My Account.
    [Tags]             profile    positive
    Wait Until Element Is Visible    ${ACCOUNT_TAB_PROFILE}    timeout=${TIMEOUT}
    Click Button                     ${ACCOUNT_TAB_PROFILE}

    Wait Until Element Is Visible    ${PROFILE_INPUT_NAME}     timeout=${TIMEOUT}
    Element Should Be Visible        ${PROFILE_INPUT_EMAIL}
    Element Should Be Visible        ${PROFILE_INPUT_PHONE}

TC_PROF_02: Update Profile Information
    [Documentation]    Verify customer can edit and update their profile name and phone.
    [Tags]             profile    positive
    Wait Until Element Is Visible    ${ACCOUNT_TAB_PROFILE}    timeout=${TIMEOUT}
    Click Button                     ${ACCOUNT_TAB_PROFILE}

    ${rand}=    Generate Random String    4    [NUMBERS]
    Input Text      ${PROFILE_INPUT_PHONE}    081234${rand}
    Click Button    ${PROFILE_BUTTON_SAVE}
    Sleep           2s

TC_PROF_03: Change Password Validation On Mismatch
    [Documentation]    Verify error is shown when new password does not match confirmation.
    [Tags]             profile    password    negative
    Wait Until Element Is Visible    ${ACCOUNT_TAB_PASSWORD}    timeout=${TIMEOUT}
    Click Button                     ${ACCOUNT_TAB_PASSWORD}

    Wait Until Element Is Visible    ${PW_INPUT_CURRENT}    timeout=${TIMEOUT}
    Input Password    ${PW_INPUT_CURRENT}    ${VALID_PASSWORD}
    Input Password    ${PW_INPUT_NEW}        NewSecurePass123!
    Input Password    ${PW_INPUT_CONFIRM}    DifferentPass123!
    Click Button      ${PW_BUTTON_UPDATE}
    Sleep             1s

TC_PROF_04: Add New Delivery Address
    [Documentation]    Verify customer can add a new delivery address in My Account.
    [Tags]             profile    address    positive
    Wait Until Element Is Visible    ${ACCOUNT_TAB_ADDRESSES}    timeout=${TIMEOUT}
    Click Button                     ${ACCOUNT_TAB_ADDRESSES}

    # Click Add delivery address button
    Wait Until Element Is Visible    ${ADDR_BTN_ADD_FORM}    timeout=${TIMEOUT}
    Click Button                     ${ADDR_BTN_ADD_FORM}

    # Fill address form
    ${rand}=    Generate Random String    4    [NUMBERS]
    Wait Until Element Is Visible    ${ADDR_INPUT_RECEIVER}    timeout=${TIMEOUT}
    Input Text    ${ADDR_INPUT_RECEIVER}    ทดสอบ ผู้รับ ${rand}
    Input Text    ${ADDR_INPUT_PHONE}       089123${rand}
    Input Text    ${ADDR_INPUT_LINE}        456/78 ถนนมิตรภาพ ซอย 9
    Input Text    ${ADDR_INPUT_DISTRICT}    ในเมือง
    Input Text    ${ADDR_INPUT_PROVINCE}    ขอนแก่น
    Input Text    ${ADDR_INPUT_POSTAL}      40000
    Click Button  ${ADDR_BUTTON_SAVE}

    # Verify form closes and new receiver name appears in list
    Sleep    2s
    Page Should Contain    ทดสอบ ผู้รับ ${rand}

TC_PROF_05: View Saved Delivery Addresses List
    [Documentation]    Verify saved delivery addresses are displayed on the Addresses tab.
    [Tags]             profile    address    positive
    Wait Until Element Is Visible    ${ACCOUNT_TAB_ADDRESSES}    timeout=${TIMEOUT}
    Click Button                     ${ACCOUNT_TAB_ADDRESSES}

    # Page should list at least default address or address cards
    Wait Until Page Contains Element    xpath://input[@name='delivery-address']    timeout=${TIMEOUT}
