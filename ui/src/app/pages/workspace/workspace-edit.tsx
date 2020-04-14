import {Location} from '@angular/common';
import {Component} from '@angular/core';
import {Button, Clickable, Link, StyledAnchorTag} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {InfoIcon} from 'app/components/icons';
import {CheckBox, RadioButton, TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SearchInput} from 'app/components/search-input';
import {SpinnerOverlay} from 'app/components/spinners';

import {CreateBillingAccountModal} from 'app/pages/workspace/create-billing-account-modal';
import {WorkspaceEditSection} from 'app/pages/workspace/workspace-edit-section';
import {
  disseminateFindings,
  PrimaryPurposeItems,
  RequestForReviewFooter,
  researchOutcomes,
  ResearchPurposeDescription,
  ResearchPurposeItem,
  ResearchPurposeItems,
  researchPurposeQuestions,
  SpecificPopulationItem,
  SpecificPopulationItems,
  toolTipText,
  toolTipTextDataUseAgreement,
  toolTipTextDemographic,
  toolTipTextStigmatization
} from 'app/pages/workspace/workspace-edit-text';
import {WorkspaceResearchSummary} from 'app/pages/workspace/workspace-research-summary';
import {userApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  renderUSD,
  sliceByHalfLength,
  withCdrVersions,
  withCurrentWorkspace,
  withRouteConfigData,
  withUserProfile
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
  DisseminateResearchEnum,
  Profile,
  ResearchOutcomeEnum,
  ResearchPurpose,
  SpecificPopulationEnum,
  Workspace,
  WorkspaceAccessLevel
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import {OverlayPanel} from 'primereact/overlaypanel';
import * as React from 'react';
import * as validate from 'validate.js';

export const styles = reactStyles({
  categoryRow: {
    display: 'flex', flexDirection: 'row', padding: '0.6rem 0', width: '95%'
  },
  checkboxRow: {
    display: 'inline-block', padding: '0.2rem 0', marginRight: '1rem'
  },
  checkboxStyle: {
    marginRight: '.31667rem', zoom: '1.5'
  },
  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: colors.primary
  },
  flexColumnBy2: {
    flex: '1 1 0',
    marginLeft: '1rem'
  },
  freeCreditsBalanceClickable: {
    display: 'inline-block',
    color: colors.accent,
    padding: '0.25rem 0 0 1rem'
  },
  freeCreditsBalanceOverlay: {
    height: 44,
    width: 150,
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: 12,
    letterSpacing: '0',
    lineHeight: '22px',
  },
  infoIcon: {
    height: '16px',
    marginLeft: '0.2rem',
    width: '16px'
  },
  longDescription: {
    position: 'relative',
    display: 'inline-block',
    minHeight: '1rem',
    cursor: 'text',
    lineHeight: '1rem',
    width: '100%'
  },
  researchPurposeRow: {
    backgroundColor: colors.white,
    borderColor: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`,
    marginLeft: '-1rem',
    paddingTop: '0.3rem',
    paddingBottom: '0.3rem'
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
  spinner: {
    position: 'fixed',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    backgroundColor: 'rgba(0, 0, 0, 0.2)',
    height: '100%',
    width: '100%',
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
  }
});

const CREATE_BILLING_ACCOUNT_OPTION_VALUE = 'CREATE_BILLING_ACCOUNT_OPTION';

// Poll parameters to check Workspace ACLs after creation of a new workspace. See
// SATURN-104 for details, eventually the root cause should be resolved by fixes
// to Sam (as part of Postgres migration).
const NEW_ACL_DELAY_POLL_TIMEOUT_MS = 60 * 1000;
const NEW_ACL_DELAY_POLL_INTERVAL_MS = 10 * 1000;

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
  profileState: {
    profile: Profile;
  };
}


export interface WorkspaceEditState {
  cdrVersionItems: Array<CdrVersion>;
  selectResearchPurpose: boolean;
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
  showResearchPurpose: boolean;
  billingAccounts: Array<BillingAccount>;
  showCreateBillingAccountModal: boolean;
  populationChecked: boolean;
}

export const WorkspaceEdit = fp.flow(withRouteConfigData(), withCurrentWorkspace(), withCdrVersions(), withUserProfile())(
  class WorkspaceEditCmp extends React.Component<WorkspaceEditProps, WorkspaceEditState> {
    constructor(props: WorkspaceEditProps) {
      super(props);
      this.state = {
        cdrVersionItems: this.createInitialCdrVersionsList(),
        workspace: this.createInitialWorkspaceState(),
        selectResearchPurpose: this.updateSelectedResearch(),
        showResearchPurpose: this.updateSelectedResearch(),
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
        showCreateBillingAccountModal: false,
        populationChecked: props.workspace ? props.workspace.researchPurpose.populationDetails.length > 0 : false,
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
          // Otherwise, use this as an opportunity to sync the fetched billing account name from
          // the source of truth, Google
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
            scientificApproach: '',
            methodsDevelopment: false,
            otherPopulationDetails: '',
            otherPurpose: false,
            otherPurposeDetails: '',
            ethics: false,
            populationDetails: [],
            populationHealth: false,
            reviewRequested: false,
            socialBehavioral: false,
            reasonForAllOfUs: '',
          }
        };
      }

      if (!fp.includes(DisseminateResearchEnum.OTHER, workspace.researchPurpose.disseminateResearchFindingList)) {
        workspace.researchPurpose.otherDisseminateResearchFindings = '';
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

    updateSelectedResearch(): boolean {
      if (this.isMode(WorkspaceEditMode.Create)) {
        return false;
      }
      const rp = this.props.workspace.researchPurpose;
      return this.researchPurposeCategoriesSelected(rp);
    }

    researchPurposeCategoriesSelected(researchPurpose: ResearchPurpose) {
      return researchPurpose.ancestry || researchPurpose.controlSet ||
        researchPurpose.diseaseFocusedResearch || researchPurpose.ethics ||
        researchPurpose.drugDevelopment || researchPurpose.methodsDevelopment ||
        researchPurpose.populationHealth || researchPurpose.socialBehavioral;
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
          data-test-id='diseaseOfFocus-input'
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
        The <i>All of Us</i> Program provides free credits for each registered user. If you use up your free credits,
        you can request additional credits or use your own <StyledAnchorTag href={'https://aousupporthelp.zendesk.' +
        'com/hc/en-us/articles/360039539411-How-to-Create-a-Billing-Account>'} target='_blank'>Google Cloud Platform
        billing account</StyledAnchorTag>
      </div>;
    }

    onResearchPurposeChange(checked: boolean) {
      // If Checkbox is selected expand the research purpose categories
      if (checked) {
        this.setState({showResearchPurpose: true, selectResearchPurpose: true});
      } else {
        this.setState({selectResearchPurpose: false});
      }
    }

    get researchPurposeCheck(): boolean {
      // If any one of the Research Purpose is selected or if the user has explicitly selected the research purpose
      return this.state.selectResearchPurpose ||
        this.researchPurposeCategoriesSelected(this.state.workspace.researchPurpose);
    }

    get iconClass(): string {
      return this.state.showResearchPurpose ? 'pi pi-angle-down' : 'pi pi-angle-right';
    }
    /**
     * Creates a form element containing the checkbox, header, and description
     * (plus optional child elements) for each of the "primary purpose of your
     * project" options.
     */
    makePrimaryPurposeForm(rp: ResearchPurposeItem, index: number): React.ReactNode {
      let children: React.ReactNode;
      // If its a sub category of Research purpose and not Education/Other
      const isResearchPurpose = ResearchPurposeItems.indexOf(rp) > -1;
      if (rp.shortName === 'diseaseFocusedResearch') {
        children = this.makeDiseaseInput();
      } else if (rp.shortName === 'otherPurpose') {
        children = <TextArea value={this.state.workspace.researchPurpose.otherPurposeDetails}
                  onChange={v => this.updateResearchPurpose('otherPurposeDetails', v)}
                  disabled={!this.state.workspace.researchPurpose.otherPurpose}
                  data-test-id='otherPrimaryPurposeText'
                  style={{marginTop: '0.5rem'}}/>;
      }

      return <div key={index} style={styles.categoryRow}>
        <CheckBox id={rp.uniqueId}
                  data-test-id={rp.shortName + '-checkbox'}
                  style={styles.checkboxStyle}
                  checked={!!this.state.workspace.researchPurpose[rp.shortName]}
                  onChange={e => this.updatePrimaryPurpose(rp.shortName, e)}/>
        <FlexColumn style={{marginTop: '-0.2rem'}}>
          <label style={{...styles.shortDescription, fontSize: isResearchPurpose ? 14 : 16}} htmlFor={rp.uniqueId}>
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

    updateOtherDisseminateResearch(value) {
      this.setState(fp.set(['workspace', 'researchPurpose', 'otherDisseminateResearchFindings'], value));
    }
    /**
     * Creates a form element containing the checkbox, header, and description
     * (plus optional child elements) for each of the "Disseminate Research" options.
     */
    makeDisseminateForm(rp, index): React.ReactNode {
      let children: React.ReactNode;
      if (rp.label === 'Other') {
        children = <TextArea value={this.state.workspace.researchPurpose.otherDisseminateResearchFindings}
                             onChange={v => this.updateOtherDisseminateResearch(v)}
                             placeholder='Specify the name of the forum (journal, scientific
                             conference, blog etc.) through which you will disseminate your
                             findings, if available.'
                             data-test-id='otherDisseminateResearch-text'
                             disabled={!this.disseminateCheckboxSelected(DisseminateResearchEnum.OTHER)}
                             style={{marginTop: '0.5rem', width: '16rem'}}/>;
      }

      return <div key={index} style={styles.categoryRow}>
        <CheckBox style={styles.checkboxStyle}
                  data-test-id={index + '-checkbox'}
                  checked={this.disseminateCheckboxSelected(rp.shortName)}
                  onChange={e => this.updateAttribute('disseminateResearchFindingList', rp.shortName, e)}/>
        <FlexColumn style={{marginTop: '-0.2rem'}}>
          <label style={styles.text}>
            {rp.label}
          </label>
          {children}
        </FlexColumn>
      </div>;
    }
    /**
     * Creates the form element for each of the "focus on specific populations"
     * options.
     */
    makeSpecificPopulationForm(item: SpecificPopulationItem): React.ReactNode {
      return <div key={item.label}><strong>{item.label} *</strong>
        {item.subCategory.map((sub, index) => <FlexRow key={sub.label}>
          <CheckBox
              manageOwnState={false}
              wrapperStyle={styles.checkboxRow}
              data-test-id={sub.shortName + '-checkbox'}
              style={styles.checkboxStyle}
              label={sub.label}
              labelStyle={styles.text}
              key={sub.label}
              checked={this.specificPopulationCheckboxSelected(sub.shortName)}
              onChange={v => this.updateSpecificPopulation(sub.shortName, v)}
              disabled={!this.state.populationChecked}/></FlexRow>)}
      </div>;
    }

    makeOutcomingResearchForm(item, I): React.ReactNode {
      return <CheckBox
          wrapperStyle={styles.checkboxRow}
          style={styles.checkboxStyle}
          label={item.label}
          labelStyle={styles.text}
          key={item.label}
          checked={this.researchOutcomeCheckboxSelected(item.shortName)}
          onChange={v => this.updateAttribute('researchOutcomeList', item.shortName, v)}
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

    get primaryPurposeIsSelected() {
      const rp = this.state.workspace.researchPurpose;
      return rp.ancestry || rp.commercialPurpose || rp.controlSet ||
          rp.diseaseFocusedResearch || rp.ethics || rp.drugDevelopment || rp.educational ||
          rp.methodsDevelopment || rp.otherPurpose || rp.populationHealth || rp.socialBehavioral;
    }

    get isOtherPrimaryPurposeValid() {
      const rp = this.state.workspace.researchPurpose;
      return !rp.otherPurpose ||
          (rp.otherPurposeDetails && rp.otherPurposeDetails.length <= 500);
    }

    get isSpecificPopulationValid() {
      const researchPurpose = this.state.workspace.researchPurpose;
      return (
          !this.state.populationChecked ||
          (
              researchPurpose.populationDetails &&
              researchPurpose.populationDetails.length !== 0
          )
      );
    }

    get isOtherSpecificPopulationValid() {
      const researchPurpose = this.state.workspace.researchPurpose;
      return this.isSpecificPopulationValid && (
          !fp.includes(SpecificPopulationEnum.OTHER, researchPurpose.populationDetails) ||
          (researchPurpose.otherPopulationDetails && researchPurpose.otherPopulationDetails.length <= 100));
    }

    get isDiseaseOfFocusValid() {
      const researchPurpose = this.state.workspace.researchPurpose;
      return !researchPurpose.diseaseFocusedResearch ||
        researchPurpose.diseaseOfFocus && researchPurpose.diseaseOfFocus.length <= 80;
    }

    get isDisseminateResearchValid() {
      const researchPurpose = this.state.workspace.researchPurpose;
      return researchPurpose.disseminateResearchFindingList &&
          researchPurpose.disseminateResearchFindingList.length !== 0;
    }

    get isOtherDisseminateResearchValid() {
      const researchPurpose = this.state.workspace.researchPurpose;
      return !fp.includes(DisseminateResearchEnum.OTHER, researchPurpose.disseminateResearchFindingList) ||
              (researchPurpose.otherDisseminateResearchFindings && researchPurpose.otherDisseminateResearchFindings.length <= 100);
    }



    get isResearchOutcome() {
      const researchPurpose = this.state.workspace.researchPurpose;
      return researchPurpose.researchOutcomeList && researchPurpose.researchOutcomeList.length !== 0 ;
    }

    updatePrimaryPurpose(cateogry, value) {
      this.updateResearchPurpose(cateogry, value);
      if (!value && !this.researchPurposeCategoriesSelected(this.state.workspace.researchPurpose)) {
        // If all research purpose cateogries are unselected un check the Research Purpose checkbox
        this.setState({selectResearchPurpose: false});
      }

    }

    updateResearchPurpose(category, value) {
      if (category === 'population' && !value) {
        this.setState(fp.set(['workspace', 'researchPurpose', 'populationDetails'], []));
      }
      this.setState(fp.set(['workspace', 'researchPurpose', category], value));
    }

    updateAttribute(attribute, populationDetails, value) {
      const selectedPopulations = fp.get(['workspace', 'researchPurpose' , attribute], this.state);
      if (value) {
        if (!!selectedPopulations) {
          this.setState(fp.set(['workspace', 'researchPurpose', attribute],
            selectedPopulations.concat([populationDetails])));
        } else {
          this.setState(fp.set(['workspace', 'researchPurpose', attribute],
              [populationDetails]));
        }
      } else {
        this.setState(fp.set(['workspace', 'researchPurpose', attribute],
          selectedPopulations.filter(v => v !== populationDetails)));
      }
    }

    updateSpecificPopulation(populationDetails, value) {
      this.updateAttribute('populationDetails', populationDetails, value);
    }

    specificPopulationCheckboxSelected(populationEnum: SpecificPopulationEnum): boolean {
      return fp.includes(populationEnum, this.state.workspace.researchPurpose.populationDetails);
    }

    disseminateCheckboxSelected(disseminateEnum: DisseminateResearchEnum): boolean {
      return fp.includes(disseminateEnum, this.state.workspace.researchPurpose.disseminateResearchFindingList);
    }

    researchOutcomeCheckboxSelected(researchOutcomeEnum: ResearchOutcomeEnum): boolean {
      return fp.includes(researchOutcomeEnum, this.state.workspace.researchPurpose.researchOutcomeList);
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
        if (!this.state.populationChecked) {
          workspace.researchPurpose.populationDetails = [];
        }

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
        error = await error.json();

        console.log(error);
        this.setState({loading: false});
        if (error.statusCode === 409) {
          this.setState({workspaceCreationConflictError: true});
        } else {
          let errorMsg;
          if (error.statusCode === 429) {
            errorMsg = 'Server is overloaded. Please try again in a few minutes.';
          } else if (error.message.includes('billing account is closed')) {
            errorMsg = error.message;
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
      const options = this.state.billingAccounts.map(a => ({
        label: a.displayName,
        value: a.name,
        disabled: !a.isOpen
      }));

      options.push({
        label: 'Create a new billing account',
        value: CREATE_BILLING_ACCOUNT_OPTION_VALUE,
        disabled: false
      });

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
            scientificApproach
          }
        }
      } = this.state;
      const {freeTierDollarQuota, freeTierUsage} = this.props.profileState.profile;
      const freeTierCreditsBalance = freeTierDollarQuota - freeTierUsage;
      // defined below in the OverlayPanel declaration
      let freeTierBalancePanel: OverlayPanel;
      const errors = validate({
        name,
        billingAccountName,
        anticipatedFindings,
        intendedStudy,
        scientificApproach,
        'primaryPurpose': this.primaryPurposeIsSelected,
        'otherPrimaryPurpose': this.isOtherPrimaryPurposeValid,
        'specificPopulation': this.isSpecificPopulationValid,
        'otherSpecificPopulation': this.isOtherSpecificPopulationValid,
        'diseaseOfFocus': this.isDiseaseOfFocusValid,
        'researchOutcoming': this.isResearchOutcome,
        'disseminate': this.isDisseminateResearchValid,
        'otherDisseminateResearchFindings': this.isOtherDisseminateResearchValid
      }, {
        name: {
          length: { minimum: 1, maximum: 80 }
        },
        billingAccountName: { presence: true },
        intendedStudy: { length: { minimum: 1, maximum: 1000 } },
        anticipatedFindings: {length: { minimum: 1, maximum: 1000 }},
        scientificApproach: { length: { minimum: 1, maximum: 1000 } },
        primaryPurpose: { truthiness: true },
        otherPrimaryPurpose: {truthiness: true},
        specificPopulation: { truthiness: true },
        diseaseOfFocus: { truthiness: true },
        researchOutcoming: {truthiness: true},
        disseminate: {truthiness: true},
        otherDisseminateResearchFindings: {truthiness: true},
        otherSpecificPopulation: {truthiness: true}

      });
      return <FadeBox  style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <div style={{width: '95%'}}>
          {this.state.loading && <SpinnerOverlay overrideStylesOverlay={styles.spinner}/>}
          <WorkspaceEditSection header={this.renderHeader()} tooltip={toolTipText.header}
                                style={{marginTop: '24px'}} largeHeader
                                required={!this.isMode(WorkspaceEditMode.Duplicate)}>
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
        <WorkspaceEditSection header='Options for duplicate workspace'>
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
            <OverlayPanel ref={(me) => freeTierBalancePanel = me} dismissable={true} appendTo={document.body}>
              <div style={styles.freeCreditsBalanceOverlay}>
                FREE CREDIT BALANCE {renderUSD(freeTierCreditsBalance)}
              </div>
            </OverlayPanel>
            <FlexRow>
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
              {freeTierCreditsBalance > 0.0 && <div style={styles.freeCreditsBalanceClickable}>
                <Clickable onClick={(e) => freeTierBalancePanel.toggle(e)}>View FREE credits balance</Clickable>
              </div>}
            </FlexRow>
          </WorkspaceEditSection>}
        <hr style={{marginTop: '1rem'}}/>
        <WorkspaceEditSection header='Research Use Statement Questions'
              description={<div style={{marginLeft: '-0.9rem', fontSize: 14}}> {ResearchPurposeDescription}
              <br/><br/>
              <i>Note that you are required to create separate Workspaces for each project
                for which you access All of Us data, hence the responses below are expected
                to be specific to the project for which you are creating this particular
                Workspace.</i></div>
            }/>

        {/*Primary purpose */}
        <WorkspaceEditSection header={researchPurposeQuestions[0].header}
            description={researchPurposeQuestions[0].description} index='1.' indent>
          <FlexRow>
            <FlexColumn>
              <FlexColumn  style={styles.researchPurposeRow}>
                <FlexRow>
                <CheckBox
                  data-test-id='researchPurpose-checkbox'
                  manageOwnState={false}
                  style={{...styles.checkboxStyle, marginLeft: '0.6rem', marginTop: '0.1rem'}}
                  checked={this.researchPurposeCheck}
                  onChange={v => this.onResearchPurposeChange(v)}/>
                  <div style={{...styles.shortDescription, marginLeft: '-0.5rem'}}>
                    <button style={{...styles.shortDescription, border: 'none'}}
                            data-test-id='research-purpose-button'
                            onClick={() => this.setState({showResearchPurpose: !this.state.showResearchPurpose})}>
                      Research purpose
                      <i className={this.iconClass} style={{verticalAlign: 'middle'}}></i>
                     </button>
                  </div>
                </FlexRow>
                  {this.state.showResearchPurpose && <FlexColumn data-test-id='research-purpose-categories'>
                    <div style={{...styles.text, marginLeft: '1.9rem'}}>
                      Choose options below to describe your research purpose
                    </div>
                    <div style={{marginLeft: '2rem'}}>
                  {ResearchPurposeItems.map(
                    (rp, i) => this.makePrimaryPurposeForm(rp, i))}
                  </div></FlexColumn>}
              </FlexColumn>

              {PrimaryPurposeItems.map((rp, i) => this.makePrimaryPurposeForm(rp, i))}
            </FlexColumn>
          </FlexRow>
        </WorkspaceEditSection>

        <WorkspaceEditSection
          header={researchPurposeQuestions[1].header} indent
          description={researchPurposeQuestions[1].description} style={{width: '50rem'}} index='2.'>
          <FlexColumn>
            {/* TextBox: scientific question(s) researcher intend to study Section*/}
            <WorkspaceResearchSummary researchPurpose={researchPurposeQuestions[2]}
                          researchValue={this.state.workspace.researchPurpose.intendedStudy}
                          onChange={v => this.updateResearchPurpose('intendedStudy', v)}
                          index='2.1' rowId='intendedStudyText'/>

            {/* TextBox: scientific approaches section*/}
            <WorkspaceResearchSummary researchPurpose={researchPurposeQuestions[3]}
                           researchValue={this.state.workspace.researchPurpose.scientificApproach}
                            onChange={v => this.updateResearchPurpose('scientificApproach', v)}
                           index='2.2' rowId='scientificApproachText'/>
            {/*TextBox: anticipated findings from the study section*/}
            <WorkspaceResearchSummary researchPurpose={researchPurposeQuestions[4]}
                           researchValue={this.state.workspace.researchPurpose.anticipatedFindings}
                           onChange={v => this.updateResearchPurpose('anticipatedFindings', v)}
                           index='2.3' rowId='anticipatedFindingsText'/>
          </FlexColumn>
        </WorkspaceEditSection>

          {/*disseminate  research Section */}
        <WorkspaceEditSection header={researchPurposeQuestions[5].header}
                              description={researchPurposeQuestions[5].description} style={{width: '50rem'}} index='3.'>
          <FlexRow>
            <FlexColumn style={styles.flexColumnBy2}>
              {disseminateFindings.slice(0, sliceByHalfLength(disseminateFindings)).map(
                (rp, i) => this.makeDisseminateForm(rp, rp.shortName))}
            </FlexColumn>
            <FlexColumn style={styles.flexColumnBy2}>
              {disseminateFindings.slice(sliceByHalfLength(disseminateFindings)).map(
                (rp, i) => this.makeDisseminateForm(rp, rp.shortName))}
            </FlexColumn>
          </FlexRow>
        </WorkspaceEditSection>

          {/*Research outcome section*/}
          <WorkspaceEditSection header={researchPurposeQuestions[6].header} index='4.'
                                description={researchPurposeQuestions[6].description}
                                style={{width: '50rem'}}>
            <FlexRow style={{marginLeft: '1rem'}}>
              <FlexColumn style={{flex: '1 1 0'}}>
                {researchOutcomes.map(
                  (rp, i) => this.makeOutcomingResearchForm(rp, i))}
              </FlexColumn>
            </FlexRow>
          </WorkspaceEditSection>

          {/*Underrespresented population section*/}
        <WorkspaceEditSection header={researchPurposeQuestions[7].header} index='5.' indent
                              description={researchPurposeQuestions[7].description}
                              style={{width: '50rem'}}>
          <div style={styles.header}>Will your study focus on any historically underrepresented populations?</div>
          <div>
            <RadioButton name='population' style={{marginRight: '0.5rem'}}
                         data-test-id='specific-population-yes'
                         onChange={v => this.setState({populationChecked: true})}
                         checked={this.state.populationChecked}/>
            <label style={styles.text}>Yes, my study will focus on one or more specific
              underrepresented populations, either on their own or in comparison to other groups.</label>
          </div>
          <div style={{...styles.text, marginLeft: '1.2rem'}}>
            <strong>If "Yes": </strong> please indicate your underrepresented population(s) of
            interest:
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
                    data-test-id='other-specialPopulation-checkbox'
                    label='Other'
                    labelStyle={styles.text}
                    checked={!!this.specificPopulationCheckboxSelected(SpecificPopulationEnum.OTHER)}
                    onChange={v => this.updateSpecificPopulation(SpecificPopulationEnum.OTHER, v)}
                    disabled={!this.state.populationChecked}
                />
                <TextInput type='text' autoFocus placeholder='Please specify'
                           value={this.state.workspace.researchPurpose.otherPopulationDetails}
                           disabled={!fp.includes(SpecificPopulationEnum.OTHER,
                             this.state.workspace.researchPurpose.populationDetails)}
                           data-test-id='other-specialPopulation-text'
                           onChange={v => this.setState(fp.set(
                             ['workspace', 'researchPurpose', 'otherPopulationDetails'], v))}/>
              </FlexColumn>
            </FlexRow>
            <hr/>
            <div>* Demographic variables for which data elements have been altered, partially
              suppressed, or generalized in the Registered Tier to protect data privacy. Refer to
              the Data Dictionary for details.</div>
            <hr/>
          </div>
          <div style={{marginTop: '0.5rem'}}>
            <RadioButton name='population'
                         style={{marginRight: '0.5rem'}}
                         data-test-id='specific-population-no'
                         onChange={v => this.setState({populationChecked: false})}
                         checked={!this.state.populationChecked}/>
            <label style={styles.text}>No, my study will not center on underrepresented populations.
              I am interested in a diverse sample in general, or I am focused on populations that
              have been well represented in prior research.</label>
          </div>
        </WorkspaceEditSection>

          {/* Request for review section*/}
        <WorkspaceEditSection header={researchPurposeQuestions[8].header} index='6.' indent>
          <FlexRow style={styles.text}><div>
            Any research that focuses on certain population characteristics or&nbsp;
            <TooltipTrigger content={toolTipTextDemographic} style={{display: 'inline-block'}}>
              <Link style={{display: 'inline-block'}}>uses
              demographic variables</Link>
            </TooltipTrigger>
            &nbsp;in analyses can result, often unintentionally,
            in findings that may be misinterpreted or misused by others to foster stigma. While it
            may not be possible to completely prevent misuse of research for stigmatizing purposes,
            data users can take important steps to minimize the risk of this happening–
            <TooltipTrigger content={toolTipTextDataUseAgreement}>
              <Link style={{display: 'inline-block'}}>taking this step is a condition of your
                Data Use Agreement.</Link>
            </TooltipTrigger>
            &nbsp;If you are concerned that your research could inadvertently stigmatize
            participants or communities, or if you are unsure, let us know. We encourage you to
            request a review of your research purpose statement by the All of Us Resource Access
            Board (RAB) as a precaution. The RAB will provide feedback and, if needed, guidance for
            modifying your research purpose or scope. To learn more, please refer to the&nbsp;
            <TooltipTrigger content={toolTipTextStigmatization} style={{display: 'inline-block'}}>
            <Link style={{display: 'inline-block'}}><i>All of Us</i> Stigmatizing Research Policy</Link>
            </TooltipTrigger>. If you
            request a review, you can expect to receive an initial response within five business days.
            During the RAB’s review, you may begin working in your workspace.</div>
          </FlexRow>
          <FlexRow style={{paddingTop: '0.3rem'}}>
            <FlexColumn>
            <label style={{...styles.header, marginBottom: '0.2rem'}}>Would you like to request a
              review of your research purpose
              statement by the Resource Access Board?</label>
            <label style={styles.text}>
                Note: Your response to this question is private and will not be displayed on the
              Research Hub.
            </label>
              <FlexColumn>
                <FlexRow>
                <RadioButton style={{marginTop: '0.2rem'}} name='reviewRequested'
                             disabled={this.isMode(WorkspaceEditMode.Edit)}
                             onChange={() => {
                               this.updateResearchPurpose('reviewRequested', true);
                             }}
                             checked={this.state.workspace.researchPurpose.reviewRequested}/>
                <label style={{...styles.text, marginLeft: '0.5rem'}}>Yes, I would like to request
                  a review of my research purpose.</label>
                </FlexRow>
                <FlexRow>
                <RadioButton style={{marginTop: '0.2rem'}} name='reviewRequested'
                             disabled={this.isMode(WorkspaceEditMode.Edit)}
                             onChange={() => {
                               this.updateResearchPurpose('reviewRequested', false);
                             }}
                             checked={!this.state.workspace.researchPurpose.reviewRequested}/>
                <label style={{...styles.text, marginLeft: '0.5rem', marginRight: '3rem'}}>No, I
                  have no concerns at this time about potential stigmatization based on my study.</label>
                </FlexRow>
              </FlexColumn>
              <label style={{...styles.text, paddingTop: '0.5rem'}}>{RequestForReviewFooter}</label>
            </FlexColumn>
          </FlexRow>
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
                {errors.billingAccountName && <div>
                  You must select a billing account</div>}
                {errors.primaryPurpose && <div> You must choose at least one primary research
                  purpose (Question 1)</div>}
                {errors.otherPrimaryPurpose && <div> Other primary purpose should be of at most 500 characters</div>}
                {errors.anticipatedFindings && <div> Answer for <i>What are the anticipated findings
                  from the study? (Question # 2.1)</i> cannot be empty</div>}
                {errors.scientificApproach && <div> Answer for <i>What are the scientific
                  approaches you plan to use for your study (Question # 2.2)</i> cannot be empty</div>}
                {errors.intendedStudy && <div> Answer for<i>What are the specific scientific question(s) you intend to study
                  (Question # 2.3)</i> cannot be empty</div>}
                {errors.specificPopulation && <div> You must specify a population of study</div>}
                {errors.diseaseOfFocus && <div> You must specify a disease of focus and it should be at most 80 characters</div>}
                {errors.researchOutcoming && <div> You must specify the outcome of the research</div>}
                {errors.disseminate && <div> You must specific how you plan to disseminate your research findings</div>}
                {errors.otherDisseminateResearchFindings && <div>
                  Disseminate Research Findings Other text should be of at most 100 characters</div>}
                {errors.otherSpecificPopulation && <div>
                  Specific Population Other text should be of at most 100 characters</div>}
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
