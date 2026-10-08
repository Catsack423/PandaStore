*** Settings ***
Documentation       Seller Product Management Test Suite for PandaStore.
...                 Covers seller adding and publishing products:
...                 - Accessing /seller/products/add
...                 - Form validation and required fields
...                 - Image upload and preview
...                 - Category search and selection
...                 - Successfully creating and listing a new product
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

*** Variables ***
${PRODUCT_UNIQUE_NAME}    ${EMPTY}

*** Test Cases ***
TC_SELLER_PROD_01: Seller Navigates To Add Product Page
    [Documentation]    Verify seller can access /seller/products/add form.
    [Tags]             seller    product    navigation
    Go To              ${SELLER_ADD_PRODUCT_URL}
    Wait Until Location Contains        /seller/products/add        timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${PRODUCT_ADD_INPUT_NAME}   timeout=${TIMEOUT}
    Page Should Contain                 Product details

TC_SELLER_PROD_02: Verify Product Form Required Fields
    [Documentation]    Verify product form contains all required fields (Name, Price, Stock, Shipping, Images).
    [Tags]             seller    product    fields
    Go To    ${SELLER_ADD_PRODUCT_URL}
    Wait Until Page Contains Element    ${PRODUCT_ADD_INPUT_NAME}    timeout=${TIMEOUT}
    Wait Until Page Contains Element    xpath://button[contains(., 'Choose images')]    timeout=${TIMEOUT}
    Page Should Contain Element         ${PRODUCT_ADD_INPUT_PRICE}
    Page Should Contain Element         ${PRODUCT_ADD_INPUT_STOCK}
    Page Should Contain Element         ${PRODUCT_ADD_INPUT_SHIPPING}

TC_SELLER_PROD_03: Search And Select Product Category
    [Documentation]    Verify seller can filter and check a category in the product category list.
    [Tags]             seller    product    category
    Go To    ${SELLER_ADD_PRODUCT_URL}
    Wait Until Page Contains Element    ${PRODUCT_ADD_CHECKBOX_FIRST}    timeout=${TIMEOUT}

    # Search categories if search input is available
    ${has_cat_search}=    Run Keyword And Return Status    Page Should Contain Element    ${PRODUCT_ADD_INPUT_CAT_SEARCH}
    IF    ${has_cat_search}
        Input Text    ${PRODUCT_ADD_INPUT_CAT_SEARCH}    a
        Sleep         1s
    END

    # Select the first available category
    Scroll Element Into View    ${PRODUCT_ADD_CHECKBOX_FIRST}
    Select Checkbox             ${PRODUCT_ADD_CHECKBOX_FIRST}
    Checkbox Should Be Selected    ${PRODUCT_ADD_CHECKBOX_FIRST}

TC_SELLER_PROD_04: Publish New Product And Verify Creation
    [Documentation]    Verify seller can fill all details and publish product, redirecting to storefront.
    [Tags]             seller    product    publish
    ${rand}=    Generate Random String    5    [NUMBERS]
    ${prod_name}=    Set Variable    Panda Gadget Test Item ${rand}
    Set Suite Variable    ${PRODUCT_UNIQUE_NAME}    ${prod_name}

    Go To    ${SELLER_ADD_PRODUCT_URL}
    Wait Until Page Contains Element    ${PRODUCT_ADD_INPUT_NAME}    timeout=${TIMEOUT}
    Wait Until Element Is Enabled       xpath://button[contains(., 'Choose images')]    timeout=${TIMEOUT}

    # Fill details
    Input Text    ${PRODUCT_ADD_INPUT_NAME}        ${prod_name}
    Input Text    ${PRODUCT_ADD_INPUT_DESC}        ${NEW_PRODUCT_DESC}
    Input Text    ${PRODUCT_ADD_INPUT_PRICE}       ${NEW_PRODUCT_PRICE}
    Input Text    ${PRODUCT_ADD_INPUT_STOCK}       ${NEW_PRODUCT_STOCK}
    Input Text    ${PRODUCT_ADD_INPUT_SHIPPING}    ${NEW_PRODUCT_SHIPPING}

    # Select category
    Scroll Element Into View    ${PRODUCT_ADD_CHECKBOX_FIRST}
    Select Checkbox             ${PRODUCT_ADD_CHECKBOX_FIRST}

    # Upload image
    Make File Input Interactable    ${PRODUCT_ADD_FILE_INPUT}
    Choose File    ${PRODUCT_ADD_FILE_INPUT}    ${TEST_IMAGE_PATH}

    # Submit form
    Scroll Element Into View            ${PRODUCT_ADD_BTN_SUBMIT}
    Wait Until Element Is Enabled       ${PRODUCT_ADD_BTN_SUBMIT}    timeout=30s
    Wait Until Keyword Succeeds         3x    2s    Click Button    ${PRODUCT_ADD_BTN_SUBMIT}

    # Verify redirection to shop storefront
    Wait Until Location Contains    /shop/    timeout=25s
    Page Should Contain             ${prod_name}
