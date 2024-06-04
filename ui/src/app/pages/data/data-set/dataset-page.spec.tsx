import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter, Route } from 'react-router-dom';
import { mount } from 'enzyme';
import { mockNavigateByUrl } from 'setupTests';

import {
  CdrVersionsApi,
  CohortsApi,
  ConceptSetsApi,
  DataSetApi,
  Domain,
  NotebooksApi,
  PrePackagedConceptSetEnum,
  WorkspaceAccessLevel,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  COMPARE_DOMAINS_FOR_DISPLAY,
  DatasetPage,
} from 'app/pages/data/data-set/dataset-page';
import { dataTabPath } from 'app/routing/utils';
import {
  dataSetApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  waitForNoSpinner,
} from 'testing/react-test-helpers';
import {
  CdrVersionsApiStub,
  cdrVersionTiersResponse,
} from 'testing/stubs/cdr-versions-api-stub';
import {
  CohortsApiStub,
  exampleCohortStubs,
} from 'testing/stubs/cohorts-api-stub';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { DataSetApiStub, stubDataSet } from 'testing/stubs/data-set-api-stub';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub, workspaceStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

describe('DataSetPage', () => {
  let datasetApiStub;
  let user;

  const getAnalyzeButton = () => {
    return screen.getByRole('button', { name: /analyze/i });
  };
  const getSaveButton = () => {
    return screen.getByRole('button', { name: /save dataset/i });
  };
  const getPreviewButton = () => {
    return screen.getByRole('button', { name: /view preview table/i });
  };

  const getSelectAllCheckbox = (): HTMLInputElement => {
    return screen.getByTestId('select-all');
  };
  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    datasetApiStub = new DataSetApiStub();
    registerApiClient(DataSetApi, datasetApiStub);
    registerApiClient(CdrVersionsApi, new CdrVersionsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(NotebooksApi, new NotebooksApiStub());
    serverConfigStore.set({
      config: { gsuiteDomain: '' },
    });
    currentWorkspaceStore.next(workspaceDataStub);
    cdrVersionStore.set(cdrVersionTiersResponse);
    user = userEvent.setup();
  });

  const componentAlt = () => {
    return render(
      <MemoryRouter
        initialEntries={[
          `${dataTabPath(
            workspaceDataStub.namespace,
            workspaceDataStub.id
          )}/data-sets/${stubDataSet().id}`,
        ]}
      >
        <Route exact path='/workspaces/:ns/:wsid/data/data-sets/:dataSetId'>
          <DatasetPage
            hideSpinner={() => {}}
            showSpinner={() => {}}
            match={{
              params: {
                ns: workspaceDataStub.namespace,
                wsid: workspaceDataStub.id,
                dataSetId: stubDataSet().id,
              },
            }}
          />
        </Route>
      </MemoryRouter>
    );
  };

  const getConceptSets = async () => {
    return await screen.findAllByTestId('concept-set-list-item');
  };

  const getConceptSetCheckbox = async (index: number) => {
    const conceptSets = await getConceptSets();
    return within(conceptSets[index]).getByRole('checkbox');
  };

  const getConditionConceptSetCheckbox = async () => {
    // First Concept set in concept set list has domain "Condition"
    return await getConceptSetCheckbox(0);
  };

  const getMeasurementConceptSetCheckbox = async () => {
    // Second Concept set in concept set list has domain "Measurement"
    return await getConceptSetCheckbox(1);
  };

  const clickConceptSetCheckboxAtIndex = async (index: number) => {
    await user.click(await getConceptSetCheckbox(index));
  };

  const clickConditionConceptSetCheckbox = async () => {
    await user.click(await getConditionConceptSetCheckbox());
  };

  const clickMeasurementConceptSetCheckbox = async () => {
    await user.click(await getMeasurementConceptSetCheckbox());
  };

  const getCohortCheckboxAtIndex = (index: number): HTMLInputElement => {
    return within(screen.getAllByTestId('cohort-list-item')[index]).getByRole(
      'checkbox'
    );
  };

  const clickCohortCheckboxAtIndex = async (index: number) => {
    await user.click(getCohortCheckboxAtIndex(index));
  };

  const getAllParticipantCheckbox = (): HTMLInputElement => {
    return within(screen.getByTestId('all-participant')).getByRole('checkbox');
  };

  const getAllValueOptions = () => screen.getAllByTestId('value-list-items');
  const getCheckedValueOptions = () => {
    return getAllValueOptions().filter(
      (value) =>
        (within(value).getByRole('checkbox') as HTMLInputElement).checked
    );
  };
  const clickValueCheckboxAtIndex = async (index: number) => {
    await user.click(within(getAllValueOptions()[index]).getByRole('checkbox'));
  };

  const getAllPrePackagedConceptSets = () =>
    screen.getAllByTestId('prePackage-concept-set-item');

  it('should render', async () => {
    componentAlt();
    await screen.findByRole('heading', { name: /datasets/i });
  });

  it('should display all concepts sets in workspace', async () => {
    componentAlt();
    expect((await screen.findAllByTestId('concept-set-list-item')).length).toBe(
      ConceptSetsApiStub.stubConceptSets().length
    );
  });

  it('should display all cohorts in workspace', async () => {
    componentAlt();
    expect((await screen.findAllByTestId('cohort-list-item')).length).toBe(
      exampleCohortStubs.length
    );
  });

  it('should display values based on Domain of Concept selected in workspace', async () => {
    componentAlt();

    await clickConditionConceptSetCheckbox();
    expect(getAllValueOptions().length).toBe(2);
    // All values should be selected by default
    expect(getCheckedValueOptions().length).toBe(2);

    // Second Concept set in concept set list has domain "Measurement"
    await clickMeasurementConceptSetCheckbox();
    expect(getAllValueOptions().length).toBe(5);
    expect(getCheckedValueOptions().length).toBe(5);
  });

  it('should select all values by default on selection on concept set only if the new domain is unique', async () => {
    componentAlt();

    // Select Condition Concept set
    await clickConditionConceptSetCheckbox();

    expect(getAllValueOptions().length).toBe(2);
    // All values should be selected by default
    expect(getCheckedValueOptions().length).toBe(2);

    // Select second concept set which is Measurement domain
    await clickMeasurementConceptSetCheckbox();
    expect(getAllValueOptions().length).toBe(5);
    expect(getCheckedValueOptions().length).toBe(5);

    // Unselect first Condition value
    await clickValueCheckboxAtIndex(0);
    expect(getCheckedValueOptions().length).toBe(4);

    // Select another condition concept set
    await clickConceptSetCheckboxAtIndex(2);
    // No change in value list since we already had selected condition concept set
    expect(getAllValueOptions().length).toBe(5);

    // Should be no change in selected values
    expect(getCheckedValueOptions().length).toBe(5);
  });

  it('should display correct values on rapid selection of multiple domains', async () => {
    componentAlt();

    // Select "Condition" and "Measurement" concept sets.
    await clickConditionConceptSetCheckbox();
    await clickMeasurementConceptSetCheckbox();

    expect(getAllValueOptions().length).toBe(5);
    expect(getCheckedValueOptions().length).toBe(5);
  });

  it('should enable buttons and links once cohorts, concepts and values are selected', async () => {
    componentAlt();
    await waitForNoSpinner();

    // Wait until
    await screen.findByText('Workspace Cohorts');
    // By default all buttons and select Value checkbox should be disabled
    expectButtonElementDisabled(getSaveButton());
    expectButtonElementDisabled(getAnalyzeButton());
    expectButtonElementDisabled(getPreviewButton());
    expect(getSelectAllCheckbox().disabled).toBeTruthy();

    // Select a cohort
    await clickCohortCheckboxAtIndex(0);

    // All buttons and links should still be disabled
    expectButtonElementDisabled(getSaveButton());
    expectButtonElementDisabled(getAnalyzeButton());
    expectButtonElementDisabled(getPreviewButton());
    expect(getSelectAllCheckbox().disabled).toBeTruthy();

    // Select a concept set
    await clickConceptSetCheckboxAtIndex(0);

    // All Buttons except analyze button should be enabled as selecting concept set selects all values
    expectButtonElementEnabled(getSaveButton());
    expectButtonElementDisabled(getAnalyzeButton());
    expectButtonElementEnabled(getPreviewButton());
    expect(getSelectAllCheckbox().disabled).toBeFalsy();

    // Unselect 'Select All' checkbox so that no values are selected for DataSet
    await user.click(getSelectAllCheckbox());

    // All buttons and links should now be disabled
    expectButtonElementDisabled(getSaveButton());
    expectButtonElementDisabled(getAnalyzeButton());
    expectButtonElementDisabled(getPreviewButton());
  });

  it('should disable display preview data table when no values are selected', async () => {
    const spy = jest.spyOn(dataSetApi(), 'previewDataSetByDomain');
    componentAlt();
    await waitForNoSpinner();

    await clickCohortCheckboxAtIndex(0);
    await clickConceptSetCheckboxAtIndex(0);

    // Unselect both values
    await clickValueCheckboxAtIndex(0);
    await clickValueCheckboxAtIndex(1);

    const previewButton = getPreviewButton();
    expectButtonElementDisabled(previewButton);

    await user.click(previewButton);

    await waitFor(() => {
      expect(spy).toHaveBeenCalledTimes(0);
    });
  });

  it('should display preview data for current domains only', async () => {
    const spy = jest.spyOn(dataSetApi(), 'previewDataSetByDomain');
    componentAlt();
    await waitForNoSpinner();

    // Select a cohort.
    await clickCohortCheckboxAtIndex(0);

    // Select "Condition" and "Measurement" concept sets.
    await clickConditionConceptSetCheckbox();
    await clickMeasurementConceptSetCheckbox();

    // Deselect "Condition".
    await clickConditionConceptSetCheckbox();

    // Click preview button to load preview
    await user.click(getPreviewButton());

    await waitFor(() => {
      expect(spy).toHaveBeenCalledTimes(1);
    });
  });

  it('should check that the Cohorts and Concept Sets "+" links go to their pages.', async () => {
    componentAlt();
    const pathPrefix = dataTabPath(
      workspaceDataStub.namespace,
      workspaceDataStub.id
    );

    // Check Cohorts "+" link
    await user.click(await screen.findByTestId('cohorts-link'));

    expect(mockNavigateByUrl).toHaveBeenCalledWith(
      pathPrefix + '/cohorts/build',
      {
        preventDefaultIfNoKeysPressed: true,
        event: expect.anything(),
      }
    );

    // Check Concept Sets "+" link
    await user.click(await screen.findByTestId('concept-sets-link'));
    expect(mockNavigateByUrl).toHaveBeenCalledWith(pathPrefix + '/concepts', {
      preventDefaultIfNoKeysPressed: true,
      event: expect.anything(),
    });
  });

  it('dataSet should show tooltip and disable SAVE button if user has READER access', async () => {
    const readWorkspace = {
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.READER,
    };
    currentWorkspaceStore.next(readWorkspace);
    componentAlt();

    await waitForNoSpinner();

    await user.hover(getSaveButton());
    expect(
      screen.getByText('Requires Owner or Writer permission')
    ).toBeInTheDocument();

    expectButtonElementDisabled(getSaveButton());
  });

  it('dataSet should disable cohort/concept PLUS ICON if user has READER access', async () => {
    const readWorkspace = {
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.READER,
    };
    currentWorkspaceStore.next(readWorkspace);
    componentAlt();
    await waitForNoSpinner();

    const cohortplusIcon = screen.getByTestId('cohorts-link');
    const conceptSetplusIcon = screen.getByTestId('concept-sets-link');

    await user.hover(cohortplusIcon);

    screen.getByText('Requires Owner or Writer permission');
    expectButtonElementDisabled(cohortplusIcon);
    expectButtonElementDisabled(conceptSetplusIcon);
  });

  it('should call load data dictionary when caret is expanded', async () => {
    const spy = jest.spyOn(dataSetApi(), 'getDataDictionaryEntry');
    componentAlt();
    await waitForNoSpinner();

    await clickCohortCheckboxAtIndex(0);
    await clickConceptSetCheckboxAtIndex(0);

    await user.click(screen.getAllByTestId('value-list-expander')[0]);

    await waitFor(() => {
      expect(spy).toHaveBeenCalledTimes(1);
    });
  });

  it('should sort domains for display only', () => {
    const domains = [
      Domain.MEASUREMENT,
      Domain.SURVEY,
      Domain.PERSON,
      Domain.CONDITION,
    ];
    domains.sort(COMPARE_DOMAINS_FOR_DISPLAY);
    expect(domains).toEqual([
      Domain.PERSON,
      Domain.CONDITION,
      Domain.MEASUREMENT,
      Domain.SURVEY,
    ]);
  });

  it('should unselect any workspace Cohort if PrePackaged is selected', async () => {
    componentAlt();
    await waitForNoSpinner();

    // Select one cohort
    await clickCohortCheckboxAtIndex(0);

    expect(getCohortCheckboxAtIndex(0).checked).toBeTruthy();

    const allParticipantCheckbox = getAllParticipantCheckbox();
    expect(allParticipantCheckbox.checked).toBeFalsy();
    await user.click(allParticipantCheckbox);

    expect(getCohortCheckboxAtIndex(0).checked).toBeFalsy();
    expect(allParticipantCheckbox.checked).toBeTruthy();
  });

  it('should unselect PrePackaged cohort is selected if Workspace Cohort is selected', async () => {
    componentAlt();
    await waitForNoSpinner();

    const allParticipantCheckbox = getAllParticipantCheckbox();
    const firstCohortCheckbox = getCohortCheckboxAtIndex(0);

    await user.click(allParticipantCheckbox);

    expect(firstCohortCheckbox.checked).toBeFalsy();
    expect(allParticipantCheckbox.checked).toBeTruthy();

    // Select one cohort
    await user.click(firstCohortCheckbox);

    expect(firstCohortCheckbox.checked).toBeTruthy();
    expect(allParticipantCheckbox.checked).toBeFalsy();
  });

  // TODO: rewrite this so it's not dependent on modifying global test state!

  it('should display Prepackaged concept set as per CDR data', async () => {
    // this test needs to modify a CDR version.
    // Let's save the original so we can restore it later.
    const originalCdrVersion = cdrVersionTiersResponse.tiers[0].versions[0];

    let { unmount } = componentAlt();
    await waitForNoSpinner();
    expect(getAllPrePackagedConceptSets().length).toBe(15);

    cdrVersionTiersResponse.tiers[0].versions[0] = {
      ...originalCdrVersion,
      hasWgsData: false,
    };

    unmount();
    ({ unmount } = componentAlt());

    await waitForNoSpinner();
    await waitFor(() => {
      expect(getAllPrePackagedConceptSets().length).toBe(14);
    });
    cdrVersionTiersResponse.tiers[0].versions[0] = {
      ...originalCdrVersion,
      hasFitbitData: false,
      hasWgsData: true,
    };
    unmount();
    ({ unmount } = componentAlt());
    await waitFor(() => {
      expect(getAllPrePackagedConceptSets().length).toBe(11);
    });
    cdrVersionTiersResponse.tiers[0].versions[0] = {
      ...originalCdrVersion,
      hasFitbitData: false,
      hasWgsData: false,
    };
    unmount();
    ({ unmount } = componentAlt());
    await waitFor(() => {
      expect(getAllPrePackagedConceptSets().length).toBe(10);
    });
    // restore original CDR Version for other tests
    cdrVersionTiersResponse.tiers[0].versions[0] = originalCdrVersion;
  });

  it('should open Export modal if Analyze is clicked and WGS concept is not selected', async () => {
    datasetApiStub.getDatasetMock = {
      ...stubDataSet(),
      conceptSets: [{ id: 345, domain: Domain.PERSON }],
      cohorts: [{ id: 1 }],
      domainValuePairs: [{ domain: Domain.PERSON, value: 'person' }],
      prePackagedConceptSet: [PrePackagedConceptSetEnum.PERSON],
    };
    componentAlt();
    await waitForNoSpinner();

    await user.click(getAnalyzeButton());
    screen.getByText('Export Dataset');
    await user.click(screen.getByRole('button', { name: /cancel/i }));
  });

  it('should open Extract modal if Analyze is clicked and WGS concept is selected', async () => {
    datasetApiStub.getDatasetMock = {
      ...stubDataSet(),
      conceptSets: [
        {
          id: ConceptSetsApiStub.stubConceptSets().find(
            (cs) => cs.domain === Domain.WHOLE_GENOME_VARIANT
          ).id,
          domain: Domain.WHOLE_GENOME_VARIANT,
        },
      ],
      cohorts: [{ id: 1 }],
      domainValuePairs: [{ domain: Domain.WHOLE_GENOME_VARIANT, value: 'wgs' }],
      prePackagedConceptSet: [PrePackagedConceptSetEnum.WHOLE_GENOME],
    };
    componentAlt();
    await waitForNoSpinner();

    await user.click(getAnalyzeButton());
    screen.getByText(
      'Would you like to extract genomic variant data as VCF files?'
    );
  });

  it('should enable Save Dataset button when selecting All Participants prepackaged cohort', async () => {
    datasetApiStub.getDatasetMock = {
      ...stubDataSet(),
      conceptSets: [{ id: 345, domain: Domain.PERSON }],
      cohorts: [{ id: 1 }],
      domainValuePairs: [{ domain: Domain.PERSON, value: 'person' }],
    };
    componentAlt();
    await waitForNoSpinner();

    // Save button is disabled since no changes have been made
    expectButtonElementDisabled(getSaveButton());
    await user.click(getAllParticipantCheckbox());
    // Save button should enable after selection
    expectButtonElementEnabled(getSaveButton());
  });
});
