*** Settings ***
Documentation       Test suite for User Authentication (Sign In & Sign Out) in PandaStore.
Resource            ../resources/variables.resource
Resource            ../resources/locators.resource
Resource            ../resources/common_keywords.resource

Test Setup          Open PandaStore Browser    ${SIGNIN_URL}
Test Teardown       Teardown With Screenshot

*** Test Cases ***
TC_LOGIN_01: Login Successfully Using Username
    [Documentation]    Test customer login with valid username and password.
    [Tags]             login    smoke    positive
    Input Text        ${SIGNIN_INPUT_USERNAME}    ${VALID_USERNAME}
    Input Password    ${SIGNIN_INPUT_PASSWORD}    ${VALID_PASSWORD}
    Scroll Element Into View    ${SIGNIN_BUTTON_SUBMIT}
    Click Button      ${SIGNIN_BUTTON_SUBMIT}

    Wait Until Location Does Not Contain    /signin    timeout=${TIMEOUT}
    # Verify account identity is accessible in header
    Wait Until Element Is Visible    ${HEADER_ACCOUNT_LINK}    timeout=${TIMEOUT}

TC_LOGIN_02: Login Successfully Using Email Address
    [Documentation]    Test customer login with registered email address and password.
    [Tags]             login    smoke    positive
    Input Text        ${SIGNIN_INPUT_USERNAME}    ${VALID_EMAIL}
    Input Password    ${SIGNIN_INPUT_PASSWORD}    ${VALID_PASSWORD}
    Scroll Element Into View    ${SIGNIN_BUTTON_SUBMIT}
    Click Button      ${SIGNIN_BUTTON_SUBMIT}

    Wait Until Location Does Not Contain    /signin    timeout=${TIMEOUT}
    Wait Until Element Is Visible    ${HEADER_ACCOUNT_LINK}    timeout=${TIMEOUT}

TC_LOGIN_03: Login Fails With Incorrect Password
    [Documentation]    Test login error message when entering an invalid password.
    [Tags]             login    negative
    Input Text        ${SIGNIN_INPUT_USERNAME}    ${VALID_USERNAME}
    Input Password    ${SIGNIN_INPUT_PASSWORD}    ${INVALID_PASSWORD}
    Scroll Element Into View    ${SIGNIN_BUTTON_SUBMIT}
    Click Button      ${SIGNIN_BUTTON_SUBMIT}

    Wait Until Element Is Visible    ${SIGNIN_ALERT_ERROR}    timeout=5s
    Element Should Contain           ${SIGNIN_ALERT_ERROR}    Invalid
    Location Should Contain          /signin

TC_LOGIN_04: Login Fails With Nonexistent Account
    [Documentation]    Test login error message when entering a non-existent account.
    [Tags]             login    negative
    Input Text        ${SIGNIN_INPUT_USERNAME}    ${INVALID_USERNAME}
    Input Password    ${SIGNIN_INPUT_PASSWORD}    ${INVALID_PASSWORD}
    Scroll Element Into View    ${SIGNIN_BUTTON_SUBMIT}
    Click Button      ${SIGNIN_BUTTON_SUBMIT}

    Wait Until Element Is Visible    ${SIGNIN_ALERT_ERROR}    timeout=5s
    Location Should Contain          /signin

TC_LOGIN_05: Customer Can Sign Out Successfully
    [Documentation]    Test customer signing out from /my-account.
    [Tags]             login    logout    positive
    # 1. Login first
    Input Text        ${SIGNIN_INPUT_USERNAME}    ${VALID_USERNAME}
    Input Password    ${SIGNIN_INPUT_PASSWORD}    ${VALID_PASSWORD}
    Scroll Element Into View    ${SIGNIN_BUTTON_SUBMIT}
    Click Button      ${SIGNIN_BUTTON_SUBMIT}
    Wait Until Location Does Not Contain    /signin    timeout=${TIMEOUT}

    # 2. Go to My Account
    Go To    ${ACCOUNT_URL}
    Wait Until Element Is Visible    ${ACCOUNT_BUTTON_SIGNOUT}    timeout=${TIMEOUT}
    Click Button    ${ACCOUNT_BUTTON_SIGNOUT}

    # 3. Verify user signed out
    Sleep    1s
    Go To    ${ACCOUNT_URL}
    Page Should Contain    Sign in to your account

TC_LOGIN_06: Navigate From Signin Page To Signup Page
    [Documentation]    Verify customer can navigate to sign up page from sign in form.
    [Tags]             login    navigation
    Wait Until Element Is Visible    ${SIGNIN_LINK_CREATE_ACCOUNT}    timeout=${TIMEOUT}
    Click Element                    ${SIGNIN_LINK_CREATE_ACCOUNT}
    Wait Until Location Contains     /signup                          timeout=${TIMEOUT}
