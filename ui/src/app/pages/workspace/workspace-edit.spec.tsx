import * as React from 'react';
import * as fp from 'lodash/fp';
import { mockNavigate } from 'setupTests';

import {
  DisseminateResearchEnum,
  ProfileApi,
  ResearchOutcomeEnum,
  SpecificPopulationEnum,
  UserApi,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import {
  getDefaultNormalizer,
  screen,
  waitFor,
  within,
} from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import {
  WorkspaceEdit,
  WorkspaceEditMode,
} from 'app/pages/workspace/workspace-edit';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import * as Authentication from 'app/utils/authentication';
import { currentWorkspaceStore } from 'app/utils/navigation';
import {
  cdrVersionStore,
  profileStore,
  serverConfigStore,
} from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderWithRouter,
} from 'testing/react-test-helpers';
import {
  altCdrVersion,
  CdrVersionsStubVariables,
  cdrVersionTiersResponse,
  controlledCdrVersion,
  defaultCdrVersion,
} from 'testing/stubs/cdr-versions-api-stub';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';
import { UserApiStub } from 'testing/stubs/user-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

jest.mock('app/utils/project-billing-info', () => ({
  getBillingAccountInfo: () =>
    new Promise((resolve) =>
      resolve({ billingAccountName: 'billing-account' })
    ),
}));

let mockHasBillingScope: jest.SpyInstance;
let mockEnsureBillingScope: jest.SpyInstance;

const WORKSPACE_NAME_TEXT = 'This is a text with space';
const FREE_TIER_OPTION_REGEX =
  /use all of us initial credits \- \$33\.33 left/i;
const USER_BILLING_OPTION_REGEX = /user billing/i;
const USER_PROVIDED_BILLING_OPTION_REGEX = /user provided billing account/i;
const OTHER_DISSEMINATION_REGEX = new RegExp(
  'specify the name of the forum \\(journal, scientific conference, blog etc\\.\\) ' +
    'through which you will disseminate your findings, if available\\.',
  'i'
);

describe('WorkspaceEdit', () => {
  let workspacesApi: WorkspacesApiStub;
  let userApi: UserApiStub;
  let workspace: WorkspaceData;
  let workspaceEditMode: WorkspaceEditMode;
  let user: UserEvent;

  const renderComponent = () => {
    return renderWithRouter(
      <WorkspaceEdit
        cancel={() => {}}
        hideSpinner={() => {}}
        showSpinner={() => {}}
        workspaceEditMode={workspaceEditMode}
      />
    );
  };

  async function expectNoTierChangesAllowed() {
    renderComponent();

    let accessTierSelection: HTMLSelectElement;
    await waitFor(() => {
      accessTierSelection = screen.getByRole('combobox', {
        name: /data access tier dropdown/i,
      }) as HTMLSelectElement;
      expect(accessTierSelection.disabled).toBe(true);
      expect(accessTierSelection.value).toBe(workspace.accessTierShortName);
    });
  }

  beforeEach(async () => {
    workspace = {
      // accessLevel is a required WorkspaceData property (WorkspaceData extends
      // from Workspace)
      accessLevel: WorkspaceAccessLevel.OWNER,
      // Apply all properties from the default workspace stub.
      ...workspaceStubs[0],
      // Override some of the researchPurpose properties, to fill required
      // fields in the workspace form.
      researchPurpose: {
        ...workspaceStubs[0].researchPurpose,
        intendedStudy: 'intendedStudy ',
        anticipatedFindings: 'anticipatedFindings ',
        scientificApproach: 'scientificApproach ',
        drugDevelopment: true,
        disseminateResearchFindingList: [
          DisseminateResearchEnum.PUBLICATION_PERSONAL_BLOG,
        ],
        researchOutcomeList: [ResearchOutcomeEnum.DECREASE_ILLNESS_BURDEN],
      },
    };

    workspaceEditMode = WorkspaceEditMode.Create;

    userApi = new UserApiStub();
    registerApiClient(UserApi, userApi);

    workspacesApi = new WorkspacesApiStub([workspace]);
    registerApiClient(WorkspacesApi, workspacesApi);

    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn(),
    });
    currentWorkspaceStore.next(workspace);
    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        freeTierBillingAccountId: 'freetier',
        defaultFreeCreditsDollarLimit: 100.0,
        gsuiteDomain: '',
      },
    });

    mockHasBillingScope = jest.spyOn(Authentication, 'hasBillingScope');
    mockEnsureBillingScope = jest.spyOn(Authentication, 'ensureBillingScope');
    mockHasBillingScope.mockImplementation(() => false);
    mockEnsureBillingScope.mockImplementation(() => {});
    // Since we are testing delays in these tests, it is helpful to set no dleay in user actions
    // themselves, so we can focus on delays in our apis:
    // https://github.com/testing-library/user-event/issues/833
    user = userEvent.setup();
  });

  it('displays workspaces create page', async () => {
    currentWorkspaceStore.next(undefined);
    renderComponent();
    await screen.findByText('Create a new workspace');

    const researchPurposeCheckbox: HTMLInputElement = screen.getByTestId(
      'researchPurpose-checkbox'
    );
    expect(researchPurposeCheckbox.checked).toEqual(false);

    const specificPopulationRadio: HTMLInputElement = screen.getByTestId(
      'specific-population-no'
    );
    expect(specificPopulationRadio.checked).toEqual(false);
  });

  it('displays workspaces duplicate page', async () => {
    workspaceEditMode = WorkspaceEditMode.Duplicate;
    renderComponent();
    await screen.findByText(`Duplicate workspace "${workspace.name}"`);

    // Ensure the 'drug development' checkbox is checked when duplicating.
    const drugDevelopmentCheckbox: HTMLInputElement = screen.getByTestId(
      'drugDevelopment-checkbox'
    ) as HTMLInputElement;
    expect(drugDevelopmentCheckbox.checked).toEqual(true);
  });

  it('displays workspaces edit page', async () => {
    workspaceEditMode = WorkspaceEditMode.Edit;
    renderComponent();
    await screen.findByText(/edit workspace/i);

    // Ensure the 'drug development' checkbox is checked when editing.
    const drugDevelopmentCheckbox: HTMLInputElement = screen.getByTestId(
      'drugDevelopment-checkbox'
    ) as HTMLInputElement;
    expect(drugDevelopmentCheckbox.checked).toEqual(true);
  });

  it('pre-fills "specific populations" form elements when editing', async () => {
    // Set the workspace state to represent a workspace which is studying a
    // specific population group.
    workspace.researchPurpose.populationDetails = [
      SpecificPopulationEnum.AGE_CHILDREN,
    ];

    workspaceEditMode = WorkspaceEditMode.Edit;
    renderComponent();

    // Ensure the radiobox and checkbox are pre-filled for the "specific
    // populations" section.
    let specificPopulationYesRadio: HTMLInputElement;
    await waitFor(() => {
      specificPopulationYesRadio = screen.queryByTestId(
        'specific-population-yes'
      );
      expect(specificPopulationYesRadio).toBeInTheDocument();
    });

    expect(specificPopulationYesRadio.checked).toEqual(true);

    const ageChildrenCheckbox: HTMLInputElement = screen.getByTestId(
      `${SpecificPopulationEnum.AGE_CHILDREN}-checkbox`
    ) as HTMLInputElement;
    expect(ageChildrenCheckbox.checked).toEqual(true);
  });

  it('should initialize cdr versions dropdown options to the default tier when creating workspaces', async () => {
    cdrVersionStore.set(cdrVersionTiersResponse);
    workspace.accessTierShortName = AccessTierShortNames.Controlled;

    workspaceEditMode = WorkspaceEditMode.Create;

    renderComponent();

    let cdrVersionSelect: HTMLElement;
    await waitFor(() => {
      cdrVersionSelect = screen.getByTestId(
        'select-cdr-version'
      ) as HTMLElement;
    });

    const cdrSelectionOptions: string[] = (
      within(cdrVersionSelect).getAllByRole('option') as HTMLOptionElement[]
    ).map((option) => option.value);

    expect(cdrSelectionOptions).toEqual([
      CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
      CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID,
    ]);
  });

  it('should initialize the specific populations checkbox as unchecked when creating workspaces', async () => {
    workspace.researchPurpose.populationDetails = [
      SpecificPopulationEnum.AGE_CHILDREN,
    ];
    workspaceEditMode = WorkspaceEditMode.Create;
    renderComponent();
    let specificPopulationYesCheckbox: HTMLInputElement;
    await waitFor(() => {
      specificPopulationYesCheckbox = screen.getByTestId(
        'specific-population-yes'
      ) as HTMLInputElement;
    });
    expect(specificPopulationYesCheckbox.checked).toEqual(false);
  });

  it('should clear all selected specific populations if NO underrepresented populations study is selected', async () => {
    // Set the workspace state to represent a workspace which is studying a
    // specific population group.
    workspace.researchPurpose.populationDetails = [
      SpecificPopulationEnum.AGE_CHILDREN,
      SpecificPopulationEnum.RACE_MENA,
      SpecificPopulationEnum.DISABILITY_STATUS,
    ];

    workspaceEditMode = WorkspaceEditMode.Edit;
    renderComponent();
    let specificPopulationYesCheckbox: HTMLInputElement;
    await waitFor(() => {
      specificPopulationYesCheckbox = screen.getByTestId(
        'specific-population-yes'
      ) as HTMLInputElement;
    });
    expect(specificPopulationYesCheckbox.checked).toEqual(true);

    expect(
      (
        screen.getByTestId(
          `${SpecificPopulationEnum.AGE_CHILDREN}-checkbox`
        ) as HTMLInputElement
      ).checked
    ).toEqual(true);

    expect(
      (
        screen.getByTestId(
          `${SpecificPopulationEnum.RACE_MENA}-checkbox`
        ) as HTMLInputElement
      ).checked
    ).toEqual(true);

    expect(
      (
        screen.getByTestId(
          `${SpecificPopulationEnum.DISABILITY_STATUS}-checkbox`
        ) as HTMLInputElement
      ).checked
    ).toEqual(true);

    await user.click(screen.getByTestId('specific-population-no'));

    const saveButton = screen.getByRole('button', { name: 'Update Workspace' });

    await waitFor(() => {
      expectButtonElementEnabled(saveButton);
    });
    await user.click(saveButton);

    const confirmSaveButton = await screen.findByRole('button', {
      name: 'Confirm',
    });

    await user.click(confirmSaveButton);

    await waitFor(() => {
      expect(
        workspacesApi.workspaces[0].researchPurpose.populationDetails.length
      ).toBe(0);
    });
  });

  it('should select Research Purpose checkbox if sub category is selected', async () => {
    renderComponent();
    // Research Purpose should not be selected and sub categories should be collapsed by defaul
    const researchPurposeCheckbox: HTMLInputElement = screen.getByTestId(
      'researchPurpose-checkbox'
    ) as HTMLInputElement;
    expect(researchPurposeCheckbox.checked).toEqual(false);

    expect(
      screen.queryByText(
        'Choose options below to describe your research purpose'
      )
    ).not.toBeInTheDocument();

    // Click the arrow icon to expand the research purpose sub categories
    const researchPurposeButton = screen.getByRole('button', {
      name: /Research Purpose/i,
    });
    await user.click(researchPurposeButton);

    // Select any sub category for Research Purpose
    const ancestoryCheckbox: HTMLInputElement = (await screen.findByTestId(
      'ancestry-checkbox'
    )) as HTMLInputElement;

    await user.click(ancestoryCheckbox);

    await waitFor(() => {
      // Research Purpose checkbox should be selected now
      expect(researchPurposeCheckbox.checked).toEqual(true);
    });

    await user.click(ancestoryCheckbox);

    // Un-selecting the sub categories should unselect the research purpose checkbox
    // BUT THE SUB CATEGORIES SHOULD STILL BE VISIBLE
    await waitFor(() => {
      // Research Purpose checkbox should not be selected now
      expect(researchPurposeCheckbox.checked).toEqual(false);
    });

    expect(
      screen.queryByText(
        'Choose options below to describe your research purpose'
      )
    ).toBeInTheDocument();

    // Clicking the icon should collapse all the research purpose sub-categories
    await user.click(researchPurposeButton);

    expect(
      screen.queryByText(
        'Choose options below to describe your research purpose'
      )
    ).not.toBeInTheDocument();
  });

  it('supports disable save button if Research Outcome is not answered', async () => {
    workspaceEditMode = WorkspaceEditMode.Duplicate;
    workspace.researchPurpose.researchOutcomeList = [];
    renderComponent();

    const saveButton = await screen.findByRole('button', {
      name: /Duplicate Workspace/i,
    });
    expectButtonElementDisabled(saveButton);
  });

  it('supports successful duplication in asynchronous mode', async () => {
    workspaceEditMode = WorkspaceEditMode.Duplicate;
    renderComponent();

    const reviewRequestFalseRadioButton = await screen.findByTestId(
      'review-request-btn-false'
    );
    user.click(reviewRequestFalseRadioButton);

    const saveButton = await screen.findByRole('button', {
      name: /Duplicate Workspace/i,
    });
    const numBefore = workspacesApi.workspaceOperations.length;

    await waitFor(() => {
      expectButtonElementEnabled(saveButton);
    });

    user.click(saveButton);

    await waitFor(() => {
      // Wait for confirm duplication modal to appear
      screen.queryByText(
        /note: this workspace will take approximately one minute to create\./i
      );
    });

    const confirmSaveButton = await screen.findByRole('button', {
      name: 'Confirm',
    });

    user.click(confirmSaveButton);

    await waitFor(() => {
      expect(workspacesApi.workspaceOperations.length).toEqual(numBefore + 1);
      expect(mockNavigate).toHaveBeenCalledTimes(1);
    });
  });

  it('defaults to upgrading the CDR Version when asynchronously duplicating a workspace with an older CDR Version', async () => {
    // init the workspace to a non-default CDR version value
    const altCdrWorkspace = {
      ...workspace,
      cdrVersionId: CdrVersionsStubVariables.ALT_WORKSPACE_CDR_VERSION_ID,
    };
    currentWorkspaceStore.next(altCdrWorkspace);

    // duplication will involve a CDR version upgrade by default
    workspaceEditMode = WorkspaceEditMode.Duplicate;

    renderComponent();

    await waitFor(() => {
      const cdrSelection = screen.getByRole('combobox', {
        name: /cdr version dropdown/i,
      }) as HTMLSelectElement;
      // default CDR version, not the existing workspace's alt CDR version
      expect(cdrSelection.value).toBe(
        CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID
      );
    });

    const { ALT_WORKSPACE_CDR_VERSION, DEFAULT_WORKSPACE_CDR_VERSION } =
      CdrVersionsStubVariables;
    const expectedUpgradeMessage = `${ALT_WORKSPACE_CDR_VERSION} to ${DEFAULT_WORKSPACE_CDR_VERSION}.`;

    await waitFor(() => {
      const cdrUpgradeMessage = screen.getByTestId(
        'cdr-version-upgrade'
      ).textContent;
      expect(cdrUpgradeMessage).toContain(altCdrWorkspace.name);
      expect(cdrUpgradeMessage).toContain(expectedUpgradeMessage);
    });
  });

  it('does not display the CDR Version upgrade message when duplicating a workspace with the latest CDR Version', async () => {
    // the standard test workspace already has the latest CDR Version but let's make it explicit with a new const
    const defaultCdrWorkspace = {
      ...workspace,
      cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    };
    currentWorkspaceStore.next(defaultCdrWorkspace);

    workspaceEditMode = WorkspaceEditMode.Duplicate;

    renderComponent();

    await waitFor(() => {
      // Ensuring the component has loaded
      expect(screen.getByText('Workspace name')).toBeInTheDocument();
      // upgrade message does not appear
      expect(screen.queryByTestId('cdr-version-upgrade')).toBeNull();
      // old CDR Version warning does not appear
      expect(screen.queryByTestId('old-cdr-version-warning')).toBeNull();
    });
  });

  it('enables access tier selection on creation when multiple tiers are present and the user has access to multiple', async () => {
    const twoTiers = [
      AccessTierShortNames.Registered,
      AccessTierShortNames.Controlled,
    ];
    workspaceEditMode = WorkspaceEditMode.Create;
    profileStore.set({
      ...profileStore.get(),
      profile: {
        ...profileStore.get().profile,
        accessTierShortNames: twoTiers,
      },
    });

    renderComponent();

    let accessTierSelection: HTMLSelectElement;
    await waitFor(() => {
      accessTierSelection = screen.getByRole('combobox', {
        name: /data access tier dropdown/i,
      }) as HTMLSelectElement;
      // defaults to registered
      expect(accessTierSelection.value).toBe(AccessTierShortNames.Registered);
    });

    // when Registered is selected, the CDR Version dropdown lists the registered tier CDR Versions
    // defaultCdrVersion and altCdrVersion, with defaultCdrVersion selected
    const cdrVersionsSelect = screen.getByRole('combobox', {
      name: /cdr version dropdown/i,
    }) as HTMLSelectElement;
    expect(cdrVersionsSelect.value).toBe(defaultCdrVersion.cdrVersionId);

    const cdrVersionSelectOptions = Array.from(
      cdrVersionsSelect.options,
      (option) => option.value
    );
    expect(cdrVersionSelectOptions).toEqual([
      defaultCdrVersion.cdrVersionId,
      altCdrVersion.cdrVersionId,
    ]);

    // when Controlled is selected, the CDR Version dropdown lists the (one) controlled tier CDR Version
    await userEvent.selectOptions(accessTierSelection, [
      AccessTierShortNames.Controlled,
    ]);

    expect(cdrVersionsSelect.value).toBe(controlledCdrVersion.cdrVersionId);
    const cdrVersionSelectOptionsAfterChange = Array.from(
      cdrVersionsSelect.options,
      (option) => option.value
    );
    expect(cdrVersionSelectOptionsAfterChange).toEqual([
      controlledCdrVersion.cdrVersionId,
    ]);
  });

  it(
    'enables the access tier selection dropdown on creation when multiple tiers are present' +
      'but prevents selection when the user does not have access',
    async () => {
      workspaceEditMode = WorkspaceEditMode.Create;
      profileStore.set({
        ...profileStore.get(),
        profile: {
          ...profileStore.get().profile,
          accessTierShortNames: [AccessTierShortNames.Registered],
        },
      });

      renderComponent();

      let accessTierSelection: HTMLSelectElement;
      await waitFor(() => {
        accessTierSelection = screen.getByRole('combobox', {
          name: /data access tier dropdown/i,
        }) as HTMLSelectElement;
        // defaults to registered
        expect(accessTierSelection.disabled).toBe(false);
        expect(accessTierSelection.value).toBe(AccessTierShortNames.Registered);
      });

      // when Registered is selected, the CDR Version dropdown lists the registered tier CDR Versions
      // defaultCdrVersion and altCdrVersion, with defaultCdrVersion selected
      const cdrVersionsSelect = screen.getByRole('combobox', {
        name: /cdr version dropdown/i,
      }) as HTMLSelectElement;
      expect(cdrVersionsSelect.value).toBe(defaultCdrVersion.cdrVersionId);

      const cdrVersionSelectOptions = Array.from(
        cdrVersionsSelect.options,
        (option) => option.value
      );
      expect(cdrVersionSelectOptions).toEqual([
        defaultCdrVersion.cdrVersionId,
        altCdrVersion.cdrVersionId,
      ]);

      // when Controlled is selected, the UnavailableTierModal appears, and the CDR Version dropdown continues to
      // list the registered tier CDR Versions
      await userEvent.selectOptions(accessTierSelection, [
        AccessTierShortNames.Controlled,
      ]);

      expect(screen.getByTestId('unavailable-tier-modal')).toBeInTheDocument();

      expect(cdrVersionsSelect.value).toBe(defaultCdrVersion.cdrVersionId);
      const cdrVersionSelectOptionsAfterChange = Array.from(
        cdrVersionsSelect.options,
        (option) => option.value
      );
      expect(cdrVersionSelectOptionsAfterChange).toEqual([
        defaultCdrVersion.cdrVersionId,
        altCdrVersion.cdrVersionId,
      ]);
    }
  );

  it('retains the tier on edit and does not permit changes - Registered', async () => {
    workspaceEditMode = WorkspaceEditMode.Edit;
    workspace.accessTierShortName = AccessTierShortNames.Registered;
    await expectNoTierChangesAllowed();
  });

  it('retains the tier on edit and does not permit changes - Controlled', async () => {
    workspaceEditMode = WorkspaceEditMode.Edit;
    workspace.accessTierShortName = AccessTierShortNames.Controlled;
    await expectNoTierChangesAllowed();
  });

  it('retains the tier on duplication and does not permit changes - Registered', async () => {
    workspaceEditMode = WorkspaceEditMode.Duplicate;
    workspace.accessTierShortName = AccessTierShortNames.Registered;
    await expectNoTierChangesAllowed();
  });

  it('retains the tier on duplication and does not permit changes - Controlled', async () => {
    workspaceEditMode = WorkspaceEditMode.Duplicate;
    workspace.accessTierShortName = AccessTierShortNames.Controlled;
    await expectNoTierChangesAllowed();
  });

  // regression test for RW-5132 - async version
  it('prevents multiple (async) Workspace creations via the same confirmation dialog', async () => {
    workspaceEditMode = WorkspaceEditMode.Duplicate;
    renderComponent();

    userEvent.click(await screen.findByTestId('review-request-btn-false'));

    const numBefore = workspacesApi.workspaceOperations.length;
    userEvent.click(
      await screen.findByRole('button', {
        name: /Duplicate Workspace/i,
      })
    );

    const confirmSaveButton = await screen.findByRole('button', {
      name: 'Confirm',
    });

    await user.click(confirmSaveButton);
    await user.click(confirmSaveButton);
    await user.click(confirmSaveButton);

    expect(workspacesApi.workspaceOperations.length).toEqual(numBefore + 1);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
  });

  it('should disallow whitespace-only intended study text', async () => {
    renderComponent();
    const intendedStudySection = await screen.getByRole('textbox', {
      name: /text area describing the intention of the study/i,
    });
    await user.type(intendedStudySection, '    ');
    const saveButton = screen.getByRole('button', {
      name: /create workspace/i,
    });
    await user.hover(saveButton);
    // Testing to see if error appears
    expect(
      screen.queryByText(
        /what are the specific scientific question\(s\) you intend to study \(question 2\.1\)/i
      )
    ).toBeInTheDocument();
  });

  it('should show warning message if research purpose summary Intended study have answer less than 50 characters', async () => {
    renderComponent();
    // Intended Study Text
    const text = 'intended Study text';
    // since its a new page the characters box for Intended study should say 1000 characters remaining
    const intendedStudySection = await screen.findByTestId('intendedStudyText');
    within(intendedStudySection).getByText('1000 characters remaining');

    const intendedStudySectionTextArea = await screen.getByRole('textbox', {
      name: /text area describing the intention of the study/i,
    });

    await user.click(intendedStudySectionTextArea);
    await user.paste(text);
    const charsRemaining = 1000 - text.length;

    within(intendedStudySection).getByText(
      `${charsRemaining.toString()} characters remaining`
    );

    // Warning message will appear onBlur
    await user.tab();
    expect(
      within(intendedStudySection).queryByText(
        'The description you entered seems too short.',
        { exact: false }
      )
    ).toBeInTheDocument();
  });

  it('should show error message if research purpose summary has reached 1000 characters', async () => {
    renderComponent();
    // Intended Study Text
    const testInput = 'a'.repeat(1000);

    // since its a new page the characters box for Intended study should say 1000 characters remaining
    const intendedStudySection = await screen.findByTestId('intendedStudyText');
    expect(intendedStudySection).toHaveTextContent('1000 characters remaining');

    const intendedStudySectionTextArea = await screen.getByRole('textbox', {
      name: /text area describing the intention of the study/i,
    });

    await user.click(intendedStudySectionTextArea);
    await user.paste(testInput);

    // Check that the character limit text has updated
    expect(intendedStudySection).toHaveTextContent('0 characters remaining');
  });

  it('should show error message if Other primary purpose is more than 500 characters', async () => {
    const errorMessage = /other primary purpose cannot exceed 500 characters/i;
    renderComponent();

    const saveButton = screen.getByRole('button', {
      name: /create workspace/i,
    });
    await user.hover(saveButton);
    // Testing to see if error appears
    expect(
      screen.queryByText('other primary purpose cannot', { exact: false })
    ).not.toBeInTheDocument();

    const otherPurposeCheckbox = screen.getByTestId('otherPurpose-checkbox');
    await userEvent.click(otherPurposeCheckbox);

    await user.hover(saveButton);
    expect(
      screen.queryByText('other primary purpose cannot be blank', {
        exact: false,
      })
    ).toBeInTheDocument();

    // Other Primary Purpose
    const validInput = fp.repeat(500, 'a');
    const otherPrimaryPurposeText = screen.getByTestId(
      'otherPrimaryPurposeText'
    );

    await user.clear(otherPrimaryPurposeText);
    await user.click(otherPrimaryPurposeText);
    await user.paste(validInput);

    await user.hover(saveButton);
    // Testing to see if error appears
    expect(
      screen.queryByText('other primary purpose cannot', { exact: false })
    ).not.toBeInTheDocument();

    const invalidInput = fp.repeat(501, 'b');
    await user.clear(otherPrimaryPurposeText);
    await user.click(otherPrimaryPurposeText);
    await user.paste(invalidInput);

    await user.hover(saveButton);
    expect(screen.queryByText(errorMessage)).toBeInTheDocument();
  });

  it('should show error message if Disease of focus is more than 80 characters', async () => {
    const errorMessage = /disease of focus cannot exceed 80 characters/i;
    renderComponent();
    const saveButton = await screen.findByRole('button', {
      name: /create workspace/i,
    });
    expectButtonElementDisabled(saveButton);
    await user.hover(saveButton);
    // Testing to see if error appears
    expect(screen.queryByText(errorMessage)).not.toBeInTheDocument();

    const researchPurposeCheckbox: HTMLInputElement = screen.getByTestId(
      'researchPurpose-checkbox'
    ) as HTMLInputElement;

    await user.click(researchPurposeCheckbox);

    const diseaseFocusedCheckbox: HTMLInputElement = screen.getByTestId(
      'diseaseFocusedResearch-checkbox'
    ) as HTMLInputElement;

    await user.click(diseaseFocusedCheckbox);

    await user.hover(saveButton);

    expect(
      screen.queryByText(/disease of focus cannot be blank/i)
    ).toBeInTheDocument();

    const validInput = fp.repeat(8, 'a');

    const diseaseOfFocusInput = screen.getByPlaceholderText(/name of disease/i);
    await user.type(diseaseOfFocusInput, validInput);

    await user.hover(saveButton);

    expect(screen.queryByText(/disease of focus/i)).not.toBeInTheDocument();

    const inValidInput = fp.repeat(81, 'b');
    await user.type(diseaseOfFocusInput, inValidInput);

    await user.hover(saveButton);

    expect(
      screen.queryByText(/disease of focus cannot exceed 80 characters/i)
    ).toBeInTheDocument();
  });

  it('should show error message if Other text for disseminate research is more than 100 characters', async () => {
    const errorMessage = /other methods of disseminating research findings/i;
    renderComponent();

    const otherDisseminationCheckbox = await screen.findByTestId(
      'OTHER-checkbox'
    );
    await user.click(otherDisseminationCheckbox);
    const validInput = fp.repeat(8, 'a');
    const otherDisseminateResearchText = screen.getByPlaceholderText(
      OTHER_DISSEMINATION_REGEX
    );
    await user.type(otherDisseminateResearchText, validInput);

    const saveButton = screen.getByRole('button', {
      name: /create workspace/i,
    });

    await user.hover(saveButton);

    expect(screen.queryByText(errorMessage)).not.toBeInTheDocument();

    const invalidInput = fp.repeat(101, 'b');
    await user.clear(otherDisseminateResearchText);
    await user.type(otherDisseminateResearchText, invalidInput);
    await user.hover(saveButton);

    expect(screen.queryByText(errorMessage)).toBeInTheDocument();
  });

  it('should show error message if Other text for Special Population is more than 100 characters', async () => {
    const errorMessage = /other specific population cannot/i;
    workspaceEditMode = WorkspaceEditMode.Edit;
    renderComponent();

    const specificPopulationYesRadio: HTMLInputElement =
      await screen.findByTestId('specific-population-yes');
    expect(specificPopulationYesRadio.checked).toEqual(true);

    const otherSpecialPopulationCheckbox = screen.getByTestId(
      'other-specialPopulation-checkbox'
    );
    await user.click(otherSpecialPopulationCheckbox);

    // Simulate entering valid input into the "other-specialPopulation-text" text field
    const validInput = 'a'.repeat(100);
    const otherSpecialPopulationTextField = screen.getByTestId(
      'other-specialPopulation-text'
    );
    await user.click(otherSpecialPopulationTextField);
    await user.paste(validInput);

    const saveButton = screen.getByRole('button', {
      name: /update workspace/i,
    });

    await user.hover(saveButton);

    expect(screen.queryByText(errorMessage)).not.toBeInTheDocument();

    const invalidInput = 'a'.repeat(101);
    await user.clear(otherSpecialPopulationTextField);
    await user.click(otherSpecialPopulationTextField);
    await user.paste(invalidInput);
    await user.hover(saveButton);

    expect(screen.queryByText(errorMessage)).toBeInTheDocument();
  });

  it('should show error message when other disseminate checked but empty', async () => {
    const errorMessage = /other methods of disseminating research findings/i;
    renderComponent();

    const otherDisseminationCheckbox = await screen.findByTestId(
      'OTHER-checkbox'
    );
    await user.click(otherDisseminationCheckbox);
    const validInput = fp.repeat(8, 'a');
    const otherDisseminateResearchText = screen.getByPlaceholderText(
      OTHER_DISSEMINATION_REGEX
    );
    await user.type(otherDisseminateResearchText, validInput);

    const saveButton = screen.getByRole('button', {
      name: /create workspace/i,
    });

    await user.hover(saveButton);

    expect(screen.queryByText(errorMessage)).not.toBeInTheDocument();

    const invalidInput = fp.repeat(8, ' ');
    await user.clear(otherDisseminateResearchText);
    await user.type(otherDisseminateResearchText, invalidInput);
    await user.hover(saveButton);

    expect(screen.queryByText(errorMessage)).toBeInTheDocument();
  });

  it('should show free tier then user billing when user not granting then grant billing scope', async () => {
    workspaceEditMode = WorkspaceEditMode.Create;
    renderComponent();

    expect(mockEnsureBillingScope).toHaveBeenCalledTimes(0);

    expect(
      await screen.findByDisplayValue(FREE_TIER_OPTION_REGEX)
    ).toBeInTheDocument();

    // PrimeReact dropdowns are hidden by default, so we need to check for the hidden attribute
    // Options are presented in the DOM as both option and li tags
    expect(
      screen.getAllByRole('option', {
        name: FREE_TIER_OPTION_REGEX,
        hidden: true,
      })[0]
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('option', {
        name: USER_BILLING_OPTION_REGEX,
        hidden: true,
      })
    ).not.toBeInTheDocument();

    mockHasBillingScope.mockImplementation(() => true);

    // Open dropdown, this triggers updating billing options
    await user.click(screen.getByTestId('billing-dropdown'));
    expect(mockEnsureBillingScope).toHaveBeenCalledTimes(1);

    // Close the dropdown in order to allow the dropdown to update?
    await user.click(screen.getByTestId('billing-dropdown'));

    expect(
      screen.getAllByRole('option', {
        name: FREE_TIER_OPTION_REGEX,
        hidden: true,
      })[0]
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('option', {
        name: USER_BILLING_OPTION_REGEX,
        hidden: true,
      })
    ).toBeInTheDocument();
  });

  it('should show free tier user account and user-billing when user granted billing scope', async () => {
    mockHasBillingScope.mockImplementation(() => true);
    workspaceEditMode = WorkspaceEditMode.Create;
    renderComponent();

    expect(
      await screen.findByDisplayValue(FREE_TIER_OPTION_REGEX)
    ).toBeInTheDocument();

    expect(mockEnsureBillingScope).toHaveBeenCalledTimes(0);

    // Need to open the dropdown to view the billing options
    await user.click(screen.getByTestId('billing-dropdown'));

    expect(
      screen.getAllByRole('option', {
        name: FREE_TIER_OPTION_REGEX,
        hidden: true,
      })[0]
    ).toBeInTheDocument();

    expect(
      screen.queryByRole('option', {
        name: USER_BILLING_OPTION_REGEX,
        hidden: true,
      })
    ).toBeInTheDocument();
  });

  it('should show free tier user account correctly when usage is undefined', async () => {
    const freeTierRegex = /use all of us initial credits \- \$34\.56 left/i;
    mockHasBillingScope.mockImplementation(() => true);
    workspaceEditMode = WorkspaceEditMode.Create;
    profileStore.set({
      ...profileStore.get(),
      profile: {
        ...profileStore.get().profile,
        freeTierUsage: undefined,
      },
    });

    renderComponent();

    expect(await screen.findByDisplayValue(freeTierRegex)).toBeInTheDocument();

    // Need to open the dropdown to view the billing options
    await user.click(screen.getByTestId('billing-dropdown'));

    expect(
      screen.getAllByRole('option', {
        name: freeTierRegex,
        hidden: true,
      })[0]
    ).toBeInTheDocument();

    expect(
      screen.queryByRole('option', {
        name: USER_BILLING_OPTION_REGEX,
        hidden: true,
      })
    ).toBeInTheDocument();
  });

  it('should show User Provided Billing Account when user does not have permission on the billing account workspace is using', async () => {
    mockHasBillingScope.mockImplementation(() => true);
    workspaceEditMode = WorkspaceEditMode.Edit;
    renderComponent();

    expect(mockEnsureBillingScope).toHaveBeenCalledTimes(0);
    // 'billing-account' is workspace's current billing account.
    // There would be 3 options: Free tier, user's billing account, workspace billing account
    expect(
      await screen.findByDisplayValue(USER_PROVIDED_BILLING_OPTION_REGEX)
    ).toBeInTheDocument();

    // Need to open the dropdown to view the billing options
    await user.click(screen.getByTestId('billing-dropdown'));

    expect(
      screen.getAllByRole('option', {
        name: FREE_TIER_OPTION_REGEX,
        hidden: true,
      })[0]
    ).toBeInTheDocument();

    expect(
      screen.queryByRole('option', {
        name: USER_BILLING_OPTION_REGEX,
        hidden: true,
      })
    ).toBeInTheDocument();

    expect(
      screen.getAllByRole('option', {
        name: USER_PROVIDED_BILLING_OPTION_REGEX,
        hidden: true,
      })[0]
    ).toBeInTheDocument();
  });

  it('Should trim workspace name', async () => {
    renderComponent();
    const workspaceNameTextBox = await screen.findByTestId('workspace-name');
    await userEvent.type(
      workspaceNameTextBox,
      '  ' + WORKSPACE_NAME_TEXT + '    '
    );
    expect(
      // By default, RTL trims the whitespace around text in the DOM when querying. Since this test
      // is verifying our own whitespace trimming, we need to disable RTL's trim option.
      // https://testing-library.com/docs/queries/about/#normalization
      screen.queryByDisplayValue(WORKSPACE_NAME_TEXT, {
        normalizer: getDefaultNormalizer({ trim: false }),
      })
    ).not.toBeInTheDocument();

    await user.tab();

    expect(
      screen.getByDisplayValue(WORKSPACE_NAME_TEXT, {
        normalizer: getDefaultNormalizer({ trim: false }),
      })
    ).toBeInTheDocument();
  });
});
