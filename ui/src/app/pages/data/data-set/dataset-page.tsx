import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexRow} from 'app/components/flex';
import {HelpSidebar} from 'app/components/help-sidebar';
import {ClrIcon} from 'app/components/icons';
import {CheckBox} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {CircleWithText} from 'app/icons/circleWithText';
import {NewDataSetModal} from 'app/pages/data/data-set/new-dataset-modal';
import {
  cohortsApi,
  conceptsApi,
  conceptSetsApi,
  dataSetApi
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {colorWithWhiteness} from 'app/styles/colors';
import {
  formatDomain,
  formatDomainString,
  ReactWrapperBase,
  toggleIncludes,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {ResourceType} from 'app/utils/resourceActions';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {
  Cohort,
  ConceptSet,
  DataSet,
  DataSetPreviewRequest,
  DataSetPreviewValueList,
  Domain,
  DomainValue,
  DomainValuePair,
  DomainValuesResponse,
  ErrorResponse,
  PrePackagedConceptSetEnum,
  Surveys,
  ValueSet,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';

export const styles = {
  selectBoxHeader: {
    fontSize: '16px',
    height: '2rem',
    lineHeight: '2rem',
    paddingRight: '0.55rem',
    color: colors.primary,
    borderBottom: `1px solid ${colors.light}`,
    display: 'flex',
    justifyContent: 'space-between',
    flexDirection: 'row'
  } as React.CSSProperties,
  listItem: {
    border: `0.5px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
    margin: '.4rem .4rem .4rem .55rem',
    height: '1.5rem',
    display: 'flex'
  },

  listItemCheckbox: {
    height: 17,
    width: 17,
    marginLeft: 10,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: colors.success
  },

  valueListItemCheckboxStyling: {
    height: 17,
    width: 17,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: colors.success
  },

  subheader: {
    fontWeight: 400,
    fontSize: '0.6rem',
    marginTop: '0.5rem',
    paddingLeft: '0.55rem',
    color: colors.primary
  },

  previewButtonBox: {
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginTop: '2.675rem',
    marginBottom: '2rem'
  } as React.CSSProperties,

  previewDataHeaderBox: {
    display: 'flex',
    flexDirection: 'row',
    position: 'relative',
    lineHeight: 'auto',
    paddingTop: '0.5rem',
    paddingBottom: '0.5rem',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
    borderBottom: `1px solid ${colors.light}`,
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 'auto'
  } as React.CSSProperties,

  previewDataHeader: {
    height: '19px',
    width: 'auto',
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '16px',
    fontWeight: 600,
    marginBottom: '1rem',
    paddingRight: '1.5rem',
    justifyContent: 'space-between',
    display: 'flex'
  },

  warningMessage: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    height: '10rem',
    marginTop: '2rem',
    fontSize: 18,
    fontWeight: 600,
    color: colorWithWhiteness(colors.dark, 0.6)
  } as React.CSSProperties,

  selectAllContainer: {
    marginLeft: 'auto',
    width: '5rem',
    display: 'flex',
    alignItems: 'center'
  },
  previewLink: {
    marginTop: '0.5rem',
    height: '1.8rem',
    width: '6.5rem',
    color: colors.secondary
  },
  footer: {
    display: 'block',
    padding: '20px',
    height: '60px',
    width: '100%'
  },
  stickyFooter: {
    backgroundColor: colors.white,
    borderTop: `1px solid ${colors.light}`,
    textAlign: 'right',
    padding: '3px 55px 50px 20px',
    position: 'fixed',
    left: '0',
    bottom: '0',
    height: '60px',
    width: '100%'
  } as React.CSSProperties,
  selectDomainForPreviewButton: (selected) => {
    return {
      marginLeft: '0.2rem',
      color: colors.accent,
      marginRight: '0.25rem',
      paddingBottom: '0.25rem',
      width: '7rem',
      borderBottom: (selected) ? '4px solid ' + colors.accent : '',
      fontWeight: (selected) ? 600 : 400,
      fontSize: '18px',
      display: 'flex',
      justifyContent: 'center',
      lineHeight: '19px'
    };
  }
};

const stylesFunction = {
  plusIconColor: (disabled) => {
    return {
      fill: disabled ? colorWithWhiteness(colors.dark, 0.4) : colors.accent
    };
  }
};

const ImmutableListItem: React.FunctionComponent <{
  name: string, onChange: Function, checked: boolean}> = ({name, onChange, checked}) => {
    return <div style={styles.listItem}>
      <input type='checkbox' value={name} onChange={() => onChange()}
             style={styles.listItemCheckbox} checked={checked}/>
      <div style={{lineHeight: '1.5rem', color: colors.primary}}>{name}</div>
    </div>;
  };

const Subheader = (props) => {
  return <div style={{...styles.subheader, ...props.style}}>{props.children}</div>;
};

export const ValueListItem: React.FunctionComponent <
  {domainValue: DomainValue, onChange: Function, checked: boolean}> =
  ({domainValue, onChange, checked}) => {
    return <div style={{display: 'flex', height: '1.2rem', marginLeft: '0.55rem'}}>
      <input type='checkbox' value={domainValue.value} onChange={() => onChange()}
             style={styles.valueListItemCheckboxStyling} checked={checked}/>
      <div style={{lineHeight: '1.5rem', wordWrap: 'break-word', color: colors.primary}}>
        {domainValue.value}</div>
    </div>;
  };

const plusLink = (dataTestId: string, path: string, disable?: boolean) => {
  return <TooltipTrigger data-test-id='plus-icon-tooltip' disabled={!disable}
                         content='Requires Owner or Writer permission'>
    <Clickable disabled={disable} data-test-id={dataTestId} href={path}
            onClick={e => {navigateAndPreventDefaultIfNoKeysPressed(e, path); }}>
    <ClrIcon shape='plus-circle' class='is-solid' size={16}
             style={stylesFunction.plusIconColor(disable)}/>
  </Clickable></TooltipTrigger>;
};

const BoxHeader = ({text= '', header =  '', subHeader = '', style= {}, ...props}) => {
  return  <div style={styles.selectBoxHeader}>
    <div style={{display: 'flex', marginLeft: '0.2rem'}}>
      <CircleWithText text={text} width='23.78px' height='23.78px'
                      style={{fill: colorWithWhiteness(colors.primary, 0.5), marginTop: '0.5rem'}}/>
      <label style={{marginLeft: '0.5rem', color: colors.primary, display: 'flex', ...style}}>
        <div style={{fontWeight: 600, marginRight: '0.3rem'}}>{header}</div>
        ({subHeader})
      </label>
    </div>
    {props.children}
  </div>;
};

interface DataSetPreviewInfo {
  isLoading: boolean;
  values?: Array<DataSetPreviewValueList>;
}

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
  prePackagedDemographics: boolean;
  prePackagedSurvey: boolean;
  loadingResources: boolean;
  openSaveModal: boolean;
  previewError: boolean;
  previewErrorText: string;
  previewList: Map<Domain, DataSetPreviewInfo>;
  selectedCohortIds: number[];
  selectedConceptSetIds: number[];
  selectedDomainValuePairs: DomainValuePair[];
  selectedPreviewDomain: Domain;
  valueSets: ValueSet[];
  valuesLoading: boolean;
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
        prePackagedDemographics: false,
        prePackagedSurvey: false,
        previewError: false,
        previewErrorText: '',
        previewList: new Map(),
        selectedCohortIds: [],
        selectedConceptSetIds: [],
        selectedPreviewDomain: Domain.CONDITION,
        selectedDomainValuePairs: [],
        valueSets: [],
        valuesLoading: false,
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
              selectedDomainValuePairs: response.domainValuePairs,
              valuesLoading: true,
            });
            if (response.prePackagedConceptSet === PrePackagedConceptSetEnum.BOTH) {
              this.setState({prePackagedSurvey: true, prePackagedDemographics: true});
            } else if (response.prePackagedConceptSet === PrePackagedConceptSetEnum.DEMOGRAPHICS) {
              this.setState({prePackagedDemographics: true});
            } else if (response.prePackagedConceptSet === PrePackagedConceptSetEnum.SURVEY) {
              this.setState({prePackagedSurvey: true});
            }
            return response;
          }));
        const [, dataSet] = await Promise.all(allPromises);
        // We can only run this command once both the dataset fetch and the
        // load resources have concluded. However, we want those to happen in
        // parallel, and one is conditional, so we add them to an array to await
        // and only run once both have finished.
        const domainList = this.getDomainsFromConceptIds(dataSet.conceptSets.map(cs => cs.id));
        if (dataSet.prePackagedConceptSet === PrePackagedConceptSetEnum.BOTH) {
          domainList.push(Domain.PERSON);
          domainList.push(Domain.SURVEY);
        } else if (dataSet.prePackagedConceptSet === PrePackagedConceptSetEnum.SURVEY) {
          domainList.push(Domain.SURVEY);
        } else if (dataSet.prePackagedConceptSet === PrePackagedConceptSetEnum.DEMOGRAPHICS) {
          domainList.push(Domain.PERSON);
        }
        this.getValuesList(fp.uniq(domainList))
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
      const domains = fp.uniq(conceptSetList.filter((conceptSet: ConceptSet) =>
        selectedConceptSetIds.includes(conceptSet.id))
        .map((conceptSet: ConceptSet) => conceptSet.domain));
      if (this.state.prePackagedSurvey) {
        domains.push(Domain.SURVEY);
      }
      if (this.state.prePackagedDemographics) {
        domains.push(Domain.PERSON);
      }
      return domains;
    }

    async getValuesList(domains: Domain[], survey?: Surveys): Promise<ValueSet[]> {
      const {namespace, id} = this.props.workspace;
      const valueSets = fp.zipWith((domain: Domain, valueSet: DomainValuesResponse) =>
          ({domain: domain, values: valueSet, survey: survey}),
        domains,
        await Promise.all(domains.map((domain) =>
          conceptsApi().getValuesFromDomain(namespace, id, domain.toString()))));
      return valueSets;
    }

    handlePrePackagedConceptSets(domain, selected) {
      const {valueSets, selectedDomainValuePairs} = this.state;
      if (!selected) {
        const updatedValueSets =
            valueSets.filter(valueSet => !(fp.contains(valueSet.domain, domain)));
        const updatedSelectedDomainValuePairs =
          selectedDomainValuePairs.filter(selectedDomainValuePair =>
                !fp.contains(selectedDomainValuePair.domain, domain));
        this.setState({valueSets: updatedValueSets, selectedDomainValuePairs: updatedSelectedDomainValuePairs});
        return;
      }
      const currentDomains = [];
      if (this.state.prePackagedDemographics) {
        currentDomains.push(Domain.PERSON);
      }
      if (this.state.prePackagedSurvey) {
        currentDomains.push(Domain.SURVEY);
      }
      const origDomains = valueSets.map(valueSet => valueSet.domain);
      const newDomains = fp.without(origDomains, currentDomains) as unknown as Domain[];

      this.setState({valuesLoading: true});
      this.getValuesList(newDomains)
        .then(newValueSets => this.setState({
          valueSets: valueSets.concat(newValueSets),
          valuesLoading: false
        }));
    }

    select(resource: ConceptSet | Cohort, rtype: ResourceType): void {
      this.setState({dataSetTouched: true});
      if (rtype === ResourceType.CONCEPT_SET) {
        const {valueSets, selectedDomainValuePairs} = this.state;
        const origSelected = this.state.selectedConceptSetIds;
        const newSelectedConceptSets =
          toggleIncludes(resource.id, origSelected)as unknown as number[];
        const currentDomains = this.getDomainsFromConceptIds(newSelectedConceptSets);
        const origDomains = valueSets.map(valueSet => valueSet.domain);
        const newDomains = fp.without(origDomains, currentDomains) as unknown as Domain[];
        const removedDomains = fp.without(currentDomains, origDomains);
        const updatedValueSets =
          valueSets.filter(valueSet => !(fp.contains(valueSet.domain, removedDomains)));
        const updatedSelectedDomainValuePairs =
          selectedDomainValuePairs.filter(selectedDomainValuePair =>
            !fp.contains(selectedDomainValuePair.domain, removedDomains));
        this.setState({
          selectedConceptSetIds: newSelectedConceptSets,
          selectedDomainValuePairs: updatedSelectedDomainValuePairs});
        if (newDomains.length > 0) {
          this.setState({valuesLoading: true});
          const cSet = resource as ConceptSet;
          if (cSet.survey != null ) {
            this.getValuesList(newDomains, cSet.survey)
                .then(newValueSets => this.setState({
                  valueSets: updatedValueSets.concat(newValueSets),
                  valuesLoading: false
                }));
          } else {
            this.getValuesList(newDomains)
                .then(newValueSets => this.setState({
                  valueSets: updatedValueSets.concat(newValueSets),
                  valuesLoading: false
                }));
          }
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
      const origSelected = this.state.selectedDomainValuePairs;
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
      this.setState({selectedDomainValuePairs: valuesSelected, dataSetTouched: true});
    }

    get selectAll() {
      return fp.isEmpty(this.state.selectedDomainValuePairs);
    }

    selectAllValues() {
      if (!this.selectAll) {
        this.setState({selectedDomainValuePairs: []});
        return;
      } else {
        const allValuesSelected = [];
        this.state.valueSets.map(valueSet => {
          valueSet.values.items.map(value => {
            allValuesSelected.push({domain: valueSet.domain, value: value.value});
          });
        });
        this.setState({selectedDomainValuePairs: allValuesSelected});
      }
    }

    get canWrite() {
      return WorkspacePermissionsUtil.canWrite(this.props.workspace.accessLevel);
    }

    disableSave() {
      return !this.state.selectedConceptSetIds || (this.state.selectedConceptSetIds.length === 0
          && !this.state.prePackagedDemographics && !this.state.prePackagedSurvey) ||
          ((!this.state.selectedCohortIds || this.state.selectedCohortIds.length === 0) &&
              !this.state.includesAllParticipants) || !this.state.selectedDomainValuePairs ||
          this.state.selectedDomainValuePairs.length === 0;
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

    getPrePackagedConceptSet() {
      let prePackagedConceptState = PrePackagedConceptSetEnum.NONE;
      if (this.state.prePackagedDemographics && this.state.prePackagedSurvey) {
        prePackagedConceptState = PrePackagedConceptSetEnum.BOTH;
      } else if (this.state.prePackagedSurvey) {
        prePackagedConceptState = PrePackagedConceptSetEnum.SURVEY;
      } else if (this.state.prePackagedDemographics) {
        prePackagedConceptState = PrePackagedConceptSetEnum.DEMOGRAPHICS;
      }
      return prePackagedConceptState;
    }

    async getPreviewList() {
      const {namespace, id} = this.props.workspace;
      const domains = fp.uniq(this.state.selectedDomainValuePairs.map(domainValue => domainValue.domain));
      const newPreviewList: Map<Domain, DataSetPreviewInfo> =
        new Map(domains.map<[Domain, DataSetPreviewInfo]>(domain => [domain, {isLoading: true, values: []}]));
      this.setState({
        previewList: newPreviewList,
        selectedPreviewDomain: domains[0]
      });
      domains.forEach(async domain => {
        const request: DataSetPreviewRequest = {
          domain: domain,
          conceptSetIds: this.state.selectedConceptSetIds,
          includesAllParticipants: this.state.includesAllParticipants,
          cohortIds: this.state.selectedCohortIds,
          prePackagedConceptSet: this.getPrePackagedConceptSet(),
          values: this.state.selectedDomainValuePairs.map(domainValue => domainValue.value)
        };
        try {
          const dataSetPreviewResp = await dataSetApi().previewDataSetByDomain(namespace, id, request);
          const newPreviewInformation = {
            isLoading: false,
            values: dataSetPreviewResp.values
          };
          this.setState({previewList: this.state.previewList.set(dataSetPreviewResp.domain, newPreviewInformation)});
        } catch (ex) {
          const exceptionResponse = await ex.json() as unknown as ErrorResponse;
          const errorText = this.generateErrorTextFromPreviewException(exceptionResponse, domain);
          this.setState({previewError: true, previewErrorText: errorText});
          console.error(ex);
        }
      });
    }

    // TODO: Move to using a response based error handling method, rather than a error based one
    generateErrorTextFromPreviewException(exceptionResponse: ErrorResponse,
      domain: Domain): string {
      switch (exceptionResponse.statusCode) {
        case 400:
          return exceptionResponse.message;
          break;
        case 404:
          return exceptionResponse.message;
          break;
        case 504:
          return `Query to load data from the All of Us Database timed out for domain:
                ${domain}. Please either try again or export dataset to a notebook to try
                there`;
          break;
        default:
          return `An unexpected error has occurred loading domain: ${domain}.
              Please submit a bug using the contact support button`;
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


    renderPreviewDataTableSection() {
      const filteredPreviewData =
          this.state.previewList.get(this.state.selectedPreviewDomain);
      return filteredPreviewData.values.length > 0 ?
        this.renderPreviewDataTable(filteredPreviewData) :
        this.renderPreviewDataTableSectionMessage(filteredPreviewData);
    }

    renderPreviewDataTable(filteredPreviewData: DataSetPreviewInfo) {
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

    renderPreviewDataTableSectionMessage(filteredPreviewData: DataSetPreviewInfo) {
      const domainDisplayed = formatDomain(this.state.selectedPreviewDomain);
      return <div style={styles.warningMessage}>
        {filteredPreviewData.isLoading ?
          <div>Generating preview for {domainDisplayed}</div> :
          <div>No values found for {domainDisplayed}</div>
        }
      </div>;
    }

    render() {
      const {namespace, id} = this.props.workspace;
      const pathPrefix = 'workspaces/' + namespace + '/' + id + '/data';
      const cohortsPath = pathPrefix + '/cohorts/build';
      const conceptSetsPath = pathPrefix + '/concepts';
      const {
        dataSet,
        dataSetTouched,
        includesAllParticipants,
        loadingResources,
        openSaveModal,
        prePackagedDemographics,
        prePackagedSurvey,
        previewError,
        previewErrorText,
        previewList,
        selectedCohortIds,
        selectedConceptSetIds,
        selectedPreviewDomain,
        selectedDomainValuePairs,
        valuesLoading,
        valueSets
      } = this.state;
      return <React.Fragment>
        <FadeBox style={{paddingTop: '1rem'}}>
          <h2 style={{paddingTop: 0, marginTop: 0}}>Datasets{this.editing &&
            dataSet !== undefined && ' - ' + dataSet.name}</h2>
          <div style={{color: colors.primary, fontSize: '14px'}}>Build a dataset by selecting the
            variables and values for one or more of your cohorts. Then export the completed Dataset
            to Notebooks where you can perform your analysis</div>
          <div style={{display: 'flex', paddingTop: '1rem'}}>
            <div style={{width: '33%', height: '80%'}}>
              <div style={{backgroundColor: 'white', border: `1px solid ${colors.light}`}}>
                <BoxHeader text='1' header='Select Cohorts' subHeader='Participants'>
                  {plusLink('cohorts-link', cohortsPath, !this.canWrite)}
                </BoxHeader>
                <div style={{height: '9rem', overflowY: 'auto'}}>
                  <Subheader>Prepackaged Cohorts</Subheader>
                  <ImmutableListItem name='All Participants' checked={includesAllParticipants}
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
                  {loadingResources && <Spinner style={{position: 'relative', top: '0.5rem',
                    left: '7rem'}}/>}
                </div>
              </div>
            </div>
            <div style={{marginLeft: '1.5rem', width: '70%'}}>
              <div style={{display: 'flex', backgroundColor: colors.white,
                border: `1px solid ${colors.light}`}}>
                <div style={{width: '60%', borderRight: `1px solid ${colors.light}`}}>
                    <BoxHeader text='2' header='Select Concept Sets' subHeader='Rows'
                               style={{paddingRight: '1rem'}}>
                      {plusLink('concept-sets-link', conceptSetsPath, !this.canWrite)}
                    </BoxHeader>
                  <div style={{height: '9rem', overflowY: 'auto'}}>
                    <Subheader>Prepackaged Concept Sets</Subheader>
                    <ImmutableListItem name='Demographics' checked={prePackagedDemographics}
                                       onChange={
                                         () => {
                                           this.setState({
                                             prePackagedDemographics: !prePackagedDemographics,
                                             dataSetTouched: true
                                           }, () => {this.handlePrePackagedConceptSets(
                                             Domain.PERSON, !prePackagedDemographics); }); }}/>
                    <ImmutableListItem name='All Surveys' checked={prePackagedSurvey}
                                       onChange={
                                         () => {
                                           this.setState({
                                             prePackagedSurvey: !prePackagedSurvey,
                                             dataSetTouched: true
                                           }, () => {this.handlePrePackagedConceptSets(
                                             Domain.SURVEY, !prePackagedSurvey); });

                                         }}/>
                    <Subheader>Workspace Concept Sets</Subheader>
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
                <div style={{width: '55%'}}>
                    <BoxHeader text='3' header='Select Values' subHeader='Columns'>
                    <div style={styles.selectAllContainer}>
                      <CheckBox style={{height: 17, width: 17}}
                                disabled={fp.isEmpty(valueSets)}
                                data-test-id='select-all'
                                onChange={() => this.selectAllValues()}
                                checked={!this.selectAll} />
                      <div style={{marginLeft: '0.25rem', fontSize: '13px', lineHeight: '17px'}}>
                        {this.selectAll ? 'Select All' : 'Deselect All'}
                      </div>
                    </div>
                  </BoxHeader>
                  <div style={{height: '9rem', overflowY: 'auto'}}>
                    {valuesLoading && <Spinner style={{position: 'relative',
                      top: '2rem', left: 'calc(50% - 36px)'}}/>}
                    {valueSets.map(valueSet =>
                      <div key={valueSet.domain}>
                        <Subheader style={{fontWeight: 'bold'}}>
                          {valueSet.survey ? 'Survey' : formatDomain(valueSet.domain)}
                        </Subheader>
                        {valueSet.values.items.map(domainValue =>
                          <ValueListItem data-test-id='value-list-items'
                            key={domainValue.value} domainValue={domainValue}
                            onChange={() => this.selectDomainValue(valueSet.domain, domainValue)}
                            checked={fp.some({domain: valueSet.domain, value: domainValue.value},
                              selectedDomainValuePairs)}/>
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
          <div style={{backgroundColor: 'white', border: `1px solid ${colors.light}`}}>
            <div style={styles.previewDataHeaderBox}>
              <div style={{display: 'flex', flexDirection: 'column'}}>
              <div style={{display: 'flex', alignItems: 'flex-end'}}>
                <div style={styles.previewDataHeader}>
                  <div>
                    <CircleWithText text={4} width='23.78px' height='23.78px'
                      style={{marignTop: '0.3rem', fill: colorWithWhiteness(colors.primary, 0.5)}}/>
                  </div>
                  <label style={{marginLeft: '0.5rem', color: colors.primary}}>
                    Preview Dataset
                  </label>
                </div>
                <div style={{color: colors.primary, fontSize: '14px', width: '60%'}}>
                  A visualization of your data table based on concept sets
                  and values you selected above. Once complete, export for analysis
                </div>
              </div>
              </div>
              <Clickable data-test-id='preview-button' style={{
                marginTop: '0.5rem',
                cursor: this.disableSave() ? 'not-allowed' : 'pointer', height: '1.8rem',
                width: '6.5rem', color: this.disableSave() ? colorWithWhiteness(colors.dark, 0.6) :
                  colors.accent}} disabled={this.disableSave()}
                onClick={() => this.getPreviewList()}>
                  View Preview Table
              </Clickable>
            </div>
            {fp.toPairs(previewList).length > 0 &&
              <div style={{display: 'flex', flexDirection: 'column'}}>
                <div style={{display: 'flex', flexDirection: 'row', paddingTop: '0.5rem'}}>
                  {fp.toPairs(previewList).map((value) => {
                    const domain: string = value[0];
                    const previewRow: DataSetPreviewInfo = value[1];
                    return <TooltipTrigger key={domain}
                                           content={
                                             'Preview for domain '
                                             + formatDomainString(domain)
                                             + ' is still loading. It may take up to one minute'
                                           }
                                           disabled={!previewRow.isLoading}
                                           side='top'>
                      <Clickable
                                 disabled={previewRow.isLoading}
                                 onClick={() =>
                                   this.setState({selectedPreviewDomain: Domain[domain]})}
                                 style={styles.selectDomainForPreviewButton(selectedPreviewDomain === Domain[domain])}>
                        <FlexRow style={{alignItems: 'center'}}>
                          {Domain[domain] === Domain.OBSERVATION ? 'SURVEY' : domain.toString()}
                          {previewRow.isLoading &&
                          <Spinner style={{marginLeft: '4px', height: '18px', width: '18px'}}/>}
                        </FlexRow>
                      </Clickable>
                    </TooltipTrigger>;
                  })}
                </div>
                {this.renderPreviewDataTableSection()}
              </div>
            }
            {fp.entries(previewList).length === 0 &&
              <div style={styles.previewButtonBox}>
                <div style={{color: colorWithWhiteness(colors.dark, 0.6),
                  fontSize: '20px', fontWeight: 400}}>
                  Select cohorts, concept sets, and values above to generate a preview table
                </div>
              </div>
            }
          </div>
        </FadeBox>
        <div>
          <div style={styles.footer} />
          <div style={styles.stickyFooter}>
            <TooltipTrigger data-test-id='save-tooltip'
              content='Requires Owner or Writer permission' disabled={this.canWrite}>
            <Button style={{marginBottom: '2rem'}} data-test-id='save-button'
              onClick ={() => this.setState({openSaveModal: true})}
              disabled={this.disableSave() || !this.canWrite}>
                {this.editing ? !(dataSetTouched && this.canWrite) ? 'Analyze' :
                  'Update and Analyze' : 'Save and Analyze'}
            </Button>
            </TooltipTrigger>
          </div>
        </div>
        {openSaveModal && <NewDataSetModal includesAllParticipants={includesAllParticipants}
                                           selectedConceptSetIds={selectedConceptSetIds}
                                           selectedCohortIds={selectedCohortIds}
                                           selectedDomainValuePairs={selectedDomainValuePairs}
                                           workspaceNamespace={namespace}
                                           workspaceId={id}
                                           prePackagedConceptSet={this.getPrePackagedConceptSet()}
                                           dataSet={dataSet ? dataSet : undefined}
                                           closeFunction={() => {
                                             this.setState({openSaveModal: false});
                                           }}
        />}
        {previewError && <Modal>
          <ModalTitle>Error Loading Dataset Preview</ModalTitle>
          <ModalBody>{previewErrorText}</ModalBody>
          <ModalFooter>
            <Button type='secondary' onClick={() => {this.setState({previewError: false}); }}>
              Close
            </Button>
          </ModalFooter>
        </Modal>}
        <HelpSidebar location='datasetBuilder' />
      </React.Fragment>;
    }
  });

export {
  DataSetPage,
  Props as DataSetPageProps
};

@Component({
  template: '<div #root style="position: relative; margin-right: 45px;"></div>'
})
export class DataSetPageComponent extends ReactWrapperBase {
  constructor() {
    super(DataSetPage, []);
  }
}
