import * as fp from 'lodash/fp';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

import {Button, Clickable, Link, StyledAnchorTag} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {CreateModal} from 'app/components/create-modal';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {CheckBox} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {CircleWithText} from 'app/icons/circleWithText';
import {ExportDatasetModal} from 'app/pages/data/data-set/export-dataset-modal';
import {GenomicExtractionModal} from 'app/pages/data/data-set/genomic-extraction-modal';
import {cohortsApi, conceptSetsApi, dataSetApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  formatDomain,
  formatDomainString,
  reactStyles, switchCase,
  toggleIncludes,
  withCdrVersions,
  withCurrentWorkspace,
  withUrlParams,
  withUserProfile
} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {getCdrVersion} from 'app/utils/cdr-versions';
import {
  currentWorkspaceStore,
  navigateAndPreventDefaultIfNoKeysPressed
} from 'app/utils/navigation';
import {apiCallWithGatewayTimeoutRetries} from 'app/utils/retry';
import {serverConfigStore} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {openZendeskWidget, supportUrls} from 'app/utils/zendesk';
import {
  CdrVersionTiersResponse,
  Cohort,
  ConceptSet,
  DataDictionaryEntry,
  DataSet,
  DataSetPreviewRequest,
  DataSetPreviewValueList, DataSetRequest,
  Domain,
  DomainValue,
  DomainValuePair,
  ErrorResponse,
  PrePackagedConceptSetEnum,
  Profile, ResourceType,
  ValueSet,
} from 'generated/fetch';

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
    width: '40rem',
    alignSelf: 'center',
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
  stickyFooter: {
    backgroundColor: colors.white,
    borderTop: `1px solid ${colors.light}`,
    textAlign: 'right',
    padding: '3px 55px 50px 20px',
    position: 'sticky',
    bottom: '0',
    height: '60px',
    width: '100%'
  },
  errorMessage: {
    backgroundColor: colorWithWhiteness(colors.highlight, .5),
    color: colors.primary,
    fontSize: '12px',
    padding: '0.5rem',
    borderRadius: '0.5em'
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

const ImmutableWorkspaceCohortListItem: React.FunctionComponent<{
  name: string, onChange: Function, checked: boolean, cohortId: number, namespace: string, wid: string}>
    = ({name, onChange, checked, cohortId, namespace, wid}) => {
      return <div style={styles.listItem}>
        <input type='checkbox' value={name} onChange={() => onChange()}
               style={styles.listItemCheckbox} checked={checked}/>
        <FlexRow style={{lineHeight: '1.5rem', color: colors.primary, width: '100%'}}>
          <div>{name}</div>
          <div style={{marginLeft: 'auto', paddingRight: '1rem'}}>
            <a href={'/workspaces/' + namespace + '/' + wid + '/data/cohorts/' + cohortId + '/review/cohort-description'}
            target='_blank'>
              <ClrIcon size='20' shape='bar-chart'/>
            </a>
          </div>
    </FlexRow>
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
      domain === Domain.PHYSICALMEASUREMENTCSS ? Domain.MEASUREMENT.toString() : domain.toString(),
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
          {!dataDictionaryEntryError && showDataDictionaryEntry && <DataDictionaryDescription dataDictionaryEntry={dataDictionaryEntry}/>}
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
  PERSON = 'Demographics',
  SURVEYS = 'All Surveys',
  FITBITHEARTRATESUMMARY = 'Fitbit Heart Rate Summary',
  FITBITACTIVITY = 'Fitbit Activity Summary',
  FITBITHEARTRATELEVEL = 'Fitbit Heart Rate Level',
  FITBITINTRADAYSTEPS = 'Fitbit Intra Day Steps',
  WHOLEGENOME = 'All whole genome variant data'
}

const PREPACKAGED_SURVEY_PERSON_DOMAIN = {
  [PrepackagedConceptSet.PERSON]: Domain.PERSON,
  [PrepackagedConceptSet.SURVEYS]: Domain.SURVEY,
};

const PREPACKAGED_WITH_FITBIT_DOMAINS = {
  [PrepackagedConceptSet.FITBITHEARTRATESUMMARY]: Domain.FITBITHEARTRATESUMMARY,
  [PrepackagedConceptSet.FITBITACTIVITY]: Domain.FITBITACTIVITY,
  [PrepackagedConceptSet.FITBITHEARTRATELEVEL]: Domain.FITBITHEARTRATELEVEL,
  [PrepackagedConceptSet.FITBITINTRADAYSTEPS]: Domain.FITBITINTRADAYSTEPS,
};

const PREPACKAGED_WITH_WHOLE_GENOME = {
  [PrepackagedConceptSet.WHOLEGENOME]: Domain.WHOLEGENOMEVARIANT
};
let PREPACKAGED_DOMAINS = PREPACKAGED_SURVEY_PERSON_DOMAIN;

interface DataSetPreviewInfo {
  isLoading: boolean;
  errorText: JSX.Element;
  values?: Array<DataSetPreviewValueList>;
}

interface Props extends WithErrorModalProps, WithSpinnerOverlayProps {
  workspace: WorkspaceData;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  urlParams: any;
  profileState: {
    profile: Profile,
    reload: Function
  };
}

enum ModalState {
  None,
  Create,
  Export,
  Extract
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
  modalState: ModalState;
  previewList: Map<Domain, DataSetPreviewInfo>;
  selectedCohortIds: number[];
  selectedConceptSetIds: number[];
  selectedDomains: Set<Domain>;
  selectedDomainValuePairs: DomainValuePair[];
  selectedPrepackagedConceptSets: Set<PrepackagedConceptSet>;
  selectedPreviewDomain: Domain;
  savingDataset: boolean;
}

export const DatasetPage = fp.flow(withUserProfile(), withCurrentWorkspace(), withUrlParams(), withCdrVersions(), withErrorModal())(
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
        modalState: ModalState.None,
        previewList: new Map(),
        selectedCohortIds: [],
        selectedConceptSetIds: [],
        selectedDomains: new Set(),
        selectedPreviewDomain: Domain.CONDITION,
        selectedPrepackagedConceptSets: new Set(),
        selectedDomainValuePairs: [],
        savingDataset: false
      };
    }

    get isCreatingNewDataset() {
      return !this.state.dataSet;
    }

    async componentDidMount() {
      this.props.hideSpinner();
      const {namespace, id} = this.props.workspace;
      const resourcesPromise = this.loadResources();
      if (getCdrVersion(this.props.workspace, this.props.cdrVersionTiersResponse).hasFitbitData) {
        PREPACKAGED_DOMAINS =   {
          ...PREPACKAGED_SURVEY_PERSON_DOMAIN,
          ...PREPACKAGED_WITH_FITBIT_DOMAINS
        };
      }
      // Only allow selection of genomics prepackaged concept sets if genomics
      // data extraction is possible, since extraction is the only action that
      // can be taken on genomics variant data from the dataset builder.
      if (serverConfigStore.get().config.enableGenomicExtraction &&
          getCdrVersion(this.props.workspace, this.props.cdrVersionTiersResponse).hasWgsData) {
        PREPACKAGED_DOMAINS = {
          ...PREPACKAGED_DOMAINS,
          ...PREPACKAGED_WITH_WHOLE_GENOME
        };
      }

      if (this.props.urlParams.dataSetId !== undefined) {
        const [, dataset] = await Promise.all([
          resourcesPromise,
          dataSetApi().getDataSet(namespace, id, this.props.urlParams.dataSetId)
        ]);

        this.loadDataset(dataset);
      }
    }

    loadDataset(dataset) {
      // This is to set the URL to reference the newly created dataset and the user is staying on the dataset page
      // A bit of a hack but I couldn't find another way to change the URL without triggering a reload
      if (window.location.href.endsWith('data-sets')) {
        history.pushState({}, '', `${window.location.href}/${dataset.id}`);
      }

      return this.setState({
        dataSet: dataset,
        dataSetTouched: false,
        includesAllParticipants: dataset.includesAllParticipants,
        selectedConceptSetIds: dataset.conceptSets.map(cs => cs.id),
        selectedCohortIds: dataset.cohorts.map(c => c.id),
        selectedDomainValuePairs: dataset.domainValuePairs,
        selectedDomains: this.getDomainsFromDataSet(dataset),
        selectedPrepackagedConceptSets: this.apiEnumToPrePackageConceptSets(dataset.prePackagedConceptSet)
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
        // existing dataset which already covers the domain. This avoids having
        // us overwrite the selected pairs on initial load.
        let morePairs = [];
        if (this.isCreatingNewDataset || !this.getDomainsFromDataSet(dataSet).has(domain)) {
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
      return this.getDomainsFromConceptSets(d.conceptSets, selectedPrepackagedConceptSets);
    }

    private getDomainsFromConceptSets(
      conceptSets: ConceptSet[], prepackagedConceptSets: Set<PrepackagedConceptSet>): Set<Domain> {
      const domains =
        conceptSets.map(cs => cs.domain === Domain.PHYSICALMEASUREMENT ?  Domain.MEASUREMENT : cs.domain).concat(
          Array.from(prepackagedConceptSets).map(p => PREPACKAGED_DOMAINS[p]));
      return new Set(domains);
    }

    private getConceptSets(ids: number[]): ConceptSet[] {
      const setsById = new Map(this.state.conceptSetList.map(cs => [cs.id, cs] as [any, any]));
      return ids.map(id => setsById.get(id));
    }

    getPrePackagedList() {
      let prepackagedList = Object.keys(PrepackagedConceptSet);
      if (!getCdrVersion(this.props.workspace, this.props.cdrVersionTiersResponse).hasFitbitData) {
        prepackagedList = prepackagedList
            .filter(prepack => !fp.startsWith('FITBIT', prepack));
      }
      if (!serverConfigStore.get().config.enableGenomicExtraction ||
          !getCdrVersion(this.props.workspace, this.props.cdrVersionTiersResponse).hasWgsData) {
        prepackagedList = prepackagedList.filter(prepack => prepack !== 'WHOLEGENOME');
      }
      return prepackagedList;
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
      const selectedCohortList = toggleIncludes(cohort.id, this.state.selectedCohortIds);
      // If Workspace Cohort is selected, un-select Pre packaged cohort
      if (selectedCohortList && selectedCohortList.length > 0) {
        this.setState({
          dataSetTouched: true,
          selectedCohortIds: selectedCohortList,
          includesAllParticipants: false
        });
      } else {
        this.setState({
          dataSetTouched: true,
          selectedCohortIds: selectedCohortList
        });
      }

    }

    selectPrePackagedCohort(): void {
      // Un-select any workspace Cohort if Pre Packaged cohort is selected
      if (!this.state.includesAllParticipants) {
        this.setState({includesAllParticipants: !this.state.includesAllParticipants, selectedCohortIds: []});
      } else {
        this.setState({includesAllParticipants: !this.state.includesAllParticipants});
      }
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
      return !this.state.selectedConceptSetIds ||
        (this.state.selectedConceptSetIds.length === 0 && this.state.selectedPrepackagedConceptSets.size === 0) ||
        ((!this.state.selectedCohortIds || this.state.selectedCohortIds.length === 0) && !this.state.includesAllParticipants) ||
        !this.state.selectedDomainValuePairs || this.state.selectedDomainValuePairs.length === 0;
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

    apiEnumToPrePackageConceptSets(v: Array<PrePackagedConceptSetEnum>): Set<PrepackagedConceptSet> {
      const re: Set<PrepackagedConceptSet> = new Set<PrepackagedConceptSet>();
      v.map(
        pre => {
          switch (pre) {
            case PrePackagedConceptSetEnum.BOTH: {
              re.add(PrepackagedConceptSet.PERSON), re.add(PrepackagedConceptSet.SURVEYS);
              break;
            }
            case PrePackagedConceptSetEnum.PERSON: {
              re.add(PrepackagedConceptSet.PERSON);
              break;
            }
            case PrePackagedConceptSetEnum.SURVEY: {
              re.add(PrepackagedConceptSet.SURVEYS);
              break;
            }
            case PrePackagedConceptSetEnum.FITBITHEARTRATESUMMARY: {
              re.add(PrepackagedConceptSet.FITBITHEARTRATESUMMARY);
              break;
            }
            case PrePackagedConceptSetEnum.FITBITHEARTRATELEVEL: {
              re.add(PrepackagedConceptSet.FITBITHEARTRATELEVEL);
              break;
            }
            case PrePackagedConceptSetEnum.FITBITINTRADAYSTEPS: {
              re.add(PrepackagedConceptSet.FITBITINTRADAYSTEPS);
              break;
            }
            case PrePackagedConceptSetEnum.FITBITACTIVITY: {
              re.add(PrepackagedConceptSet.FITBITACTIVITY);
              break;
            }
            case PrePackagedConceptSetEnum.WHOLEGENOME: {
              re.add(PrepackagedConceptSet.WHOLEGENOME);
              break;
            }
            case PrePackagedConceptSetEnum.NONE:
            default:
              break;
          }
        }
      );
      return re;

    }

    getPrePackagedConceptSetApiEnum() {
      const {selectedPrepackagedConceptSets} = this.state;
      const selectedPrePackagedConceptSDetEnum = new Array<PrePackagedConceptSetEnum>();
      selectedPrepackagedConceptSets.forEach(selectedPrepackagedConceptSet => {
        switch (selectedPrepackagedConceptSet) {
          case PrepackagedConceptSet.PERSON:
            selectedPrePackagedConceptSDetEnum.push(PrePackagedConceptSetEnum.PERSON);
            break;
          case PrepackagedConceptSet.SURVEYS:
            selectedPrePackagedConceptSDetEnum.push(PrePackagedConceptSetEnum.SURVEY);
            break;
          case PrepackagedConceptSet.FITBITACTIVITY:
            selectedPrePackagedConceptSDetEnum.push(PrePackagedConceptSetEnum.FITBITACTIVITY);
            break;
          case PrepackagedConceptSet.FITBITINTRADAYSTEPS:
            selectedPrePackagedConceptSDetEnum.push(PrePackagedConceptSetEnum.FITBITINTRADAYSTEPS);
            break;
          case PrepackagedConceptSet.FITBITHEARTRATESUMMARY:
            selectedPrePackagedConceptSDetEnum.push(PrePackagedConceptSetEnum.FITBITHEARTRATESUMMARY);
            break;
          case PrepackagedConceptSet.FITBITHEARTRATELEVEL:
            selectedPrePackagedConceptSDetEnum.push(PrePackagedConceptSetEnum.FITBITHEARTRATELEVEL);
            break;
          case PrepackagedConceptSet.WHOLEGENOME:
            selectedPrePackagedConceptSDetEnum.push(PrePackagedConceptSetEnum.WHOLEGENOME);
            break;

        }
      });
      return selectedPrePackagedConceptSDetEnum;
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
      if (domain === Domain.WHOLEGENOMEVARIANT) {
        this.setState(state => ({previewList: state.previewList.set(domain, {
          isLoading: false,
          errorText: null,
          values: []
        })}));
        return;
      }
      const {namespace, id} = this.props.workspace;
      const domainRequest: DataSetPreviewRequest = {
        domain: domain,
        conceptSetIds: this.state.selectedConceptSetIds,
        includesAllParticipants: this.state.includesAllParticipants,
        cohortIds: this.state.selectedCohortIds,
        prePackagedConceptSet: this.getPrePackagedConceptSetApiEnum(),
        values: this.state.selectedDomainValuePairs
            .filter(values => values.domain === domain)
            .map( domainValue => domainValue.value)
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

    async createDataset(name, desc) {
      AnalyticsTracker.DatasetBuilder.Create();
      const {namespace, id} = this.props.workspace;

      return dataSetApi()
        .createDataSet(namespace, id, this.createDatasetRequest(name, desc))
        .then(dataset => this.loadDataset(dataset));
    }

    async saveDataset() {
      AnalyticsTracker.DatasetBuilder.Save();
      const {namespace, id} = this.props.workspace;

      this.setState({savingDataset: true});
      dataSetApi().updateDataSet(namespace, id, this.state.dataSet.id, this.updateDatasetRequest())
        .then(dataset => this.loadDataset(dataset))
        .catch(e => {
          console.error(e);
          this.props.showErrorModal('Save Dataset Error', 'Please refresh and try again');
        })
        .finally(() => this.setState({savingDataset: false}));
    }

    createDatasetRequest(name, desc): DataSetRequest {
      return {
        name,
        description: desc,
        ...{
          includesAllParticipants: this.state.includesAllParticipants,
          conceptSetIds: this.state.selectedConceptSetIds,
          cohortIds: this.state.selectedCohortIds,
          domainValuePairs: this.state.selectedDomainValuePairs,
          prePackagedConceptSet: this.getPrePackagedConceptSetApiEnum()
        }
      };
    }

    updateDatasetRequest(): DataSetRequest {
      return {
        ...this.createDatasetRequest(
          this.state.dataSet.name,
          this.state.dataSet.description
        ),
        etag: this.state.dataSet.etag
      };
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
          return <FlexRow style={styles.errorMessage}>
            <ClrIcon
              shape={'warning-standard'}
              class={'is-solid'}
              size={26}
              style={{
                color: colors.warning,
                flex: '0 0 auto'
              }}
            />
            <div style={{paddingLeft: '0.25rem'}}>
              The preview table could not be loaded. Please try again by clicking the ‘View Preview Table’ as
              some queries take longer to load. If the error keeps happening, please <Link style={{
                display: 'inline-block'}} onClick={() => this.openZendeskWidget()}>contact us</Link>. You can also
              export your dataset directly for analysis by clicking the ‘Analyze’ button, without viewing the preview
              table.
            </div>
          </FlexRow>;
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
      let selectedPreviewDomain = this.state.selectedPreviewDomain.toString();
      // Had to do the following since typescript changes the key by removing _ therefore changing the domain string
      // which resulted in map check from selectedPreviewDomain to give undefined result always
      if (this.state.selectedPreviewDomain && this.state.selectedPreviewDomain.toString().startsWith('FITBIT')) {
        switch (this.state.selectedPreviewDomain.toString()) {
          case 'FITBITHEARTRATESUMMARY': selectedPreviewDomain = 'FITBIT_HEART_RATE_SUMMARY'; break;
          case 'FITBITHEARTRATELEVEL': selectedPreviewDomain = 'FITBIT_HEART_RATE_LEVEL'; break;
          case 'FITBITACTIVITY': selectedPreviewDomain = 'FITBIT_ACTIVITY'; break;
          case 'FITBITINTRADAYSTEPS': selectedPreviewDomain = 'FITBIT_INTRADAY_STEPS'; break;
        }
      }
      let filteredPreviewData;
      this.state.previewList.forEach((map, entry) => {
        if (entry.toString() === selectedPreviewDomain && !filteredPreviewData) {
          filteredPreviewData = map;
        }
      });
      return filteredPreviewData && filteredPreviewData.values.length > 0 ?
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
          <div>Generating preview for {domainDisplayed}</div> : <div>
            {filteredPreviewData.errorText && <div>{filteredPreviewData.errorText}</div>}
            {/* If there is no error that means no data was return*/}
            {!filteredPreviewData.errorText && <div>No Results found for {domainDisplayed}</div>}
            </div>
        }
      </div>;
    }

    onClickExport() {
      this.setState(state => {
        return {
          modalState: state.selectedDomains.has(Domain.WHOLEGENOMEVARIANT) ? ModalState.Extract : ModalState.Export
        };
      });
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
        previewList,
        selectedCohortIds,
        selectedConceptSetIds,
        selectedPreviewDomain,
        selectedDomains,
        selectedDomainValuePairs,
        selectedPrepackagedConceptSets
      } = this.state;
      const exportError = !this.canWrite ? 'Requires Owner or Writer permission' :
        dataSetTouched ? 'Pending changes must be saved' : '';

      return <React.Fragment>
        {this.state.savingDataset && <SpinnerOverlay opacity={0.3}/>}

        <FadeBox style={{paddingTop: '1rem'}}>
          <h2 style={{paddingTop: 0, marginTop: 0}}>Datasets{!this.isCreatingNewDataset &&
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
                  <ImmutableListItem name='All Participants' data-test-id='all-participant'
                                     checked={includesAllParticipants}
                                     onChange={
                                       () => this.selectPrePackagedCohort()}/>
                  <Subheader>Workspace Cohorts</Subheader>
                  {!loadingResources && this.state.cohortList.map(cohort =>
                    <ImmutableWorkspaceCohortListItem key={cohort.id} name={cohort.name}
                                      data-test-id='cohort-list-item'
                                      checked={selectedCohortIds.includes(cohort.id)}
                                      cohortId={cohort.id}
                                      namespace={namespace}
                                      wid={id}
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
                  <div style={{height: '9rem', overflowY: 'auto'}} data-test-id='prePackage-concept-set'>
                    <Subheader>Prepackaged Concept Sets</Subheader>
                    {this.getPrePackagedList()
                        .map((prepackaged: PrepackagedConceptSet) => {
                          const p = PrepackagedConceptSet[prepackaged];
                          return <ImmutableListItem name={p}  data-test-id='prePackage-concept-set-item'
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
                    <StyledAnchorTag href={supportUrls.dataDictionary} target='_blank'>
                      Learn more
                    </StyledAnchorTag>&nbsp;in the data dictionary
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
                    // Strip underscores so we get the correct enum value
                    const domainEnumValue = Domain[domain.replace(/_/g, '')];
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
                                 onClick={() => this.setState({selectedPreviewDomain: domainEnumValue})}
                                 style={stylesFunction.selectDomainForPreviewButton(selectedPreviewDomain === domainEnumValue)}>
                        <FlexRow style={{alignItems: 'center', overflow: 'auto', wordBreak: 'break-all'}}>
                          {formatDomainString(domain)}
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
        <div style={styles.stickyFooter}>
            <TooltipTrigger data-test-id='save-tooltip'
                            content='Requires Owner or Writer permission' disabled={this.canWrite}>
              {this.isCreatingNewDataset ?
                <Button style={{marginBottom: '2rem', marginRight: '1rem'}} data-test-id='save-button'
                        onClick ={() => this.setState({modalState: ModalState.Create})}
                        disabled={this.disableSave() || !this.canWrite || !dataSetTouched}>
                  Create Dataset
                </Button> :
                <Button style={{marginBottom: '2rem', marginRight: '1rem'}} data-test-id='save-button'
                        onClick ={() => this.saveDataset()}
                        disabled={this.state.savingDataset || this.disableSave() || !this.canWrite || !dataSetTouched}>
                  Save Dataset
                </Button>}
            </TooltipTrigger>

          <TooltipTrigger data-test-id='export-tooltip'
                          content={exportError}
                          disabled={!exportError}>
            <Button style={{marginBottom: '2rem'}} data-test-id='analyze-button'
                    onClick ={() => this.onClickExport()}
                    disabled={this.disableSave() || !!exportError}>
              Analyze
            </Button>
          </TooltipTrigger>
        </div>

        {
          switchCase(this.state.modalState,
            [ModalState.Create, () =>
              <CreateModal entityName='Dataset'
                           getExistingNames={async() => {
                             const resources = await workspacesApi()
                               .getWorkspaceResources(namespace, id, {typesToFetch: [ResourceType.DATASET]});
                             return resources.map(resource => resource.dataSet.name);
                           }}
                           save={(name, desc) => this.createDataset(name, desc)}
                           close={() => this.setState({modalState: ModalState.None})}/>],
            [ModalState.Export, () =>
              <ExportDatasetModal dataset={dataSet}
                                  closeFunction={() => this.setState({modalState: ModalState.None})}/>],
            [ModalState.Extract, () =>
              <GenomicExtractionModal dataSet={dataSet}
                                      workspaceNamespace={namespace}
                                      workspaceFirecloudName={id}
                                      title={'Would you like to extract genomic variant data as VCF files?'}
                                      cancelText={'Skip'}
                                      confirmText={'Extract & Continue'}
                                      closeFunction={() => this.setState({modalState: ModalState.Export})}/>
            ]
          )
        }

      </React.Fragment>;
    }
  });
