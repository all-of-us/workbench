import * as fp from 'lodash/fp';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button, Clickable, Link, StyledAnchorTag} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {InfoIcon} from 'app/components/icons';
import {CheckBox, RadioButton, TextArea, TextInput} from 'app/components/inputs';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SearchInput} from 'app/components/search-input';
import {SpinnerOverlay} from 'app/components/spinners';
import {AoU, AouTitle} from 'app/components/text-wrappers';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
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
  formatFreeCreditsUSD,
  reactStyles,
  sliceByHalfLength,
  withCdrVersions,
  withCurrentWorkspace,
  withUserProfile
} from 'app/utils';
import {AccessTierShortNames} from 'app/utils/access-tiers';
import {AnalyticsTracker} from 'app/utils/analytics';
import {
  getCdrVersion,
  getCdrVersionTier,
  getDefaultCdrVersionForTier,
  hasDefaultCdrVersion
} from 'app/utils/cdr-versions';
import {reportError} from 'app/utils/errors';
import {currentWorkspaceStore, navigate, nextWorkspaceWarmupStore} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/stores';
import {getBillingAccountInfo} from 'app/utils/workbench-gapi-client';
import {WorkspaceData} from 'app/utils/workspace-data';
import {openZendeskWidget, supportUrls} from 'app/utils/zendesk';
import {
  ArchivalStatus,
  BillingAccount,
  CdrVersion,
  CdrVersionTiersResponse,
  DisseminateResearchEnum,
  Profile,
  ResearchOutcomeEnum,
  ResearchPurpose,
  SpecificPopulationEnum,
  Workspace,
  WorkspaceAccessLevel
} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';
import {OverlayPanel} from 'primereact/overlaypanel';
import {OldCdrVersionModal} from './old-cdr-version-modal';

export const styles = reactStyles({
  categoryRow: {
    display: 'flex',
    flexDirection: 'row',
    padding: '0.6rem 0',
    width: '95%'
  },
  checkboxRow: {
    display: 'inline-block',
    padding: '0.2rem 0',
    marginRight: '1rem'
  },
  checkboxStyle: {
    marginRight: '.31667rem',
    zoom: '1.5'
  },
  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: colors.primary
  },
  fieldHeader: {
    fontWeight: 600,
    lineHeight: '24px',
    color: colors.primary,
    fontSize: 14,
    marginBottom: '0.2rem'
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
    fontSize: '14px',
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
  selectInput: {
    borderColor: 'rgb(151, 151, 151)',
    borderRadius: '6px',
    height: '1.5rem',
  },
  researchPurposeDescription: {
    marginLeft: '-0.9rem',
    fontSize: 14,
    backgroundColor: colorWithWhiteness(colors.accent, 0.85)
  },
  link: {
    color: colors.accent,
    cursor: 'pointer',
    textDecoration: 'none'
  },
  cdrVersionUpgrade: {
    padding: '16px',
    boxSizing: 'border-box',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderRadius: '5px',
    color: colors.primary,
    fontFamily: 'Montserrat',
    letterSpacing: 0,
    lineHeight: '22px',
    borderColor: colors.accent,
    backgroundColor: colorWithWhiteness(colors.accent, 0.85),
    maxWidth: 'fit-content',
  },
  accessTierSpacing: {
    width: '11em',
  },
  cdrVersionSpacing: {
    width: '30em',
  }
});

const CREATE_BILLING_ACCOUNT_OPTION_VALUE = 'CREATE_BILLING_ACCOUNT_OPTION';

// default to creating workspaces in the Registered Tier
const DEFAULT_ACCESS_TIER = AccessTierShortNames.Registered;

// Poll parameters to check Workspace ACLs after creation of a new workspace. See
// SATURN-104 for details, eventually the root cause should be resolved by fixes
// to Sam (as part of Postgres migration).
const NEW_ACL_DELAY_POLL_TIMEOUT_MS = 60 * 1000;
const NEW_ACL_DELAY_POLL_INTERVAL_MS = 10 * 1000;

export enum WorkspaceEditMode { Create = 1, Edit = 2, Duplicate = 3 }

function getDiseaseNames(keyword) {
  const baseurl = serverConfigStore.get().config.firecloudURL;
  const url = baseurl + '/duos/autocomplete/' + keyword;
  return fetch(encodeURI(url)).then((response) => {
    return response.json();
  }).then((matches) => {
    const labeledMatches = fp.filter((elt) => elt.hasOwnProperty('label'))(matches);
    const diseases = fp.map((elt) => elt['label'])(labeledMatches);
    return diseases;
  });
}

interface UpgradeProps {
  srcWorkspace: Workspace;
  destWorkspace: Workspace;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
}
const CdrVersionUpgrade = (props: UpgradeProps) => {
  const {srcWorkspace, destWorkspace, cdrVersionTiersResponse} = props;
  const fromCdrVersion = <span style={{fontWeight: 'bold'}}>{getCdrVersion(srcWorkspace, cdrVersionTiersResponse).name}</span>;
  const toCdrVersion = <span style={{fontWeight: 'bold'}}>{getCdrVersion(destWorkspace, cdrVersionTiersResponse).name}</span>;

  return <div data-test-id='cdr-version-upgrade' style={styles.cdrVersionUpgrade}>
    <div>{`You're duplicating the workspace "${srcWorkspace.name}" to upgrade from`} {fromCdrVersion} to {toCdrVersion}.</div>
    <div>Your original workspace will be unaffected. To work with the new data, simply use the new workspace.</div>
  </div>;
};

export interface WorkspaceEditProps extends WithSpinnerOverlayProps {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  workspace: WorkspaceData;
  cancel: Function;
  profileState: {
    profile: Profile;
  };
  workspaceEditMode: WorkspaceEditMode;
}

export interface WorkspaceEditState {
  billingAccounts: Array<BillingAccount>;
  cdrVersions: Array<CdrVersion>;
  cloneUserRole: boolean;
  loading: boolean;
  populationChecked: boolean;
  selectResearchPurpose: boolean;
  showCdrVersionModal: boolean;
  showConfirmationModal: boolean;
  showCreateBillingAccountModal: boolean;
  showResearchPurpose: boolean;
  showStigmatizationDetails: boolean;
  showUnderservedPopulationDetails: boolean;
  workspace: Workspace;
  workspaceCreationConflictError: boolean;
  workspaceCreationError: boolean;
  workspaceCreationErrorMessage: string;
  workspaceNewAclDelayed: boolean;
  workspaceNewAclDelayedContinueFn: Function;
}

export const WorkspaceEdit = fp.flow(withCurrentWorkspace(), withCdrVersions(), withUserProfile())(
  class WorkspaceEditCmp extends React.Component<WorkspaceEditProps, WorkspaceEditState> {
    constructor(props: WorkspaceEditProps) {
      super(props);
      this.state = {
        billingAccounts: [],
        cdrVersions: props.workspace ? this.getCdrVersions(props.workspace.accessTierShortName) : this.getCdrVersions(DEFAULT_ACCESS_TIER),
        cloneUserRole: false,
        loading: false,
        populationChecked: props.workspace ? props.workspace.researchPurpose.populationDetails.length > 0 : undefined,
        selectResearchPurpose: this.updateSelectedResearch(),
        showCdrVersionModal: false,
        showConfirmationModal: false,
        showCreateBillingAccountModal: false,
        showResearchPurpose: this.updateSelectedResearch(),
        showStigmatizationDetails: false,
        showUnderservedPopulationDetails: false,
        workspace: this.initializeWorkspaceState(),
        workspaceCreationConflictError: false,
        workspaceCreationError: false,
        workspaceCreationErrorMessage: '',
        workspaceNewAclDelayed: false,
        workspaceNewAclDelayedContinueFn: () => {},
      };
    }

    cancel(): void {
      history.back();
    }

    async fetchBillingAccounts() {
      const billingAccounts = (await userApi().listBillingAccounts()).billingAccounts;

      if (this.isMode(WorkspaceEditMode.Create) || this.isMode(WorkspaceEditMode.Duplicate)) {
        const maybeFreeTierAccount = billingAccounts.find(billingAccount => billingAccount.isFreeTier);
        if (maybeFreeTierAccount) {
          this.setState(prevState => fp.set(
            ['workspace', 'billingAccountName'],
            maybeFreeTierAccount.name,
            prevState));
        }
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
      this.props.hideSpinner();
      await this.fetchBillingAccounts();
    }

    createWorkspace(): Workspace {
      return {
        name: '',
        accessTierShortName: DEFAULT_ACCESS_TIER,
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
          reviewRequested: undefined,
          socialBehavioral: false,
          reasonForAllOfUs: '',
        }
      };
    }

    /**
     * Creates the initial workspace state object. For a CREATE mode dialog,
     * this is effectively an empty Workspace object. For EDIT or DUPLICATE
     * mode, this will be based on props.workspace.
     *
     * This is where logic lives to auto-set the CDR version and
     * "reviewRequested" flag, which depend on the workspace state & edit mode.
     */
    initializeWorkspaceState(): Workspace {
      // create a new Workspace from scratch or copy the props into a new object so our modifications here don't affect them
      const workspace: Workspace = this.isMode(WorkspaceEditMode.Create) ? this.createWorkspace() : {...this.props.workspace};

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
        // unselect to prevent unneeded re-review
        workspace.researchPurpose.reviewRequested = undefined;
      }

      // We preselect the default CDR version when a new workspace is being
      // created (via create or duplicate)
      if (this.isMode(WorkspaceEditMode.Create) || this.isMode(WorkspaceEditMode.Duplicate)) {
        const cdrVersion = getDefaultCdrVersionForTier(workspace.accessTierShortName, this.props.cdrVersionTiersResponse);
        workspace.cdrVersionId = cdrVersion.cdrVersionId;
      }

      return workspace;
    }

    getCdrVersions(accessTierShortName: string): Array<CdrVersion> {
      if (this.isMode(WorkspaceEditMode.Edit)) {
        // In edit mode, you cannot modify the CDR version, therefore it's fine
        // to show archived CDRs in the drop-down so that it accurately displays
        // the current value.
        return this.getAllCdrVersionsForTier(accessTierShortName);
      } else {
        return this.getLiveCdrVersionsForTier(accessTierShortName);
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

    getLiveCdrVersionsForTier(accessTierShortName: string): Array<CdrVersion> {
      const versionsForTier = this.getAllCdrVersionsForTier(accessTierShortName);
      const liveCdrVersions = versionsForTier.filter(cdr => cdr.archivalStatus === ArchivalStatus.LIVE);
      if (liveCdrVersions.length === 0) {
        throw Error('no live CDR versions were found');
      }

      return liveCdrVersions;
    }

    getAllCdrVersionsForTier(accessTierShortName: string): Array<CdrVersion> {
      return getCdrVersionTier(accessTierShortName, this.props.cdrVersionTiersResponse).versions;
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

    openContactWidget() {
      const {profileState: {profile: {contactEmail, familyName, givenName, username}}} = this.props;
      openZendeskWidget(givenName, familyName, username, contactEmail);
    }

    renderBillingDescription() {
      return <div>
        The <AouTitle/> provides $300 in free credits per user. Please refer to
        <StyledAnchorTag href={supportUrls.billing} target='_blank'> &nbsp;this article
        </StyledAnchorTag> to learn more about the free credit
        program and how it can be used. Once you have used up your free credits, you can request
        additional credits by <span style={styles.link} onClick={() => this.openContactWidget()}>
        contacting support</span>.
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
          <label style={{...styles.shortDescription, fontSize: 14}} htmlFor={rp.uniqueId}>
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
                             placeholder={'Specify the name of the forum (journal, scientific conference, blog etc.)' +
                             ' through which you will disseminate your findings, if available.'}
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
      return <div key={item.label}>
        <div style={{fontWeight: 'bold', marginBottom: '0.3rem'}}>{item.label} *</div>
        {item.subCategory.map((sub, index) =>
            <FlexRow key={sub.label} style={{...styles.categoryRow, paddingTop: '0rem'}}>
              <CheckBox
                  manageOwnState={false}
                  wrapperStyle={styles.checkboxRow}
                  data-test-id={sub.shortName + '-checkbox'}
                  style={{...styles.checkboxStyle, marginTop: '0.1rem'}}
                  key={sub.label}
                  checked={this.specificPopulationCheckboxSelected(sub.shortName)}
                  onChange={v => this.updateSpecificPopulation(sub.shortName, v)}
                  disabled={!this.state.populationChecked}/>
              <FlexColumn>
                <label style={styles.text}>
                  {sub.label}
                </label>
              </FlexColumn>
            </FlexRow>)}
      </div>;
    }

    makeOutcomingResearchForm(item, index): React.ReactNode {
      return <div key={index} style={{...styles.categoryRow, paddingTop: '0rem'}}>
        <CheckBox
            style={styles.checkboxStyle}
            key={item.label}
            checked={this.researchOutcomeCheckboxSelected(item.shortName)}
            onChange={v => this.updateAttribute('researchOutcomeList', item.shortName, v)}
        />
        <FlexColumn style={{marginTop: '-0.2rem'}}>
          <label style={styles.text}>
            {item.label}
          </label>
        </FlexColumn>
      </div>;
    }

    renderHeader() {
      // use workspace name from props instead of state here
      // because it's a record of the initial value
      const {workspace, workspaceEditMode} = this.props;
      switch (workspaceEditMode) {
        case WorkspaceEditMode.Create:
          return 'Create a new workspace';
        case WorkspaceEditMode.Edit:
          return 'Edit workspace \"' + workspace.name + '\"';
        case WorkspaceEditMode.Duplicate:
          return 'Duplicate workspace \"' + workspace.name + '\"';
      }
    }

    renderButtonText() {
      switch (this.props.workspaceEditMode) {
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

    updatePrimaryPurpose(category, value) {
      this.updateResearchPurpose(category, value);
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
          workspace.researchPurpose.needsReviewPrompt = false;
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
            ${this.props.workspaceEditMode === WorkspaceEditMode.Create ?
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
      return this.props.workspaceEditMode === mode;
    }

    buildBillingAccountOptions() {
      const {enableBillingUpgrade} = serverConfigStore.get().config;
      const options = this.state.billingAccounts.map(a => ({
        label: a.displayName,
        value: a.name,
        disabled: !a.isOpen
      }));
      if (enableBillingUpgrade) {
        options.push({
          label: 'Create a new billing account',
          value: CREATE_BILLING_ACCOUNT_OPTION_VALUE,
          disabled: false
        });
      }

      return options;
    }

    // are we currently performing a CDR Version Upgrade?
    // i.e. a Duplication from a workspace with an older CDR Version to the default version
    isCdrVersionUpgrade() {
      const {workspace: srcWorkspace} = this.props;
      const {workspace: destWorkspace} = this.state;
      return this.isMode(WorkspaceEditMode.Duplicate) &&
          srcWorkspace.cdrVersionId !== destWorkspace.cdrVersionId &&
          hasDefaultCdrVersion(destWorkspace, this.props.cdrVersionTiersResponse);
    }

    /**
     * Validates the current workspace state. This is a pass-through to validate.js
     * which returns the standard error object if any validation errors occur.
     */
    private validate(): any {
      const {
        populationChecked,
        workspace: {
          name,
          billingAccountName,
          researchPurpose: {
            anticipatedFindings,
            diseaseFocusedResearch,
            diseaseOfFocus,
            disseminateResearchFindingList,
            intendedStudy,
            otherDisseminateResearchFindings,
            otherPopulationDetails,
            otherPurpose,
            otherPurposeDetails,
            populationDetails,
            scientificApproach,
            researchOutcomeList,
            reviewRequested
          }
        }
      } = this.state;
      let values: object = {
        name,
        billingAccountName,
        anticipatedFindings,
        intendedStudy,
        populationChecked,
        reviewRequested,
        scientificApproach,
        researchOutcomeList,
        disseminateResearchFindingList,
        'primaryPurpose': this.primaryPurposeIsSelected
      };

      const lengthMessages = (prefix = '') => ({
        tooShort: `${prefix} cannot be blank`,
        tooLong: `${prefix} cannot exceed %{count} characters`
      });

      // TODO: This validation spec should include error messages which get
      // surfaced directly. Currently these constraints are entirely separate
      // from the user facing error strings we render.
      const constraints: object = {
        name: {
          length: { minimum: 1, maximum: 80, ...lengthMessages('Name')}
        },
        // The prefix for these lengthMessages require HTML formatting
        // The prefix string is omitted here and included in the React template below
        billingAccountName: { presence: true },
        intendedStudy: { length: { minimum: 1, maximum: 1000, ...lengthMessages() } },
        populationChecked: { presence: true },
        anticipatedFindings: {length: { minimum: 1, maximum: 1000, ...lengthMessages() }},
        reviewRequested: { presence: true },
        scientificApproach: { length: { minimum: 1, maximum: 1000, ...lengthMessages() } },
        researchOutcomeList: { presence: {allowEmpty: false} },
        disseminateResearchFindingList: { presence: {allowEmpty: false} },
        primaryPurpose: { truthiness: true }
      };

      // Conditionally include optional fields for validation.
      if (otherPurpose) {
        const {tooShort, tooLong} = lengthMessages('Other primary purpose');
        values = {...values, otherPurposeDetails};
        constraints['otherPurposeDetails'] = {
          presence: {
            allowEmpty: false,
            message: tooShort
          },
          length: {
            maximum: 500,
            tooLong
          }
        };
      }
      if (populationChecked) {
        values = {...values, populationDetails};
        constraints['populationDetails'] = {
          presence: true
        };
      }
      if (populationDetails &&
          populationDetails.includes(SpecificPopulationEnum.OTHER)) {
        const {tooShort, tooLong} = lengthMessages('Other Specific Population');
        values = {...values, otherPopulationDetails};
        constraints['otherPopulationDetails'] = {
          presence: {
            allowEmpty: false,
            message: tooShort
          },
          length: {
            maximum: 100,
            tooLong
          }
        };
      }
      if (diseaseFocusedResearch) {
        const {tooShort, tooLong} = lengthMessages('Disease of Focus');
        values = {...values, diseaseOfFocus};
        constraints['diseaseOfFocus'] = {
          presence: {
            allowEmpty: false,
            message: tooShort
          },
          length: {
            maximum: 80,
            tooLong
          }
        };
      }
      if (disseminateResearchFindingList &&
          disseminateResearchFindingList.includes(DisseminateResearchEnum.OTHER)) {
        const {tooShort, tooLong} = lengthMessages('Other methods of disseminating research findings');
        values = {...values, otherDisseminateResearchFindings};
        constraints['otherDisseminateResearchFindings'] = {
          presence: {
            allowEmpty: false,
            message: tooShort
          },
          length: {
            maximum: 100,
            tooLong
          }
        };
      }
      return validate(values, constraints, {fullMessages: false});
    }

    // show the Access Tiers selection dropdown only when there are multiple tiers to choose from
    enableAccessTierSelection(): boolean {
      const {tiers} = this.props.cdrVersionTiersResponse || {tiers: []};
      return tiers.length > 1;
    }

    render() {
      const {enableBillingUpgrade} = serverConfigStore.get().config;
      const {
        workspace: {
          name,
          billingAccountName,
          cdrVersionId,
          accessTierShortName,
          researchPurpose: {
            anticipatedFindings,
            intendedStudy,
            scientificApproach,
            otherPopulationDetails,
            populationDetails,
            reviewRequested
          }
        },
        cdrVersions,
        loading,
        populationChecked,
        showCdrVersionModal,
        showConfirmationModal,
        showCreateBillingAccountModal,
        showResearchPurpose,
        workspaceCreationConflictError,
        workspaceCreationError,
        workspaceCreationErrorMessage,
        workspaceNewAclDelayed
      } = this.state;
      const {cdrVersionTiersResponse, profileState: {profile: {freeTierDollarQuota, freeTierUsage}}} = this.props;
      const freeTierCreditsBalance = freeTierDollarQuota - freeTierUsage;
      // defined below in the OverlayPanel declaration
      let freeTierBalancePanel: OverlayPanel;

      const errors = this.validate();
      return <FadeBox  style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <div style={{width: '1120px'}}>
          {loading && <SpinnerOverlay overrideStylesOverlay={styles.spinner}/>}
          {!hasDefaultCdrVersion(this.state.workspace, cdrVersionTiersResponse) && showCdrVersionModal &&
            <OldCdrVersionModal
                onCancel={() => {
                  this.setState(fp.set(['workspace', 'cdrVersionId'],
                    getDefaultCdrVersionForTier(this.state.workspace.accessTierShortName, cdrVersionTiersResponse).cdrVersionId));
                  this.setState({showCdrVersionModal: false});
                }}
                onContinue={() => this.setState({showCdrVersionModal: false})}
            />}
          {this.isCdrVersionUpgrade() && <CdrVersionUpgrade
              srcWorkspace={this.props.workspace}
              destWorkspace={this.state.workspace}
              cdrVersionTiersResponse={cdrVersionTiersResponse}
          />}
          <WorkspaceEditSection header={this.renderHeader()} tooltip={toolTipText.header}
                                style={{marginTop: '24px'}} largeHeader
                                required={!this.isMode(WorkspaceEditMode.Duplicate)}>
          <FlexRow style={{alignItems: 'baseline'}}>
            <FlexColumn>
              <div style={styles.fieldHeader}>
                Workspace name
              </div>
              <TextInput data-test-id='workspace-name'
                         type='text'
                         style={styles.textInput}
                         autoFocus
                         placeholder='Workspace Name'
                         value = {name}
                         onChange={v => this.setState(fp.set(['workspace', 'name'], v))}/>
            </FlexColumn>
            {this.enableAccessTierSelection() &&
              <FlexColumn>
                <div style={styles.fieldHeader}>
                  Data access tier
                  <TooltipTrigger content={toolTipText.tierSelect}>
                    <InfoIcon style={styles.infoIcon}/>
                  </TooltipTrigger>
                </div>
                <TooltipTrigger
                  content='To use a different access tier, create a new workspace.'
                  disabled={this.isMode(WorkspaceEditMode.Create)}>
                  <div data-test-id='select-access-tier' style={{...styles.select, ...styles.accessTierSpacing}}>
                    <select style={{...styles.selectInput, ...styles.accessTierSpacing}}
                            value={accessTierShortName}
                            onChange={(v: React.FormEvent<HTMLSelectElement>) => {
                              const selectedTier = v.currentTarget.value;
                              this.setState(fp.flow(
                                fp.set(['workspace', 'accessTierShortName'], selectedTier),
                                fp.set(['cdrVersions'], this.getCdrVersions(selectedTier)),
                                fp.set(['workspace', 'cdrVersionId'],
                                  getDefaultCdrVersionForTier(selectedTier, cdrVersionTiersResponse).cdrVersionId)));
                            }}
                            disabled={!this.isMode(WorkspaceEditMode.Create)}>
                      {cdrVersionTiersResponse.tiers.map((tier, i) => (
                          <option key={tier.accessTierShortName} value={tier.accessTierShortName}>
                            {tier.accessTierDisplayName}
                          </option>
                      ))}
                    </select>
                  </div>
                </TooltipTrigger>
              </FlexColumn>}
            <FlexColumn>
              <div style={styles.fieldHeader}>
                Dataset version
                <TooltipTrigger content={toolTipText.cdrSelect}>
                  <InfoIcon style={styles.infoIcon}/>
                </TooltipTrigger>
              </div>
              <TooltipTrigger
                content='To use a different dataset version, duplicate or create a new workspace.'
                disabled={!(this.isMode(WorkspaceEditMode.Edit))}>
              <div data-test-id='select-cdr-version' style={{...styles.select, ...styles.cdrVersionSpacing}}>
                <select style={{...styles.selectInput, ...styles.cdrVersionSpacing}}
                  value={cdrVersionId}
                  onChange={(v: React.FormEvent<HTMLSelectElement>) => {
                    const selectedVersion = v.currentTarget.value;
                    this.setState(fp.set(['workspace', 'cdrVersionId'], selectedVersion));
                    this.setState({showCdrVersionModal: selectedVersion !==
                          getDefaultCdrVersionForTier(this.state.workspace.accessTierShortName, cdrVersionTiersResponse).cdrVersionId});
                  }}
                  disabled={this.isMode(WorkspaceEditMode.Edit)}>
                    {cdrVersions.map((version, i) => (
                      <option key={version.cdrVersionId} value={version.cdrVersionId}>
                        {version.name}
                      </option>
                    ))}
                </select>
              </div>
            </TooltipTrigger>
            </FlexColumn>
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
        {(!this.isMode(WorkspaceEditMode.Edit) || this.props.workspace.accessLevel === WorkspaceAccessLevel.OWNER) &&
          <WorkspaceEditSection header={<div><AoU/> billing account</div>}
                                description={this.renderBillingDescription()} descriptionStyle={{marginLeft: '0rem'}}>
            <div style={styles.fieldHeader}>
              Select account
            </div>
            <OverlayPanel ref={(me) => freeTierBalancePanel = me} dismissable={true} appendTo={document.body}>
              <div style={styles.freeCreditsBalanceOverlay}>
                FREE CREDIT BALANCE {formatFreeCreditsUSD(freeTierCreditsBalance)}
              </div>
            </OverlayPanel>
            <FlexRow>
              <Dropdown style={{width: '14rem'}}
                        value={billingAccountName}
                        options={this.buildBillingAccountOptions()}
                        disabled={(freeTierCreditsBalance < 0.0) && !enableBillingUpgrade}
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
              <div style={styles.freeCreditsBalanceClickable}>
                <Clickable onClick={(e) => freeTierBalancePanel.toggle(e)}>View free credits balance</Clickable>
              </div>
            </FlexRow>
          </WorkspaceEditSection>}
        <hr style={{marginTop: '1rem'}}/>
        <WorkspaceEditSection header={<FlexRow style={{alignItems: 'center'}}>
          <div>Research Use Statement Questions</div>
          <StyledAnchorTag href={supportUrls.researchPurpose}
                           target='_blank' style={{marginLeft: '1rem', fontSize: 14, lineHeight: '18px', fontWeight: 400}}>
            Best practices for Research Use Statement questions
          </StyledAnchorTag>
        </FlexRow>} largeHeader={true}
              description={<div style={styles.researchPurposeDescription}>
                <div style={{margin: '0.5rem', paddingTop: '0.5rem'}}>{ResearchPurposeDescription}
              <br/><br/>
              </div></div>
            }/>

        {/*Primary purpose */}
          <WorkspaceEditSection header={researchPurposeQuestions[0].header} publiclyDisplayed={true}
                                description={researchPurposeQuestions[0].description} index='1.'
                                indent={true}>
          <FlexRow>
            <FlexColumn>
              <FlexColumn  style={styles.researchPurposeRow}>
                <FlexRow>
                <CheckBox
                  data-test-id='researchPurpose-checkbox'
                  manageOwnState={false}
                  style={{...styles.checkboxStyle, margin: '0.1rem 0.25rem 0 0.6rem'}}
                  checked={this.researchPurposeCheck}
                  onChange={v => this.onResearchPurposeChange(v)}/>
                  <div style={{...styles.shortDescription}}>
                    <button style={{...styles.shortDescription, border: 'none'}}
                            data-test-id='research-purpose-button'
                            onClick={() => this.setState({showResearchPurpose: !showResearchPurpose})}>
                      <label style={{fontSize: 14}}>Research purpose</label>
                      <i className={this.iconClass} style={{verticalAlign: 'middle'}}></i>
                     </button>
                  </div>
                </FlexRow>
                  {showResearchPurpose && <FlexColumn data-test-id='research-purpose-categories'>
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
          header={researchPurposeQuestions[1].header} indent={true} publiclyDisplayed={true}
          description={researchPurposeQuestions[1].description} style={{width: '48rem'}} index='2.'>
          <FlexColumn>
            {/* TextBox: scientific question(s) researcher intend to study Section*/}
            <WorkspaceResearchSummary
                researchPurpose={researchPurposeQuestions[2]}
                researchValue={intendedStudy}
                onChange={v => this.updateResearchPurpose('intendedStudy', v)}
                index='2.1'
                id='intendedStudyText'
            />

            {/* TextBox: scientific approaches section*/}
            <WorkspaceResearchSummary
                researchPurpose={researchPurposeQuestions[3]}
                researchValue={scientificApproach}
                onChange={v => this.updateResearchPurpose('scientificApproach', v)}
                index='2.2'
                id='scientificApproachText'
            />
            {/*TextBox: anticipated findings from the study section*/}
            <WorkspaceResearchSummary
                researchPurpose={researchPurposeQuestions[4]}
                researchValue={anticipatedFindings}
                onChange={v => this.updateResearchPurpose('anticipatedFindings', v)}
                index='2.3'
                id='anticipatedFindingsText'
            />
          </FlexColumn>
        </WorkspaceEditSection>

          {/*disseminate  research Section */}
        <WorkspaceEditSection header={researchPurposeQuestions[5].header}
                              description={researchPurposeQuestions[5].description} style={{width: '48rem'}} index='3.'>
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
                                style={{width: '48rem'}}>
            <FlexRow style={{marginLeft: '1rem'}}>
              <FlexColumn style={{flex: '1 1 0'}}>
                {researchOutcomes.map(
                  (rp, i) => this.makeOutcomingResearchForm(rp, i))}
              </FlexColumn>
            </FlexRow>
          </WorkspaceEditSection>

          {/*Underrespresented population section*/}
        <WorkspaceEditSection header={researchPurposeQuestions[7].header} index='5.' indent={true}
                              description={researchPurposeQuestions[7].description}
                              style={{width: '48rem'}} publiclyDisplayed={true}>
          <div style={styles.header}>Will your study focus on any historically underrepresented populations?</div>
          <div>
            <RadioButton name='population' style={{marginRight: '0.5rem'}}
                         data-test-id='specific-population-yes'
                         onChange={v => this.setState({populationChecked: true})}
                         checked={populationChecked}/>
            <label style={styles.text}>Yes, my study will focus on one or more specific
              underrepresented populations, either on their own or in comparison to other groups.</label>
          </div>
          <div style={{...styles.text, marginLeft: '1.2rem'}}>
            <strong>If "Yes,"</strong>&nbsp;please indicate your underrepresented population(s) of
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
                    labelStyle={{...styles.text, fontWeight: 'bold'}}
                    checked={!!this.specificPopulationCheckboxSelected(SpecificPopulationEnum.OTHER)}
                    onChange={v => this.updateSpecificPopulation(SpecificPopulationEnum.OTHER, v)}
                    disabled={!populationChecked}
                />
                <TextInput type='text' autoFocus placeholder='Please specify'
                           value={otherPopulationDetails}
                           disabled={!fp.includes(SpecificPopulationEnum.OTHER, populationDetails)}
                           data-test-id='other-specialPopulation-text'
                           onChange={v => this.setState(fp.set(
                             ['workspace', 'researchPurpose', 'otherPopulationDetails'], v))}/>
              </FlexColumn>
            </FlexRow>
            <hr/>
            <FlexRow>
              <div style={{marginRight: '0.2rem'}}>*</div>
              <div>Demographic variables for which data elements have been altered, partially
                suppressed, or generalized in the Registered Tier to protect data privacy. Refer to
                the Data Dictionary for details.</div>
            </FlexRow>

            <hr/>
          </div>
          <FlexRow style={{marginTop: '0.5rem'}}>
            <RadioButton name='population'
                         style={{marginRight: '0.5rem', marginTop: '0.3rem'}}
                         data-test-id='specific-population-no'
                         onChange={v => this.setState({populationChecked: false})}
                         checked={populationChecked === false}/>
            <label style={styles.text}>No, my study will not center on underrepresented populations.
              I am interested in a diverse sample in general, or I am focused on populations that
              have been well represented in prior research.</label>
          </FlexRow>
        </WorkspaceEditSection>

          {/* Request for review section*/}
        <WorkspaceEditSection header={researchPurposeQuestions[8].header} index='6.' indent={true}>
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
            request a review of your research purpose statement by the <AoU/> Resource Access
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
                             data-test-id='review-request-btn-true'
                             onChange={() => {
                               this.updateResearchPurpose('reviewRequested', true);
                             }}
                             checked={reviewRequested}/>
                <label style={{...styles.text, marginLeft: '0.5rem'}}>Yes, I would like to request
                  a review of my research purpose.</label>
                </FlexRow>
                <FlexRow>
                <RadioButton style={{marginTop: '0.2rem'}} name='reviewRequested'
                             data-test-id='review-request-btn-false'
                             onChange={() => {
                               this.updateResearchPurpose('reviewRequested', false);
                             }}
                             checked={reviewRequested === false}/>
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
                    onClick = {() => this.cancel()}>
              Cancel
            </Button>
            <TooltipTrigger content={
              errors && <BulletAlignedUnorderedList>
                {errors.name && <li>{errors.name}</li>}
                {errors.billingAccountName && <li>
                  You must select a billing account</li>}
                {errors.primaryPurpose && <li> You must choose at least one primary research
                  purpose (Question 1)</li>}
                {errors.diseaseOfFocus && <li>{errors.diseaseOfFocus}</li>}
                {errors.otherPurposeDetails && <li>{errors.otherPurposeDetails}</li>}
                {errors.intendedStudy && <li> Answer for<i>What are the specific scientific question(s) you intend to study
                  (Question 2.1)</i> {errors.intendedStudy}</li>}
                {errors.scientificApproach && <li> Answer for <i>What are the scientific
                  approaches you plan to use for your study (Question 2.2)</i> {errors.scientificApproach}</li>}
                {errors.anticipatedFindings && <li> Answer for <i>What are the anticipated findings
                  from the study? (Question 2.3)</i> {errors.anticipatedFindings}</li>}
                {errors.disseminateResearchFindingList && <li>
                  You must specific how you plan to disseminate your research findings (Question 3)</li>}
                {errors.otherDisseminateResearchFindings && <li>{errors.otherDisseminateResearchFindings}</li>}
                {errors.researchOutcomeList && <li> You must specify the outcome of the research (Question 4)</li>}
                {errors.populationChecked && <li>You must pick an answer Population of interest question (Question 5)</li>}
                {errors.populationDetails && <li> You must specify a population of study (Question 5)</li>}
                {errors.otherPopulationDetails && <li>{errors.otherPopulationDetails}</li>}
                {errors.reviewRequested && <li>You must pick an answer for review of stigmatizing research (Question 6)</li>}
              </BulletAlignedUnorderedList>
            } disabled={!errors}>
              <Button type='primary'
                      onClick={() => this.setState({showConfirmationModal: true})}
                      disabled={errors || loading || showCdrVersionModal}
                      data-test-id='workspace-save-btn'>
                {this.renderButtonText()}
              </Button>
            </TooltipTrigger>
          </FlexRow>
        </div>
        {workspaceCreationError &&
        <Modal>
          <ModalTitle>Error:</ModalTitle>
          <ModalBody>
            { workspaceCreationErrorMessage }
          </ModalBody>
          <ModalFooter>
            <Button onClick = {() => this.cancel()}
                type='secondary' style={{marginRight: '2rem'}}>
              Cancel
              {this.props.workspaceEditMode === WorkspaceEditMode.Create ?
                ' Creation' : ' Update'}
                </Button>
            <Button type='primary' onClick={() => this.resetWorkspaceEditor()}>Keep Editing</Button>
          </ModalFooter>
        </Modal>
        }
        {showCreateBillingAccountModal &&
          <CreateBillingAccountModal onClose={() => this.setState({showCreateBillingAccountModal: false})} />}
        {workspaceCreationConflictError &&
        <Modal>
          <ModalTitle>{this.props.workspaceEditMode === WorkspaceEditMode.Create ?
              'Error: ' : 'Conflicting update:'}</ModalTitle>
          <ModalBody>
            {this.props.workspaceEditMode === WorkspaceEditMode.Create ?
              'You already have a workspace named ' + name +
              ' Please choose another name' :
              'Another client has modified this workspace since the beginning of this editing ' +
              'session. Please reload to avoid overwriting those changes.'}
          </ModalBody>
          <ModalFooter>
            <Button type='secondary' onClick = {() => this.cancel()}
                    style={{marginRight: '2rem'}}>Cancel Creation</Button>
            <Button type='primary' onClick={() => this.resetWorkspaceEditor()}>Keep Editing</Button>
          </ModalFooter>
        </Modal>
        }
        {workspaceNewAclDelayed &&
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
        {showConfirmationModal &&
          <Modal width={500}>
            <ModalTitle style={{fontSize: '16px', marginBottom: 0}}>
              {this.renderButtonText()}
            </ModalTitle>
            <ModalBody style={{color: colors.primary, lineHeight: '1rem', marginTop: '0.25rem'}}>
              {loading && <SpinnerOverlay overrideStylesOverlay={styles.spinner}/>}
              <div>Your responses to these questions:</div>
              <div style={{margin: '0.25rem 0 0.25rem 1rem'}}>
                <span style={{fontWeight: 600}}>Primary purpose of your project</span> (Question 1)<br/>
                <span style={{fontWeight: 600}}>Summary of research purpose</span> (Question 2)<br/>
                <span style={{fontWeight: 600}}>Population of interest</span> (Question 5)<br/>
              </div>
              <div style={{marginBottom: '1rem'}}>
                Will be
                <a style={{color: colors.accent}}
                  href='https://www.researchallofus.org/research-projects-directory/'
                  target='_blank'> displayed publicly </a>
                 to inform <AoU/> research participants. Therefore, please verify that you have provided sufficiently detailed
                 responses in plain language.
              </div>
              <div>You can also make changes to your answers after you create your workspace.</div>
            </ModalBody>
            <ModalFooter>
              <Button
                  type='secondary'
                  disabled={errors || loading}
                  style={{marginRight: '1rem'}}
                  onClick={() => this.setState({showConfirmationModal: false})}>
                Keep Editing
              </Button>
              <Button
                  type='primary'
                  disabled={errors || loading || showCdrVersionModal}
                  onClick={() => this.onSaveClick()}
                  data-test-id='workspace-confirm-save-btn'>
                Confirm
              </Button>
            </ModalFooter>
          </Modal>
        }
        </div>
      </FadeBox> ;
    }
  });
