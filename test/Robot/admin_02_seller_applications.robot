*** Settings ***
Documentation       Admin Seller Applications Review Test Suite for PandaStore.
...                 Covers admin reviewing seller onboarding requests:
...                 - Viewing all seller applications on /admin
...                 - Filtering applications by status (All, Under review, Action needed, Approved, Rejected)
...                 - Refreshing applications list
...                 - Opening application details dialog
...                 - Verifying applicant KYC & bank details
...                 - Approving or Rejecting applications
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

*** Test Cases ***
TC_ADMIN_APP_01: Admin Views Seller Applications Dashboard
    [Documentation]    Verify admin can view the list of seller applications on /admin.
    [Tags]             admin    applications    view
    Go To             ${ADMIN_URL}
    Wait Until Location Contains        /admin              timeout=${TIMEOUT}
    Wait Until Page Contains Element    ${ADMIN_HEADING}    timeout=${TIMEOUT}
    Page Should Contain                 All applications

TC_ADMIN_APP_02: Admin Filters Applications By Status
    [Documentation]    Verify admin can filter applications using the status dropdown.
    [Tags]             admin    applications    filter
    Go To    ${ADMIN_URL}
    Wait Until Page Contains Element    ${ADMIN_FILTER_STATUS}    timeout=${TIMEOUT}

    # Filter by PENDING (Under review)
    Select From List By Value    ${ADMIN_FILTER_STATUS}    PENDING
    Sleep                        1s
    List Selection Should Be     ${ADMIN_FILTER_STATUS}    PENDING

    # Filter by APPROVED
    Select From List By Value    ${ADMIN_FILTER_STATUS}    APPROVED
    Sleep                        1s
    List Selection Should Be     ${ADMIN_FILTER_STATUS}    APPROVED

    # Reset to ALL
    Select From List By Value    ${ADMIN_FILTER_STATUS}    ALL
    Sleep                        1s
    List Selection Should Be     ${ADMIN_FILTER_STATUS}    ALL

TC_ADMIN_APP_03: Admin Refreshes Applications List
    [Documentation]    Verify admin can trigger list refresh using the Refresh button.
    [Tags]             admin    applications    refresh
    Go To    ${ADMIN_URL}
    Wait Until Page Contains Element    ${ADMIN_BTN_REFRESH}    timeout=${TIMEOUT}
    Wait Until Element Is Enabled       ${ADMIN_BTN_REFRESH}    timeout=${TIMEOUT}
    Scroll Element Into View            ${ADMIN_BTN_REFRESH}
    Wait Until Keyword Succeeds         3x    2s    Click Button    ${ADMIN_BTN_REFRESH}
    Sleep                               1s
    Page Should Contain Element         ${ADMIN_HEADING}

TC_ADMIN_APP_04: Admin Opens Application Review Dialog
    [Documentation]    Verify admin can click Review/View application to open modal details dialog.
    [Tags]             admin    applications    modal
    Go To    ${ADMIN_URL}
    Wait Until Page Contains Element    ${ADMIN_HEADING}    timeout=${TIMEOUT}
    Wait Until Element Is Enabled       ${ADMIN_FILTER_STATUS}    timeout=${TIMEOUT}

    ${has_apps}=    Run Keyword And Return Status
    ...    Wait Until Page Contains Element    ${ADMIN_BTN_REVIEW}    timeout=10s
    IF    ${has_apps}
        Wait Until Element Is Enabled       ${ADMIN_BTN_REVIEW}    timeout=10s
        Scroll Element Into View            ${ADMIN_BTN_REVIEW}
        Wait Until Keyword Succeeds         3x    2s    Click Button    ${ADMIN_BTN_REVIEW}
        Wait Until Page Contains Element    ${ADMIN_DIALOG_TITLE}    timeout=${TIMEOUT}
        Page Should Contain                 Shop details
        Page Should Contain                 Seller identity
        Page Should Contain                 Payment and documents

        # Close dialog if open
        ${has_close}=    Run Keyword And Return Status    Page Should Contain Element    xpath://button[@aria-label='Close' or contains(., 'Cancel')]
        IF    ${has_close}
            Click Element    xpath://button[@aria-label='Close' or contains(., 'Cancel')]
        END
    ELSE
        Log    No seller applications currently found in the list.
    END

TC_ADMIN_APP_05: Admin Approves A Pending Seller Application
    [Documentation]    Verify admin can approve a pending application from review modal.
    [Tags]             admin    applications    approve
    Go To    ${ADMIN_URL}
    Wait Until Page Contains Element    ${ADMIN_FILTER_STATUS}    timeout=${TIMEOUT}
    Select From List By Value           ${ADMIN_FILTER_STATUS}    PENDING
    Sleep                               1s

    ${has_pending}=    Run Keyword And Return Status
    ...    Wait Until Page Contains Element    ${ADMIN_BTN_REVIEW}    timeout=5s
    IF    ${has_pending}
        Click Button    ${ADMIN_BTN_REVIEW}
        Wait Until Page Contains Element    ${ADMIN_DECISION_APPROVE}    timeout=${TIMEOUT}

        # Select Approve decision
        Click Button    ${ADMIN_DECISION_APPROVE}
        Wait Until Element Is Visible    ${ADMIN_BTN_CONFIRM_DECISION}   timeout=${TIMEOUT}
        Click Button    ${ADMIN_BTN_CONFIRM_DECISION}
        Sleep           2s
    ELSE
        Log    No pending applications available to approve.
    END
