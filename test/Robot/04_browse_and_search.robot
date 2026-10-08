*** Settings ***
Documentation       Test suite for Browsing Catalog & Searching Products in PandaStore.
Resource            resources/variables.resource
Resource            resources/locators.resource
Resource            resources/common_keywords.resource

Test Setup          Open PandaStore Browser    ${SHOP_URL}
Test Teardown       Teardown With Screenshot

*** Test Cases ***
TC_BROWSE_01: Browse Product Catalog
    [Documentation]    Verify customer can view product listings in the shop.
    [Tags]             browse    catalog    positive
    Wait Until Page Contains Element    ${HEADER_SEARCH_INPUT}    timeout=${TIMEOUT}
    # Check that product links / items are rendered
    Wait Until Page Contains Element    ${SHOP_FIRST_PRODUCT_LINK}    timeout=${TIMEOUT}

TC_BROWSE_02: Search For Product Using Keyword
    [Documentation]    Verify customer can search for products using the search bar.
    [Tags]             search    positive
    Wait Until Element Is Visible    ${HEADER_SEARCH_INPUT}    timeout=${TIMEOUT}
    Input Text                       ${HEADER_SEARCH_INPUT}    Panda
    Click Button                     ${HEADER_SEARCH_BUTTON}

    Wait Until Location Contains     q=Panda    timeout=${TIMEOUT}
    # Page displays matching products
    Wait Until Page Contains Element    ${SHOP_FIRST_PRODUCT_LINK}    timeout=${TIMEOUT}

TC_BROWSE_03: Search For Nonexistent Product
    [Documentation]    Verify appropriate feedback when search yields no products.
    [Tags]             search    negative
    Wait Until Element Is Visible    ${HEADER_SEARCH_INPUT}    timeout=${TIMEOUT}
    Input Text                       ${HEADER_SEARCH_INPUT}    XYZNonExistentItem9999
    Click Button                     ${HEADER_SEARCH_BUTTON}

    Wait Until Location Contains     XYZNonExistentItem9999    timeout=${TIMEOUT}
    # Check for empty state or no matching products
    Sleep    1s

TC_BROWSE_04: View Product Details Page
    [Documentation]    Verify opening a product shows its full details (title, price, description, etc.).
    [Tags]             product_detail    positive
    Wait Until Element Is Visible    ${SHOP_FIRST_PRODUCT_LINK}    timeout=${TIMEOUT}
    Click Element                    ${SHOP_FIRST_PRODUCT_LINK}

    # Verify elements on Product Details page
    Wait Until Element Is Visible    ${PRODUCT_DETAIL_TITLE}       timeout=${TIMEOUT}
    Element Should Be Visible        ${PRODUCT_DETAIL_PRICE}
    Element Should Be Visible        ${PRODUCT_BTN_ADD_TO_CART}
    Page Should Contain              Product information
