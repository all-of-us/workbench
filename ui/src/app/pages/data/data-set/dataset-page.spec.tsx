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

import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button, Clickable } from 'app/components/buttons';
import {
  COMPARE_DOMAINS_FOR_DISPLAY,
  DatasetPage,
} from 'app/pages/data/data-set/dataset-page';
import { ExportDatasetModal } from 'app/pages/data/data-set/export-dataset-modal';
import { GenomicExtractionModal } from 'app/pages/data/data-set/genomic-extraction-modal';
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
  waitOneTickAndUpdate,
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

  const previewLinkWrapper = (wrapper) =>
    wrapper.find(Clickable).find(`[data-test-id="preview-button"]`).first();

  const btnWrapper = (wrapper, btnId) =>
    wrapper.find(Button).find(`[data-test-id="${btnId}"]`).first();

  const analyzeBtnWrapper = (wrapper) => btnWrapper(wrapper, 'analyze-button');
  const saveBtnWrapper = (wrapper) => btnWrapper(wrapper, 'save-button');
  const getAnalyzeButton = () => {
    return screen.getByRole('button', { name: /analyze/i });
  };
  const getCreateButton = () => {
    return screen.getByRole('button', { name: /create dataset/i });
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

  const component = () => {
    return mount(
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

  const clickCohortCheckboxAtIndex = async (index: number) => {
    await user.click(
      within(
        (
          await screen.findAllByTestId('cohort-list-item')
        )[index]
      ).getByRole('checkbox')
    );
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

    // Wait until
    await screen.findByText('Workspace Cohorts');
    // By default all buttons and select Value checkbox should be disabled
    expectButtonElementDisabled(getSaveButton());
    expectButtonElementDisabled(getAnalyzeButton());
    expectButtonElementDisabled(getPreviewButton());
    expect(getSelectAllCheckbox().disabled).toBeTruthy();

    // Select a cohort
    await user.click(
      within((await screen.findAllByTestId('cohort-list-item'))[0]).getByRole(
        'checkbox'
      )
    );

    // All buttons and links should still be disabled
    expectButtonElementDisabled(getSaveButton());
    expectButtonElementDisabled(getAnalyzeButton());
    expectButtonElementDisabled(getPreviewButton());
    expect(getSelectAllCheckbox().disabled).toBeTruthy();

    // Select a concept set
    await clickCohortCheckboxAtIndex(0);

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

  it('should display preview data table once preview button is clicked', async () => {
    const spy = jest.spyOn(dataSetApi(), 'previewDataSetByDomain');
    componentAlt();

    // Select one cohort , concept and value
    await clickCohortCheckboxAtIndex(0);
    await clickConceptSetCheckboxAtIndex(0);
    await clickValueCheckboxAtIndex(0);

    // Select another value preview data api should not be called now
    await clickValueCheckboxAtIndex(1);

    // Click preview button to load preview
    await user.click(getPreviewButton());
    waitFor(() => {
      expect(spy).toHaveBeenCalledTimes(1);
    });
  });

  it('should display preview data for current domains only', async () => {
    const spy = jest.spyOn(dataSetApi(), 'previewDataSetByDomain');
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    // Select a cohort.
    wrapper
      .find('[data-test-id="cohort-list-item"]')
      .first()
      .find('input')
      .first()
      .simulate('change');
    wrapper.update();

    // Select "Condition" and "Measurement" concept sets.
    let conceptSetEls = wrapper.find('[data-test-id="concept-set-list-item"]');
    conceptSetEls.at(0).find('input').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);
    conceptSetEls.at(1).find('input').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);

    // Deselect "Condition".
    conceptSetEls = wrapper.find('[data-test-id="concept-set-list-item"]');
    conceptSetEls.at(0).find('input').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);

    // Click preview button to load preview
    wrapper
      .find({ 'data-test-id': 'preview-button' })
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should check that the Cohorts and Concept Sets "+" links go to their pages.', async () => {
    const wrapper = component();
    const pathPrefix = dataTabPath(
      workspaceDataStub.namespace,
      workspaceDataStub.id
    );

    // Check Cohorts "+" link
    wrapper.find({ 'data-test-id': 'cohorts-link' }).first().simulate('click');

    expect(mockNavigateByUrl).toHaveBeenCalledWith(
      pathPrefix + '/cohorts/build',
      {
        preventDefaultIfNoKeysPressed: true,
        event: expect.anything(),
      }
    );

    // Check Concept Sets "+" link
    wrapper
      .find({ 'data-test-id': 'concept-sets-link' })
      .first()
      .simulate('click');
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
    const wrapper = component();
    const isTooltipDisable = wrapper
      .find({ 'data-test-id': 'save-tooltip' })
      .first()
      .props().disabled;
    const isSaveButtonDisable = wrapper
      .find({ 'data-test-id': 'save-button' })
      .first()
      .props().disabled;
    expect(isTooltipDisable).toBeFalsy();
    expect(isSaveButtonDisable).toBeTruthy();
  });

  it('dataSet should disable cohort/concept PLUS ICON if user has READER access', async () => {
    const readWorkspace = {
      ...workspaceStubs[0],
      accessLevel: WorkspaceAccessLevel.READER,
    };
    currentWorkspaceStore.next(readWorkspace);
    const wrapper = component();
    const plusIconTooltip = wrapper.find({
      'data-test-id': 'plus-icon-tooltip',
    });
    const cohortplusIcon = wrapper.find({ 'data-test-id': 'cohorts-link' });
    const conceptSetplusIcon = wrapper.find({
      'data-test-id': 'concept-sets-link',
    });
    expect(plusIconTooltip.first().props().disabled).toBeFalsy();
    expect(cohortplusIcon.first().props().disabled).toBeTruthy();
    expect(conceptSetplusIcon.first().props().disabled).toBeTruthy();
  });

  it('should call load data dictionary when caret is expanded', async () => {
    const spy = jest.spyOn(dataSetApi(), 'getDataDictionaryEntry');
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    // Select one cohort , concept and value
    wrapper
      .find('[data-test-id="cohort-list-item"]')
      .first()
      .find('input')
      .first()
      .simulate('change');
    wrapper.update();

    wrapper
      .find('[data-test-id="concept-set-list-item"]')
      .first()
      .find('input')
      .first()
      .simulate('change');

    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="value-list-expander"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(spy).toHaveBeenCalledTimes(1);
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
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    // Select one cohort
    wrapper
      .find('[data-test-id="cohort-list-item"]')
      .first()
      .find('input')
      .first()
      .simulate('change');

    expect(
      wrapper.find('[data-test-id="cohort-list-item"]').first().props().checked
    ).toBeTruthy();
    expect(
      wrapper.find('[data-test-id="all-participant"]').props().checked
    ).toBeFalsy();

    wrapper
      .find('[data-test-id="all-participant"]')
      .first()
      .find('input')
      .first()
      .simulate('change');

    expect(
      wrapper.find('[data-test-id="cohort-list-item"]').first().props().checked
    ).toBeFalsy();
    expect(
      wrapper.find('[data-test-id="all-participant"]').props().checked
    ).toBeTruthy();
  });

  it('should unselect PrePackaged cohort is selected if Workspace Cohort is selected', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    wrapper
      .find('[data-test-id="all-participant"]')
      .first()
      .find('input')
      .first()
      .simulate('change');

    expect(
      wrapper.find('[data-test-id="cohort-list-item"]').first().props().checked
    ).toBeFalsy();
    expect(
      wrapper.find('[data-test-id="all-participant"]').props().checked
    ).toBeTruthy();

    // Select one cohort
    wrapper
      .find('[data-test-id="cohort-list-item"]')
      .first()
      .find('input')
      .first()
      .simulate('change');

    expect(
      wrapper.find('[data-test-id="cohort-list-item"]').first().props().checked
    ).toBeTruthy();
    expect(
      wrapper.find('[data-test-id="all-participant"]').props().checked
    ).toBeFalsy();
  });

  // TODO: rewrite this so it's not dependent on modifying global test state!

  it('should display Prepackaged concept set as per CDR data', async () => {
    // this test needs to modify a CDR version.
    // Let's save the original so we can restore it later.
    const originalCdrVersion = cdrVersionTiersResponse.tiers[0].versions[0];

    let wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="prePackage-concept-set-item"]').length
    ).toBe(15);

    cdrVersionTiersResponse.tiers[0].versions[0] = {
      ...originalCdrVersion,
      hasWgsData: false,
    };
    wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="prePackage-concept-set-item"]').length
    ).toBe(14);

    cdrVersionTiersResponse.tiers[0].versions[0] = {
      ...originalCdrVersion,
      hasFitbitData: false,
      hasWgsData: true,
    };
    wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="prePackage-concept-set-item"]').length
    ).toBe(11);

    cdrVersionTiersResponse.tiers[0].versions[0] = {
      ...originalCdrVersion,
      hasFitbitData: false,
      hasWgsData: false,
    };
    wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="prePackage-concept-set-item"]').length
    ).toBe(10);

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
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="analyze-button"]').simulate('click');
    expect(wrapper.find(ExportDatasetModal).exists()).toBeTruthy();
    wrapper
      .find(ExportDatasetModal)
      .find('[data-test-id="export-dataset-modal-cancel-button"]')
      .simulate('click');
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
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="analyze-button"]').simulate('click');
    expect(wrapper.find(GenomicExtractionModal).exists()).toBeTruthy();
  });

  it('should enable Save Dataset button when selecting All Participants prepackaged cohort', async () => {
    datasetApiStub.getDatasetMock = {
      ...stubDataSet(),
      conceptSets: [{ id: 345, domain: Domain.PERSON }],
      cohorts: [{ id: 1 }],
      domainValuePairs: [{ domain: Domain.PERSON, value: 'person' }],
    };
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    // Save button is disabled since no changes have been made
    expect(
      wrapper
        .find(Button)
        .find('[data-test-id="save-button"]')
        .first()
        .prop('disabled')
    ).toBeTruthy();

    wrapper
      .find('[data-test-id="all-participant"]')
      .first()
      .find('input')
      .first()
      .simulate('change');
    // Save button should enable after selection
    expect(
      wrapper
        .find(Button)
        .find('[data-test-id="save-button"]')
        .first()
        .prop('disabled')
    ).toBeFalsy();
  });
});
