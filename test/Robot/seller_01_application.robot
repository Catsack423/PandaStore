*** Settings ***
Documentation       Seller Application Test Suite for PandaStore.
...                 Covers customer applying to become a seller:
...                 - Accessing /seller-application and application status
...                 - Step 1: Shop details (name, description, phone, email, address)
...                 - Step 2: KYC & Payout details (ID card, bank account, document uploads)
...                 - Verifying submitted application status (Under review)
Resource            resources/variables.resource
Resource            resources/locators.resource
Resource            resources/common_keywords.resource

Suite Setup         Open PandaStore Browser    ${HOME_URL}
Suite Teardown      Close PandaStore Browser
Test Teardown       Capture Screenshot On Test Complete

*** Variables ***
${APPLY_TEST_USER}     ${EMPTY}
${APPLY_TEST_PASS}     ${EMPTY}
${APPLY_TEST_EMAIL}    ${EMPTY}
${APPLY_SHOP_NAME_GEN}    ${EMPTY}

*** Test Cases ***
TC_SELLER_APP_01: Customer Navigates To Seller Application Page
    [Documentation]    Verify customer can navigate to /seller-application and click Apply to sell.
    [Tags]             seller    application    navigation
    ${u}    ${e}    ${p}    ${n}    ${pw}=    Generate Random Customer Credentials
    Set Suite Variable    ${APPLY_TEST_USER}        ${u}
    Set Suite Variable    ${APPLY_TEST_EMAIL}       ${e}
    Set Suite Variable    ${APPLY_TEST_PASS}        ${pw}
    Set Suite Variable    ${APPLY_SHOP_NAME_GEN}    Shop_${u}

    # Register new customer first so they have a fresh account without existing seller application
    Go To Sign Up Page
    Input Text        ${SIGNUP_INPUT_USERNAME}      ${u}
    Input Text        ${SIGNUP_INPUT_FULLNAME}      ${n}
    Input Text        ${SIGNUP_INPUT_EMAIL}         ${e}
    Input Text        ${SIGNUP_INPUT_PHONE}         ${p}
    Input Password    ${SIGNUP_INPUT_PASSWORD}      ${pw}
    Input Password    ${SIGNUP_INPUT_CONFIRM_PW}    ${pw}
    Scroll Element Into View    ${SIGNUP_BUTTON_SUBMIT}
    Click Button      ${SIGNUP_BUTTON_SUBMIT}
    Wait Until Location Does Not Contain    /signup    timeout=${TIMEOUT}
    Login If Not Already Logged In    ${u}    ${pw}

    # Navigate to Seller Application overview
    Go To    ${SELLER_STATUS_URL}
    Wait Until Location Contains    /seller-application    timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${APPLY_LINK_APPLY_NOW}    timeout=${TIMEOUT}
    Scroll Element Into View            ${APPLY_LINK_APPLY_NOW}
    Wait Until Keyword Succeeds         3x    2s    Click Element    ${APPLY_LINK_APPLY_NOW}
    Wait Until Location Contains        /seller-application/apply    timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${APPLY_INPUT_SHOP_NAME}     timeout=${TIMEOUT}

TC_SELLER_APP_02: Fill And Complete Step 1 Shop Details
    [Documentation]    Verify customer can fill Step 1 shop profile and advance to Step 2.
    [Tags]             seller    application    step1
    Go To    ${SELLER_APPLY_URL}
    Wait Until Page Contains Element    ${APPLY_INPUT_SHOP_NAME}    timeout=${TIMEOUT}

    Input Text    ${APPLY_INPUT_SHOP_NAME}     ${APPLY_SHOP_NAME_GEN}
    Input Text    ${APPLY_INPUT_SHOP_DESC}     ${APPLY_SHOP_DESC}
    Input Text    ${APPLY_INPUT_SHOP_PHONE}    ${APPLY_SHOP_PHONE}
    Input Text    ${APPLY_INPUT_SHOP_EMAIL}    ${APPLY_TEST_EMAIL}
    Input Text    ${APPLY_INPUT_SHOP_ADDR}     ${APPLY_SHOP_ADDRESS}

    Scroll Element Into View    ${APPLY_BTN_CONTINUE}
    Click Button    ${APPLY_BTN_CONTINUE}

    # Verify moved to Step 2 (Identity & Payment)
    Wait Until Page Contains Element    ${APPLY_INPUT_FIRST_NAME}    timeout=${TIMEOUT}
    Page Should Contain                 Verify the person behind the shop

TC_SELLER_APP_03: Complete Step 2 KYC, Payout Information And Document Uploads
    [Documentation]    Verify customer can complete Step 2 with citizen ID, bank info, and uploaded documents.
    [Tags]             seller    application    step2    upload
    Wait Until Page Contains Element    ${APPLY_INPUT_FIRST_NAME}    timeout=${TIMEOUT}

    # Legal Identity
    Input Text    ${APPLY_INPUT_FIRST_NAME}    ${APPLY_FIRST_NAME}
    Input Text    ${APPLY_INPUT_LAST_NAME}     ${APPLY_LAST_NAME}
    Input Text    ${APPLY_INPUT_ID_CARD}       ${APPLY_ID_CARD_NO}
    Make File Input Interactable               ${APPLY_FILE_ID_CARD}
    Choose File   ${APPLY_FILE_ID_CARD}        ${TEST_IMAGE_PATH}

    # Bank Account
    Input Text    ${APPLY_INPUT_BANK_NAME}        ${APPLY_BANK_NAME}
    Input Text    ${APPLY_INPUT_BANK_ACC_NAME}    ${APPLY_BANK_ACC_NAME}
    Input Text    ${APPLY_INPUT_BANK_ACC_NO}      ${APPLY_BANK_ACC_NO}
    Make File Input Interactable                  ${APPLY_FILE_BANK_BOOK}
    Choose File   ${APPLY_FILE_BANK_BOOK}         ${TEST_IMAGE_PATH}

    # Wait until documents uploaded and submit button is enabled
    Scroll Element Into View    ${APPLY_BTN_SUBMIT}
    Wait Until Element Is Enabled    ${APPLY_BTN_SUBMIT}    timeout=20s
    Click Button    ${APPLY_BTN_SUBMIT}

    # Should redirect back to /seller-application and show Under review
    Wait Until Location Contains    /seller-application    timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${APPLY_STATUS_BADGE}  timeout=${TIMEOUT}
    Page Should Contain                 Under review

TC_SELLER_APP_04: Verify Submitted Application Status On Dashboard
    [Documentation]    Verify applicant sees Under Review status badge and details on /seller-application.
    [Tags]             seller    application    status
    Go To    ${SELLER_STATUS_URL}
    Wait Until Location Contains        /seller-application    timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${APPLY_STATUS_BADGE}  timeout=${TIMEOUT}
    Page Should Contain                 Under review
    Page Should Contain                 ${APPLY_SHOP_NAME_GEN}
