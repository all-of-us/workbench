import * as React from 'react';
import { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  CdrVersionTiersResponse,
  Cohort,
  ConceptSet,
  DataDictionaryEntry,
  DataSet,
  DataSetPreviewRequest,
  DataSetPreviewValueList,
  DataSetRequest,
  Domain,
  DomainValue,
  DomainValuePair,
  ErrorResponse,
  PrePackagedConceptSetEnum,
  Profile,
  ResourceType,
} from 'generated/fetch';

import { AlertInfo } from 'app/components/alert';
import {
  Button,
  Clickable,
  LinkButton,
  StyledExternalLink,
  StyledRouterLink,
} from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { CreateModal } from 'app/components/create-modal';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { CheckBox } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import {
  WithErrorModalProps,
  withErrorModalWrapper,
} from 'app/components/with-error-modal-wrapper';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { CircleWithText } from 'app/icons/circleWithText';
import { ExportDatasetModal } from 'app/pages/data/data-set/export-dataset-modal';
import { GenomicExtractionModal } from 'app/pages/data/data-set/genomic-extraction-modal';
import {
  cohortsApi,
  conceptSetsApi,
  dataSetApi,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  formatDomain,
  formatDomainString,
  reactStyles,
  switchCase,
  toggleIncludes,
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { getCdrVersion } from 'app/utils/cdr-versions';
import { currentWorkspaceStore, useNavigation } from 'app/utils/navigation';
import { apiCallWithGatewayTimeoutRetries } from 'app/utils/retry';
import { MatchParams, serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';
import { openZendeskWidget, supportUrls } from 'app/utils/zendesk';

export const styles = reactStyles({
  dataDictionaryHeader: {
    fontSize: '16px',
    color: colors.primary,
    textTransform: 'uppercase',
  },

  dataDictionarySubheader: {
    fontSize: '13px',
    fontWeight: 600,
    color: colors.primary,
    paddingTop: '0.75rem',
  },

  dataDictionaryText: {
    color: colors.primary,
    fontSize: '13px',
    lineHeight: '20px',
  },

  selectBoxHeader: {
    fontSize: '16px',
    height: '3rem',
    lineHeight: '3rem',
    paddingRight: '0.825rem',
    color: colors.primary,
    borderBottom: `1px solid ${colors.light}`,
    display: 'flex',
    justifyContent: 'space-between',
    flexDirection: 'row',
    minWidth: '22.5rem',
  },

  listItem: {
    border: `0.5px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
    margin: '.6rem .6rem .6rem .825rem',
    height: '2.25rem',
    display: 'flex',
  },

  listItemCheckbox: {
    height: 17,
    width: 17,
    marginLeft: 10,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: colors.success,
  },

  valueListItemCheckboxStyling: {
    height: 17,
    width: 17,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: colors.success,
  },

  subheader: {
    fontWeight: 400,
    fontSize: '0.9rem',
    marginTop: '0.75rem',
    paddingLeft: '0.825rem',
    color: colors.primary,
  },

  previewButtonBox: {
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginTop: '4.0125rem',
    marginBottom: '3rem',
  },

  previewDataHeaderBox: {
    display: 'flex',
    flexDirection: 'row',
    position: 'relative',
    lineHeight: 'auto',
    paddingTop: '0.75rem',
    paddingBottom: '0.75rem',
    paddingLeft: '0.75rem',
    paddingRight: '0.75rem',
    borderBottom: `1px solid ${colors.light}`,
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 'auto',
  },

  previewDataHeader: {
    height: '19px',
    width: 'auto',
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '16px',
    fontWeight: 600,
    marginBottom: '1.5rem',
    paddingRight: '2.25rem',
    justifyContent: 'space-between',
    display: 'flex',
  },

  warningMessage: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    height: '15rem',
    width: '60rem',
    alignSelf: 'center',
    marginTop: '3rem',
    fontSize: 18,
    fontWeight: 600,
    color: colorWithWhiteness(colors.dark, 0.6),
  },

  selectAllContainer: {
    marginLeft: 'auto',
    display: 'flex',
    alignItems: 'center',
  },
  previewLink: {
    marginTop: '0.75rem',
    height: '2.7rem',
    width: '9.75rem',
    color: colors.secondary,
  },
  stickyFooter: {
    backgroundColor: colors.white,
    borderTop: `1px solid ${colors.light}`,
    textAlign: 'right',
    padding: '3px 55px 50px 20px',
    position: 'sticky',
    bottom: '0',
    height: '60px',
    width: '100%',
  },
  errorMessage: {
    backgroundColor: colorWithWhiteness(colors.highlight, 0.5),
    color: colors.primary,
    fontSize: '12px',
    padding: '0.75rem',
    borderRadius: '0.5em',
  },
  cohortItemName: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  listItemName: {
    lineHeight: '2.25rem',
    color: colors.primary,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
});

const stylesFunction = {
  plusIconColor: (disabled) => {
    return {
      fill: disabled ? colorWithWhiteness(colors.dark, 0.4) : colors.accent,
    };
  },
  selectDomainForPreviewButton: (selected) => {
    return {
      marginLeft: '0.3rem',
      color: colors.accent,
      marginRight: '0.375rem',
      paddingBottom: '0.375rem',
      width: '10.5rem',
      borderBottom: selected ? '4px solid ' + colors.accent : '',
      fontWeight: selected ? 600 : 400,
      fontSize: '18px',
      display: 'flex',
      justifyContent: 'center',
      lineHeight: '19px',
    };
  },
};

const DOMAIN_DISPLAY_ORDER = {
  // Person domain is always first as the canonical primary table. Everything
  // else is alphabetized. To add further ordering constraints, add more
  // entries to this map with sort values.
  [Domain.PERSON]: 0,
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

const checkNameWidth = (element: HTMLDivElement) =>
  element.offsetWidth < element.scrollWidth;

const ImmutableListItem: React.FunctionComponent<{
  name: string;
  isSubitem: boolean;
  onChange: Function;
  checked: boolean;
  showSourceConceptIcon?: boolean;
}> = ({ name, isSubitem, onChange, checked, showSourceConceptIcon }) => {
  const [showNameTooltip, setShowNameTooltip] = useState(false);
  return (
    <div style={styles.listItem}>
      <input
        type='checkbox'
        value={name}
        onChange={() => onChange()}
        style={
          isSubitem
            ? { ...styles.listItemCheckbox, marginLeft: 30 }
            : styles.listItemCheckbox
        }
        checked={checked}
      />
      <TooltipTrigger disabled={!showNameTooltip} content={<div>{name}</div>}>
        <div
          style={styles.listItemName}
          onMouseOver={(e) =>
            setShowNameTooltip(checkNameWidth(e.target as HTMLDivElement))
          }
        >
          {showSourceConceptIcon && (
            <ClrIcon
              className='is-solid'
              shape='exclamation-triangle'
              size={20}
            />
          )}
          {name}
        </div>
      </TooltipTrigger>
    </div>
  );
};

const ImmutableWorkspaceCohortListItem: React.FunctionComponent<{
  name: string;
  onChange: Function;
  checked: boolean;
  cohortId: number;
  namespace: string;
  wid: string;
}> = ({ name, onChange, checked, cohortId, namespace, wid }) => {
  const [showNameTooltip, setShowNameTooltip] = useState(false);
  return (
    <div style={styles.listItem}>
      <input
        type='checkbox'
        value={name}
        onChange={() => onChange()}
        style={styles.listItemCheckbox}
        checked={checked}
      />
      <FlexRow
        style={{
          lineHeight: '2.25rem',
          color: colors.primary,
          width: '100%',
          minWidth: 0,
        }}
      >
        <TooltipTrigger disabled={!showNameTooltip} content={<div>{name}</div>}>
          <div
            style={styles.cohortItemName}
            onMouseOver={(e) =>
              setShowNameTooltip(checkNameWidth(e.target as HTMLDivElement))
            }
          >
            {name}
          </div>
        </TooltipTrigger>
        <div style={{ marginLeft: 'auto', paddingRight: '1.5rem' }}>
          <StyledRouterLink
            path={`/workspaces/${namespace}/${wid}/data/cohorts/${cohortId}/reviews/cohort-description`}
            target='_blank'
          >
            <ClrIcon size='20' shape='bar-chart' />
          </StyledRouterLink>
        </div>
      </FlexRow>
    </div>
  );
};

const Subheader = (props) => {
  return (
    <div style={{ ...styles.subheader, ...props.style }}>{props.children}</div>
  );
};

interface DataDictionaryPopupProps {
  dataDictionaryEntry: DataDictionaryEntry;
}

const DataDictionaryDescription: React.FunctionComponent<
  DataDictionaryPopupProps
> = ({ dataDictionaryEntry }) => {
  return (
    <div
      style={{
        width: '100%',
        borderTop: `1px solid ${colorWithWhiteness(colors.dark, 0.6)}`,
      }}
    >
      {dataDictionaryEntry ? (
        <FlexColumn style={{ padding: '0.75rem' }}>
          <div style={{ ...styles.dataDictionarySubheader, paddingTop: 0 }}>
            Description
          </div>
          <div style={styles.dataDictionaryText}>
            {dataDictionaryEntry.description}
          </div>
          <div style={styles.dataDictionarySubheader}>Relevant OMOP Table</div>
          <div style={styles.dataDictionaryText}>
            {dataDictionaryEntry.relevantOmopTable}
          </div>
          <div style={styles.dataDictionarySubheader}>Type</div>
          <div style={styles.dataDictionaryText}>
            {dataDictionaryEntry.fieldType}
          </div>
          <div style={styles.dataDictionarySubheader}>Data Provenance</div>
          <div style={styles.dataDictionaryText}>
            {dataDictionaryEntry.dataProvenance}
            {dataDictionaryEntry.dataProvenance.includes('PPI')
              ? `: ${dataDictionaryEntry.sourcePpiModule}`
              : null}
          </div>
        </FlexColumn>
      ) : (
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <Spinner style={{ height: 36, width: 36, margin: '0.75rem' }} />
        </div>
      )}
    </div>
  );
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
  ValueListItemProps,
  ValueListItemState
> {
  constructor(props) {
    super(props);
    this.state = {
      dataDictionaryEntry: undefined,
      dataDictionaryEntryError: false,
      showDataDictionaryEntry: false,
    };
  }

  fetchDataDictionaryEntry() {
    const { domain, domainValue } = this.props;

    dataSetApi()
      .getDataDictionaryEntry(
        parseInt(currentWorkspaceStore.getValue().cdrVersionId, 10),
        domain === Domain.PHYSICALMEASUREMENTCSS
          ? Domain.MEASUREMENT.toString()
          : domain.toString(),
        domainValue.value
      )
      .then((dataDictionaryEntry) => {
        this.setState({ dataDictionaryEntry });
      })
      .catch(() => {
        this.setState({ dataDictionaryEntryError: true });
      });
  }

  toggleDataDictionaryEntry() {
    this.setState({
      showDataDictionaryEntry: !this.state.showDataDictionaryEntry,
    });

    if (this.state.dataDictionaryEntry === undefined) {
      this.fetchDataDictionaryEntry();
    }
  }

  render() {
    const { checked, domainValue, onChange } = this.props;
    const {
      dataDictionaryEntry,
      dataDictionaryEntryError,
      showDataDictionaryEntry,
    } = this.state;

    return (
      <div
        style={{
          ...styles.listItem,
          height: 'auto',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingLeft: '10px',
          paddingRight: '10px',
        }}
      >
        <FlexRow style={{ width: '100%' }}>
          <input
            type='checkbox'
            value={domainValue.value}
            onChange={() => onChange()}
            style={{
              ...styles.listItemCheckbox,
              marginTop: 11,
              marginLeft: 0,
              marginRight: 0,
            }}
            checked={checked}
          />
          <div style={{ width: '100%' }}>
            <FlexRow
              style={{
                justifyContent: 'space-between',
                width: '100%',
                alignItems: 'center',
              }}
            >
              <div
                style={{
                  lineHeight: '2.25rem',
                  paddingLeft: 10,
                  wordWrap: 'break-word',
                  color: colors.primary,
                }}
              >
                {domainValue.value}
              </div>
              <Clickable
                onClick={() => this.toggleDataDictionaryEntry()}
                data-test-id='value-list-expander'
              >
                <ClrIcon
                  shape='angle'
                  style={{
                    transform: showDataDictionaryEntry
                      ? 'rotate(180deg)'
                      : 'rotate(90deg)',
                    color: colors.accent,
                    height: 18,
                    width: 18,
                  }}
                />
              </Clickable>
            </FlexRow>
            {!dataDictionaryEntryError && showDataDictionaryEntry && (
              <DataDictionaryDescription
                dataDictionaryEntry={dataDictionaryEntry}
              />
            )}
            {dataDictionaryEntryError && showDataDictionaryEntry && (
              <div>Data Dictionary Entry not found.</div>
            )}
          </div>
        </FlexRow>
      </div>
    );
  }
}

const PlusLink = ({
  dataTestId,
  path,
  disable,
}: {
  dataTestId: string;
  path: string;
  disable?: boolean;
}) => {
  const [, navigateByUrl] = useNavigation();

  return (
    <TooltipTrigger
      data-test-id='plus-icon-tooltip'
      disabled={!disable}
      content='Requires Owner or Writer permission'
    >
      <Clickable
        disabled={disable}
        data-test-id={dataTestId}
        href={path}
        onClick={(e) => {
          navigateByUrl(path, {
            preventDefaultIfNoKeysPressed: true,
            event: e,
          });
        }}
      >
        <ClrIcon
          shape='plus-circle'
          class='is-solid'
          size={16}
          style={stylesFunction.plusIconColor(disable)}
        />
      </Clickable>
    </TooltipTrigger>
  );
};

const StepNumber = ({ step, style = {} }) => {
  return (
    <CircleWithText
      text={step}
      width='23.78px'
      height='23.78px'
      style={{ fill: colorWithWhiteness(colors.primary, 0.5), ...style }}
    />
  );
};

const BoxHeader = ({
  step = '',
  header = '',
  subHeader = '',
  style = {},
  ...props
}) => {
  return (
    <div style={styles.selectBoxHeader}>
      <div style={{ display: 'flex', marginLeft: '0.3rem' }}>
        <StepNumber step={step} style={{ marginTop: '0.75rem' }} />
        <label
          style={{
            marginLeft: '0.75rem',
            color: colors.primary,
            display: 'flex',
            ...style,
          }}
        >
          <div style={{ fontWeight: 600, marginRight: '0.45rem' }}>
            {header}
          </div>
          ({subHeader})
        </label>
      </div>
      {props.children}
    </div>
  );
};

// TODO(RW-3508): Refactor the API model for prepackaged concept sets to be less
// rigid, and more extensible to future additions of prepackaged concept sets.
// For now, this client-side enum tracks the desired state: a set of selectable
// prepackaged concept sets.

const prepackagedAllSurveyConceptSetToString = {
  PERSON: 'Demographics',
  SURVEY: 'All Surveys',
  FITBIT_HEART_RATE_SUMMARY: 'Fitbit Heart Rate Summary',
  FITBIT_ACTIVITY: 'Fitbit Activity Summary',
  FITBIT_HEART_RATE_LEVEL: 'Fitbit Heart Rate Level',
  FITBIT_INTRADAY_STEPS: 'Fitbit Intra Day Steps',
  FITBIT_SLEEP_DAILY_SUMMARY: 'Fitbit Sleep Daily Summary',
  FITBIT_SLEEP_LEVEL: 'Fitbit Sleep Level',
  WHOLE_GENOME: 'Short Read Whole Genome Sequencing Data',
  ZIP_CODE_SOCIOECONOMIC: 'Zip Code Socioeconomic Status Data',
};

const prepackagedSurveyConceptSetToString = {
  SURVEY_BASICS: 'The Basics',
  SURVEY_LIFESTYLE: 'Lifestyle',
  SURVEY_OVERALL_HEALTH: 'Overall Health',
  SURVEY_HEALTHCARE_ACCESS_UTILIZATION: 'Healthcare Access & Utilization',
  SURVEY_COPE: 'COVID-19 Paricipant Experience(COPE)',
  SURVEY_SDOH: 'Social Determinants of Health',
  SURVEY_COVID_VACCINE: 'COVID-19 Vaccine',
  SURVEY_PFHH: 'Personal and Family Health History',
};

const PREPACKAGED_SURVEY_PERSON_DOMAIN = {
  [PrePackagedConceptSetEnum.PERSON]: Domain.PERSON,
  [PrePackagedConceptSetEnum.SURVEY]: Domain.SURVEY,
};

const PREPACKAGED_SURVEY_DOMAINS = {
  [PrePackagedConceptSetEnum.SURVEYBASICS]: Domain.SURVEY,
  [PrePackagedConceptSetEnum.SURVEYLIFESTYLE]: Domain.SURVEY,
  [PrePackagedConceptSetEnum.SURVEYOVERALLHEALTH]: Domain.SURVEY,
  [PrePackagedConceptSetEnum.SURVEYHEALTHCAREACCESSUTILIZATION]: Domain.SURVEY,
  [PrePackagedConceptSetEnum.SURVEYCOPE]: Domain.SURVEY,
  [PrePackagedConceptSetEnum.SURVEYSDOH]: Domain.SURVEY,
  [PrePackagedConceptSetEnum.SURVEYCOVIDVACCINE]: Domain.SURVEY,
  [PrePackagedConceptSetEnum.SURVEYPFHH]: Domain.SURVEY,
};

const PREPACKAGED_WITH_FITBIT_DOMAINS = {
  [PrePackagedConceptSetEnum.FITBITHEARTRATESUMMARY]:
    Domain.FITBITHEARTRATESUMMARY,
  [PrePackagedConceptSetEnum.FITBITACTIVITY]: Domain.FITBITACTIVITY,
  [PrePackagedConceptSetEnum.FITBITHEARTRATELEVEL]: Domain.FITBITHEARTRATELEVEL,
  [PrePackagedConceptSetEnum.FITBITINTRADAYSTEPS]: Domain.FITBITINTRADAYSTEPS,
};

const PREPACKAGED_WITH_FITBIT_SLEEP_DOMAINS = {
  [PrePackagedConceptSetEnum.FITBITSLEEPDAILYSUMMARY]:
    Domain.FITBITSLEEPDAILYSUMMARY,
  [PrePackagedConceptSetEnum.FITBITSLEEPLEVEL]: Domain.FITBITSLEEPLEVEL,
};

const PREPACKAGED_WITH_WHOLE_GENOME = {
  [PrePackagedConceptSetEnum.WHOLEGENOME]: Domain.WHOLEGENOMEVARIANT,
};

const PREPACKAGED_WITH_ZIP_CODE_SOCIOECONOMIC = {
  [PrePackagedConceptSetEnum.ZIPCODESOCIOECONOMIC]: Domain.ZIPCODESOCIOECONOMIC,
};
let PREPACKAGED_DOMAINS = {};
let prepackagedConceptSetToString = {};

// For converting domain strings to type Domain
const reverseDomainEnum = {
  OBSERVATION: Domain.OBSERVATION,
  PROCEDURE: Domain.PROCEDURE,
  DRUG: Domain.DRUG,
  CONDITION: Domain.CONDITION,
  MEASUREMENT: Domain.MEASUREMENT,
  DEVICE: Domain.DEVICE,
  DEATH: Domain.DEATH,
  VISIT: Domain.VISIT,
  SURVEY: Domain.SURVEY,
  PERSON: Domain.PERSON,
  PHYSICAL_MEASUREMENT: Domain.PHYSICALMEASUREMENT,
  ALL_EVENTS: Domain.ALLEVENTS,
  LAB: Domain.LAB,
  VITAL: Domain.VITAL,
  FITBIT: Domain.FITBIT,
  FITBIT_HEART_RATE_SUMMARY: Domain.FITBITHEARTRATESUMMARY,
  FITBIT_HEART_RATE_LEVEL: Domain.FITBITHEARTRATELEVEL,
  FITBIT_ACTIVITY: Domain.FITBITACTIVITY,
  FITBIT_INTRADAY_STEPS: Domain.FITBITINTRADAYSTEPS,
  FITBIT_SLEEP_DAILY_SUMMARY: Domain.FITBITSLEEPDAILYSUMMARY,
  FITBIT_SLEEP_LEVEL: Domain.FITBITSLEEPLEVEL,
  PHYSICAL_MEASUREMENT_CSS: Domain.PHYSICALMEASUREMENTCSS,
  WHOLE_GENOME_VARIANT: Domain.WHOLEGENOMEVARIANT,
  ZIP_CODE_SOCIOECONOMIC: Domain.ZIPCODESOCIOECONOMIC,
  ARRAY_DATA: Domain.ARRAYDATA,
};

// Temp workaround to prevent errors from mismatched upper and lower case values
function domainValuePairsToLowercase(domainValuePairs: DomainValuePair[]) {
  return domainValuePairs.map(({ domain, value }) => ({
    domain,
    value: value.toLowerCase(),
  }));
}

interface DomainWithConceptSetId {
  domain: Domain;
  conceptSetId: number;
}

interface DataSetPreviewInfo {
  isLoading: boolean;
  errorText: JSX.Element;
  values?: Array<DataSetPreviewValueList>;
}

interface Props
  extends WithErrorModalProps,
    WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {
  workspace: WorkspaceData;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  profileState: {
    profile: Profile;
    reload: Function;
  };
}

enum ModalState {
  None,
  Create,
  Export,
  Extract,
}

export const DatasetPage = fp.flow(
  withUserProfile(),
  withCurrentWorkspace(),
  withCdrVersions(),
  withErrorModalWrapper(),
  withRouter
)(
  ({
    cdrVersionTiersResponse,
    hideSpinner,
    match: {
      params: { dataSetId },
    },
    profileState: { profile },
    showErrorModal,
    workspace,
  }: Props) => {
    const [cohortList, setCohortList] = useState([]);
    const [conceptSetList, setConceptSetList] = useState([]);
    const [crossDomainConceptSetList, setCrossDomainConceptSetList] = useState(
      new Set()
    );
    const [dataSet, setDataSet] = useState(undefined);
    const [dataSetTouched, setDataSetTouched] = useState(false);
    const [domainValueSetIsLoading, setDomainValueSetIsLoading] = useState(
      new Set()
    );
    const [domainValueSetLookup, setDomainValueSetLookup] = useState(new Map());
    const [headerTooltipId, setHeaderTooltipId] = useState(undefined);
    const [includesAllParticipants, setIncludesAllParticipants] =
      useState(false);
    const [loadingResources, setLoadingResources] = useState(true);
    const [modalState, setModalState] = useState(ModalState.None);
    const [previewList, setPreviewList] = useState(new Map());
    const [selectedCohortIds, setSelectedCohortIds] = useState([]);
    const [selectedConceptSetIds, setSelectedConceptSetIds] = useState([]);
    const [selectedDomains, setSelectedDomains] = useState(new Set());
    const [
      selectedDomainsWithConceptSetIds,
      setSelectedDomainsWithConceptSetIds,
    ] = useState(new Set());
    const [selectedPreviewDomain, setSelectedPreviewDomain] = useState(
      Domain.CONDITION
    );
    const [selectedPrepackagedConceptSets, setSelectedPrepackagedConceptSets] =
      useState([]);
    const [selectedDomainValuePairs, setSelectedDomainValuePairs] = useState(
      []
    );
    const [savingDataset, setSavingDataset] = useState(false);

    const updatePrepackagedDomains = () => {
      PREPACKAGED_DOMAINS = PREPACKAGED_SURVEY_PERSON_DOMAIN;
      prepackagedConceptSetToString = prepackagedAllSurveyConceptSetToString;
      const { hasFitbitData, hasFitbitSleepData, hasWgsData } = getCdrVersion(
        workspace,
        cdrVersionTiersResponse
      );
      PREPACKAGED_DOMAINS = {
        ...PREPACKAGED_DOMAINS,
        ...PREPACKAGED_SURVEY_DOMAINS,
      };
      prepackagedConceptSetToString = {
        ...prepackagedConceptSetToString,
        ...prepackagedSurveyConceptSetToString,
      };
      if (hasFitbitData) {
        PREPACKAGED_DOMAINS = {
          ...PREPACKAGED_DOMAINS,
          ...PREPACKAGED_WITH_FITBIT_DOMAINS,
        };
      }
      if (hasFitbitSleepData) {
        PREPACKAGED_DOMAINS = {
          ...PREPACKAGED_DOMAINS,
          ...PREPACKAGED_WITH_FITBIT_SLEEP_DOMAINS,
        };
      }
      // Only allow selection of genomics prepackaged concept sets if genomics
      // data extraction is possible, since extraction is the only action that
      // can be taken on genomics variant data from the dataset builder.
      if (
        serverConfigStore.get().config.enableGenomicExtraction &&
        hasWgsData
      ) {
        PREPACKAGED_DOMAINS = {
          ...PREPACKAGED_DOMAINS,
          ...PREPACKAGED_WITH_WHOLE_GENOME,
        };
      }
      // Add Zipcode Socioeconomic status data if were in controlled tier dataset
      if (workspace.accessTierShortName === 'controlled') {
        PREPACKAGED_DOMAINS = {
          ...PREPACKAGED_DOMAINS,
          ...PREPACKAGED_WITH_ZIP_CODE_SOCIOECONOMIC,
        };
      }
    };

    const getDomainsFromDataSet = (d: DataSet) => {
      const domains = domainValuePairsToLowercase(d.domainValuePairs).map(
        ({ domain }) => domain
      );
      return new Set(domains);
    };

    const getIdsAndDomainsFromConceptSets = (
      conceptSets: ConceptSet[],
      prepackagedConceptSets: PrePackagedConceptSetEnum[]
    ): DomainWithConceptSetId[] => {
      return conceptSets
        .map((cs) => ({
          conceptSetId: cs.id,
          domain:
            cs.domain === Domain.PHYSICALMEASUREMENT
              ? Domain.MEASUREMENT
              : cs.domain,
        }))
        .concat(
          prepackagedConceptSets.map((p) => ({
            conceptSetId: null,
            domain: PREPACKAGED_DOMAINS[p],
          }))
        );
    };

    const getDomainsWithConceptSetIdsFromDataSet = (d: DataSet) => {
      return getIdsAndDomainsFromConceptSets(
        d.conceptSets,
        d.prePackagedConceptSet
      );
    };

    const loadValueSetForDomain = async (
      domainWithConceptSetId: DomainWithConceptSetId
    ) => {
      // TODO(RW-4426): There is a lot of complexity here around loading domain
      // values which is static data for a given CDR version. Consider
      // refactoring this page to load all schema data before rendering.
      const { namespace, id } = workspace;
      const updatedCrossDomainConceptSetList = crossDomainConceptSetList;
      const updatedSelectedDomainsWithConceptSetIds =
        selectedDomainsWithConceptSetIds;
      const updatedSelectedDomains = selectedDomains;
      let updatedSelectedDomainValuePairs = selectedDomainValuePairs;
      const newLoading = new Set(domainValueSetIsLoading);
      const newLookup = new Map(domainValueSetLookup);
      const values = await dataSetApi().getValuesFromDomain(
        namespace,
        id,
        domainWithConceptSetId.domain.toString(),
        domainWithConceptSetId.conceptSetId
      );
      // Delete the selected domain - conceptId pair and add the domains from the getValuesFromDomain response
      updatedSelectedDomainsWithConceptSetIds.delete(domainWithConceptSetId);
      values.items.forEach((domainWithDomainValues) => {
        const domain = reverseDomainEnum[domainWithDomainValues.domain];
        if (
          ![domain, Domain.PHYSICALMEASUREMENTCSS].includes(
            domainWithConceptSetId.domain
          )
        ) {
          updatedCrossDomainConceptSetList.add(
            domainWithConceptSetId.conceptSetId
          );
        }
        updatedSelectedDomainsWithConceptSetIds.add({
          conceptSetId: domainWithConceptSetId.conceptSetId,
          domain,
        });
        // If the domain has already been loaded, autoselect all of
        // its value pairs. The desired product behavior is that when a new
        // set of domain values appears, all boxes begin as checked.
        if (domainValueSetLookup.has(domain)) {
          const morePairs = domainValueSetLookup
            .get(domain)
            .values.map((v) => ({
              domain: reverseDomainEnum[domainWithDomainValues.domain],
              value: v.value,
            }));
          updatedSelectedDomains.add(domain);
          updatedSelectedDomainValuePairs =
            updatedSelectedDomainValuePairs.concat(morePairs);
          // If any of the domain has not yet been loaded, add the schema
          // (value sets) for it.
        } else {
          newLookup.set(domain, {
            domain,
            values: domainWithDomainValues.items,
          });

          // Autoselect the newly added domain, except if we're editing an
          // existing dataset which already covers the domain. This avoids having
          // us overwrite the selected pairs on initial load.
          const morePairs = [];
          if (!dataSet || !getDomainsFromDataSet(dataSet).has(domain)) {
            domainWithDomainValues.items.forEach((v) =>
              morePairs.push({
                domain: reverseDomainEnum[domainWithDomainValues.domain],
                value: v.value,
              })
            );
            updatedSelectedDomainValuePairs =
              updatedSelectedDomainValuePairs.concat(morePairs);
          }
          updatedSelectedDomains.add(domain);
        }
      });
      newLoading.delete(domainWithConceptSetId.domain);
      setDomainValueSetLookup(newLookup);
      setDomainValueSetIsLoading(newLoading);
      setSelectedDomainsWithConceptSetIds(
        updatedSelectedDomainsWithConceptSetIds
      );
      setCrossDomainConceptSetList(updatedCrossDomainConceptSetList);
      setSelectedDomains(updatedSelectedDomains);
      setSelectedDomainValuePairs(updatedSelectedDomainValuePairs);
    };

    const loadValueSetsForDataset = async (loadedDataset: DataSet) => {
      // TODO(RW-4426): There is a lot of complexity here around loading domain
      // values which is static data for a given CDR version. Consider
      // refactoring this page to load all schema data before rendering.
      const { namespace, id } = workspace;
      const newCrossDomainConceptSetList = new Set();
      const domainsWithConceptSetIds =
        getDomainsWithConceptSetIdsFromDataSet(loadedDataset);
      const updatedSelectedDomains = getDomainsFromDataSet(loadedDataset);
      const newLoading = new Set(domainValueSetIsLoading);
      const newLookup = new Map(domainValueSetLookup);
      const promises = domainsWithConceptSetIds.map(
        ({ conceptSetId, domain }) =>
          dataSetApi().getValuesFromDomain(
            namespace,
            id,
            domain.toString(),
            conceptSetId
          )
      );
      const domainsWithValues = await Promise.all(promises);
      domainsWithValues.forEach((values, index) => {
        const domainWithConceptSetId = domainsWithConceptSetIds[index];
        values.items.forEach((domainWithDomainValues) => {
          const domain = reverseDomainEnum[domainWithDomainValues.domain];
          if (
            ![domain, Domain.PHYSICALMEASUREMENTCSS].includes(
              domainWithConceptSetId.domain
            )
          ) {
            newCrossDomainConceptSetList.add(
              domainWithConceptSetId.conceptSetId
            );
          }
          const updateIndex = domainsWithConceptSetIds.findIndex(
            (dc) =>
              dc.conceptSetId !== null &&
              dc.conceptSetId === domainWithConceptSetId.conceptSetId
          );
          if (updateIndex > -1) {
            domainsWithConceptSetIds[updateIndex].domain = domain;
          }
          if (!domainValueSetLookup.has(domain)) {
            newLookup.set(domain, {
              domain,
              values: domainWithDomainValues.items,
            });
          }
          updatedSelectedDomains.add(domain);
        });
        newLoading.delete(domainWithConceptSetId.domain);
      });
      setDomainValueSetLookup(newLookup);
      setDomainValueSetIsLoading(newLoading);
      setSelectedDomainsWithConceptSetIds(new Set(domainsWithConceptSetIds));
      setCrossDomainConceptSetList(newCrossDomainConceptSetList);
      setSelectedDomains(updatedSelectedDomains);
    };

    const loadDataset = (dataset: DataSet, initialLoad?: boolean) => {
      // This is to set the URL to reference the newly created dataset and the user is staying on the dataset page
      // A bit of a hack but I couldn't find another way to change the URL without triggering a reload
      if (window.location.href.endsWith('data-sets')) {
        history.pushState({}, '', `${window.location.href}/${dataset.id}`);
      }
      setDataSet(dataset);
      setDataSetTouched(false);
      // We only need to set selections on the initial load of a saved dataset,
      // not for creating/updating since the selections will already be set
      if (initialLoad) {
        setIncludesAllParticipants(dataset.includesAllParticipants);
        setSelectedConceptSetIds(dataset.conceptSets.map((cs) => cs.id));
        setSelectedCohortIds(dataset.cohorts.map((c) => c.id));
        setSelectedDomainValuePairs(
          domainValuePairsToLowercase(dataset.domainValuePairs)
        );
        setSelectedPrepackagedConceptSets(dataset.prePackagedConceptSet);
        setDomainValueSetIsLoading(
          new Set(dataset.conceptSets.map(({ domain }) => domain))
        );
        loadValueSetsForDataset(dataset);
      }
    };

    const loadResources = async () => {
      try {
        const { namespace, id } = workspace;
        const resourceCalls: Array<Promise<any>> = [
          conceptSetsApi().getConceptSetsInWorkspace(namespace, id),
          cohortsApi().getCohortsInWorkspace(namespace, id),
        ];
        if (dataSetId) {
          resourceCalls.push(
            dataSetApi().getDataSet(namespace, id, +dataSetId)
          );
        }
        const [conceptSets, cohorts, dataset] = await Promise.all(
          resourceCalls
        );
        setConceptSetList(conceptSets.items);
        setCohortList(cohorts.items);
        setLoadingResources(false);
        if (dataset) {
          loadDataset(dataset, true);
        }
      } catch (error) {
        console.error(error);
      }
    };

    useEffect(() => {
      hideSpinner();
      updatePrepackagedDomains();
      loadResources();
    }, [dataSetId]);

    const getPrePackagedList = () => {
      // Use PREPACKAGED_DOMAINS to filter and return only prepackaged sets for this CDR version
      return Object.keys(PrePackagedConceptSetEnum).filter((prepackaged) =>
        Object.keys(PREPACKAGED_DOMAINS).includes(
          PrePackagedConceptSetEnum[prepackaged]
        )
      );
    };

    const selectPrePackagedConceptSet = (
      prepackaged: PrePackagedConceptSetEnum,
      selected: boolean
    ) => {
      const updatedPrepackaged = new Set(selectedPrepackagedConceptSets);
      const updatedDomainsWithConceptSetIds = new Set(
        selectedDomainsWithConceptSetIds
      );
      if (selected) {
        updatedPrepackaged.add(prepackaged);
        updatedDomainsWithConceptSetIds.add({
          conceptSetId: null,
          domain: PREPACKAGED_DOMAINS[prepackaged],
        });
        // check surveys
        if (prepackaged === PrePackagedConceptSetEnum.SURVEY) {
          Object.keys(PREPACKAGED_SURVEY_DOMAINS).forEach((prepackagedSurvey) =>
            updatedPrepackaged.add(prepackagedSurvey)
          );
        } else if (
          prepackaged !== PrePackagedConceptSetEnum.SURVEY &&
          Object.keys(PREPACKAGED_SURVEY_DOMAINS).every((key) =>
            updatedPrepackaged.has(key.valueOf())
          )
        ) {
          updatedPrepackaged.add(PrePackagedConceptSetEnum.SURVEY);
        }
      } else {
        // check if unselected is survey
        if (prepackaged === PrePackagedConceptSetEnum.SURVEY) {
          Object.keys(PREPACKAGED_SURVEY_DOMAINS).forEach((prepackagedSurvey) =>
            updatedPrepackaged.delete(prepackagedSurvey)
          );
        }
        // if *any* of the individual survey is unselected, unselect all-surveys
        // code here ...
        if (
          prepackaged !== PrePackagedConceptSetEnum.SURVEY &&
          updatedPrepackaged.has(prepackaged) &&
          prepackaged.toString().startsWith('SURVEY_')
        ) {
          updatedPrepackaged.delete(PrePackagedConceptSetEnum.SURVEY);
        }
        updatedPrepackaged.delete(prepackaged);
        updatedDomainsWithConceptSetIds.forEach(
          (domainWithConceptSetId: DomainWithConceptSetId) => {
            if (
              (PREPACKAGED_DOMAINS[prepackaged] === Domain.SURVEY &&
                domainWithConceptSetId.domain === Domain.SURVEY &&
                domainWithConceptSetId.conceptSetId === null &&
                !Array.from(updatedPrepackaged).some((domain) =>
                  domain.toString().includes('SURVEY')
                )) ||
              (PREPACKAGED_DOMAINS[prepackaged] !== Domain.SURVEY &&
                domainWithConceptSetId.conceptSetId === null &&
                domainWithConceptSetId.domain ===
                  PREPACKAGED_DOMAINS[prepackaged])
            ) {
              updatedDomainsWithConceptSetIds.delete(domainWithConceptSetId);
            }
          }
        );
        setSelectedDomains(
          new Set(
            Array.from(updatedDomainsWithConceptSetIds).map(
              ({ domain }) => domain
            )
          )
        );
        setSelectedDomainValuePairs(
          selectedDomainValuePairs.filter((p) =>
            Array.from(updatedDomainsWithConceptSetIds).some(
              (d: DomainWithConceptSetId) => d.domain === p.domain
            )
          )
        );
      }
      setSelectedDomainsWithConceptSetIds(updatedDomainsWithConceptSetIds);
      setSelectedPrepackagedConceptSets(Array.from(updatedPrepackaged));
      setDataSetTouched(true);
      if (selected) {
        setDomainValueSetIsLoading(
          domainValueSetIsLoading.add(PREPACKAGED_DOMAINS[prepackaged])
        );
        loadValueSetForDomain({
          conceptSetId: null,
          domain: PREPACKAGED_DOMAINS[prepackaged],
        });
      }
    };

    const selectConceptSet = (conceptSet: ConceptSet, selected: boolean) => {
      let updatedConceptSetIds: number[];
      const updatedDomainsWithConceptSetIds = new Set(
        selectedDomainsWithConceptSetIds
      );
      if (selected) {
        updatedConceptSetIds = selectedConceptSetIds.concat([conceptSet.id]);
        updatedDomainsWithConceptSetIds.add({
          conceptSetId: conceptSet.id,
          domain: conceptSet.domain,
        });
      } else {
        updatedConceptSetIds = fp.pull(conceptSet.id, selectedConceptSetIds);
        // Iterate the set since it's possible to have multiple domains per concept set for some source concepts
        updatedDomainsWithConceptSetIds.forEach(
          (domainWithConceptSetId: DomainWithConceptSetId) => {
            if (conceptSet.id === domainWithConceptSetId.conceptSetId) {
              updatedDomainsWithConceptSetIds.delete(domainWithConceptSetId);
            }
          }
        );
        crossDomainConceptSetList.delete(conceptSet.id);
        setSelectedDomains(
          new Set(
            Array.from(updatedDomainsWithConceptSetIds).map(
              ({ domain }) => domain
            )
          )
        );
        setSelectedDomainValuePairs(
          selectedDomainValuePairs.filter((p) =>
            Array.from(updatedDomainsWithConceptSetIds).some(
              (d: DomainWithConceptSetId) => d.domain === p.domain
            )
          )
        );
      }
      setCrossDomainConceptSetList(crossDomainConceptSetList);
      setSelectedDomainsWithConceptSetIds(updatedDomainsWithConceptSetIds);
      setSelectedConceptSetIds(updatedConceptSetIds);
      setDataSetTouched(true);
      if (selected) {
        setDomainValueSetIsLoading(
          domainValueSetIsLoading.add(conceptSet.domain)
        );
        loadValueSetForDomain({
          conceptSetId: conceptSet.id,
          domain: conceptSet.domain,
        });
      }
    };

    const selectCohort = (cohort: Cohort) => {
      const selectedCohortList = toggleIncludes(cohort.id, selectedCohortIds);
      setDataSetTouched(true);
      setSelectedCohortIds(selectedCohortList);
      // If Workspace Cohort is selected, un-select Pre packaged cohort
      if (selectedCohortList && selectedCohortList.length > 0) {
        setIncludesAllParticipants(false);
      }
    };

    const selectPrePackagedCohort = () => {
      setDataSetTouched(true);
      setIncludesAllParticipants(!includesAllParticipants);
      // Un-select any workspace Cohort if Pre Packaged cohort is selected
      setSelectedCohortIds(!includesAllParticipants ? [] : selectedCohortIds);
    };

    const selectDomainValue = (domain: Domain, domainValue: DomainValue) => {
      const valueSets = domainValueSetLookup.get(domain).values;
      const origSelected = selectedDomainValuePairs;
      const selectObj = { domain: domain, value: domainValue.value };
      let valuesSelected;
      if (fp.some(selectObj, origSelected)) {
        valuesSelected = fp.remove(
          (dv) =>
            dv.domain === selectObj.domain && dv.value === selectObj.value,
          origSelected
        );
      } else {
        valuesSelected = origSelected.concat(selectObj);
      }
      // Sort the values selected as per the order display rather than appending top end
      valuesSelected = valuesSelected.sort(
        (a, b) =>
          valueSets.findIndex(({ value }) => a.value === value) -
          valueSets.findIndex(({ value }) => b.value === value)
      );
      setSelectedDomainValuePairs(valuesSelected);
      setDataSetTouched(true);
    };

    const valuesCount = () => {
      let count = 0;
      selectedDomains.forEach((d) => {
        // Only counted loaded domains.
        const v = domainValueSetLookup.get(d);
        if (v) {
          count += v.values.length;
        }
      });
      return count;
    };

    // Returns true if selected values set is empty or is not equal to the total values displayed
    const allValuesSelected = () => {
      return (
        !fp.isEmpty(selectedDomainValuePairs) &&
        selectedDomainValuePairs.length === valuesCount()
      );
    };

    const selectAllValues = () => {
      if (allValuesSelected) {
        setSelectedDomainValuePairs([]);
        return;
      } else {
        const selectedValuesList = [];
        domainValueSetLookup.forEach((valueSet) => {
          valueSet.values.map((value) => {
            selectedValuesList.push({
              domain: valueSet.domain,
              value: value.value,
            });
          });
        });
        setSelectedDomainValuePairs(selectedValuesList);
      }
    };

    const canWrite = () => {
      return WorkspacePermissionsUtil.canWrite(workspace.accessLevel);
    };

    const disableSave = () => {
      return (
        !selectedConceptSetIds ||
        (selectedConceptSetIds.length === 0 &&
          selectedPrepackagedConceptSets.length === 0) ||
        ((!selectedCohortIds || selectedCohortIds.length === 0) &&
          !includesAllParticipants) ||
        !selectedDomainValuePairs ||
        selectedDomainValuePairs.length === 0
      );
    };

    const getDataTableValue = (data) => {
      // convert data model from api :
      // [{value[0]: '', queryValue: []}, {value[1]: '', queryValue: []}]
      // to compatible with DataTable
      // {value[0]: queryValue[0], value[1]: queryValue[1]}

      return fp.flow(
        fp.map(({ value, queryValue }) =>
          fp.map((v) => [value, v], queryValue)
        ),
        fp.unzip,
        fp.map(fp.fromPairs)
      )(data);
    };

    // TODO: Move to using a response based error handling method, rather than a error based one
    const generateErrorTextFromPreviewException = (
      exceptionResponse: ErrorResponse
    ): JSX.Element => {
      switch (exceptionResponse.statusCode) {
        case 400:
          return <div>{exceptionResponse.message}</div>;
        case 404:
          return <div>{exceptionResponse.message}</div>;
        case 504:
          return (
            <div>
              The preview table cannot be loaded because the query took too long
              to run. Please export this Dataset to a Notebook by clicking the
              Analyze button.
            </div>
          );
        default:
          return (
            <FlexRow style={styles.errorMessage}>
              <ClrIcon
                shape={'warning-standard'}
                class={'is-solid'}
                size={26}
                style={{
                  color: colors.warning,
                  flex: '0 0 auto',
                }}
              />
              <div style={{ paddingLeft: '0.375rem' }}>
                The preview table could not be loaded. Please try again by
                clicking the ‘View Preview Table’ as some queries take longer to
                load. If the error keeps happening, please{' '}
                <LinkButton
                  style={{
                    display: 'inline-block',
                  }}
                  onClick={() =>
                    openZendeskWidget(
                      profile.givenName,
                      profile.familyName,
                      profile.username,
                      profile.contactEmail
                    )
                  }
                >
                  contact us
                </LinkButton>
                . You can also export your dataset directly for analysis by
                clicking the ‘Analyze’ button, without viewing the preview
                table.
              </div>
            </FlexRow>
          );
      }
    };

    const getPreviewByDomain = async (domain: Domain) => {
      if (domain === Domain.WHOLEGENOMEVARIANT) {
        setPreviewList(
          (prevPreviewList) =>
            new Map(
              prevPreviewList.set(domain, {
                isLoading: false,
                errorText: null,
                values: [],
              })
            )
        );
        return;
      }
      const { namespace, id } = workspace;
      const domainRequest: DataSetPreviewRequest = {
        domain: domain,
        conceptSetIds: selectedConceptSetIds,
        includesAllParticipants: includesAllParticipants,
        cohortIds: selectedCohortIds,
        prePackagedConceptSet: selectedPrepackagedConceptSets,
        values: selectedDomainValuePairs
          .filter((values) => values.domain === domain)
          .map((domainValue) => domainValue.value),
      };
      let newPreviewInformation;
      try {
        const domainPreviewResponse = await apiCallWithGatewayTimeoutRetries(
          () =>
            dataSetApi().previewDataSetByDomain(namespace, id, domainRequest)
        );
        newPreviewInformation = {
          isLoading: false,
          errorText: null,
          values: domainPreviewResponse.values,
        };
      } catch (ex) {
        const exceptionResponse = (await ex.json()) as unknown as ErrorResponse;
        const errorText =
          generateErrorTextFromPreviewException(exceptionResponse);
        newPreviewInformation = {
          isLoading: false,
          errorText: errorText,
          values: [],
        };
      }
      setPreviewList(
        (prevList) => new Map(prevList.set(domain, newPreviewInformation))
      );
    };

    const getPreviewList = async () => {
      const domains = fp.uniq(
        selectedDomainValuePairs.map((domainValue) => domainValue.domain)
      );
      const newPreviewList: Map<Domain, DataSetPreviewInfo> = new Map(
        domains.map<[Domain, DataSetPreviewInfo]>((domain) => [
          domain,
          {
            isLoading: true,
            errorText: null,
            values: [],
          },
        ])
      );
      setPreviewList(newPreviewList);
      setSelectedPreviewDomain(domains[0]);
      domains.forEach((domain) => getPreviewByDomain(domain));
    };

    const createDatasetRequest = (datasetName, desc): DataSetRequest => {
      return {
        name: datasetName,
        description: desc,
        ...{
          includesAllParticipants,
          conceptSetIds: selectedConceptSetIds,
          cohortIds: selectedCohortIds,
          domainValuePairs: selectedDomainValuePairs,
          prePackagedConceptSet: selectedPrepackagedConceptSets,
        },
      };
    };

    const createDataset = async (datasetName, desc) => {
      AnalyticsTracker.DatasetBuilder.Create();
      const { namespace, id } = workspace;

      dataSetApi()
        .createDataSet(namespace, id, createDatasetRequest(datasetName, desc))
        .then((dataset) => loadDataset(dataset));
    };

    const updateDatasetRequest = (): DataSetRequest => {
      return {
        ...createDatasetRequest(dataSet.name, dataSet.description),
        etag: dataSet.etag,
      };
    };

    const saveDataset = async () => {
      AnalyticsTracker.DatasetBuilder.Save();
      const { namespace, id } = workspace;

      setSavingDataset(true);
      dataSetApi()
        .updateDataSet(namespace, id, dataSet.id, updateDatasetRequest())
        .then((dataset) => loadDataset(dataset))
        .catch((e) => {
          console.error(e);
          showErrorModal('Save Dataset Error', 'Please refresh and try again');
        })
        .finally(() => setSavingDataset(false));
    };

    const getHeaderValue = (text) => {
      const dataTestId = 'data-test-id-' + text;
      return (
        <TooltipTrigger
          data-test-id={dataTestId}
          side='top'
          content={text}
          disabled={headerTooltipId !== text}
        >
          <div
            style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}
            title={text}
            onMouseOver={({ target }) => {
              const { offsetWidth, scrollWidth } = target as HTMLDivElement;
              if (offsetWidth < scrollWidth) {
                setHeaderTooltipId(text);
              }
            }}
            onMouseOut={() => setHeaderTooltipId(undefined)}
          >
            {text}
          </div>
        </TooltipTrigger>
      );
    };

    const renderPreviewDataTable = (
      filteredPreviewData: DataSetPreviewInfo
    ) => {
      return (
        <DataTable
          key={selectedPreviewDomain}
          scrollable={true}
          breakpoint='0px'
          style={{ width: '100%' }}
          value={getDataTableValue(filteredPreviewData.values)}
        >
          {filteredPreviewData.values.map(({ value }) => (
            <Column
              key={value}
              header={getHeaderValue(value)}
              headerStyle={{ textAlign: 'left', width: '7.5rem' }}
              style={{ width: '7.5rem' }}
              bodyStyle={{ hyphens: 'auto' }}
              field={value}
            />
          ))}
        </DataTable>
      );
    };

    const renderPreviewDataTableSectionMessage = (
      filteredPreviewData: DataSetPreviewInfo
    ) => {
      const domainDisplayed = formatDomain(selectedPreviewDomain);
      return (
        <div style={styles.warningMessage}>
          {filteredPreviewData.isLoading ? (
            <div>Generating preview for {domainDisplayed}</div>
          ) : (
            <div>
              {filteredPreviewData.errorText && (
                <div>{filteredPreviewData.errorText}</div>
              )}
              {/* If there is no error that means no data was return*/}
              {!filteredPreviewData.errorText && (
                <div>No Results found for {domainDisplayed}</div>
              )}
            </div>
          )}
        </div>
      );
    };

    const renderPreviewDataTableSection = () => {
      let selectedPreviewDomainString = selectedPreviewDomain.toString();
      // Had to do the following since typescript changes the key by removing _ therefore changing the domain string
      // which resulted in map check from selectedPreviewDomain to give undefined result always
      if (selectedPreviewDomain?.toString().startsWith('FITBIT')) {
        switch (selectedPreviewDomain.toString()) {
          case 'FITBITHEARTRATESUMMARY':
            selectedPreviewDomainString = 'FITBIT_HEART_RATE_SUMMARY';
            break;
          case 'FITBITHEARTRATELEVEL':
            selectedPreviewDomainString = 'FITBIT_HEART_RATE_LEVEL';
            break;
          case 'FITBITACTIVITY':
            selectedPreviewDomainString = 'FITBIT_ACTIVITY';
            break;
          case 'FITBITINTRADAYSTEPS':
            selectedPreviewDomainString = 'FITBIT_INTRADAY_STEPS';
            break;
        }
      }
      let filteredPreviewData;
      previewList.forEach((map, entry) => {
        if (
          entry.toString() === selectedPreviewDomainString &&
          !filteredPreviewData
        ) {
          filteredPreviewData = map;
        }
      });
      return filteredPreviewData && filteredPreviewData.values.length > 0
        ? renderPreviewDataTable(filteredPreviewData)
        : renderPreviewDataTableSectionMessage(filteredPreviewData);
    };

    const onClickExport = () => {
      setModalState(
        selectedDomains.has(Domain.WHOLEGENOMEVARIANT)
          ? ModalState.Extract
          : ModalState.Export
      );
    };

    const { namespace, id } = workspace;
    const pathPrefix = 'workspaces/' + namespace + '/' + id + '/data';
    const cohortsPath = pathPrefix + '/cohorts/build';
    const conceptSetsPath = pathPrefix + '/concepts';
    const exportError = !canWrite()
      ? 'Requires Owner or Writer permission'
      : dataSetTouched
      ? 'Pending changes must be saved'
      : '';

    return (
      <React.Fragment>
        {savingDataset && <SpinnerOverlay opacity={0.3} />}

        <FadeBox style={{ paddingTop: '1.5rem' }}>
          <h2 style={{ paddingTop: 0, marginTop: 0 }}>
            Datasets
            {!!dataSet && ' - ' + dataSet.name}
          </h2>
          <div style={{ color: colors.primary, fontSize: '14px' }}>
            Build a dataset by selecting the variables and values for one or
            more of your cohorts. Then export the completed dataset to Notebooks
            where you can perform your analysis
          </div>
          <div style={{ display: 'flex', paddingTop: '1.5rem' }}>
            <div
              style={{
                width: '31%',
                height: '80%',
                minWidth: styles.selectBoxHeader.minWidth,
              }}
            >
              <div
                style={{
                  backgroundColor: 'white',
                  border: `1px solid ${colors.light}`,
                }}
              >
                <BoxHeader
                  step='1'
                  header='Select Cohorts'
                  subHeader='Participants'
                >
                  <PlusLink
                    dataTestId='cohorts-link'
                    path={cohortsPath}
                    disable={!canWrite()}
                  />
                </BoxHeader>
                <div style={{ height: '13.5rem', overflowY: 'auto' }}>
                  <Subheader>Prepackaged Cohorts</Subheader>
                  <ImmutableListItem
                    name='All Participants'
                    isSubitem={false}
                    data-test-id='all-participant'
                    checked={includesAllParticipants}
                    onChange={() => selectPrePackagedCohort()}
                  />
                  <Subheader>Workspace Cohorts</Subheader>
                  {!loadingResources &&
                    cohortList.map((cohort) => (
                      <ImmutableWorkspaceCohortListItem
                        key={cohort.id}
                        name={cohort.name}
                        data-test-id='cohort-list-item'
                        checked={selectedCohortIds.includes(cohort.id)}
                        cohortId={cohort.id}
                        namespace={namespace}
                        wid={id}
                        onChange={() => selectCohort(cohort)}
                      />
                    ))}
                  {loadingResources && (
                    <Spinner
                      style={{
                        position: 'relative',
                        top: '0.75rem',
                        left: '10.5rem',
                      }}
                    />
                  )}
                </div>
              </div>
            </div>
            <div
              style={{
                width: '34.5%',
                height: '80%',
                marginLeft: '1.125rem',
                minWidth: styles.selectBoxHeader.minWidth,
              }}
            >
              <div
                style={{
                  backgroundColor: 'white',
                  border: `1px solid ${colors.light}`,
                }}
              >
                <BoxHeader
                  step='2'
                  header='Select Concept Sets'
                  subHeader='Rows'
                  style={{ paddingRight: '1.5rem' }}
                >
                  <PlusLink
                    dataTestId='concept-sets-link'
                    path={conceptSetsPath}
                    disable={!canWrite()}
                  />
                </BoxHeader>
                <div
                  style={{
                    height: '13.5rem',
                    overflowY: 'auto',
                    pointerEvents:
                      domainValueSetIsLoading.size > 0 ? 'none' : 'auto',
                  }}
                  data-test-id='prePackage-concept-set'
                >
                  <Subheader>Prepackaged Concept Sets</Subheader>
                  {getPrePackagedList().map((prepackaged) => {
                    const p = PrePackagedConceptSetEnum[prepackaged];
                    return (
                      <ImmutableListItem
                        name={prepackagedConceptSetToString[p] || p}
                        isSubitem={Object.keys(
                          PREPACKAGED_SURVEY_DOMAINS
                        ).includes(p)}
                        data-test-id='prePackage-concept-set-item'
                        key={prepackaged}
                        checked={selectedPrepackagedConceptSets.includes(p)}
                        onChange={() =>
                          selectPrePackagedConceptSet(
                            p,
                            !selectedPrepackagedConceptSets.includes(p)
                          )
                        }
                      />
                    );
                  })}
                  <Subheader>Workspace Concept Sets</Subheader>
                  {!loadingResources &&
                    conceptSetList.map((conceptSet) => (
                      <ImmutableListItem
                        key={conceptSet.id}
                        name={conceptSet.name}
                        isSubitem={false}
                        data-test-id='concept-set-list-item'
                        checked={selectedConceptSetIds.includes(conceptSet.id)}
                        onChange={() =>
                          selectConceptSet(
                            conceptSet,
                            !selectedConceptSetIds.includes(conceptSet.id)
                          )
                        }
                        showSourceConceptIcon={crossDomainConceptSetList.has(
                          conceptSet.id
                        )}
                      />
                    ))}
                  {loadingResources && (
                    <Spinner
                      style={{
                        position: 'relative',
                        top: '3rem',
                        left: '15rem',
                      }}
                    />
                  )}
                </div>
              </div>
            </div>
            <div
              style={{
                width: '31.5%',
                height: '80%',
                marginLeft: '1.125rem',
                minWidth: styles.selectBoxHeader.minWidth,
              }}
            >
              <div
                style={{
                  backgroundColor: 'white',
                  border: `1px solid ${colors.light}`,
                }}
              >
                <BoxHeader step='3' header='Select Values' subHeader='Columns'>
                  <div style={styles.selectAllContainer}>
                    <CheckBox
                      style={{ height: 17, width: 17 }}
                      manageOwnState={false}
                      disabled={selectedDomains.size === 0}
                      data-test-id='select-all'
                      onChange={() => selectAllValues()}
                      checked={allValuesSelected()}
                    />
                    <div
                      style={{
                        marginLeft: '0.375rem',
                        fontSize: '13px',
                        lineHeight: '17px',
                      }}
                    >
                      {allValuesSelected ? 'Deselect All' : 'Select All'}
                    </div>
                  </div>
                </BoxHeader>
                <div
                  style={{
                    height: selectedDomains.size > 0 ? '11.4375rem' : '13.5rem',
                    overflowY: 'auto',
                  }}
                >
                  {domainValueSetIsLoading.size > 0 && (
                    <Spinner
                      style={{
                        position: 'relative',
                        top: '3rem',
                        left: 'calc(50% - 36px)',
                      }}
                    />
                  )}
                  {Array.from(selectedDomains)
                    .sort(COMPARE_DOMAINS_FOR_DISPLAY)
                    .map(
                      (domain: Domain) =>
                        domainValueSetLookup.has(domain) && (
                          <div key={domain}>
                            <Subheader style={{ fontWeight: 'bold' }}>
                              {formatDomain(domain)}
                            </Subheader>
                            {domainValueSetLookup
                              .get(domain)
                              .values.map((domainValue) => (
                                <ValueListItem
                                  data-test-id='value-list-items'
                                  key={domainValue.value}
                                  domain={domain}
                                  domainValue={domainValue}
                                  onChange={() =>
                                    selectDomainValue(domain, domainValue)
                                  }
                                  checked={fp.some(
                                    {
                                      domain: domain,
                                      value: domainValue.value,
                                    },
                                    selectedDomainValuePairs
                                  )}
                                />
                              ))}
                          </div>
                        )
                    )}
                </div>
                {selectedDomains.size > 0 && (
                  <FlexRow
                    style={{
                      width: '100%',
                      height: '2.0625rem',
                      backgroundColor: colorWithWhiteness(colors.dark, 0.9),
                      color: colors.primary,
                      paddingLeft: '0.6rem',
                      fontSize: '13px',
                      lineHeight: '16px',
                      alignItems: 'center',
                    }}
                  >
                    <StyledExternalLink
                      href={supportUrls.dataDictionary}
                      target='_blank'
                    >
                      Learn more
                    </StyledExternalLink>
                    &nbsp;in the data dictionary
                  </FlexRow>
                )}
              </div>
            </div>
          </div>
          {crossDomainConceptSetList.size > 0 && (
            <AlertInfo>
              <ClrIcon
                className='is-solid'
                shape='exclamation-triangle'
                size={20}
              />
              This Concept Set contains source concepts (ICD9CM/ICD10CM/CPT4)
              that may be present in multiple domains, which may yield multiple
              data tables.
            </AlertInfo>
          )}
        </FadeBox>
        <FadeBox style={{ marginTop: '1.5rem' }}>
          <div
            style={{
              backgroundColor: 'white',
              border: `1px solid ${colors.light}`,
            }}
          >
            <div style={styles.previewDataHeaderBox}>
              <FlexColumn>
                <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                  <div style={styles.previewDataHeader}>
                    <div>
                      <StepNumber step='4' />
                    </div>
                    <label
                      style={{ marginLeft: '0.75rem', color: colors.primary }}
                    >
                      Preview Dataset
                    </label>
                  </div>
                  <div
                    style={{
                      color: colors.primary,
                      fontSize: '14px',
                      width: '60%',
                    }}
                  >
                    A visualization of your data table based on concept sets and
                    values you selected above. Once complete, export for
                    analysis
                  </div>
                </div>
              </FlexColumn>
              <Clickable
                data-test-id='preview-button'
                style={{
                  marginTop: '0.75rem',
                  cursor: disableSave() ? 'not-allowed' : 'pointer',
                  height: '2.7rem',
                  width: '9.75rem',
                  color: disableSave()
                    ? colorWithWhiteness(colors.dark, 0.6)
                    : colors.accent,
                }}
                disabled={disableSave()}
                onClick={() => {
                  AnalyticsTracker.DatasetBuilder.ViewPreviewTable();
                  getPreviewList();
                }}
              >
                View Preview Table
              </Clickable>
            </div>
            {fp.toPairs(previewList).length > 0 && (
              <FlexColumn>
                <FlexRow style={{ paddingTop: '0.75rem' }}>
                  {fp.toPairs(previewList).map((value) => {
                    const domain: string = value[0];
                    // Strip underscores so we get the correct enum value
                    const domainEnumValue = Domain[domain.replace(/_/g, '')];
                    const previewRow: DataSetPreviewInfo = value[1];
                    return (
                      <TooltipTrigger
                        key={domain}
                        content={
                          'Preview for domain ' +
                          formatDomainString(domain) +
                          ' is still loading. It may take up to one minute'
                        }
                        disabled={!previewRow.isLoading}
                        side='top'
                      >
                        <Clickable
                          disabled={previewRow.isLoading}
                          onClick={() =>
                            setSelectedPreviewDomain(domainEnumValue)
                          }
                          style={stylesFunction.selectDomainForPreviewButton(
                            selectedPreviewDomain === domainEnumValue
                          )}
                        >
                          <FlexRow
                            style={{
                              alignItems: 'center',
                              overflow: 'auto',
                              wordBreak: 'break-all',
                            }}
                          >
                            {formatDomainString(domain)}
                            {previewRow.isLoading && (
                              <Spinner
                                style={{
                                  marginLeft: '4px',
                                  height: '18px',
                                  width: '18px',
                                }}
                              />
                            )}
                          </FlexRow>
                        </Clickable>
                      </TooltipTrigger>
                    );
                  })}
                </FlexRow>
                {renderPreviewDataTableSection()}
              </FlexColumn>
            )}
            {fp.entries(previewList).length === 0 && (
              <div style={styles.previewButtonBox}>
                <div
                  style={{
                    color: colorWithWhiteness(colors.dark, 0.6),
                    fontSize: '20px',
                    fontWeight: 400,
                  }}
                >
                  Select cohorts, concept sets, and values above to generate a
                  preview table
                </div>
              </div>
            )}
          </div>
        </FadeBox>
        <div style={styles.stickyFooter}>
          <TooltipTrigger
            data-test-id='save-tooltip'
            content='Requires Owner or Writer permission'
            disabled={canWrite()}
          >
            {!dataSet ? (
              <Button
                style={{ marginBottom: '3rem', marginRight: '1.5rem' }}
                data-test-id='save-button'
                onClick={() => setModalState(ModalState.Create)}
                disabled={disableSave() || !canWrite() || !dataSetTouched}
              >
                Create Dataset
              </Button>
            ) : (
              <Button
                style={{ marginBottom: '3rem', marginRight: '1.5rem' }}
                data-test-id='save-button'
                onClick={() => saveDataset()}
                disabled={
                  savingDataset ||
                  disableSave() ||
                  !canWrite() ||
                  !dataSetTouched
                }
              >
                Save Dataset
              </Button>
            )}
          </TooltipTrigger>

          <TooltipTrigger
            data-test-id='export-tooltip'
            content={exportError}
            disabled={!exportError}
          >
            <Button
              style={{ marginBottom: '3rem' }}
              data-test-id='analyze-button'
              onClick={() => onClickExport()}
              disabled={disableSave() || !!exportError}
            >
              Analyze
            </Button>
          </TooltipTrigger>
        </div>

        {switchCase(
          modalState,
          [
            ModalState.Create,
            () => (
              <CreateModal
                entityName='Dataset'
                getExistingNames={async () => {
                  const resources =
                    await workspacesApi().getWorkspaceResourcesV2(
                      namespace,
                      id,
                      [ResourceType.DATASET.toString()]
                    );
                  return resources.map((resource) => resource.dataSet.name);
                }}
                save={(name, desc) => createDataset(name, desc)}
                close={() => setModalState(ModalState.None)}
              />
            ),
          ],
          [
            ModalState.Export,
            () => (
              <ExportDatasetModal
                {...{ workspace }}
                dataset={dataSet}
                closeFunction={() => setModalState(ModalState.None)}
              />
            ),
          ],
          [
            ModalState.Extract,
            () => (
              <GenomicExtractionModal
                dataSet={dataSet}
                workspaceNamespace={namespace}
                workspaceFirecloudName={id}
                title={
                  'Would you like to extract genomic variant data as VCF files?'
                }
                cancelText={'Skip'}
                confirmText={'Extract & Continue'}
                closeFunction={() => setModalState(ModalState.Export)}
              />
            ),
          ]
        )}
      </React.Fragment>
    );
  }
);
