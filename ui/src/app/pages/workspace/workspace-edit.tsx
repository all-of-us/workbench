import {Location} from '@angular/common';
import {Component} from '@angular/core';
import {Button, Link} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {CheckBox, RadioButton, TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SearchInput} from 'app/components/search-input';
import {SpinnerOverlay} from 'app/components/spinners';
import {TwoColPaddedTable} from 'app/components/tables';
import {CreateBillingAccountModal} from 'app/pages/workspace/create-billing-account-modal';
import {userApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  sliceByHalfLength,
  withCdrVersions,
  withCurrentWorkspace,
  withRouteConfigData
} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {reportError} from 'app/utils/errors';
import {currentWorkspaceStore, navigate, nextWorkspaceWarmupStore, serverConfigStore} from 'app/utils/navigation';
import {getBillingAccountInfo} from 'app/utils/workbench-gapi-client';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  ArchivalStatus,
  BillingAccount,
  CdrVersion,
  CdrVersionListResponse,
  DataAccessLevel,
  SpecificPopulationEnum,
  Workspace,
  WorkspaceAccessLevel
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import * as validate from 'validate.js';
import {TextColumn} from "app/components/text-column";

export const ResearchPurposeDescription =
  <div style={{display: 'inline'}}>The <i>All of Us</i> Research Program requires each user
   of <i>All of Us</i> data to provide a meaningful description of the intended purpose of data use
   for each workspace they create. The responses provided below will be posted publicly in
   the <i>All of Us</i> Research Hub website to inform research participants.</div>;

interface ResearchPurposeItem {
  shortName: string;
  shortDescription: string;
  longDescription: React.ReactNode;
  uniqueId?: string;
}

export const ResearchPurposeItems: Array<ResearchPurposeItem> = [
  {
    shortName: 'diseaseFocusedResearch',
    shortDescription: 'Disease-focused research',
    longDescription: <div>The primary purpose of the research is to learn more about a particular
    disease or disorder (for example, type 2 diabetes), a trait (for example, blood pressure),
    or a set of related conditions (for example, autoimmune diseases, psychiatric disorders).</div>
  }, {
    shortName: 'methodsDevelopment',
    shortDescription: 'Methods development/validation study',
    longDescription: <div>The primary purpose of the use of <i>All of Us</i> data is to develop
    and/or validate specific methods/tools for analyzing or interpreting data (e.g. statistical
    methods for describing data trends, developing more powerful methods to detect
    gene-environment or other types of interactions in genome-wide association studies).</div>
  }, {
    shortName: 'controlSet',
    shortDescription: 'Research Control',
    longDescription: <div><i>All of Us</i> data will be used as a reference or control dataset
      for comparison with another dataset from a different resource (e.g. Case-control
      studies).</div>
  }, {
    shortName: 'ancestry',
    shortDescription: 'Genetic Research',
    longDescription: <div>Research concerning genetics (i.e. the study of genes, genetic variations
      and heredity) in the context of diseases or ancestry.</div>
  }, {
    shortName: 'socialBehavioral',
    shortDescription: 'Social/Behavioral Research',
    longDescription: <div>The research focuses on the social or behavioral phenomena or determinants
      of health.</div>
  }, {
    shortName: 'populationHealth',
    shortDescription: 'Population Health/Public Health Research',
    longDescription: <div>The primary purpose of using <i>All of Us</i> data is to investigate
      health behaviors, outcomes, access and disparities in populations.</div>
  }, {
    shortName: 'drugDevelopment',
    shortDescription: 'Drug/Therapeutics Development Research',
    longDescription: <div>Primary focus of the research is drug/therapeutics development. The data
      will be used to understand treatment-gene interactions or treatment outcomes relevant
      to the therapeutic(s) of interest.</div>
  },  {
    shortName: 'commercialPurpose',
    shortDescription: 'For-Profit Purpose',
    longDescription: <div>The data will be used by a for-profit entity for research or product
      or service development (e.g. for understanding drug responses as part of a
      pharmaceutical company's drug development or market research efforts).</div>
  }, {
    shortName: 'educational',
    shortDescription: 'Educational Purpose',
    longDescription: <div>The data will be used for education purposes (e.g. for a college research
      methods course, to educate students on population-based research approaches).</div>
  }, {
    shortName: 'otherPurpose',
    shortDescription: 'Other Purpose',
    longDescription: <div>If your Purpose of Use is different from the options listed above, please
      select "Other Purpose" and provide details regarding your purpose of data use here
      (500 character limit).</div>
  }
];
ResearchPurposeItems.forEach(item => {
  item.uniqueId = fp.uniqueId('research-purpose');
});

export const toolTipText = {
  header: <div>A Workspace is your place to store and analyze data for a specific project.Each
    Workspace is a separate Google bucket that serves as a dedicated space for file storage.
    You can share this Workspace with other users, allowing them to view or edit your work. Your
    Workspace is where you will go to build concept sets and cohorts and launch Notebooks for
    performing analyses on your cohorts.</div>,
  cdrSelect: <div>The curated data repository (CDR) is where research data from the <i>All of Us</i>
    Research Program is stored. The CDR is periodically updated as new data becomes available for
    use. You can select which version of the CDR you wish to query in this Workspace.</div>,
  researchPurpose: <div>You  are required to describe your research purpose, or the reason why you
    are conducting this study. This information, along with your name, will be posted on the
    publicly available <i>All of Us</i> website (https://www.researchallofus.org/) to inform our
    participants and other stakeholders about what kind of research their data is being used
    for.</div>,
};

export const researchPurposeQuestions = [
  {
    header: '1. What is the primary purpose of your project?',
    description: <div>(Please select as many options below as describe your
      research purpose)</div>
  }, {
    header: <div>2. Provide the reason for choosing <i>All of Us</i> data
      for your investigation</div>,
    description: <div>(Free text; 500 Character limit)</div>
  }, {
    header: '3. What are the specific scientific question(s) you intend to study?',
    description: <div>If you are exploring the data at this stage to formalize a specific research
      question, please describe the reason for exploring the data, and the scientific
      question you hope to be able to answer using the data. <br/>
      (Free text; 500 Character limit)</div>
  }, {
    header: '4. What are your anticipated findings from this study?',
    description: <div>(Layperson language; 2000 Character limit)</div>
  }, {
    header: '5. Will your study or data analysis focus on specific population(s)? \
      Or do you intend to study your phenotype, disease, or condition of interest with \
      a focus on comparative analysis of a specific demographic group (for example \
      a group based on race/ethnicity, gender, or age)?',
    description: <div/>
  }
];

const CREATE_BILLING_ACCOUNT_OPTION_VALUE = 'CREATE_BILLING_ACCOUNT_OPTION';

interface SpecificPopulationItem {
  label: string;
  shortName: SpecificPopulationEnum;
  ubrLabel: string;
  ubrDescription: string;
}

export const SpecificPopulationItems: Array<SpecificPopulationItem> = [
  {
    label: 'Race/Ethnicity',
    shortName: SpecificPopulationEnum.RACEETHNICITY,
    ubrLabel: 'Ancestry (Race/Ethnicity)',
    ubrDescription: 'American Indian and Alaska Native (AIAN); Black, African American, or ' +
       'African; Middle Eastern or North African (MENA); Native Hawaiian or Other Pacific ' +
       'Islander (NHPI); Hispanic, Latino, or Spanish (H/L/S); Multi-Ancestry (2+ Races)'
  }, {
    label: 'Age Groups',
    shortName: SpecificPopulationEnum.AGEGROUPS,
    ubrLabel: 'Age',
    ubrDescription: 'Children (0-11); Adolescents (12-17); Older Adults (65-74); Older ' +
      'Adults (75+)'
  }, {
    label: 'Sex',
    shortName: SpecificPopulationEnum.SEX,
    ubrLabel: 'Sex',
    ubrDescription: 'Intersex'
  }, {
    label: 'Gender Identity',
    shortName: SpecificPopulationEnum.GENDERIDENTITY,
    ubrLabel: 'Gender Identity (GI)',
    ubrDescription: 'Nonbinary; Transgender; or Other Gender Identity Choices'
  }, {
    label: 'Sexual Orientation',
    shortName: SpecificPopulationEnum.SEXUALORIENTATION,
    ubrLabel: 'Sexual Orientation (SO)',
    ubrDescription: 'Gay; Lesbian; Bisexual; Queer; Other Sexual Orientation Choices'
  }, {
    label: 'Geography (e.g. Rural, urban, suburban, etc.)',
    shortName: SpecificPopulationEnum.GEOGRAPHY,
    ubrLabel: 'Geography',
    ubrDescription: 'Rural and Non-Metropolitan Zip codes'
  }, {
    label: 'Disability status',
    shortName: SpecificPopulationEnum.DISABILITYSTATUS,
    ubrLabel: 'Disability Status',
    ubrDescription: 'Physical and Cognitive Disabilities'
  }, {
    label: 'Access to care',
    shortName: SpecificPopulationEnum.ACCESSTOCARE,
    ubrLabel: 'Access to Care',
    ubrDescription: 'Limited access to care; Cannot easily obtain or access medical care'
  }, {
    label: 'Education level',
    shortName: SpecificPopulationEnum.EDUCATIONLEVEL,
    ubrLabel: 'Educational Attainment',
    ubrDescription: 'Less than high school graduate or General Education Development (GED)'
  }, {
    label: 'Income level',
    shortName: SpecificPopulationEnum.INCOMELEVEL,
    ubrLabel: 'Income Level',
    ubrDescription: 'Less than USD 25,000 [for a family of four]'
  }
];

// Poll parameters to check Workspace ACLs after creation of a new workspace. See
// SATURN-104 for details, eventually the root cause should be resolved by fixes
// to Sam (as part of Postgres migration).
const NEW_ACL_DELAY_POLL_TIMEOUT_MS = 60 * 1000;
const NEW_ACL_DELAY_POLL_INTERVAL_MS = 10 * 1000;

const styles = reactStyles({
  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: colors.primary
  },

  requiredText: {
    fontSize: '13px',
    fontStyle: 'italic',
    fontWeight: 400,
    color: colors.primary,
    marginLeft: '0.2rem'
  },

  text: {
    fontSize: '13px',
    color: colors.primary,
    fontWeight: 400,
    lineHeight: '24px'
  },

  textInput: {
    width: '20rem',
    borderColor: 'rgb(151, 151, 151)',
    borderRadius: '6px',
    marginRight: '20px',
    marginBottom: '5px'
  },

  infoIcon: {
    height: '16px',
    marginLeft: '0.2rem',
    width: '16px'
  },
  select: {
    display: 'inline-block',
    verticalAlign: 'middle',
    position: 'relative',
    overflow: 'visible',
    width: '11.3rem',
    marginRight: '20px'
  },
  shortDescription: {
    color: colors.primary,
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '24px',
    cursor: 'pointer'
  },
  longDescription : {
    position: 'relative',
    display: 'inline-block',
    minHeight: '1rem',
    cursor: 'text',
    lineHeight: '1rem',
    width: '95%'
  },
  categoryRow: {
    display: 'flex', flexDirection: 'row', padding: '0.6rem 0',
  },
  checkboxStyle: {
    marginRight: '.31667rem', zoom: '1.5'
  },
  checkboxRow: {
    display: 'inline-block', padding: '0.2rem 0', marginRight: '1rem'
  },
});

export const WorkspaceEditSection = (props) => {
  return <div key={props.header} style={{marginBottom: '0.5rem'}}>
    <FlexRow style={{marginBottom: (props.largeHeader ? 12 : 0),
      marginTop: (props.largeHeader ? 12 : 24)}}>
      <div style={{...styles.header,
        fontSize: (props.largeHeader ? 20 : 16)}}>
        {props.header}
      </div>
      {props.required && <div style={styles.requiredText}>
        (Required)
      </div>
      }
      {props.tooltip && <TooltipTrigger content={props.tooltip}>
        <InfoIcon style={{...styles.infoIcon,  marginTop: '0.2rem'}}/>
      </TooltipTrigger>
      }
    </FlexRow>
    {props.subHeader && <div style={{...styles.header, color: colors.primary, fontSize: 14}}>
      {props.subHeader}
    </div>
    }
    <div style={styles.text}>
      {props.description}
    </div>
    <div style={{marginTop: '0.5rem'}}>
      {props.children}
    </div>
  </div>;
};

export enum WorkspaceEditMode { Create = 1, Edit = 2, Duplicate = 3 }

function getDiseaseNames(keyword) {
  const baseurl = serverConfigStore.getValue().firecloudURL;
  const url = baseurl + '/duos/autocomplete/' + keyword;
  return fetch(encodeURI(url)).then((response) => {
    return response.json();
  }).then((matches) => {
    const labeledMatches = fp.filter((elt) => elt.hasOwnProperty('label'))(matches);
    const diseases = fp.map((elt) => elt['label'])(labeledMatches);
    return diseases;
  });
}

export interface WorkspaceEditProps {
  routeConfigData: any;
  cdrVersionListResponse: CdrVersionListResponse;
  workspace: WorkspaceData;
  cancel: Function;
}

export interface WorkspaceEditState {
  cdrVersionItems: Array<CdrVersion>;
  workspace: Workspace;
  workspaceCreationConflictError: boolean;
  workspaceCreationError: boolean;
  workspaceCreationErrorMessage: string;
  workspaceNewAclDelayed: boolean;
  workspaceNewAclDelayedContinueFn: Function;
  cloneUserRole: boolean;
  loading: boolean;
  showUnderservedPopulationDetails: boolean;
  showStigmatizationDetails: boolean;
  billingAccounts: Array<BillingAccount>;
  showCreateBillingAccountModal: boolean;
}

export const WorkspaceEdit = fp.flow(withRouteConfigData(), withCurrentWorkspace(), withCdrVersions())(
  class WorkspaceEditCmp extends React.Component<WorkspaceEditProps, WorkspaceEditState> {
    constructor(props: WorkspaceEditProps) {
      super(props);
      this.state = {
        cdrVersionItems: this.createInitialCdrVersionsList(),
        workspace: this.createInitialWorkspaceState(),
        workspaceCreationConflictError: false,
        workspaceCreationError: false,
        workspaceCreationErrorMessage: '',
        workspaceNewAclDelayed: false,
        workspaceNewAclDelayedContinueFn: () => {},
        cloneUserRole: false,
        loading: false,
        showUnderservedPopulationDetails: false,
        showStigmatizationDetails: false,
        billingAccounts: [],
        showCreateBillingAccountModal: false
      };
    }

    async fetchBillingAccounts() {
      const billingAccounts = (await userApi().listBillingAccounts()).billingAccounts;

      if (this.isMode(WorkspaceEditMode.Create) || this.isMode(WorkspaceEditMode.Duplicate)) {
        this.setState(prevState => fp.set(
          ['workspace', 'billingAccountName'],
          billingAccounts.find(billingAccount => billingAccount.isFreeTier).name,
          prevState));
      } else if (this.isMode(WorkspaceEditMode.Edit)) {
        const fetchedBillingInfo = await getBillingAccountInfo(this.props.workspace.namespace);

        if (!billingAccounts.find(billingAccount => billingAccount.name === fetchedBillingInfo.billingAccountName)) {
          // If the user has owner access on the workspace but does not have access to the billing account
          // that it is attached to, keep the server's current value for billingAccountName and add a shim
          // entry into billingAccounts so the dropdown entry is not empty.
          //
          // The server will not perform an updateBillingInfo call if the received billingAccountName
          // is the same as what is currently stored.
          //
          // This can happen if a workspace is shared to another researcher as an owner.
          billingAccounts.push({
            name: this.props.workspace.billingAccountName,
            displayName: 'User Provided Billing Account',
            isFreeTier: false,
            isOpen: true
          });

          if (fetchedBillingInfo.billingAccountName !== this.props.workspace.billingAccountName) {
            // This should never happen but it means the database is out of sync with Google
            // and does not have the correct billing account stored.
            // We cannot send over the correct billing account info since the current user
            // does not have permissions to set it.

            reportError({
              name: 'Out of date billing account name',
              message: `Workspace ${this.props.workspace.namespace} has an out of date billing account name. ` +
                `Stored value is ${this.props.workspace.billingAccountName}. ` +
                `True value is ${fetchedBillingInfo.billingAccountName}`
            });
          }
        } else {
          // Otherwise, use this as an opportunity to sync the fetched billing account name from the source of truth, Google
          this.setState(prevState => fp.set(
            ['workspace', 'billingAccountName'], fetchedBillingInfo.billingAccountName, prevState));
        }
      }

      this.setState({billingAccounts});
    }

    async componentDidMount() {
      if (serverConfigStore.getValue().enableBillingLockout) {
        this.fetchBillingAccounts();
      } else {
        // This is a temporary hack to set the billing account name property to anything
        // so that it passes validation. The server ignores the value if the feature flag
        // is turned off so any value is fine.
        this.setState(fp.set(['workspace', 'billingAccountName'], 'free-tier'));
      }
    }

    /**
     * Creates the initial workspace state object. For a CREATE mode dialog,
     * this is effectively an empty Workspace object. For EDIT or DUPLICATE
     * mode, this will be based on props.workspace.
     *
     * This is where logic lives to auto-set the CDR version and
     * "reviewRequested" flag, which depend on the workspace state & edit mode.
     */
    createInitialWorkspaceState(): Workspace {
      let workspace: Workspace = this.props.workspace;
      if (this.isMode(WorkspaceEditMode.Create)) {
        workspace = {
          name: '',
          dataAccessLevel: DataAccessLevel.Registered,
          cdrVersionId: '',
          researchPurpose: {
            ancestry: false,
            anticipatedFindings: '',
            commercialPurpose: false,
            controlSet: false,
            diseaseFocusedResearch: false,
            diseaseOfFocus: '',
            drugDevelopment: false,
            educational: false,
            intendedStudy: '',
            methodsDevelopment: false,
            otherPopulationDetails: '',
            otherPurpose: false,
            otherPurposeDetails: '',
            population: false,
            populationDetails: [],
            populationHealth: false,
            reviewRequested: false,
            socialBehavioral: false,
            reasonForAllOfUs: '',
          }
        };
      }

      // Replace potential nulls with empty string or empty array
      if (workspace.researchPurpose.populationDetails == null) {
        workspace.researchPurpose.populationDetails = [];
      }
      if (workspace.researchPurpose.diseaseOfFocus == null) {
        workspace.researchPurpose.diseaseOfFocus = '';
      }

      if (this.isMode(WorkspaceEditMode.Duplicate)) {
        // This is the only field which is not automatically handled/differentiated
        // on the API level.
        workspace.name = 'Duplicate of ' + workspace.name;
        // if the original workspace was reviewed, it's unlikely that we need a re-review
        workspace.researchPurpose.reviewRequested = false;
      }

      const selectedCdrIsLive = this.getLiveCdrVersions().some(
        cdr => cdr.cdrVersionId === workspace.cdrVersionId);
      // We preselect the default CDR version when a new workspace is being
      // created (via create or duplicate), but leave as-is if the selected CDR
      // version is live.
      if (this.isMode(WorkspaceEditMode.Create) ||
        (this.isMode(WorkspaceEditMode.Duplicate) && !selectedCdrIsLive)) {
        workspace.cdrVersionId = this.props.cdrVersionListResponse.defaultCdrVersionId;
      }

      return workspace;
    }

    createInitialCdrVersionsList(): Array<CdrVersion> {
      if (this.isMode(WorkspaceEditMode.Edit)) {
        // In edit mode, you cannot modify the CDR version, therefore it's fine
        // to show archived CDRs in the drop-down so that it accurately displays
        // the current value.
        return this.getAllCdrVersions();
      } else {
        return this.getLiveCdrVersions();
      }
    }

    getLiveCdrVersions(): Array<CdrVersion> {
      const cdrResp = this.props.cdrVersionListResponse;
      const liveCdrVersions = cdrResp.items.filter(cdr => cdr.archivalStatus === ArchivalStatus.LIVE);
      if (liveCdrVersions.length === 0) {
        throw Error('no live CDR versions were found');
      }

      return liveCdrVersions;
    }

    getAllCdrVersions(): Array<CdrVersion> {
      return [...this.props.cdrVersionListResponse.items];
    }

    makeDiseaseInput(): React.ReactNode {
      return (
        <SearchInput
          enabled={this.state.workspace.researchPurpose.diseaseFocusedResearch}
          placeholder='Name of Disease'
          value={this.state.workspace.researchPurpose.diseaseOfFocus}
          onSearch={getDiseaseNames}
          tooltip='You must select disease focused research to enter a disease of focus'
          onChange={(disease) => this.setState(fp.set([
            'workspace',
            'researchPurpose',
            'diseaseOfFocus'
          ], disease))}/>
      );
    }

    renderBillingDescription() {
      return <div>
        The <i> Us</i> Program provides ${serverConfigStore.getValue().defaultFreeCreditsDollarLimit.toFixed(0)} in
        free credits per user. When free credits are exhausted, you will need to provide a valid Google Cloud Platform billing account.
        At any time, you can update your Workspace billing account.
      </div>;
    }

    /**
     * Creates a form element containing the checkbox, header, and description
     * (plus optional child elements) for each of the "primary purpose of your
     * project" options.
     */
    makePrimaryPurposeForm(rp: ResearchPurposeItem, index: number): React.ReactNode {
      let children: React.ReactNode;
      if (rp.shortName === 'diseaseFocusedResearch') {
        children = this.makeDiseaseInput();
      } else if (rp.shortName === 'otherPurpose') {
        children = <TextArea value={this.state.workspace.researchPurpose.otherPurposeDetails}
                  onChange={v => this.updateResearchPurpose('otherPurposeDetails', v)}
                  disabled={!this.state.workspace.researchPurpose.otherPurpose}
                  style={{marginTop: '0.5rem'}}/>;
      }

      return <div key={index} style={styles.categoryRow}>
        <CheckBox id={rp.uniqueId}
                  data-test-id={rp.shortName + '-checkbox'}
                  style={styles.checkboxStyle}
                  checked={!!this.state.workspace.researchPurpose[rp.shortName]}
                  onChange={e => this.updateResearchPurpose(rp.shortName, e)}/>
        <FlexColumn style={{marginTop: '-0.2rem'}}>
          <label style={styles.shortDescription} htmlFor={rp.uniqueId}>
            {rp.shortDescription}
          </label>
          <div>
            <label style={{...styles.longDescription, ...styles.text}}>
              {rp.longDescription}
            </label>
            {children}
          </div>
        </FlexColumn>
      </div>;
    }

    /**
     * Creates the form element for each of the "focus on specific populations"
     * options.
     */
    makeSpecificPopulationForm(item: SpecificPopulationItem): React.ReactNode {
      return <CheckBox
        wrapperStyle={styles.checkboxRow}
        style={styles.checkboxStyle}
        label={item.label}
        labelStyle={styles.text}
        key={item.label}
        data-test-id={item.shortName + '-checkbox'}
        checked={this.specificPopulationCheckboxSelected(item.shortName)}
        onChange={v => this.updateSpecificPopulation(item.shortName, v)}
        disabled={!this.state.workspace.researchPurpose.population}
        />;
    }

    renderHeader() {
      switch (this.props.routeConfigData.mode) {
        case WorkspaceEditMode.Create:
          return 'Create a new Workspace';
        case WorkspaceEditMode.Edit:
          return 'Edit workspace \"' + this.state.workspace.name + '\"';
        case WorkspaceEditMode.Duplicate:
          // use workspace name from props instead of state here
          // because it's a record of the initial value
          return 'Duplicate workspace \"' + this.props.workspace.name + '\"';
      }
    }

    renderButtonText() {
      switch (this.props.routeConfigData.mode) {
        case WorkspaceEditMode.Create: return 'Create Workspace';
        case WorkspaceEditMode.Edit: return 'Update Workspace';
        case WorkspaceEditMode.Duplicate: return 'Duplicate Workspace';
      }
    }

    get categoryIsSelected() {
      const rp = this.state.workspace.researchPurpose;
      return rp.ancestry || rp.commercialPurpose || rp.controlSet || rp.diseaseFocusedResearch ||
        rp.drugDevelopment || rp.educational || rp.methodsDevelopment || rp.otherPurpose ||
        rp.populationHealth || rp.socialBehavioral;
    }

    get isSpecificPopulationValid() {
      const researchPurpose = this.state.workspace.researchPurpose;
      return (
          !researchPurpose.population ||
          (
              researchPurpose.populationDetails &&
              researchPurpose.populationDetails.length !== 0
          )
      );
    }

    get isDiseaseOfFocusValid() {
      const researchPurpose = this.state.workspace.researchPurpose;
      return !researchPurpose.diseaseFocusedResearch ||
        researchPurpose.diseaseOfFocus;
    }

    updateResearchPurpose(category, value) {
      this.setState(fp.set(['workspace', 'researchPurpose', category], value));
    }

    updateSpecificPopulation(populationDetails, value) {
      const selectedPopulations = this.state.workspace.researchPurpose.populationDetails;
      if (value) {
        if (!!selectedPopulations) {
          this.setState(fp.set(['workspace', 'researchPurpose', 'populationDetails'],
            selectedPopulations.concat([populationDetails])));
        } else {
          this.setState(fp.set(['workspace', 'researchPurpose', 'populationDetails'],
            [populationDetails]));
        }
      } else {
        this.setState(fp.set(['workspace', 'researchPurpose', 'populationDetails'],
          selectedPopulations.filter(v => v !== populationDetails)));
      }
    }

    specificPopulationCheckboxSelected(populationEnum: SpecificPopulationEnum): boolean {
      return fp.includes(populationEnum, this.state.workspace.researchPurpose.populationDetails);
    }

    onSaveClick() {
      if (this.isMode(WorkspaceEditMode.Create)) {
        AnalyticsTracker.Workspaces.Create();
      } else if (this.isMode(WorkspaceEditMode.Duplicate)) {
        AnalyticsTracker.Workspaces.Duplicate();
      } else if (this.isMode(WorkspaceEditMode.Edit)) {
        AnalyticsTracker.Workspaces.Edit();
      }

      this.saveWorkspace();
    }

    async saveWorkspace() {
      try {
        this.setState({loading: true});
        let workspace = this.state.workspace;
        if (this.isMode(WorkspaceEditMode.Create)) {
          workspace =
            await workspacesApi().createWorkspace(this.state.workspace);
        } else if (this.isMode(WorkspaceEditMode.Duplicate)) {
          const cloneWorkspace = await workspacesApi().cloneWorkspace(
            this.props.workspace.namespace, this.props.workspace.id,
            {
              includeUserRoles: this.state.cloneUserRole,
              workspace: this.state.workspace
            });
          workspace = cloneWorkspace.workspace;
        } else {
          workspace = await workspacesApi()
              .updateWorkspace(this.state.workspace.namespace, this.state.workspace.id,
                  {workspace: this.state.workspace});
          // TODO: Investigate removing this GET call, the response from Update should suffice here.
          await workspacesApi()
            .getWorkspace(this.state.workspace.namespace, this.state.workspace.id)
            .then(ws => currentWorkspaceStore.next({
              ...ws.workspace,
              accessLevel: ws.accessLevel
            }));
          navigate(['workspaces', workspace.namespace, workspace.id, 'data']);
          return;
        }

        // Remaining logic covers newly created workspace (creates or clones). The high complexity
        // in this case is to paper over Sam consistency issues on initial creation (see RW-2818).
        let accessLevel = null;
        let pollTimedOut = false;
        setTimeout(() => pollTimedOut = true, NEW_ACL_DELAY_POLL_TIMEOUT_MS);
        while (!pollTimedOut) {
          ({workspace, accessLevel} = await workspacesApi().getWorkspace(workspace.namespace, workspace.id));
          if (accessLevel === WorkspaceAccessLevel.OWNER) {
            break;
          }
          await new Promise((accept) => setTimeout(accept, NEW_ACL_DELAY_POLL_INTERVAL_MS));
        }

        const navigateToWorkspace = () => navigate(['workspaces', workspace.namespace, workspace.id, 'data']);
        if (accessLevel !== WorkspaceAccessLevel.OWNER) {
          reportError(new Error(
            `ACLs failed to propagate for workspace ${workspace.namespace}/${workspace.id}` +
            ` accessLevel: ${accessLevel}`));
          // We intentionally do not preload the created workspace via nextWorkspaceWarmupStore in
          // this situation. This forces a workspace fetch on navigation, which is desired as ACLs
          // might have finally propagated by the time the navigate button is clicked.
          this.setState({
            loading: false,
            workspaceNewAclDelayed: true,
            workspaceNewAclDelayedContinueFn: navigateToWorkspace
          });
          return;
        }

        // Preload the newly created workspace to avoid a redundant GET on the following navigate.
        // This is also important for guarding against the ACL delay issue, as we have observed
        // that even after confirming OWNER access, subsequent calls to GET may still yield NOACCESS.
        nextWorkspaceWarmupStore.next({...workspace, accessLevel});
        navigateToWorkspace();

      } catch (error) {
        console.log(error);
        this.setState({loading: false});
        if (error.status === 409) {
          this.setState({workspaceCreationConflictError: true});
        } else {
          let errorMsg;
          if (error.status === 429) {
            errorMsg = 'Server is overloaded. Please try again in a few minutes.';
          } else {
            errorMsg = `Could not
            ${this.props.routeConfigData.mode === WorkspaceEditMode.Create ?
              ' create ' : ' update '} workspace.`;
          }

          this.setState({
            workspaceCreationError: true,
            workspaceCreationErrorMessage: errorMsg
          });
        }
      }
    }

    resetWorkspaceEditor() {
      this.setState({
        workspaceCreationError : false,
        workspaceCreationConflictError : false
      });
    }

    isMode(mode) {
      return this.props.routeConfigData.mode === mode;
    }

    buildBillingAccountOptions() {
      const options = this.state.billingAccounts.map(a => ({label: a.displayName, value: a.name}));
      options.push({label: 'Create a new billing account', value: CREATE_BILLING_ACCOUNT_OPTION_VALUE});
      return options;
    }

    render() {
      const {
        workspace: {
          name,
          billingAccountName,
          researchPurpose: {
            anticipatedFindings,
            intendedStudy,
            reasonForAllOfUs
          }
        },
      } = this.state;
      const errors = validate({
        name,
        billingAccountName,
        anticipatedFindings,
        intendedStudy,
        reasonForAllOfUs,
        'primaryPurpose': this.categoryIsSelected,
        'specificPopulation': this.isSpecificPopulationValid,
        'diseaseOfFocus': this.isDiseaseOfFocusValid
      }, {
        name: {
          length: { minimum: 1, maximum: 80 }
        },
        billingAccountName: { presence: true },
        intendedStudy: { presence: true },
        anticipatedFindings: {presence: true },
        reasonForAllOfUs: { presence: true },
        primaryPurpose: { truthiness: true },
        specificPopulation: { truthiness: true },
        diseaseOfFocus: { truthiness: true }
      });
      return <FadeBox  style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <div style={{width: '95%'}}>
          {this.state.loading && <SpinnerOverlay overrideStylesOverlay={{
            position: 'fixed',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            backgroundColor: 'rgba(0, 0, 0, 0.2)',
            height: '100%',
            width: '100%',
          }}/>}
          <WorkspaceEditSection header={this.renderHeader()} tooltip={toolTipText.header}
                              section={{marginTop: '24px'}} largeHeader required={!this.isMode(WorkspaceEditMode.Duplicate)}>
          <FlexRow>
            <TextInput type='text' style={styles.textInput} autoFocus placeholder='Workspace Name'
              value = {this.state.workspace.name}
              onChange={v => this.setState(fp.set(['workspace', 'name'], v))}/>
            <TooltipTrigger
                content='To use a different dataset version, duplicate or create a new workspace.'
                disabled={!(this.isMode(WorkspaceEditMode.Edit))}>
              <div style={styles.select}>
                <select style={{borderColor: 'rgb(151, 151, 151)', borderRadius: '6px',
                  height: '1.5rem', width: '12rem'}}
                  value={this.state.workspace.cdrVersionId}
                  onChange={(v: React.FormEvent<HTMLSelectElement>) => {
                    this.setState(fp.set(['workspace', 'cdrVersionId'], v.currentTarget.value));
                  }}
                  disabled={this.isMode(WorkspaceEditMode.Edit)}>
                    {this.state.cdrVersionItems.map((version, i) => (
                      <option key={version.cdrVersionId} value={version.cdrVersionId}>
                        {version.name}
                      </option>
                    ))}
                </select>
              </div>
            </TooltipTrigger>
            <TooltipTrigger content={toolTipText.cdrSelect}>
              <InfoIcon style={{...styles.infoIcon}}/>
            </TooltipTrigger>
          </FlexRow>
        </WorkspaceEditSection>
        {this.isMode(WorkspaceEditMode.Duplicate) &&
        <WorkspaceEditSection header='Options for duplicate workspace'
          >
          <CheckBox
            style={styles.checkboxStyle}
            label='Share workspace with the same set of collaborators'
            labelStyle={styles.text}
            onChange={v => this.setState({cloneUserRole: v})}/>
        </WorkspaceEditSection>
        }
        {serverConfigStore.getValue().enableBillingLockout &&
          (!this.isMode(WorkspaceEditMode.Edit) || this.props.workspace.accessLevel === WorkspaceAccessLevel.OWNER) &&
          <WorkspaceEditSection header={<div><i>All of Us</i> Billing account</div>}
                                description={this.renderBillingDescription()}>
            <div style={{...styles.header, color: colors.primary, fontSize: 14, marginBottom: '0.2rem'}}>
              Select account
            </div>
            <Dropdown style={{width: '14rem'}}
                      value={this.state.workspace.billingAccountName}
                      options={this.buildBillingAccountOptions()}
                      onChange={e => {
                        if (e.value === CREATE_BILLING_ACCOUNT_OPTION_VALUE) {
                          this.setState({
                            showCreateBillingAccountModal: true
                          });
                        } else {
                          this.setState(fp.set(['workspace', 'billingAccountName'], e.value));
                        }
                      }}
            />
            <div style={styles.text}>
              <b>TIP: </b> For billing accounts, if creating a new billing account, refresh this page after creating account.
            </div>
          </WorkspaceEditSection>
        }
        <WorkspaceEditSection header='Research Use Statement Questions'
            description={<div> {ResearchPurposeDescription} Therefore, please provide
              sufficiently detailed responses at a 5th grade reading level.  Your responses
              will not be used to make decisions about data access. <br/> <br/>
              <i>Note that you are required to create separate Workspaces for each project
                for which you access All of Us data, hence the responses below are expected
                to be specific to the project for which you are creating this particular
                Workspace.</i></div>
            }/>
        <WorkspaceEditSection header={researchPurposeQuestions[0].header}
            description={researchPurposeQuestions[0].description} required>
          <FlexRow>
            <FlexColumn style={{flex: '1 1 0'}}>
              {ResearchPurposeItems.slice(0, sliceByHalfLength(ResearchPurposeItems))
                .map((rp, i) => this.makePrimaryPurposeForm(rp, i))}
            </FlexColumn>
            <FlexColumn style={{flex: '1 1 0'}}>
              {ResearchPurposeItems.slice(sliceByHalfLength(ResearchPurposeItems))
                .map((rp, i) => this.makePrimaryPurposeForm(rp, i))}
            </FlexColumn>
          </FlexRow>
        </WorkspaceEditSection>
        <WorkspaceEditSection
          header={researchPurposeQuestions[1].header}
          description={researchPurposeQuestions[1].description} required>
          <TextArea value={this.state.workspace.researchPurpose.reasonForAllOfUs}
                    onChange={v => this.updateResearchPurpose('reasonForAllOfUs', v)}/>
        </WorkspaceEditSection>
        <WorkspaceEditSection
          header={researchPurposeQuestions[2].header}
          description={researchPurposeQuestions[2].description} required>
          <TextArea value={this.state.workspace.researchPurpose.intendedStudy}
                    onChange={v => this.updateResearchPurpose('intendedStudy', v)}/>
        </WorkspaceEditSection>
        <WorkspaceEditSection header={researchPurposeQuestions[3].header}
                              description={researchPurposeQuestions[3].description} required>
          <TextArea value={this.state.workspace.researchPurpose.anticipatedFindings}
                    onChange={v => this.updateResearchPurpose('anticipatedFindings', v)}/>
        </WorkspaceEditSection>
        <WorkspaceEditSection required header={researchPurposeQuestions[4].header}>
          <Link onClick={() => this.setState({showUnderservedPopulationDetails:
              !this.state.showUnderservedPopulationDetails})}>
            More info on underserved populations
            {this.state.showUnderservedPopulationDetails ? <ClrIcon shape='caret' dir='up'/> :
              <ClrIcon shape='caret' dir='down'/>}
          </Link>
          {this.state.showUnderservedPopulationDetails && <div style={styles.text}>
            A primary mission of the <i>All of Us</i> Research Program is to include research
            participants who are medically underserved or are historically underrepresented in
            Biomedical Research, or who, because of systematic social disadvantage, experience
            health disparities.  As a way to assess the research being conducted with a focus on
            these populations, <i>All of Us</i> requires that you indicate the demographic
            categories you intend to focus your analysis on.
          </div>}
          <div style={{marginTop: '0.5rem'}}>
            <RadioButton name='population' style={{marginRight: '0.5rem'}}
                         data-test-id='specific-population-no'
                         onChange={v => this.updateResearchPurpose('population', false)}
                         checked={!this.state.workspace.researchPurpose.population}/>
            <label style={styles.text}>No, I am not interested in focusing on
              specific population(s) in my research.</label>
          </div>
          <div>
            <RadioButton name='population' style={{marginRight: '0.5rem'}}
                         data-test-id='specific-population-yes'
                         onChange={v => this.updateResearchPurpose('population', true)}
                         checked={this.state.workspace.researchPurpose.population}/>
            <label style={styles.text}>Yes, I am interested in the focused study of specific
              population(s), either on their own or in comparison to other groups.</label>
          </div>
          <div style={{...styles.text, marginLeft: '2rem'}}>
            <strong>If "Yes": </strong> Please specify the demographic category or categories of the
            population(s) that you are interested in exploring in your study.
            Select as many as applicable.
            <FlexRow style={{flex: '1 1 0', marginTop: '0.5rem'}}>
              <FlexColumn>
                {SpecificPopulationItems.slice(0, sliceByHalfLength(SpecificPopulationItems) + 1).map(sp =>
                  this.makeSpecificPopulationForm(sp))}
              </FlexColumn>
              <FlexColumn>
                {SpecificPopulationItems.slice(sliceByHalfLength(SpecificPopulationItems) + 1).map(sp =>
                  this.makeSpecificPopulationForm(sp))}
                <CheckBox
                    wrapperStyle={styles.checkboxRow}
                    style={styles.checkboxStyle}
                    label='Other'
                    labelStyle={styles.text}
                    checked={this.specificPopulationCheckboxSelected(SpecificPopulationEnum.OTHER)}
                    onChange={v => this.updateSpecificPopulation(SpecificPopulationEnum.OTHER, v)}
                    disabled={!this.state.workspace.researchPurpose.population}
                />
                <TextInput type='text' autoFocus placeholder='Please specify'
                           value={this.state.workspace.researchPurpose.otherPopulationDetails}
                           disabled={!fp.includes(SpecificPopulationEnum.OTHER,
                             this.state.workspace.researchPurpose.populationDetails)}
                           onChange={v => this.setState(fp.set(
                             ['workspace', 'researchPurpose', 'otherPopulationDetails'], v))}/>
              </FlexColumn>
            </FlexRow>
          </div>
        </WorkspaceEditSection>
        <WorkspaceEditSection header='Request a review of your research purpose for potential
                                      stigmatization of research participants'>
          <Link onClick={() => this.setState({showStigmatizationDetails:
              !this.state.showStigmatizationDetails})}>
            More info on stigmatization
            {this.state.showStigmatizationDetails ? <ClrIcon shape='caret' dir='up'/> :
              <ClrIcon shape='caret' dir='down'/>}
          </Link>
          {this.state.showStigmatizationDetails &&
            <div>
              <div style={styles.text}>
                Populations that are historically medically underserved or underrepresented in
                biomedical research are also more vulnerable to stigmatization. If your population
                of interest includes the following categories defined as Underrepresented in
                Biomedical Research (UBR) by the <i>All of Us</i> Research Program, you are
                encouraged to request a review of your research purpose by the Resource Access
                Board (RAB).
              </div>
              <TwoColPaddedTable header={true} headerLeft='Diversity Categories'
                 headerRight='Groups that are Underrepresented in Biomedical Research (UBR)*'
                 cellWidth={{left: '30%', right: '70%'}}
                 contentLeft={SpecificPopulationItems.map(sp => sp.ubrLabel)}
                 contentRight={SpecificPopulationItems.map(sp => sp.ubrDescription)}/>
            </div>
          }
          <FlexRow style={{paddingTop: '0.3rem'}}>
            <label style={styles.text}>
              <div>
              If you are concerned that your research may result in <a href='/definitions/stigmatization' target='_blank'>
              stigmatization of research participants</a>,
              please request review of your research purpose by the <i>All of Us</i>  Resource Access Board (RAB). The RAB
              will provide feedback regarding the potential for stigmatizing specific groups of participants, and if
              needed, guidance for modifying your research purpose/scope. Even if you request a review, you will be
              able to continue creating the Workspace and proceed with your research, while RAB reviews your research
              purpose.
              </div>
              <div style={{marginTop: '0.5rem'}}>Would you like to request a review of your research purpose?</div>
            </label>
          </FlexRow>
          <div>
            <RadioButton name='reviewRequested'
                         disabled={this.isMode(WorkspaceEditMode.Edit)}
                         onChange={() => {
                           this.updateResearchPurpose('reviewRequested', true);
                         }}
                         checked={this.state.workspace.researchPurpose.reviewRequested}/>
            <label style={{...styles.text, marginLeft: '0.5rem', marginRight: '3rem'}}>Yes</label>
            <RadioButton name='reviewRequested'
                         disabled={this.isMode(WorkspaceEditMode.Edit)}
                         onChange={() => {
                           this.updateResearchPurpose('reviewRequested', false);
                         }}
                         checked={!this.state.workspace.researchPurpose.reviewRequested}/>
            <label style={{...styles.text, marginLeft: '0.5rem', marginRight: '3rem'}}>No</label>
          </div>
        </WorkspaceEditSection>
        <div>
          <FlexRow style={{marginTop: '1rem', marginBottom: '1rem'}}>
            <Button type='secondary' style={{marginRight: '1rem'}}
                    onClick = {() => this.props.cancel()}>
              Cancel
            </Button>
            <TooltipTrigger content={
              errors && <ul>
                {errors.name && <div>{errors.name}</div>}
                {errors.billingAccountName && <div>You must select a billing account</div>}
                {errors.primaryPurpose && <div>You must choose at least one primary research purpose</div>}
                {errors.reasonForAllOfUs && <div>You must specify a reason for using <i>All of Us</i> data</div>}
                {errors.intendedStudy && <div>You must specify a field of intended study</div>}
                {errors.anticipatedFindings && <div>You must specify anticipated findings</div>}
                {errors.specificPopulation && <div>You must specify a population of study</div>}
                {errors.diseaseOfFocus && <div>You must specify a disease of focus</div>}
              </ul>
            } disabled={!errors}>
              <Button type='primary'
                      onClick={() => this.onSaveClick()}
                      disabled={errors || this.state.loading}
                      data-test-id='workspace-save-btn'>
                {this.renderButtonText()}
              </Button>
            </TooltipTrigger>
          </FlexRow>
        </div>
        {this.state.workspaceCreationError &&
        <Modal>
          <ModalTitle>Error:</ModalTitle>
          <ModalBody>
            { this.state.workspaceCreationErrorMessage }
          </ModalBody>
          <ModalFooter>
            <Button onClick = {() => this.props.cancel()}
                type='secondary' style={{marginRight: '2rem'}}>
              Cancel
              {this.props.routeConfigData.mode === WorkspaceEditMode.Create ?
                ' Creation' : ' Update'}
                </Button>
            <Button type='primary' onClick={() => this.resetWorkspaceEditor()}>Keep Editing</Button>
          </ModalFooter>
        </Modal>
        }
        {this.state.showCreateBillingAccountModal &&
          <CreateBillingAccountModal onClose={() => this.setState({showCreateBillingAccountModal: false})} />}
        {this.state.workspaceCreationConflictError &&
        <Modal>
          <ModalTitle>{this.props.routeConfigData.mode === WorkspaceEditMode.Create ?
              'Error: ' : 'Conflicting update:'}</ModalTitle>
          <ModalBody>
            {this.props.routeConfigData.mode === WorkspaceEditMode.Create ?
              'You already have a workspace named ' + this.state.workspace.name +
              ' Please choose another name' :
              'Another client has modified this workspace since the beginning of this editing ' +
              'session. Please reload to avoid overwriting those changes.'}
          </ModalBody>
          <ModalFooter>
            <Button type='secondary' onClick = {() => this.props.cancel()}
                    style={{marginRight: '2rem'}}>Cancel Creation</Button>
            <Button type='primary' onClick={() => this.resetWorkspaceEditor()}>Keep Editing</Button>
          </ModalFooter>
        </Modal>
        }
        {this.state.workspaceNewAclDelayed &&
        <Modal>
          <ModalTitle>Workspace permissions delay</ModalTitle>
          <ModalBody>
            The permissions for this workspace are currently being set up. You can continue to use
            this workspace as a 'Reader'. Please refresh the workspace page in a few minutes to be
            able to create Cohorts, Datasets and Notebooks.
          </ModalBody>
          <ModalFooter>
            <Button type='primary' data-test-id='workspace-acl-delay-btn'
                    onClick={() => this.state.workspaceNewAclDelayedContinueFn()}>
              Continue
            </Button>
          </ModalFooter>
        </Modal>
        }
        </div>
      </FadeBox> ;
    }

  });

@Component({
  template: '<div #root></div>'
})
export class WorkspaceEditComponent extends ReactWrapperBase {

  constructor(private _location: Location) {
    super(WorkspaceEdit, ['cancel']);
    this.cancel = this.cancel.bind(this);
  }

  cancel(): void {
    this._location.back();
  }
}
