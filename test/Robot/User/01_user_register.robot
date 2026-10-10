*** Settings ***
Documentation       Test suite for User Registration (Customer Sign Up) in PandaStore.
Resource            ../resources/variables.resource
Resource            ../resources/locators.resource
Resource            ../resources/common_keywords.resource

Test Setup          Open PandaStore Browser    ${SIGNUP_URL}
Test Teardown       Teardown With Screenshot

*** Test Cases ***
TC_REG_01: Register Customer Successfully With Valid Details
    [Documentation]    Test customer registration with valid, unique information.
    [Tags]             register    smoke    positive
    ${u}    ${e}    ${p}    ${n}    ${pw}=    Generate Random Customer Credentials

    Input Text        ${SIGNUP_INPUT_USERNAME}      ${u}
    Input Text        ${SIGNUP_INPUT_FULLNAME}      ${n}
    Input Text        ${SIGNUP_INPUT_EMAIL}         ${e}
    Input Text        ${SIGNUP_INPUT_PHONE}         ${p}
    Input Password    ${SIGNUP_INPUT_PASSWORD}      ${pw}
    Input Password    ${SIGNUP_INPUT_CONFIRM_PW}    ${pw}
    Click Signup Submit Button

    # Verify registration succeeds and navigates away from /signup
    ${redirected}=    Run Keyword And Return Status
    ...    Wait Until Location Does Not Contain    /signup    timeout=10s
    IF    not ${redirected}
        Click Signup Submit Button
        Wait Until Location Does Not Contain    /signup    timeout=20s
    END

TC_REG_02: Register Fails When Required Fields Are Empty
    [Documentation]    Test form validation when submitting empty registration fields.
    [Tags]             register    negative
    Click Signup Submit Button
    # Form HTML5/React validation prevents submission or shows inline error
    Wait Until Page Contains Element    ${SIGNUP_INPUT_USERNAME}    timeout=5s
    Location Should Contain             /signup

TC_REG_03: Register Fails When Password Is Too Short
    [Documentation]    Test validation error when password length is less than 6 characters.
    [Tags]             register    negative
    ${u}    ${e}    ${p}    ${n}    ${pw}=    Generate Random Customer Credentials

    Input Text        ${SIGNUP_INPUT_USERNAME}      ${u}
    Input Text        ${SIGNUP_INPUT_FULLNAME}      ${n}
    Input Text        ${SIGNUP_INPUT_EMAIL}         ${e}
    Input Text        ${SIGNUP_INPUT_PHONE}         ${p}
    Input Password    ${SIGNUP_INPUT_PASSWORD}      12345
    Input Password    ${SIGNUP_INPUT_CONFIRM_PW}    12345
    Click Signup Submit Button

    Wait Until Page Contains    Password must contain at least 6 characters    timeout=5s
    Location Should Contain     /signup

TC_REG_04: Register Fails When Passwords Do Not Match
    [Documentation]    Test validation error when confirm password does not match password.
    [Tags]             register    negative
    ${u}    ${e}    ${p}    ${n}    ${pw}=    Generate Random Customer Credentials

    Input Text        ${SIGNUP_INPUT_USERNAME}      ${u}
    Input Text        ${SIGNUP_INPUT_FULLNAME}      ${n}
    Input Text        ${SIGNUP_INPUT_EMAIL}         ${e}
    Input Text        ${SIGNUP_INPUT_PHONE}         ${p}
    Input Password    ${SIGNUP_INPUT_PASSWORD}      ${pw}
    Input Password    ${SIGNUP_INPUT_CONFIRM_PW}    DifferentPassword999!
    Click Signup Submit Button

    Wait Until Page Contains    Passwords do not match    timeout=5s
    Location Should Contain     /signup

TC_REG_05: Register Fails When Email Format Is Invalid
    [Documentation]    Test validation error when email is not properly formatted.
    [Tags]             register    negative
    ${u}    ${e}    ${p}    ${n}    ${pw}=    Generate Random Customer Credentials

    Input Text        ${SIGNUP_INPUT_USERNAME}      ${u}
    Input Text        ${SIGNUP_INPUT_FULLNAME}      ${n}
    Input Text        ${SIGNUP_INPUT_EMAIL}         ${INVALID_EMAIL}
    Input Text        ${SIGNUP_INPUT_PHONE}         ${p}
    Input Password    ${SIGNUP_INPUT_PASSWORD}      ${pw}
    Input Password    ${SIGNUP_INPUT_CONFIRM_PW}    ${pw}
    Click Signup Submit Button

    # Browser or page validation catches invalid email
    Location Should Contain    /signup

TC_REG_06: Navigate From Signup Page To Signin Page
    [Documentation]    Verify customer can navigate to the sign in page via link.
    [Tags]             register    navigation
    Wait Until Element Is Visible    ${SIGNUP_LINK_SIGNIN}    timeout=${TIMEOUT}
    Click Element                    ${SIGNUP_LINK_SIGNIN}
    Wait Until Location Contains     /signin                  timeout=${TIMEOUT}
