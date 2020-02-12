import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button, Clickable, Link} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {CheckBox} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {CircleWithText} from 'app/icons/circleWithText';
import {NewDataSetModal} from 'app/pages/data/data-set/new-dataset-modal';
import {cohortsApi, conceptSetsApi, dataSetApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  formatDomain,
  formatDomainString,
  reactStyles,
  ReactWrapperBase,
  toggleIncludes,
  withCurrentWorkspace,
  withUrlParams,
  withUserProfile
} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {currentWorkspaceStore, navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {apiCallWithGatewayTimeoutRetries} from 'app/utils/retry';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {openZendeskWidget} from 'app/utils/zendesk';
import {
  BillingStatus,
  Cohort,
  ConceptSet,
  DataDictionaryEntry,
  DataSet,
  DataSetPreviewRequest,
  DataSetPreviewValueList,
  Domain,
  DomainValue,
  DomainValuePair,
  ErrorResponse,
  PrePackagedConceptSetEnum,
  Profile,
  ValueSet,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';

export const styles = reactStyles({
  dataDictionaryHeader: {
    fontSize: '16px',
    color: colors.primary,
    textTransform: 'uppercase'
  },

  dataDictionarySubheader: {
    fontSize: '13px',
    fontWeight: 600,
    color: colors.primary,
    paddingTop: '0.5rem'
  },

  dataDictionaryText: {
    color: colors.primary,
    fontSize: '13px',
    lineHeight: '20px'
  },

  selectBoxHeader: {
    fontSize: '16px',
    height: '2rem',
    lineHeight: '2rem',
    paddingRight: '0.55rem',
    color: colors.primary,
    borderBottom: `1px solid ${colors.light}`,
    display: 'flex',
    justifyContent: 'space-between',
    flexDirection: 'row',
    minWidth: '15rem'
  },

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
    borderBottom: `1px solid ${colors.light}`,
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 'auto'
  },

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
  },

  selectAllContainer: {
    marginLeft: 'auto',
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
  }
});

const stylesFunction = {
  plusIconColor: (disabled) => {
    return {
      fill: disabled ? colorWithWhiteness(colors.dark, 0.4) : colors.accent
    };
  },
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

const DOMAIN_DISPLAY_ORDER = {
  // Person domain is always first as the canonical primary table. Everything
  // else is alphabetized. To add further ordering constraints, add more
  // entries to this map with sort values.
  [Domain.PERSON]: 0
};

// Exported for testing.
export const COMPARE_DOMAINS_FOR_DISPLAY = (a: Domain, b: Domain) => {
  if (a in DOMAIN_DISPLAY_ORDER && b in DOMAIN_DISPLAY_ORDER) {
    return DOMAIN_DISPLAY_ORDER[a] - DOMAIN_DISPLAY_ORDER[b];
  } else if (a in DOMAIN_DISPLAY_ORDER) {
    return -1;
  } else if (b in DOMAIN_DISPLAY_ORDER) {
    return 1;
  }
  return a.toString().localeCompare(b.toString());
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

interface DataDictionaryPopupProps {
  dataDictionaryEntry: DataDictionaryEntry;
}

const DataDictionaryDescription: React.FunctionComponent<DataDictionaryPopupProps> = ({dataDictionaryEntry}) => {
  return <div style={{width: '100%', borderTop: `1px solid ${colorWithWhiteness(colors.dark, 0.6)}`}}>
    {dataDictionaryEntry ? <FlexColumn style={{padding: '0.5rem'}}>
      <div style={{...styles.dataDictionarySubheader, paddingTop: 0}}>Description</div>
      <div style={styles.dataDictionaryText}>{dataDictionaryEntry.description}</div>
      <div style={styles.dataDictionarySubheader}>Relevant OMOP Table</div>
      <div style={styles.dataDictionaryText}>{dataDictionaryEntry.relevantOmopTable}</div>
      <div style={styles.dataDictionarySubheader}>Type</div>
      <div style={styles.dataDictionaryText}>{dataDictionaryEntry.fieldType}</div>
      <div style={styles.dataDictionarySubheader}>Data Provenance</div>
      <div style={styles.dataDictionaryText}>
        {dataDictionaryEntry.dataProvenance}{dataDictionaryEntry.dataProvenance.includes('PPI') ?
        `: ${dataDictionaryEntry.sourcePpiModule}` : null}</div>
    </FlexColumn> : <div style={{display: 'flex', justifyContent: 'center'}}>
      <Spinner style={{height: 36, width: 36, margin: '0.5rem'}}/></div>}
  </div>;
};

interface ValueListItemProps {
  checked: boolean;
  domain: Domain;
  domainValue: DomainValue;
  onChange: Function;
}

interface ValueListItemState {
  dataDictionaryEntry: DataDictionaryEntry;
  dataDictionaryEntryError: boolean;
  showDataDictionaryEntry: boolean;
}



export class ValueListItem extends React.Component<
  ValueListItemProps, ValueListItemState> {

  constructor(props) {
    super(props);
    this.state = {
      dataDictionaryEntry: undefined,
      dataDictionaryEntryError: false,
      showDataDictionaryEntry: false,
    };
  }

  fetchDataDictionaryEntry() {
    const {domain, domainValue} = this.props;

    dataSetApi().getDataDictionaryEntry(
      parseInt(currentWorkspaceStore.getValue().cdrVersionId, 10),
      domain.toString(),
      domainValue.value).then(dataDictionaryEntry => {
        this.setState({dataDictionaryEntry});
      }).catch(e => {
        this.setState({dataDictionaryEntryError: true});
      });
  }

  toggleDataDictionaryEntry() {
    this.setState({showDataDictionaryEntry: !this.state.showDataDictionaryEntry});

    if (this.state.dataDictionaryEntry === undefined) {
      this.fetchDataDictionaryEntry();
    }
  }

  render() {
    const {checked, domainValue, onChange} = this.props;
    const {dataDictionaryEntry, dataDictionaryEntryError, showDataDictionaryEntry} = this.state;

    return <div style={{
      ...styles.listItem,
      height: 'auto',
      justifyContent: 'space-between',
      alignItems: 'center',
      paddingLeft: '10px',
      paddingRight: '10px'
    }}>
      <FlexRow style={{width: '100%'}}>
        <input type='checkbox' value={domainValue.value} onChange={() => onChange()}
               style={{...styles.listItemCheckbox, marginTop: 11, marginLeft: 0, marginRight: 0}} checked={checked}/>
        <div style={{width: '100%'}}>
          <FlexRow style={{justifyContent: 'space-between', width: '100%', alignItems: 'center'}}>
            <div style={{lineHeight: '1.5rem', paddingLeft: 10, wordWrap: 'break-word', color: colors.primary}}>
              {domainValue.value}</div>
            <Clickable onClick={() => this.toggleDataDictionaryEntry()} data-test-id='value-list-expander'>
              <ClrIcon shape='angle' style={{transform: showDataDictionaryEntry ? 'rotate(180deg)' : 'rotate(90deg)',
                color: colors.accent, height: 18, width: 18}} />
            </Clickable>
          </FlexRow>
          {showDataDictionaryEntry && <DataDictionaryDescription dataDictionaryEntry={dataDictionaryEntry}/>}
          {dataDictionaryEntryError && showDataDictionaryEntry && <div>Data Dictionary Entry not found.</div>}
        </div>
      </FlexRow>
    </div>;
  }
}

const plusLink = (dataTestId: string, path: string, disable?: boolean) => {
  return <TooltipTrigger data-test-id='plus-icon-tooltip' disabled={!disable}
                         content='Requires Owner or Writer permission'>
    <Clickable disabled={disable} data-test-id={dataTestId} href={path}
            onClick={e => {navigateAndPreventDefaultIfNoKeysPressed(e, path); }}>
    <ClrIcon shape='plus-circle' class='is-solid' size={16}
             style={stylesFunction.plusIconColor(disable)}/>
  </Clickable></TooltipTrigger>;
};

const StepNumber = ({step, style = {}}) => {
  return <CircleWithText text={step} width='23.78px' height='23.78px'
                         style={{fill: colorWithWhiteness(colors.primary, 0.5), ...style}}/>;
};

const BoxHeader = ({step = '', header =  '', subHeader = '', style = {}, ...props}) => {
  return  <div style={styles.selectBoxHeader}>
    <div style={{display: 'flex', marginLeft: '0.2rem'}}>
      <StepNumber step={step} style={{marginTop: '0.5rem'}}/>
      <label style={{marginLeft: '0.5rem', color: colors.primary, display: 'flex', ...style}}>
        <div style={{fontWeight: 600, marginRight: '0.3rem'}}>{header}</div>
        ({subHeader})
      </label>
    </div>
    {props.children}
  </div>;
};

// TODO(RW-3508): Refactor the API model for prepackaged concept sets to be less
// rigid, and more extensible to future additions of prepackaged concept sets.
// For now, this client-side enum tracks the desired state: a set of selectable
// prepackaged concept sets.

// Enum values are the display values.
enum PrepackagedConceptSet {
  DEMOGRAPHICS = 'Demographics',
  SURVEYS = 'All Surveys'
}

const PREPACKAGED_DOMAINS = {
  [PrepackagedConceptSet.DEMOGRAPHICS]: Domain.PERSON,
  [PrepackagedConceptSet.SURVEYS]: Domain.SURVEY
};

interface DataSetPreviewInfo {
  isLoading: boolean;
  errorText: JSX.Element;
  values?: Array<DataSetPreviewValueList>;
}

interface Props {
  workspace: WorkspaceData;
  urlParams: any;
  profileState: {
    profile: Profile,
    reload: Function
  };
}

interface State {
  cohortList: Cohort[];
  conceptSetList: ConceptSet[];
  creatingConceptSet: boolean;
  dataSet: DataSet;
  dataSetTouched: boolean;

  // Lazily poplulated. This information is static, so entries are not removed
  // even if no concept sets for a given domain are actively selected.
  domainValueSetIsLoading: Set<Domain>;
  domainValueSetLookup: Map<Domain, ValueSet>;

  includesAllParticipants: boolean;
  loadingResources: boolean;
  openSaveModal: boolean;
  previewList: Map<Domain, DataSetPreviewInfo>;
  selectedCohortIds: number[];
  selectedConceptSetIds: number[];
  selectedDomains: Set<Domain>;
  selectedDomainValuePairs: DomainValuePair[];
  selectedPrepackagedConceptSets: Set<PrepackagedConceptSet>;
  selectedPreviewDomain: Domain;
}

const DataSetPage = fp.flow(withUserProfile(), withCurrentWorkspace(), withUrlParams())(
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
        domainValueSetIsLoading: new Set(),
        domainValueSetLookup: new Map(),
        includesAllParticipants: false,
        loadingResources: true,
        openSaveModal: false,
        previewList: new Map(),
        selectedCohortIds: [],
        selectedConceptSetIds: [],
        selectedDomains: new Set(),
        selectedPreviewDomain: Domain.CONDITION,
        selectedPrepackagedConceptSets: new Set(),
        selectedDomainValuePairs: [],
      };
    }

    get editing() {
      return this.props.urlParams.dataSetId !== undefined;
    }

    async componentDidMount() {
      const {namespace, id} = this.props.workspace;
      const resourcesPromise = this.loadResources();
      if (!this.editing) {
        return;
      }

      const [, dataSet] = await Promise.all([
        resourcesPromise,
        dataSetApi().getDataSet(namespace, id, this.props.urlParams.dataSetId)
      ]);

      const selectedPrepackagedConceptSets = this.apiEnumToPrePackageConceptSets(dataSet.prePackagedConceptSet);
      this.setState({
        dataSet,
        includesAllParticipants: dataSet.includesAllParticipants,
        selectedConceptSetIds: dataSet.conceptSets.map(cs => cs.id),
        selectedCohortIds: dataSet.cohorts.map(c => c.id),
        selectedDomainValuePairs: dataSet.domainValuePairs,
        selectedDomains: this.getDomainsFromDataSet(dataSet),
        selectedPrepackagedConceptSets,
      });
    }

    async componentDidUpdate({}, prevState: State) {
      // If any domains were dropped, we want to drop any domain/value pair selections.
      const droppedDomains = Array.from(prevState.selectedDomains)
          .filter(d => !this.state.selectedDomains.has(d));
      if (droppedDomains.length > 0) {
        this.setState(({selectedDomains, selectedDomainValuePairs}) => ({
          selectedDomainValuePairs: selectedDomainValuePairs.filter(p => selectedDomains.has(p.domain))
        }));
      }

      // After a state update, first check whether any new domains have been added.
      const newDomains = Array.from(this.state.selectedDomains)
          .filter(d => !prevState.selectedDomains.has(d));
      if (!newDomains.length) {
        return;
      }

      // TODO(RW-4426): There is a lot of complexity here around loading domain
      // values which is static data for a given CDR version. Consider
      // refactoring this page to load all schema data before rendering.

      // If any of these new domains have already been loaded, autoselect all of
      // their value pairs. The desired product behavior is that when a new
      // set of domain values appears, all boxes begin as checked.
      const loadedDomains = newDomains
        .filter(d => this.state.domainValueSetLookup.has(d));
      if (loadedDomains.length > 0) {
        this.setState(({domainValueSetLookup, selectedDomainValuePairs}) => {
          const morePairs = fp.flatMap(
            d => domainValueSetLookup.get(d).values.items.map(v => ({
              domain: d, value: v.value
            })), loadedDomains);

          return {
            selectedDomainValuePairs: selectedDomainValuePairs.concat(morePairs)
          };
        });
      }

      // If any of these domains has not yet been loaded, request the schema
      // (value sets) for them.
      const domainsToLoad = newDomains.filter(
        d => !this.state.domainValueSetIsLoading.has(d) && !this.state.domainValueSetLookup.has(d));
      if (domainsToLoad.length > 0) {
        this.setState(({domainValueSetIsLoading, domainValueSetLookup}) => {
          const newLoading = new Set(domainValueSetIsLoading);
          domainsToLoad.forEach((d) => {
            if (!domainValueSetIsLoading.has(d) && !domainValueSetLookup.has(d)) {
              // This will also autoselect the newly loaded values.
              this.loadValueSetForDomain(d);
              newLoading.add(d);
            }
          });
          return {domainValueSetIsLoading: newLoading};
        });
      }
    }

    private async loadValueSetForDomain(domain: Domain) {
      const {namespace, id} = this.props.workspace;
      const values = await dataSetApi().getValuesFromDomain(namespace, id, domain.toString());
      this.setState(({dataSet, domainValueSetIsLoading, domainValueSetLookup, selectedDomainValuePairs}) => {
        const newLoading = new Set(domainValueSetIsLoading);
        const newLookup = new Map(domainValueSetLookup);

        newLoading.delete(domain);
        newLookup.set(domain, {domain, values});

        // Autoselect the newly added domain, except if we're editing an
        // existing dataset. This avoids having us overwrite the selected pairs
        // on initial load.
        let morePairs = [];
        if (!this.getDomainsFromDataSet(dataSet).has(domain)) {
          morePairs = values.items.map(v => ({domain, value: v.value}));
        }

        return {
          domainValueSetIsLoading: newLoading,
          domainValueSetLookup: newLookup,
          selectedDomainValuePairs: selectedDomainValuePairs.concat(morePairs)
        };
      });
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

    private getDomainsFromDataSet(d: DataSet) {
      const selectedPrepackagedConceptSets = this.apiEnumToPrePackageConceptSets(d.prePackagedConceptSet);
      return this.getDomainsFromConceptSets(d.conceptSets, selectedPrepackagedConceptSets)
    }

    private getDomainsFromConceptSets(
      conceptSets: ConceptSet[], prepackagedConceptSets: Set<PrepackagedConceptSet>): Set<Domain> {
      const domains =
        conceptSets.map(cs => cs.domain).concat(
          Array.from(prepackagedConceptSets).map(p => PREPACKAGED_DOMAINS[p]));
      return new Set(domains);
    }

    private getConceptSets(ids: number[]): ConceptSet[] {
      const setsById = new Map(this.state.conceptSetList.map(cs => [cs.id, cs] as [any, any]));
      return ids.map(id => setsById.get(id));
    }

    selectPrePackagedConceptSet(prepackaged: PrepackagedConceptSet, selected: boolean) {
      this.setState(({selectedConceptSetIds, selectedPrepackagedConceptSets}) => {
        const updatedPrepackaged = new Set(selectedPrepackagedConceptSets);
        if (selected) {
          updatedPrepackaged.add(prepackaged);
        } else {
          updatedPrepackaged.delete(prepackaged);
        }
        const selectedDomains = this.getDomainsFromConceptSets(
          this.getConceptSets(selectedConceptSetIds), updatedPrepackaged);
        return {
          selectedDomains,
          selectedPrepackagedConceptSets: updatedPrepackaged,
          dataSetTouched: true
        };
      });
    }

    selectConceptSet(conceptSet: ConceptSet, selected: boolean): void {
      this.setState(({selectedConceptSetIds, selectedPrepackagedConceptSets}) => {
        let updatedConceptSetIds: number[];
        if (selected) {
          updatedConceptSetIds = selectedConceptSetIds.concat([conceptSet.id]);
        } else {
          updatedConceptSetIds = fp.pull(conceptSet.id, selectedConceptSetIds);
        }
        const selectedDomains = this.getDomainsFromConceptSets(
          this.getConceptSets(updatedConceptSetIds), selectedPrepackagedConceptSets);
        return {
          selectedDomains,
          selectedConceptSetIds: updatedConceptSetIds,
          dataSetTouched: true
        };
      });
    }

    selectCohort(cohort: Cohort): void {
      this.setState({
        dataSetTouched: true,
        selectedCohortIds: toggleIncludes(cohort.id, this.state.selectedCohortIds)
      });
    }

    selectDomainValue(domain: Domain, domainValue: DomainValue): void {
      const valueSets = this.state.domainValueSetLookup.get(domain).values.items;
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

    // Returns true if selected values set is empty or is not equal to the total values displayed
    get allValuesSelected() {
      return !fp.isEmpty(this.state.selectedDomainValuePairs) &&
          this.state.selectedDomainValuePairs.length === this.valuesCount;
    }

    get valuesCount() {
      let count = 0 ;
      this.state.selectedDomains.forEach(d => {
        // Only counted loaded domains.
        const v = this.state.domainValueSetLookup.get(d);
        if (v) {
          count += v.values.items.length;
        }
      });
      return count;
    }

    selectAllValues() {
      if (this.allValuesSelected) {
        this.setState({selectedDomainValuePairs: []});
        return;
      } else {
        const selectedValuesList = [];
        this.state.domainValueSetLookup.forEach(valueSet => {
          valueSet.values.items.map(value => {
            selectedValuesList.push({domain: valueSet.domain, value: value.value});
          });
        });
        this.setState({selectedDomainValuePairs: selectedValuesList});
      }
    }

    get canWrite() {
      return WorkspacePermissionsUtil.canWrite(this.props.workspace.accessLevel);
    }

    disableSave() {
      return !this.state.selectedConceptSetIds || (this.state.selectedConceptSetIds.length === 0
          && this.state.selectedPrepackagedConceptSets.size === 0) ||
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

    apiEnumToPrePackageConceptSets(v: PrePackagedConceptSetEnum): Set<PrepackagedConceptSet> {
      switch (v) {
        case PrePackagedConceptSetEnum.BOTH:
          return new Set([PrepackagedConceptSet.DEMOGRAPHICS, PrepackagedConceptSet.SURVEYS]);
        case PrePackagedConceptSetEnum.DEMOGRAPHICS:
          return new Set([PrepackagedConceptSet.DEMOGRAPHICS]);
        case PrePackagedConceptSetEnum.SURVEY:
          return new Set([PrepackagedConceptSet.SURVEYS]);
        case PrePackagedConceptSetEnum.NONE:
        default:
          return new Set();
      }
    }

    getPrePackagedConceptSetApiEnum() {
      const {selectedPrepackagedConceptSets} = this.state;
      if (selectedPrepackagedConceptSets.has(PrepackagedConceptSet.DEMOGRAPHICS) &&
          selectedPrepackagedConceptSets.has(PrepackagedConceptSet.SURVEYS)) {
        return PrePackagedConceptSetEnum.BOTH;
      } else if (selectedPrepackagedConceptSets.has(PrepackagedConceptSet.SURVEYS)) {
        return PrePackagedConceptSetEnum.SURVEY;
      } else if (selectedPrepackagedConceptSets.has(PrepackagedConceptSet.DEMOGRAPHICS)) {
        return PrePackagedConceptSetEnum.DEMOGRAPHICS;
      }
      return PrePackagedConceptSetEnum.NONE;
    }

    async getPreviewList() {
      const domains = fp.uniq(this.state.selectedDomainValuePairs.map(domainValue => domainValue.domain));
      const newPreviewList: Map<Domain, DataSetPreviewInfo> =
        new Map(domains.map<[Domain, DataSetPreviewInfo]>(domain => [domain, {
          isLoading: true, errorText: null, values: []}]));
      this.setState({
        previewList: newPreviewList,
        selectedPreviewDomain: domains[0]
      });
      domains.forEach(async domain => {
        this.getPreviewByDomain(domain);
      });
    }

    async getPreviewByDomain(domain: Domain) {
      const {namespace, id} = this.props.workspace;
      const domainRequest: DataSetPreviewRequest = {
        domain: domain,
        conceptSetIds: this.state.selectedConceptSetIds,
        includesAllParticipants: this.state.includesAllParticipants,
        cohortIds: this.state.selectedCohortIds,
        prePackagedConceptSet: this.getPrePackagedConceptSetApiEnum(),
        values: this.state.selectedDomainValuePairs.map(domainValue => domainValue.value)
      };
      let newPreviewInformation;
      try {
        const domainPreviewResponse = await apiCallWithGatewayTimeoutRetries(
          () => dataSetApi().previewDataSetByDomain(namespace, id, domainRequest));
        newPreviewInformation = {
          isLoading: false,
          errorText: null,
          values: domainPreviewResponse.values
        };
      } catch (ex) {
        const exceptionResponse = await ex.json() as unknown as ErrorResponse;
        const errorText = this.generateErrorTextFromPreviewException(exceptionResponse);
        newPreviewInformation = {
          isLoading: false,
          errorText: errorText,
          values: []
        };
      }
      this.setState(state => ({previewList: state.previewList.set(domain, newPreviewInformation)}));
    }

    openZendeskWidget() {
      const {profile} = this.props.profileState;
      openZendeskWidget(profile.givenName, profile.familyName, profile.username, profile.contactEmail);
    }

    // TODO: Move to using a response based error handling method, rather than a error based one
    generateErrorTextFromPreviewException(exceptionResponse: ErrorResponse): JSX.Element {
      switch (exceptionResponse.statusCode) {
        case 400:
          return <div>{exceptionResponse.message}</div>;
        case 404:
          return <div>{exceptionResponse.message}</div>;
        case 504:
          return <div>The preview table cannot be loaded because the query took too long to run.
            Please export this Dataset to a Notebook by clicking the Analyze button.</div>;
        default:
          return <div>An unexpected error has occurred while running your Dataset query.
            Please <Link style={{display: 'inline-block'}} onClick={() => this.openZendeskWidget()}>
              create a bug report</Link> for our team.</div>;
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
        <div style={{overflow: 'hidden', textOverflow: 'ellipsis'}} title={text}>
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
                  bodyStyle={{hyphens: 'auto'}} field={value.value}/>
        )}
      </DataTable>;
    }

    renderPreviewDataTableSectionMessage(filteredPreviewData: DataSetPreviewInfo) {
      const domainDisplayed = formatDomain(this.state.selectedPreviewDomain);
      return <div style={styles.warningMessage}>
        {filteredPreviewData.isLoading ?
          <div>Generating preview for {domainDisplayed}</div> :
          <div>{filteredPreviewData.errorText}</div>
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
        domainValueSetIsLoading,
        domainValueSetLookup,
        includesAllParticipants,
        loadingResources,
        openSaveModal,
        previewList,
        selectedCohortIds,
        selectedConceptSetIds,
        selectedPreviewDomain,
        selectedDomains,
        selectedDomainValuePairs,
        selectedPrepackagedConceptSets
      } = this.state;
      return <React.Fragment>
        <FadeBox style={{paddingTop: '1rem'}}>
          <h2 style={{paddingTop: 0, marginTop: 0}}>Datasets{this.editing &&
            dataSet !== undefined && ' - ' + dataSet.name}</h2>
          <div style={{color: colors.primary, fontSize: '14px'}}>Build a dataset by selecting the
            variables and values for one or more of your cohorts. Then export the completed dataset
            to Notebooks where you can perform your analysis</div>
          <div style={{display: 'flex', paddingTop: '1rem'}}>
            <div style={{width: '33%', height: '80%', minWidth: styles.selectBoxHeader.minWidth}}>
              <div style={{backgroundColor: 'white', border: `1px solid ${colors.light}`}}>
                <BoxHeader step='1' header='Select Cohorts' subHeader='Participants'>
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
                                        () => this.selectCohort(cohort)}/>
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
                    <BoxHeader step='2' header='Select Concept Sets' subHeader='Rows'
                               style={{paddingRight: '1rem'}}>
                      {plusLink('concept-sets-link', conceptSetsPath, !this.canWrite)}
                    </BoxHeader>
                  <div style={{height: '9rem', overflowY: 'auto'}}>
                    <Subheader>Prepackaged Concept Sets</Subheader>
                    {Object.keys(PrepackagedConceptSet).map((prepackaged: PrepackagedConceptSet) => {
                      const p = PrepackagedConceptSet[prepackaged];
                      return <ImmutableListItem name={p}
                                         key={prepackaged}
                                         checked={selectedPrepackagedConceptSets.has(p)}
                                         onChange={() => this.selectPrePackagedConceptSet(
                                           p, !selectedPrepackagedConceptSets.has(p))
                                         }/>;
                    })}
                    <Subheader>Workspace Concept Sets</Subheader>
                    {!loadingResources && this.state.conceptSetList.map(conceptSet =>
                        <ImmutableListItem key={conceptSet.id} name={conceptSet.name}
                                          data-test-id='concept-set-list-item'
                                          checked={selectedConceptSetIds.includes(conceptSet.id)}
                                          onChange={
                                          () => this.selectConceptSet(
                                            conceptSet, !selectedConceptSetIds.includes(conceptSet.id))
                                          }/>)
                    }
                    {loadingResources && <Spinner style={{position: 'relative', top: '2rem',
                      left: '10rem'}}/>}
                  </div>
                </div>
                <div style={{width: '55%'}}>
                    <BoxHeader step='3' header='Select Values' subHeader='Columns'>
                    <div style={styles.selectAllContainer}>
                      <CheckBox style={{height: 17, width: 17}}
                                manageOwnState={false}
                                disabled={selectedDomains.size === 0}
                                data-test-id='select-all'
                                onChange={() => this.selectAllValues()}
                                checked={this.allValuesSelected} />
                      <div style={{marginLeft: '0.25rem', fontSize: '13px', lineHeight: '17px'}}>
                        {this.allValuesSelected ? 'Deselect All' : 'Select All'}
                      </div>
                    </div>
                  </BoxHeader>
                  <div style={{height: selectedDomains.size > 0 ? '7.625rem' : '9rem', overflowY: 'auto'}}>
                    {domainValueSetIsLoading.size > 0 && <Spinner style={{position: 'relative',
                      top: '2rem', left: 'calc(50% - 36px)'}}/>}
                    {Array.from(selectedDomains).sort(COMPARE_DOMAINS_FOR_DISPLAY).map(domain =>
                      domainValueSetLookup.has(domain) &&
                      <div key={domain}>
                        <Subheader style={{fontWeight: 'bold'}}>
                          {formatDomain(domain)}
                        </Subheader>
                        {domainValueSetLookup.get(domain).values.items.map(domainValue =>
                          <ValueListItem data-test-id='value-list-items'
                            key={domainValue.value} domain={domain} domainValue={domainValue}
                            onChange={() => this.selectDomainValue(domain, domainValue)}
                            checked={fp.some({domain: domain, value: domainValue.value},
                              selectedDomainValuePairs)}/>
                        )}
                      </div>)
                    }
                  </div>
                  {selectedDomains.size > 0 && <FlexRow style={{
                    width: '100%', height: '1.375rem', backgroundColor: colorWithWhiteness(colors.dark, 0.9),
                    color: colors.primary, paddingLeft: '0.4rem', fontSize: '13px', lineHeight: '16px',
                    alignItems: 'center'}}>
                    <a href={'https://aousupporthelp.zendesk.com/hc/en-us/articles/' +
                    '360033200232-Data-Dictionary-for-Registered-Tier-CDR'} target='_blank'
                       style={{color: colors.accent}}>
                      Learn more
                    </a>&nbsp;in the data dictionary
                  </FlexRow>}
                </div>
              </div>
            </div>
          </div>
        </FadeBox>
        <FadeBox style={{marginTop: '1rem'}}>
          <div style={{backgroundColor: 'white', border: `1px solid ${colors.light}`}}>
            <div style={styles.previewDataHeaderBox}>
              <FlexColumn>
              <div style={{display: 'flex', alignItems: 'flex-end'}}>
                <div style={styles.previewDataHeader}>
                  <div>
                    <StepNumber step='4'/>
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
              </FlexColumn>
              <Clickable data-test-id='preview-button'
                         style={{
                           marginTop: '0.5rem',
                           cursor: this.disableSave() ? 'not-allowed' : 'pointer',
                           height: '1.8rem',
                           width: '6.5rem',
                           color: this.disableSave() ? colorWithWhiteness(colors.dark, 0.6) : colors.accent
                         }}
                         disabled={this.disableSave()}
                         onClick={() => {
                           AnalyticsTracker.DatasetBuilder.ViewPreviewTable();
                           this.getPreviewList();
                         }}>
                  View Preview Table
              </Clickable>
            </div>
            {fp.toPairs(previewList).length > 0 &&
              <FlexColumn>
                <FlexRow style={{paddingTop: '0.5rem'}}>
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
                                 style={stylesFunction.selectDomainForPreviewButton(selectedPreviewDomain === Domain[domain])}>
                        <FlexRow style={{alignItems: 'center'}}>
                          {domain.toString()}
                          {previewRow.isLoading &&
                          <Spinner style={{marginLeft: '4px', height: '18px', width: '18px'}}/>}
                        </FlexRow>
                      </Clickable>
                    </TooltipTrigger>;
                  })}
                </FlexRow>
                {this.renderPreviewDataTableSection()}
              </FlexColumn>
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
                                           billingLocked={this.props.workspace.billingStatus === BillingStatus.INACTIVE}
                                           prePackagedConceptSet={this.getPrePackagedConceptSetApiEnum()}
                                           dataSet={dataSet ? dataSet : undefined}
                                           closeFunction={() => {
                                             this.setState({openSaveModal: false});
                                           }}
        />}
      </React.Fragment>;
    }
  });

export {
  DataSetPage
};

@Component({
  template: '<div #root></div>'
})
export class DataSetPageComponent extends ReactWrapperBase {
  constructor() {
    super(DataSetPage, []);
  }
}
