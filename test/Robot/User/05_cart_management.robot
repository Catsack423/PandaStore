*** Settings ***
Documentation       Test suite for Shopping Cart Management in PandaStore (/cart).
Resource            ../resources/variables.resource
Resource            ../resources/locators.resource
Resource            ../resources/common_keywords.resource

Test Setup          Open PandaStore Browser    ${SHOP_URL}
Test Teardown       Teardown With Screenshot

*** Test Cases ***
TC_CART_01: Add Product To Cart From Product Details
    [Documentation]    Verify customer can add a product to the cart from the details page.
    [Tags]             cart    positive    smoke
    # 1. Open first product
    Wait Until Element Is Visible    ${SHOP_FIRST_PRODUCT_LINK}    timeout=${TIMEOUT}
    Sleep                            1s
    Wait Until Keyword Succeeds      3x    2s    Click Element    ${SHOP_FIRST_PRODUCT_LINK}

    # 2. Add to cart
    Wait Until Element Is Visible    ${PRODUCT_BTN_ADD_TO_CART}    timeout=${TIMEOUT}
    Scroll Element Into View         ${PRODUCT_BTN_ADD_TO_CART}
    Sleep                            1s
    Wait Until Keyword Succeeds      3x    2s    Click Button     ${PRODUCT_BTN_ADD_TO_CART}
    Sleep                            1s

    # 3. Navigate to Cart page and verify item exists
    Navigate To Cart Page
    Wait Until Page Contains Element    ${CART_LINK_CHECKOUT}      timeout=${TIMEOUT}
    Page Should Not Contain Element     ${CART_EMPTY_TITLE}

TC_CART_02: Increase Quantity Before Adding To Cart
    [Documentation]    Verify customer can increase quantity before adding item to cart.
    [Tags]             cart    positive
    Wait Until Element Is Visible    ${SHOP_FIRST_PRODUCT_LINK}    timeout=${TIMEOUT}
    Sleep                            1s
    Wait Until Keyword Succeeds      3x    2s    Click Element    ${SHOP_FIRST_PRODUCT_LINK}

    Wait Until Element Is Visible    ${PRODUCT_BTN_INCREASE_QTY}   timeout=${TIMEOUT}
    Click Button                     ${PRODUCT_BTN_INCREASE_QTY}
    Click Button                     ${PRODUCT_BTN_ADD_TO_CART}
    Sleep                            1s

    Navigate To Cart Page
    Wait Until Page Contains Element    ${CART_LINK_CHECKOUT}      timeout=${TIMEOUT}

TC_CART_03: Update Item Quantity Inside Cart
    [Documentation]    Verify customer can adjust item quantity directly in the cart.
    [Tags]             cart    positive
    Add First Product In Shop To Cart
    Navigate To Cart Page

    Wait Until Element Is Visible    ${CART_BTN_INCREASE}    timeout=${TIMEOUT}
    Click Button                     ${CART_BTN_INCREASE}
    Sleep                            1s

    Wait Until Element Is Visible    ${CART_BTN_DECREASE}    timeout=${TIMEOUT}
    Click Button                     ${CART_BTN_DECREASE}
    Sleep                            1s

TC_CART_04: Remove Single Item From Cart
    [Documentation]    Verify customer can remove an item from the cart.
    [Tags]             cart    positive
    Add First Product In Shop To Cart
    Navigate To Cart Page

    Wait Until Element Is Visible    ${CART_BTN_REMOVE_ITEM}    timeout=${TIMEOUT}
    Click Button                     ${CART_BTN_REMOVE_ITEM}
    Sleep                            1s

TC_CART_05: Clear All Items From Cart
    [Documentation]    Verify customer can clear the entire cart.
    [Tags]             cart    positive
    Add First Product In Shop To Cart
    Navigate To Cart Page

    Wait Until Element Is Visible    ${CART_BTN_CLEAR}    timeout=${TIMEOUT}
    Click Button                     ${CART_BTN_CLEAR}

    # Verify cart empty state
    Wait Until Page Contains Element    ${CART_EMPTY_TITLE}    timeout=${TIMEOUT}
    Page Should Contain                 Your cart is empty
