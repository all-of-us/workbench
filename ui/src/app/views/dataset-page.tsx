import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';

import {
  cohortsApi,
  conceptsApi,
  conceptSetsApi,
  dataSetApi
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  toggleIncludes,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActionsReact';
import {WorkspaceData} from 'app/utils/workspace-data';
import {NewDataSetModal} from 'app/views/new-dataset-modal';
import {
  Cohort,
  ConceptSet,
  DataSet,
  DataSetPreviewList,
  Domain,
  DomainValue,
  DomainValuePair,
  DomainValuesResponse,
  ValueSet,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import {ClrIcon} from 'app/components/icons';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';

export const styles = reactStyles({
  selectBoxHeader: {
    fontSize: '16px',
    height: '2rem',
    lineHeight: '2rem',
    paddingLeft: '0.55rem',
    paddingRight: '0.55rem',
    color: colors.blue[7],
    borderBottom: '1px solid #E5E5E5',
    display: 'flex',
    justifyContent: 'space-between',
    flexDirection: 'row'
  },

  listItem: {
    border: '0.5px solid #C3C3C3',
    margin: '.4rem',
    height: '1.5rem',
    display: 'flex'
  },

  listItemCheckbox: {
    height: 17,
    width: 17,
    marginLeft: 10,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: colors.green[2]
  },

  valueListItemCheckboxStyling: {
    height: 17,
    width: 17,
    marginLeft: 10,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: colors.green[2]
  },

  subheader: {
    fontWeight: 400,
    fontSize: '0.6rem',
    marginTop: '0.5rem',
    color: colors.purple[0]
  },

  previewButtonBox: {
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginTop: '2.675rem',
    marginBottom: '2rem'
  },

  previewDataHeaderBox: {
    display: 'flex',
    flexDirection: 'row',
    position: 'relative',
    lineHeight: 'auto',
    paddingTop: '0.5rem',
    paddingBottom: '0.5rem',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
    borderBottom: '1px solid #E5E5E5',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 'auto'
  },

  previewDataHeader: {
    height: '19px',
    width: '160px',
    color: colors.blue[7],
    fontFamily: 'Montserrat',
    fontSize: '16px',
    fontWeight: 600
  },

  warningMessage: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    height: '10rem'
  }
});

const ImmutableListItem: React.FunctionComponent <{
  name: string, onChange: Function, checked: boolean}> = ({name, onChange, checked}) => {
    return <div style={styles.listItem}>
      <input type='checkbox' value={name} onChange={() => onChange()}
             style={styles.listItemCheckbox} checked={checked}/>
      <div style={{lineHeight: '1.5rem'}}>{name}</div>
    </div>;
  };

const Subheader = (props) => {
  return <div style={{...styles.subheader, ...props.style}}>{props.children}</div>;
};

export const ValueListItem: React.FunctionComponent <
  {domainValue: DomainValue, onChange: Function, checked: boolean}> =
  ({domainValue, onChange, checked}) => {
    return <div style={{display: 'flex', color: 'black', height: '1.2rem'}}>
      <input type='checkbox' value={domainValue.value} onChange={() => onChange()}
             style={styles.valueListItemCheckboxStyling} checked={checked}/>
      <div style={{lineHeight: '1.5rem', wordWrap: 'break-word'}}>{domainValue.value}</div>
    </div>;
  };

interface Props {
  workspace: WorkspaceData;
  urlParams: any;
}

interface State {
  cohortList: Cohort[];
  conceptSetList: ConceptSet[];
  creatingConceptSet: boolean;
  dataSet: DataSet;
  dataSetTouched: boolean;
  includesAllParticipants: boolean;
  loadingResources: boolean;
  openSaveModal: boolean;
  previewList: Array<DataSetPreviewList>;
  previewDataLoading: boolean;
  selectedCohortIds: number[];
  selectedConceptSetIds: number[];
  selectedPreviewDomain: string;
  selectedValues: DomainValuePair[];
  valueSets: ValueSet[];
  valuesLoading: boolean;
  selectAll: boolean;
}

const DataSetPage = fp.flow(withCurrentWorkspace(), withUrlParams())(
  class extends React.Component<Props, State> {
    dt: any;
    constructor(props) {
      super(props);
      this.state = {
        cohortList: [],
        conceptSetList: [],
        creatingConceptSet: false,
        dataSet: undefined,
        dataSetTouched: false,
        includesAllParticipants: false,
        loadingResources: true,
        openSaveModal: false,
        previewList: [],
        previewDataLoading: false,
        selectedCohortIds: [],
        selectedConceptSetIds: [],
        selectedPreviewDomain: '',
        selectedValues: [],
        valueSets: [],
        valuesLoading: false,
        selectAll: false
      };
    }

    get editing() {
      return this.props.urlParams.dataSetId !== undefined;
    }

    async componentDidMount() {
      const {namespace, id} = this.props.workspace;
      const allPromises = [];
      allPromises.push(this.loadResources());
      if (this.editing) {
        allPromises.push(dataSetApi().getDataSet(
          namespace, id, this.props.urlParams.dataSetId).then((response) => {
            this.setState({
              dataSet: response,
              includesAllParticipants: response.includesAllParticipants,
              selectedConceptSetIds: response.conceptSets.map(cs => cs.id),
              selectedCohortIds: response.cohorts.map(c => c.id),
              selectedValues: response.values,
              valuesLoading: true,
            });
            return response;
          }));
        const [, dataSet] = await Promise.all(allPromises);
        // We can only run this command once both the data set fetch and the
        // load resources have concluded. However, we want those to happen in
        // parallel, and one is conditional, so we add them to an array to await
        // and only run once both have finished.
        this.getValuesList(this.getDomainsFromConceptIds(dataSet.conceptSets.map(cs => cs.id)))
          .then(valueSets => this.setState({valueSets: valueSets, valuesLoading: false}));
      }
    }

    async loadResources(): Promise<void> {
      try {
        const {namespace, id} = this.props.workspace;
        const [conceptSets, cohorts] = await Promise.all([
          conceptSetsApi().getConceptSetsInWorkspace(namespace, id),
          cohortsApi().getCohortsInWorkspace(namespace, id)]);
        this.setState({conceptSetList: conceptSets.items, cohortList: cohorts.items,
          loadingResources: false});
        return Promise.resolve();
      } catch (error) {
        console.error(error);
        return Promise.resolve();
      }
    }

    getDomainsFromConceptIds(selectedConceptSetIds: number[]): Domain[] {
      const {conceptSetList} = this.state;
      return fp.uniq(conceptSetList.filter((conceptSet: ConceptSet) =>
        selectedConceptSetIds.includes(conceptSet.id))
        .map((conceptSet: ConceptSet) => conceptSet.domain));
    }

    async getValuesList(domains: Domain[]): Promise<ValueSet[]> {
      const {namespace, id} = this.props.workspace;
      const valueSets = fp.zipWith((domain: Domain, valueSet: DomainValuesResponse) =>
          ({domain: domain, values: valueSet}),
        domains,
        await Promise.all(domains.map((domain) =>
          conceptsApi().getValuesFromDomain(namespace, id, domain.toString()))));
      return valueSets;
    }

    select(resource: ConceptSet | Cohort, rtype: ResourceType): void {
      this.setState({dataSetTouched: true});
      if (rtype === ResourceType.CONCEPT_SET) {
        const {valueSets, selectedValues} = this.state;
        const origSelected = this.state.selectedConceptSetIds;
        const newSelectedConceptSets =
          toggleIncludes(resource.id, origSelected)as unknown as number[];
        const currentDomains = this.getDomainsFromConceptIds(newSelectedConceptSets);
        const origDomains = valueSets.map(valueSet => valueSet.domain);
        const newDomains = fp.without(origDomains, currentDomains) as unknown as Domain[];
        const removedDomains = fp.without(currentDomains, origDomains);
        const updatedValueSets =
          valueSets.filter(valueSet => !(fp.contains(valueSet.domain, removedDomains)));
        const updatedSelectedValues =
          selectedValues.filter(selectedValue =>
            !fp.contains(selectedValue.domain, removedDomains));
        this.setState({
          selectedConceptSetIds: newSelectedConceptSets,
          selectedValues: updatedSelectedValues});
        if (newDomains.length > 0) {
          this.setState({valuesLoading: true});
          this.getValuesList(newDomains)
            .then(newValueSets => this.setState({
              valueSets: updatedValueSets.concat(newValueSets),
              valuesLoading: false
            }));
        } else {
          this.setState({valueSets: updatedValueSets});
        }
      } else {
        this.setState({selectedCohortIds: toggleIncludes(resource.id,
          this.state.selectedCohortIds) as unknown as number[]});
      }
    }

    selectDomainValue(domain: Domain, domainValue: DomainValue): void {
      const valueSets = this.state.valueSets
          .filter(value => value.domain === domain)
          .map(valueSet => valueSet.values.items)[0];
      const origSelected = this.state.selectedValues;
      const selectObj = {domain: domain, value: domainValue.value};
      let valuesSelected = [];
      if (fp.some(selectObj, origSelected)) {
        valuesSelected = fp.remove((dv) => dv.domain === selectObj.domain
            && dv.value === selectObj.value, origSelected);

      } else {
        valuesSelected = (origSelected).concat(selectObj);
      }
      // Sort the values selected as per the order display rather than appending top end
      valuesSelected = valuesSelected.sort((a, b) =>
          valueSets.findIndex(({value}) => a.value === value) -
          valueSets.findIndex(({value}) => b.value === value));
      this.setState({selectedValues: valuesSelected, dataSetTouched: true});
    }

    selectAllValues() {
      if (this.state.selectAll) {
        this.setState({
          selectedValues: [],
          selectAll: !this.state.selectAll});
        return;
      }

      const allValuesSelected = [];
      this.state.valueSets.map(valueSet => {
        valueSet.values.items.map(value => {
          allValuesSelected.push({domain: valueSet.domain, value: value.value});
        });
      });
      this.setState({selectedValues: allValuesSelected, selectAll: !this.state.selectAll});
    }

    disableSave() {
      return !this.state.selectedConceptSetIds || this.state.selectedConceptSetIds.length === 0 ||
          ((!this.state.selectedCohortIds || this.state.selectedCohortIds.length === 0)
                && !this.state.includesAllParticipants) ||
            !this.state.selectedValues || this.state.selectedValues.length === 0;
    }

    getDataTableValue(data) {
      // convert data model from api :
      // [{value[0]: '', queryValue: []}, {value[1]: '', queryValue: []}]
      // to compatible with DataTable
      // {value[0]: queryValue[0], value[1]: queryValue[1]}

      const tableData = fp.flow(
        fp.map(({value, queryValue}) => fp.map(v => [value, v], queryValue)),
        fp.unzip,
        fp.map(fp.fromPairs)
      )(data);
      return tableData;
    }

    async getPreviewList() {
      this.setState({previewList: [], previewDataLoading: true});
      const {namespace, id} = this.props.workspace;
      const request = {
        name: '',
        description: '',
        conceptSetIds: this.state.selectedConceptSetIds,
        includesAllParticipants: this.state.includesAllParticipants,
        cohortIds: this.state.selectedCohortIds,
        values: this.state.selectedValues
      };
      try {
        const dataSetPreviewResp = await dataSetApi().previewQuery(namespace, id, request);
        this.setState({
          previewList: dataSetPreviewResp.domainValue,
          selectedPreviewDomain: dataSetPreviewResp.domainValue[0].domain
        });
      } catch (ex) {
        console.error(ex);
      } finally {
        this.setState({previewDataLoading: false});
      }
    }

    isEllipsisActive(text) {
      if (this.dt) {
        const columnIndex = this.dt.props.children.findIndex(child => child.key === text);
        const columnTitlesDOM = document.getElementsByClassName('p-column-title');
        if (columnTitlesDOM && columnTitlesDOM.item(columnIndex)) {
          const element = columnTitlesDOM.item(columnIndex).children[0] as HTMLElement;
          if (element.offsetWidth < element.scrollWidth) {
            return false;
          }
        }
      }
      return true;
    }

    getHeaderValue(value) {
      const text = value.value;
      const dataTestId = 'data-test-id-' + text;
      return <TooltipTrigger data-test-id={dataTestId} side='top' content={text}
                             disabled={this.isEllipsisActive(text)}>
        <div style={{overflow: 'hidden', textOverflow: 'ellipsis'}}>
          {text}
        </div>
      </TooltipTrigger>;
    }


    renderPreviewDataTable() {
      const filteredPreviewData =
          this.state.previewList.filter(
            preview => fp.contains(preview.domain, this.state.selectedPreviewDomain))[0];
      return <DataTable ref={el => this.dt = el} key={this.state.selectedPreviewDomain}
                        scrollable={true} style={{width: '100%'}}
                        value={this.getDataTableValue(filteredPreviewData.values)}>
        {filteredPreviewData.values.map(value =>
          <Column key={value.value} header={this.getHeaderValue(value)}
                  headerStyle={{textAlign: 'left', width: '5rem'}} style={{width: '5rem'}}
                  field={value.value}/>
        )}
      </DataTable>;
    }

    updateDataSet() {
      const {namespace, id} = this.props.workspace;
      const {dataSet} = this.state;
      const request = {
        name: dataSet.name,
        description: dataSet.description,
        includesAllParticipants: this.state.includesAllParticipants,
        conceptSetIds: this.state.selectedConceptSetIds,
        cohortIds: this.state.selectedCohortIds,
        values: this.state.selectedValues,
        etag: dataSet.etag
      };
      dataSetApi().updateDataSet(namespace, id, dataSet.id, request)
        .then(() => window.history.back());
    }

    get isRefreshPreviewDisabled() {
      return this.disableSave() || this.state.previewList.length === 0;
    }

    render() {
      const {namespace, id} = this.props.workspace;
      const wsPathPrefix = 'workspaces/' + namespace + '/' + id;
      const cohortsPath = wsPathPrefix + '/cohorts';
      const conceptSetsPath = wsPathPrefix + '/concepts/sets';
      const {
        dataSet,
        dataSetTouched,
        includesAllParticipants,
        loadingResources,
        openSaveModal,
        previewDataLoading,
        previewList,
        selectedCohortIds,
        selectedConceptSetIds,
        selectedPreviewDomain,
        selectedValues,
        valuesLoading,
        valueSets
      } = this.state;
      return <React.Fragment>
        <FadeBox style={{marginTop: '1rem'}}>
          <h2 style={{marginTop: 0}}>Datasets{this.editing &&
            dataSet !== undefined && ' - ' + dataSet.name}</h2>
          <div style={{color: '#000000', fontSize: '14px'}}>Build a dataset by selecting the
            variables and values for one or more of your cohorts. Then export the completed dataset
            to Notebooks where you can perform your analysis</div>
          <div style={{display: 'flex'}}>
            <div style={{width: '33%'}}>
              <h2>Select Cohorts</h2>
              <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
                <div style={styles.selectBoxHeader}>
                  Cohorts
                    <a data-test-id='cohorts-link' href={cohortsPath}
                      onClick={e => {
                        navigateAndPreventDefaultIfNoKeysPressed(e, cohortsPath);
                      }}>
                      <ClrIcon shape='plus-circle' class='is-solid' size={16}
                           style={{fill: '#216FB4'}}/>
                           {/*todo: move ^ to a style?*/}
                    </a>
                </div>
                <div style={{height: '10rem', overflowY: 'auto'}}>
                  <Subheader>Prepackaged Cohorts</Subheader>
                  <ImmutableListItem name='All AoU Participants' checked={includesAllParticipants}
                                     onChange={
                                       () => this.setState({
                                         includesAllParticipants: !includesAllParticipants,
                                         dataSetTouched: true
                                       })}/>
                  <Subheader>Workspace Cohorts</Subheader>
                  {!loadingResources && this.state.cohortList.map(cohort =>
                    <ImmutableListItem key={cohort.id} name={cohort.name}
                                      data-test-id='cohort-list-item'
                                      checked={selectedCohortIds.includes(cohort.id)}
                                      onChange={
                                        () => this.select(cohort, ResourceType.COHORT)}/>
                    )
                  }
                  {loadingResources && <Spinner style={{position: 'relative', top: '2rem',
                    left: '10rem'}}/>}
                </div>
              </div>
            </div>
            <div style={{marginLeft: '1.5rem', width: '65%'}}>
              <h2>Select Concept Sets</h2>
              <div style={{display: 'flex', backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
                <div style={{width: '60%', borderRight: '1px solid #E5E5E5'}}>
                  <div style={styles.selectBoxHeader}>
                    Concept Sets
                    <a data-test-id='concept-sets-link' href={conceptSetsPath}
                       onClick={e => {
                         navigateAndPreventDefaultIfNoKeysPressed(e, conceptSetsPath);
                       }}>
                    <ClrIcon shape='plus-circle' class='is-solid' size={16}
                             style={{fill: '#216FB4'}}/>
                    </a>
                  </div>
                  <div style={{height: '10rem', overflowY: 'auto'}}>
                    {!loadingResources && this.state.conceptSetList.map(conceptSet =>
                        <ImmutableListItem key={conceptSet.id} name={conceptSet.name}
                                          data-test-id='concept-set-list-item'
                                          checked={selectedConceptSetIds.includes(conceptSet.id)}
                                          onChange={
                                            () => this.select(conceptSet, ResourceType.CONCEPT_SET)
                                          }/>)
                    }
                    {loadingResources && <Spinner style={{position: 'relative', top: '2rem',
                      left: '10rem'}}/>}
                  </div>
                </div>
                <div style={{width: '40%'}}>
                  <div style={styles.selectBoxHeader}>
                    <div>
                      Values
                    </div>
                    <Clickable data-test-id='select-all'
                               style={{marginLeft: 'auto'}}
                               onClick={() => this.selectAllValues()}>
                      Select All
                    </Clickable>
                  </div>
                  <div style={{height: '10rem', overflowY: 'auto'}}>
                    {valuesLoading && <Spinner style={{position: 'relative',
                      top: '2rem', left: 'calc(50% - 36px)'}}/>}
                    {valueSets.map(valueSet =>
                      <div key={valueSet.domain} style={{marginLeft: '0.5rem'}}>
                        <div style={{fontSize: '13px', fontWeight: 600, color: 'black'}}>
                          {fp.capitalize(valueSet.domain.toString())}
                        </div>
                        {valueSet.values.items.map(domainValue =>
                          <ValueListItem data-test-id='value-list-items'
                            key={domainValue.value} domainValue={domainValue}
                            onChange={() => this.selectDomainValue(valueSet.domain, domainValue)}
                            checked={fp.some({domain: valueSet.domain, value: domainValue.value},
                              selectedValues)}/>
                        )}
                      </div>)
                    }
                  </div>
                </div>
              </div>
            </div>
          </div>
        </FadeBox>
        <FadeBox style={{marginTop: '1rem'}}>
          <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
            <div style={styles.previewDataHeaderBox}>
              <div style={{display: 'flex', flexDirection: 'column'}}>
              <div style={{display: 'flex', alignItems: 'flex-end'}}>
                <div style={styles.previewDataHeader}>
                  Preview Data Set
                </div>
                <Clickable data-test-id='refresh-preview-clickable-text'
                           disabled={this.isRefreshPreviewDisabled}
                           onClick={() => this.getPreviewList()}
                           style={{fontSize: '12px',
                             cursor: this.isRefreshPreviewDisabled ? 'not-allowed' : 'pointer',
                             fontWeight: 600, lineHeight: '15px',
                             color: this.isRefreshPreviewDisabled ? colors.gray[4] : colors.blue[0]
                           }}>
                    Refresh Preview
                  </Clickable>
              </div>
              <div style={{color: '#000000', fontSize: '14px'}}>A visualization of your data table
                based on the variable and value you selected above
              </div>
              </div>
              <Button data-test-id='save-button' style={{marginRight: '0.5rem'}}
                      onClick={this.editing ? () => this.updateDataSet() :
                () => this.setState({openSaveModal: true})}
                disabled={this.disableSave() || (this.editing && !dataSetTouched)}>
                {this.editing ? 'UPDATE DATA SET' : 'SAVE DATA SET'}
              </Button>
            </div>
            {previewDataLoading && <div style={styles.warningMessage}>
              <Spinner style={{position: 'relative', top: '2rem'}} />
              <div style={{top: '3rem', position: 'relative'}}>
                It may take up to few minutes to load the data
              </div>
            </div>}
            {previewList.length > 0 &&
              <div style={{display: 'flex', flexDirection: 'column'}}>
                <div style={{display: 'flex', flexDirection: 'row'}}>
                  {previewList.map(previewRow =>
                     <Clickable key={previewRow.domain}
                               onClick={() =>
                                 this.setState({selectedPreviewDomain: previewRow.domain})}
                               style={{
                                 lineHeight: '32px', fontSize: '18px',
                                 fontWeight: (selectedPreviewDomain === previewRow.domain)
                                     ? 600 : 400,
                                 textDecoration:
                                    (selectedPreviewDomain === previewRow.domain) ? 'underline' : ''
                               }}>
                       <div key={previewRow.domain}
                           style={{
                             marginLeft: '0.2rem', color: colors.blue[0], paddingRight: '3rem'}}>
                         {previewRow.domain}
                       </div>
                     </Clickable>
                  )}
                </div>
                {this.renderPreviewDataTable()}
              </div>
            }
            {previewList.length === 0 && !previewDataLoading &&
              <div style={styles.previewButtonBox}>
                <div style={{color: colors.gray[4], fontSize: '20px', fontWeight: 400}}>
                  Select cohorts, concept sets, and values above to generate a preview table
                </div>
                <Button data-test-id='preview-button' disabled={this.disableSave()}
                        style={{marginTop: '0.5rem', height: '1.8rem', width: '6.5rem'}}
                        onClick={() => this.getPreviewList()}>
                  Preview Table
                </Button>
              </div>
            }
          </div>
        </FadeBox>
        {openSaveModal && <NewDataSetModal includesAllParticipants={includesAllParticipants}
                                           selectedConceptSetIds={selectedConceptSetIds}
                                           selectedCohortIds={selectedCohortIds}
                                           selectedValues={selectedValues}
                                           workspaceNamespace={namespace}
                                           workspaceId={id}
                                           closeFunction={() => {
                                             this.setState({openSaveModal: false});
                                           }}
        />}
      </React.Fragment>;
    }
  });

export {
  DataSetPage,
  Props as DataSetPageProps
};

@Component({
  template: '<div #root></div>'
})
export class DataSetPageComponent extends ReactWrapperBase {
  constructor() {
    super(DataSetPage, []);
  }
}
