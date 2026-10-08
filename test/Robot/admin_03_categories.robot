*** Settings ***
Documentation       Admin Categories Management Test Suite for PandaStore.
...                 Covers product categories CRUD by administrator:
...                 - Accessing /admin/categories
...                 - Viewing category list and count
...                 - Validating form rules (max 20 characters)
...                 - Creating a new product category
...                 - Deleting a category with confirmation dialog
Resource            resources/variables.resource
Resource            resources/locators.resource
Resource            resources/common_keywords.resource

Suite Setup         Open PandaStore Browser And Login As Admin
Suite Teardown      Close PandaStore Browser
Test Teardown       Capture Screenshot On Test Complete

*** Keywords ***
Open PandaStore Browser And Login As Admin
    Open PandaStore Browser    ${HOME_URL}
    Login As Admin            ${ADMIN_USERNAME}    ${ADMIN_PASSWORD}

*** Variables ***
${NEW_CATEGORY_NAME}    ${EMPTY}

*** Test Cases ***
TC_ADMIN_CAT_01: Admin Navigates To Category Management Page
    [Documentation]    Verify admin can navigate to /admin/categories.
    [Tags]             admin    categories    navigation
    Go To             ${BASE_URL}/admin/categories
    Wait Until Location Contains        /admin/categories      timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${ADMIN_CAT_INPUT_NAME}    timeout=${TIMEOUT}
    Page Should Contain                 Product categories

TC_ADMIN_CAT_02: Admin Reviews Existing Category List And Count
    [Documentation]    Verify admin sees total category count and list of existing categories.
    [Tags]             admin    categories    list
    Go To    ${BASE_URL}/admin/categories
    Wait Until Page Contains Element    ${ADMIN_CAT_INPUT_NAME}    timeout=${TIMEOUT}
    Page Should Contain                 total
    Page Should Contain                 Add a category

TC_ADMIN_CAT_03: Form Enforces Category Name Character Limit
    [Documentation]    Verify category name field restricts input to 20 characters maximum.
    [Tags]             admin    categories    validation
    Go To    ${BASE_URL}/admin/categories
    Wait Until Page Contains Element    ${ADMIN_CAT_INPUT_NAME}    timeout=${TIMEOUT}
    Element Attribute Value Should Be   ${ADMIN_CAT_INPUT_NAME}    maxLength    20

TC_ADMIN_CAT_04: Admin Creates A New Product Category
    [Documentation]    Verify admin can fill category name, description, and save successfully.
    [Tags]             admin    categories    create
    ${rand}=    Generate Random String    4    [LETTERS]
    ${cat_name}=    Set Variable    Cat_${rand}
    Set Suite Variable    ${NEW_CATEGORY_NAME}    ${cat_name}

    Go To    ${BASE_URL}/admin/categories
    Wait Until Page Contains Element    ${ADMIN_CAT_INPUT_NAME}    timeout=${TIMEOUT}

    Input Text    ${ADMIN_CAT_INPUT_NAME}    ${cat_name}
    Input Text    ${ADMIN_CAT_INPUT_DESC}    Automated test category description for ${cat_name}
    Scroll Element Into View    ${ADMIN_CAT_BTN_ADD}
    Click Button                ${ADMIN_CAT_BTN_ADD}
    Sleep                       2s

    # Verify new category appears in list
    Page Should Contain    ${cat_name}

TC_ADMIN_CAT_05: Admin Deletes A Category With Confirmation Dialog
    [Documentation]    Verify admin can click Delete, confirm in the ConfirmDialog, and remove category.
    [Tags]             admin    categories    delete
    Go To    ${BASE_URL}/admin/categories
    Wait Until Page Contains Element    ${ADMIN_CAT_FIRST_DELETE}    timeout=${TIMEOUT}
    Scroll Element Into View            ${ADMIN_CAT_FIRST_DELETE}
    Wait Until Keyword Succeeds         3x    2s    Click Button    ${ADMIN_CAT_FIRST_DELETE}

    # Verify confirmation dialog opens
    Wait Until Element Is Visible       ${ADMIN_CAT_CONFIRM_DELETE}    timeout=${TIMEOUT}
    Page Should Contain                 Are you sure you want to delete this category?
    Wait Until Keyword Succeeds         3x    2s    Click Button    ${ADMIN_CAT_CONFIRM_DELETE}
    Sleep                               2s
