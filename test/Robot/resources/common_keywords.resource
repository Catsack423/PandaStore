*** Settings ***
Documentation       Common reusable keywords and action helpers for PandaStore tests.
Library             SeleniumLibrary    screenshot_root_directory=${CURDIR}/../../img/robot
Library             String
Library             DateTime
Resource            variables.resource
Resource            locators.resource

*** Keywords ***
# ==============================================================================
# BROWSER LIFECYCLE KEYWORDS
# ==============================================================================
Open PandaStore Browser
    [Arguments]    ${url}=${HOME_URL}
    ${options}=    Evaluate    sys.modules['selenium.webdriver'].ChromeOptions()    sys
    IF    '${HEADLESS}' == 'True'
        ${_}=    Evaluate    $options.add_argument('--headless=new')
    END
    ${_}=    Evaluate    $options.add_argument('--no-sandbox')
    ${_}=    Evaluate    $options.add_argument('--disable-dev-shm-usage')
    ${_}=    Evaluate    $options.add_argument('--disable-gpu')
    ${_}=    Evaluate    $options.add_argument('--window-size=1440,900')
    Open Browser    ${url}    ${BROWSER}    options=${options}
    Maximize Browser Window
    Set Selenium Speed    ${DELAY}
    Set Selenium Timeout    ${TIMEOUT}
    Set Selenium Implicit Wait    ${IMPLICIT_WAIT}

Close PandaStore Browser
    Close All Browsers

Teardown With Screenshot
    [Documentation]    Capture screenshot to img/robot before closing browser.
    ${clean_name}=    Replace String Using Regexp    ${TEST NAME}    [^a-zA-Z0-9_-]    _
    Run Keyword And Ignore Error    Capture Page Screenshot    filename=${SCREENSHOT_DIR}/${clean_name}_{index}.png
    Close PandaStore Browser

Capture Screenshot On Test Complete
    [Documentation]    Capture screenshot to img/robot without closing browser (for sequential suites).
    ${clean_name}=    Replace String Using Regexp    ${TEST NAME}    [^a-zA-Z0-9_-]    _
    Run Keyword And Ignore Error    Capture Page Screenshot    filename=${SCREENSHOT_DIR}/${clean_name}_{index}.png

Make File Input Interactable
    [Arguments]    ${locator}
    ${elem}=    Get WebElement    ${locator}
    Execute JavaScript    arguments[0].style.display = 'block'; arguments[0].style.visibility = 'visible'; arguments[0].style.opacity = '1'; arguments[0].style.width = '100px'; arguments[0].style.height = '30px'; arguments[0].classList.remove('sr-only');    ARGUMENTS    ${elem}


# ==============================================================================
# AUTHENTICATION KEYWORDS
# ==============================================================================
Go To Sign In Page
    Go To    ${SIGNIN_URL}
    Wait Until Element Is Visible    ${SIGNIN_INPUT_USERNAME}    timeout=${TIMEOUT}

Go To Sign Up Page
    Go To    ${SIGNUP_URL}
    Wait Until Element Is Visible    ${SIGNUP_INPUT_USERNAME}    timeout=${TIMEOUT}

Click Signup Submit Button
    Wait Until Element Is Visible    ${SIGNUP_BUTTON_SUBMIT}    timeout=${TIMEOUT}
    Scroll Element Into View         ${SIGNUP_BUTTON_SUBMIT}
    ${sub_btn}=    Get WebElement    ${SIGNUP_BUTTON_SUBMIT}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${sub_btn}

Login As Customer
    [Arguments]    ${username}=${VALID_USERNAME}    ${password}=${VALID_PASSWORD}
    Go To Sign In Page
    Input Text    ${SIGNIN_INPUT_USERNAME}    ${username}
    Input Password    ${SIGNIN_INPUT_PASSWORD}    ${password}
    Wait Until Element Is Visible    ${SIGNIN_BUTTON_SUBMIT}    timeout=${TIMEOUT}
    ${btn}=    Get WebElement        ${SIGNIN_BUTTON_SUBMIT}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${btn}
    # Wait until login completes and redirects away from signin
    Wait Until Location Does Not Contain    /signin    timeout=${TIMEOUT}

Login If Not Already Logged In
    [Arguments]    ${username}=${VALID_USERNAME}    ${password}=${VALID_PASSWORD}
    Go To    ${ACCOUNT_URL}
    ${is_signin_required}=    Run Keyword And Return Status    Element Should Be Visible    ${SIGNIN_INPUT_USERNAME}
    IF    ${is_signin_required}
        Login As Customer    ${username}    ${password}
    END

Logout Customer
    Delete All Cookies
    Run Keyword And Ignore Error    Execute JavaScript    window.localStorage.clear(); window.sessionStorage.clear();
    Go To    ${HOME_URL}
    Sleep    1s

# ==============================================================================
# TEST DATA GENERATION
# ==============================================================================
Generate Random Customer Credentials
    ${rand_str}=         Generate Random String    6    [LOWER]
    ${rand_digits}=      Generate Random String    8    [NUMBERS]
    ${test_username}=    Set Variable    user_${rand_str}
    ${test_email}=       Set Variable    ${test_username}@testpanda.com
    ${test_phone}=       Set Variable    08${rand_digits}
    ${test_fullname}=    Set Variable    Test User ${rand_str}
    ${test_password}=    Set Variable    PandaPass123!
    RETURN    ${test_username}    ${test_email}    ${test_phone}    ${test_fullname}    ${test_password}

# ==============================================================================
# NAVIGATION & FLOW HELPERS
# ==============================================================================
Navigate To Shop Page
    Go To    ${SHOP_URL}
    Wait Until Page Contains Element    ${HEADER_SEARCH_INPUT}    timeout=${TIMEOUT}

Navigate To Cart Page
    Go To    ${CART_URL}
    Wait Until Page Contains Element    ${CART_HEADING}    timeout=${TIMEOUT}

Navigate To Checkout Page
    Go To    ${CHECKOUT_URL}
    Wait Until Location Contains    /checkout    timeout=${TIMEOUT}

Navigate To My Account Page
    Go To    ${ACCOUNT_URL}
    Wait Until Location Contains    /my-account    timeout=${TIMEOUT}

Navigate To Order History Page
    Go To    ${ORDER_HISTORY_URL}
    Wait Until Location Contains    /order-history    timeout=${TIMEOUT}

Add First Product In Shop To Cart
    Navigate To Shop Page
    Wait Until Page Contains Element    ${SHOP_FIRST_PRODUCT_LINK}    timeout=${TIMEOUT}
    Scroll Element Into View            ${SHOP_FIRST_PRODUCT_LINK}
    ${item}=    Get WebElement          ${SHOP_FIRST_PRODUCT_LINK}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${item}
    Wait Until Element Is Visible       ${PRODUCT_BTN_ADD_TO_CART}    timeout=${TIMEOUT}
    Scroll Element Into View            ${PRODUCT_BTN_ADD_TO_CART}
    ${cart_btn}=    Get WebElement      ${PRODUCT_BTN_ADD_TO_CART}
    Execute JavaScript    arguments[0].click();    ARGUMENTS    ${cart_btn}
    Sleep    1s

# ==============================================================================
# ADMIN & SELLER WORKFLOW KEYWORDS
# ==============================================================================
Login As Admin
    [Arguments]    ${username}=${ADMIN_USERNAME}    ${password}=${ADMIN_PASSWORD}
    Logout Customer
    Login As Customer    ${username}    ${password}
    Go To    ${ADMIN_URL}
    Wait Until Location Contains    /admin    timeout=${TIMEOUT}

Login As Seller
    [Arguments]    ${username}=${SELLER_USERNAME}    ${password}=${SELLER_PASSWORD}
    Logout Customer
    Login As Customer    ${username}    ${password}
    Go To    ${SELLER_DASHBOARD_URL}
    Wait Until Location Contains    /seller    timeout=${TIMEOUT}

Submit Seller Application
    [Arguments]    ${shop_name}=${APPLY_SHOP_NAME}
    Go To    ${SELLER_APPLY_URL}
    Wait Until Page Contains Element    ${APPLY_INPUT_SHOP_NAME}    timeout=${TIMEOUT}

    # Step 1: Shop details
    Input Text    ${APPLY_INPUT_SHOP_NAME}     ${shop_name}
    Input Text    ${APPLY_INPUT_SHOP_DESC}     ${APPLY_SHOP_DESC}
    Input Text    ${APPLY_INPUT_SHOP_PHONE}    ${APPLY_SHOP_PHONE}
    Input Text    ${APPLY_INPUT_SHOP_EMAIL}    ${APPLY_SHOP_EMAIL}
    Input Text    ${APPLY_INPUT_SHOP_ADDR}     ${APPLY_SHOP_ADDRESS}
    Scroll Element Into View    ${APPLY_BTN_CONTINUE}
    Click Button    ${APPLY_BTN_CONTINUE}

    # Step 2: Identity & Bank details
    Wait Until Page Contains Element    ${APPLY_INPUT_FIRST_NAME}    timeout=${TIMEOUT}
    Input Text    ${APPLY_INPUT_FIRST_NAME}       ${APPLY_FIRST_NAME}
    Input Text    ${APPLY_INPUT_LAST_NAME}        ${APPLY_LAST_NAME}
    Input Text    ${APPLY_INPUT_ID_CARD}          ${APPLY_ID_CARD_NO}
    Make File Input Interactable                  ${APPLY_FILE_ID_CARD}
    Choose File   ${APPLY_FILE_ID_CARD}           ${TEST_IMAGE_PATH}

    Input Text    ${APPLY_INPUT_BANK_NAME}        ${APPLY_BANK_NAME}
    Input Text    ${APPLY_INPUT_BANK_ACC_NAME}    ${APPLY_BANK_ACC_NAME}
    Input Text    ${APPLY_INPUT_BANK_ACC_NO}      ${APPLY_BANK_ACC_NO}
    Make File Input Interactable                  ${APPLY_FILE_BANK_BOOK}
    Choose File   ${APPLY_FILE_BANK_BOOK}         ${TEST_IMAGE_PATH}

    Scroll Element Into View         ${APPLY_BTN_SUBMIT}
    Wait Until Element Is Enabled    ${APPLY_BTN_SUBMIT}    timeout=20s
    Click Button                     ${APPLY_BTN_SUBMIT}
    Wait Until Location Contains     /seller-application    timeout=${TIMEOUT}

Admin Approve Application
    Go To    ${ADMIN_URL}
    Wait Until Page Contains Element    ${ADMIN_HEADING}    timeout=${TIMEOUT}
    Wait Until Element Is Visible       ${ADMIN_BTN_REVIEW} timeout=${TIMEOUT}
    Click Button    ${ADMIN_BTN_REVIEW}

    Wait Until Element Is Visible       ${ADMIN_DECISION_APPROVE}     timeout=${TIMEOUT}
    Click Button    ${ADMIN_DECISION_APPROVE}
    Wait Until Element Is Visible       ${ADMIN_BTN_CONFIRM_DECISION} timeout=${TIMEOUT}
    Click Button    ${ADMIN_BTN_CONFIRM_DECISION}
    Sleep           2s

Seller Add New Product
    [Arguments]    ${name}=${NEW_PRODUCT_NAME}    ${price}=${NEW_PRODUCT_PRICE}    ${stock}=${NEW_PRODUCT_STOCK}
    Go To    ${SELLER_ADD_PRODUCT_URL}
    Wait Until Page Contains Element    ${PRODUCT_ADD_INPUT_NAME}    timeout=${TIMEOUT}

    # Fill details first
    Input Text    ${PRODUCT_ADD_INPUT_NAME}        ${name}
    Input Text    ${PRODUCT_ADD_INPUT_DESC}        ${NEW_PRODUCT_DESC}
    Input Text    ${PRODUCT_ADD_INPUT_PRICE}       ${price}
    Input Text    ${PRODUCT_ADD_INPUT_STOCK}       ${stock}
    Input Text    ${PRODUCT_ADD_INPUT_SHIPPING}    ${NEW_PRODUCT_SHIPPING}

    # Select Category
    Scroll Element Into View    ${PRODUCT_ADD_CHECKBOX_FIRST}
    Select Checkbox             ${PRODUCT_ADD_CHECKBOX_FIRST}

    # Upload image
    Make File Input Interactable    ${PRODUCT_ADD_FILE_INPUT}
    Choose File   ${PRODUCT_ADD_FILE_INPUT}        ${TEST_IMAGE_PATH}

    # Submit product
    Scroll Element Into View         ${PRODUCT_ADD_BTN_SUBMIT}
    Wait Until Element Is Enabled    ${PRODUCT_ADD_BTN_SUBMIT}    timeout=30s
    Wait Until Keyword Succeeds      3x    2s    Click Button    ${PRODUCT_ADD_BTN_SUBMIT}
    Sleep                            2s
