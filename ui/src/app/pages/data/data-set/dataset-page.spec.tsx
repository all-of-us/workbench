import {mount} from 'enzyme';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {COMPARE_DOMAINS_FOR_DISPLAY, DatasetPage} from 'app/pages/data/data-set/dataset-page';
import {ExportDatasetModal} from 'app/pages/data/data-set/export-dataset-modal';
import {GenomicExtractionModal} from 'app/pages/data/data-set/genomic-extraction-modal';
import {dataSetApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, NavStore, urlParamsStore} from 'app/utils/navigation';
import {cdrVersionStore, serverConfigStore} from 'app/utils/stores';
import {
  CdrVersionsApi,
  CohortsApi,
  ConceptSetsApi,
  DataSetApi,
  Domain, PrePackagedConceptSetEnum,
  WorkspaceAccessLevel, WorkspacesApi
} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {CdrVersionsApiStub, cdrVersionTiersResponse} from 'testing/stubs/cdr-versions-api-stub';
import {CohortsApiStub, exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {DataSetApiStub, stubDataSet} from 'testing/stubs/data-set-api-stub';
import {workspaceDataStub, workspaceStubs, WorkspaceStubVariables} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

describe('DataSetPage', () => {
  let datasetApiStub;

  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    datasetApiStub = new DataSetApiStub();
    registerApiClient(DataSetApi, datasetApiStub);
    registerApiClient(CdrVersionsApi, new CdrVersionsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    serverConfigStore.set({config: {enableGenomicExtraction: true, gsuiteDomain: ''}});
    currentWorkspaceStore.next(workspaceDataStub);
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('should render', async() => {
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
  });

  it ('should display all concepts sets in workspace', async() => {
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-list-item"]').length)
      .toBe(ConceptSetsApiStub.stubConceptSets().length);
  });

  it('should display all cohorts in workspace', async() => {
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="cohort-list-item"]').length)
      .toBe(exampleCohortStubs.length);
  });

  it('should display values based on Domain of Concept selected in workspace', async() => {
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // First Concept set in concept set list has domain "Condition"
    const conditionConceptSet = wrapper.find('[data-test-id="concept-set-list-item"]').first()
        .find('input').first();
    conditionConceptSet.simulate('change');
    await waitOneTickAndUpdate(wrapper);
    let valueListItems = wrapper.find('[data-test-id="value-list-items"]');
    expect(valueListItems.length).toBe(2);
    let checkedValuesList = valueListItems.filterWhere(value => value.props().checked);

    // All values should be selected by default
    expect(checkedValuesList.length).toBe(2);

    // Second Concept set in concept set list has domain "Measurement"
    const measurementConceptSet = wrapper.find('[data-test-id="concept-set-list-item"]').at(1)
        .find('input').first();
    measurementConceptSet.simulate('change');
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    valueListItems = wrapper.find('[data-test-id="value-list-items"]');
    checkedValuesList = valueListItems.filterWhere(value => value.props().checked);
    expect(valueListItems.length).toBe(5);
    expect(checkedValuesList.length).toBe(5);
  });

  it('should select all values by default on selection on concept set only if the new domain is unique',
    async() => {
      const wrapper = mount(<DatasetPage/>);
      await waitOneTickAndUpdate(wrapper);

      // Select Condition Concept set
      const conditionConceptSet = wrapper.find('[data-test-id="concept-set-list-item"]').first()
          .find('input').first();
      conditionConceptSet.simulate('change');
      await waitOneTickAndUpdate(wrapper);
      let valueListItems = wrapper.find('[data-test-id="value-list-items"]');
      expect(valueListItems.length).toBe(2);
      let checkedValuesList = valueListItems.filterWhere(value => value.props().checked);

      // All values should be selected by default
      expect(checkedValuesList.length).toBe(2);

      // Select second concept set which is Measurement domain
      const measurementConceptSet = wrapper.find('[data-test-id="concept-set-list-item"]').at(1)
          .find('input').first();
      measurementConceptSet.simulate('change');
      await waitOneTickAndUpdate(wrapper);
      valueListItems = wrapper.find('[data-test-id="value-list-items"]');
      checkedValuesList = valueListItems.filterWhere(value => value.props().checked);
      // All values condition + measurement will be selected
      expect(valueListItems.length).toBe(5);
      expect(checkedValuesList.length).toBe(5);

      // Unselect first Condition value
      valueListItems.first().find('input').first().simulate('change');
      valueListItems = wrapper.find('[data-test-id="value-list-items"]');
      checkedValuesList = valueListItems.filterWhere(value => value.props().checked);
      expect(checkedValuesList.length).toBe(4);

      // Select another condition concept set
      const secondConditionConceptSet =
          wrapper.find('[data-test-id="concept-set-list-item"]').at(2)
          .find('input').first();
      secondConditionConceptSet.simulate('change');
      await waitOneTickAndUpdate(wrapper);
      valueListItems = wrapper.find('[data-test-id="value-list-items"]');
      checkedValuesList = valueListItems.filterWhere(value => value.props().checked);

      // No change in value list since we already had selected condition concept set
      expect(valueListItems.length).toBe(5);

      // Should be no change in selected values
      expect(checkedValuesList.length).toBe(4);
    });

  it('should display correct values on rapid selection of multiple domains', async() => {
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Select "Condition" and "Measurement" concept sets.
    const conceptSetEls = wrapper.find('[data-test-id="concept-set-list-item"]');
    conceptSetEls.at(0).find('input').first().simulate('change');
    conceptSetEls.at(1).find('input').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);

    const valueListItems = wrapper.find('[data-test-id="value-list-items"]');
    const checkedValuesList = valueListItems.filterWhere(value => value.props().checked);
    expect(valueListItems.length).toBe(5);
    expect(checkedValuesList.length).toBe(5);
  });

  it('should enable save button and preview button once cohorts, concepts and values are selected',
    async() => {
      const wrapper = mount(<DatasetPage />);
      await waitOneTickAndUpdate(wrapper);

      // Preview Button and Save Button should be disabled by default
      const saveButton = wrapper.find(Button).find('[data-test-id="save-button"]')
        .first();
      expect(saveButton.prop('disabled')).toBeTruthy();
      const previewButton = wrapper.find(Clickable).find('[data-test-id="preview-button"]')
        .first();
      expect(previewButton.prop('disabled')).toBeTruthy();

      // After all cohort concept and values are selected all the buttons will be enabled

      wrapper.find('[data-test-id="cohort-list-item"]').first()
        .find('input').first().simulate('change');
      wrapper.update();

      wrapper.find('[data-test-id="concept-set-list-item"]').first()
        .find('input').first().simulate('change');

      await waitOneTickAndUpdate(wrapper);

      wrapper.find('[data-test-id="value-list-items"]').find('input').first()
        .simulate('change');

      // Buttons should now be enabled
      const buttons = wrapper.find(Button);
      expect(buttons.find('[data-test-id="save-button"]').first().prop('disabled'))
        .toBeFalsy();
      expect(wrapper.find(Clickable).find('[data-test-id="preview-button"]').first()
        .prop('disabled')).toBeFalsy();
    });

  it('should display preview data table once preview button is clicked', async() => {
    const spy = jest.spyOn(dataSetApi(), 'previewDataSetByDomain');
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);

    // Select one cohort , concept and value
    wrapper.find('[data-test-id="cohort-list-item"]').first()
      .find('input').first().simulate('change');
    wrapper.update();

    wrapper.find('[data-test-id="concept-set-list-item"]').first()
      .find('input').first().simulate('change');

    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="value-list-items"]').find('input').first()
      .simulate('change');

    await waitOneTickAndUpdate(wrapper);

    // Select another value preview data api should not be called now
    wrapper.find('[data-test-id="value-list-items"]').at(1)
      .find('input').first().simulate('click');

    // Click preview button to load preview
    wrapper.find({'data-test-id': 'preview-button'}).first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should display preview data for current domains only', async() => {
    const spy = jest.spyOn(dataSetApi(), 'previewDataSetByDomain');
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);

    // Select a cohort.
    wrapper.find('[data-test-id="cohort-list-item"]').first()
      .find('input').first().simulate('change');
    wrapper.update();

    // Select "Condition" and "Measurement" concept sets.
    let conceptSetEls = wrapper.find('[data-test-id="concept-set-list-item"]');
    conceptSetEls.at(0).find('input').first().simulate('change');
    conceptSetEls.at(1).find('input').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);

    // Deselect "Condition".
    conceptSetEls = wrapper.find('[data-test-id="concept-set-list-item"]');
    conceptSetEls.at(0).find('input').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);

    // Click preview button to load preview
    wrapper.find({'data-test-id': 'preview-button'}).first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should check that the Cohorts and Concept Sets "+" links go to their pages.', async() => {
    const wrapper = mount(<DatasetPage />);
    const pathPrefix = 'workspaces/' + workspaceDataStub.namespace + '/' + workspaceDataStub.id + '/data';

    // Mock out navigateByUrl
    const navSpy = jest.fn();
    NavStore.navigateByUrl = navSpy;

    // Check Cohorts "+" link
    wrapper.find({'data-test-id': 'cohorts-link'}).first().simulate('click');
    expect(navSpy).toHaveBeenCalledWith(pathPrefix + '/cohorts/build');

    // Check Concept Sets "+" link
    wrapper.find({'data-test-id': 'concept-sets-link'}).first().simulate('click');
    expect(navSpy).toHaveBeenCalledWith(pathPrefix + '/concepts');
  });

  it('dataSet should show tooltip and disable SAVE button if user has READER access', async() => {
    const readWorkspace = {...workspaceStubs[0], accessLevel: WorkspaceAccessLevel.READER};
    currentWorkspaceStore.next(readWorkspace);
    const wrapper = mount(<DatasetPage />);
    const isTooltipDisable =
        wrapper.find({'data-test-id': 'save-tooltip'}).first().props().disabled;
    const isSaveButtonDisable =
        wrapper.find({'data-test-id': 'save-button'}).first().props().disabled;
    expect(isTooltipDisable).toBeFalsy();
    expect(isSaveButtonDisable).toBeTruthy();
  });

  it('dataSet should disable cohort/concept PLUS ICON if user has READER access', async() => {
    const readWorkspace = {...workspaceStubs[0], accessLevel: WorkspaceAccessLevel.READER};
    currentWorkspaceStore.next(readWorkspace);
    const wrapper = mount(<DatasetPage />);
    const plusIconTooltip = wrapper.find({'data-test-id': 'plus-icon-tooltip'});
    const cohortplusIcon = wrapper.find({'data-test-id': 'cohorts-link'});
    const conceptSetplusIcon = wrapper.find({'data-test-id': 'concept-sets-link'});
    expect(plusIconTooltip.first().props().disabled).toBeFalsy();
    expect(cohortplusIcon.first().props().disabled).toBeTruthy();
    expect(conceptSetplusIcon.first().props().disabled).toBeTruthy();
  });

  it('should call load data dictionary when caret is expanded', async() => {
    const spy = jest.spyOn(dataSetApi(), 'getDataDictionaryEntry');
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);

    // Select one cohort , concept and value
    wrapper.find('[data-test-id="cohort-list-item"]').first()
      .find('input').first().simulate('change');
    wrapper.update();

    wrapper.find('[data-test-id="concept-set-list-item"]').first()
      .find('input').first().simulate('change');

    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="value-list-expander"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should sort domains for display only', () => {
    const domains = [Domain.MEASUREMENT, Domain.SURVEY, Domain.PERSON, Domain.CONDITION];
    domains.sort(COMPARE_DOMAINS_FOR_DISPLAY);
    expect(domains).toEqual([Domain.PERSON, Domain.CONDITION, Domain.MEASUREMENT, Domain.SURVEY]);
  });

  it('should unselect any workspace Cohort if PrePackaged is selected', async() => {
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);
    // Select one cohort
    wrapper.find('[data-test-id="cohort-list-item"]').first()
      .find('input').first().simulate('change');

    expect(wrapper.find('[data-test-id="cohort-list-item"]').first().props().checked).toBeTruthy();
    expect(wrapper.find('[data-test-id="all-participant"]').props().checked).toBeFalsy();

    wrapper.find('[data-test-id="all-participant"]').first()
      .find('input').first().simulate('change');

    expect(wrapper.find('[data-test-id="cohort-list-item"]').first().props().checked).toBeFalsy();
    expect(wrapper.find('[data-test-id="all-participant"]').props().checked).toBeTruthy();
  });

  it('should unselect PrePackaged cohort is selected if Workspace Cohort is selected', async() => {
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="all-participant"]').first()
      .find('input').first().simulate('change');

    expect(wrapper.find('[data-test-id="cohort-list-item"]').first().props().checked).toBeFalsy();
    expect(wrapper.find('[data-test-id="all-participant"]').props().checked).toBeTruthy();

    // Select one cohort
    wrapper.find('[data-test-id="cohort-list-item"]').first()
      .find('input').first().simulate('change');

    expect(wrapper.find('[data-test-id="cohort-list-item"]').first().props().checked).toBeTruthy();
    expect(wrapper.find('[data-test-id="all-participant"]').props().checked).toBeFalsy();
  });

  it('should display Pre packaged concept set as per CDR data', async() => {
    let wrapper = mount(<DatasetPage/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="prePackage-concept-set-item"]').length).toBe(7);

    cdrVersionTiersResponse.tiers[0].versions[0].hasWgsData = false;
    wrapper = mount(<DatasetPage/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="prePackage-concept-set-item"]').length).toBe(6);


    cdrVersionTiersResponse.tiers[0].versions[0].hasFitbitData = false;
    cdrVersionTiersResponse.tiers[0].versions[0].hasWgsData = true;
    wrapper = mount(<DatasetPage/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="prePackage-concept-set-item"]').length).toBe(3);


    cdrVersionTiersResponse.tiers[0].versions[0].hasFitbitData = false;
    cdrVersionTiersResponse.tiers[0].versions[0].hasWgsData = false;
    wrapper = mount(<DatasetPage/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="prePackage-concept-set-item"]').length).toBe(2);
  });

  it('should display Pre packaged concept set per genomics extraction flag', async() => {
    cdrVersionTiersResponse.tiers[0].versions[0].hasFitbitData = true;
    cdrVersionTiersResponse.tiers[0].versions[0].hasWgsData = true;

    let wrapper = mount(<DatasetPage/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="prePackage-concept-set-item"]').length).toBe(7);

    serverConfigStore.set({config: {enableGenomicExtraction: false, gsuiteDomain: ''}});
    wrapper = mount(<DatasetPage/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="prePackage-concept-set-item"]').length).toBe(6);
  });

  it('should open Export modal if Analyze is clicked and WGS concept is not selected', async() => {
    datasetApiStub.getDatasetMock = {
      ...stubDataSet(),
      conceptSets: [{id: 345, domain: Domain.PERSON}],
      cohorts: [{id: 0}],
      domainValuePairs: [{domain: Domain.PERSON, value: 'person'}],
      prePackagedConceptSet: [PrePackagedConceptSetEnum.PERSON],
    };
    urlParamsStore.next({dataSetId: 0});
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="analyze-button"]').simulate('click');
    expect(wrapper.find(ExportDatasetModal).exists()).toBeTruthy();
    wrapper.find(ExportDatasetModal).find('[data-test-id="export-dataset-modal-cancel-button"]').simulate('click');
  });

  it('should open Extract modal if Analyze is clicked and WGS concept is selected', async() => {
    datasetApiStub.getDatasetMock = {
      ...stubDataSet(),
      conceptSets: [{
        id: ConceptSetsApiStub.stubConceptSets().find(cs => cs.domain === Domain.WHOLEGENOMEVARIANT).id,
        domain: Domain.WHOLEGENOMEVARIANT
      }],
      cohorts: [{id: 0}],
      domainValuePairs: [{domain: Domain.WHOLEGENOMEVARIANT, value: 'wgs'}],
      prePackagedConceptSet: [PrePackagedConceptSetEnum.WHOLEGENOME],
    };
    urlParamsStore.next({dataSetId: 0});
    const wrapper = mount(<DatasetPage />);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="analyze-button"]').simulate('click');
    expect(wrapper.find(GenomicExtractionModal).exists()).toBeTruthy();
  });

});
