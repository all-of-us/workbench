import {Location} from '@angular/common';
import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {InfoIcon} from 'app/components/icons';
import {CheckBox, TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {cdrVersionsApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {reactStyles} from 'app/utils';
import {ReactWrapperBase, withCurrentWorkspace, withRouteConfigData} from 'app/utils';
import {navigate, userProfileStore} from 'app/utils/navigation';
import {WorkspaceUnderservedPopulation} from 'app/views/workspace-edit-underserved-population/component';
import {CdrVersion, DataAccessLevel, Workspace} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';

export const ResearchPurposeItems = {
  diseaseFocusedResearch: {
    shortDescription: 'Disease-focused research',
    longDescription: 'The primary purpose of the research is to learn more about a particular \
    disease or disorder (for example, type 2 diabetes), a trait (for example, blood pressure), \
    or a set of related conditions (for example, autoimmune diseases, psychiatric disorders).'
  },
  methodsDevelopment: {
    shortDescription: 'Methods development/validation study',
    longDescription: 'The primary purpose of the research is to develop and/or validate new \
    methods/tools for analyzing or interpreting data (for example, developing more powerful \
    methods to detect epistatic, gene-environment, or other types of complex interactions in \
    genome-wide association studies). Data will be used for developing and/or validating new \
    methods.'
  },
  aggregateAnalysis: {
    shortDescription: 'Aggregate analysis to understand variation in general population',
    longDescription: 'The primary purpose of the research is to understand variation in the \
    general population (for example, genetic substructure of a population).'
  },
  controlSet: {
    shortDescription: 'Control set',
    longDescription: 'All of Us data will be used to increase the number of controls \
    available for a comparison group (for example, a case-control study) to another \
    dataset.'
  },
  ancestry: {
    shortDescription: 'Population origins or ancestry',
    longDescription: 'The primary purpose of the research is to study the ancestry \
    or origins of a specific population.'
  },
  population: {
    shortDescription: 'Restricted to a specific population',
    longDescription: 'This research will focus on a specific population group. \
    For example: a specific gender, age group or ethnic group.'
  },
  commercialPurpose: {
    shortDescription: 'Commercial purpose/entity',
    longDescription: 'The study is conducted by a for-profit entity and/or in \
    support of a commercial activity.'
  },
  containsUnderservedPopulation: {
    shortDescription: 'Focus on an underserved population',
    longDescription: 'This research will focus on, or include findings on, distinguishing \
    characteristics related to one or more underserved populations'
  },
  requestReview: {
    shortDescription: 'Request a review of your research purpose'
  }
};

export const toolTipText = {
  header: `A Workspace is your place to store and analyze data for a specific project.Each
    Workspace is a separate Google bucket that serves as a dedicated space for file storage.
    You can share this Workspace with other users, allowing them to view or edit your work. Your
    Workspace is where you will go to build concept sets and cohorts and launch Notebooks for
    performing analyses on your cohorts.`,
  cdrSelect: `The curated data repository (CDR) is where research data from the All of Us Research
    Program is stored. The CDR is periodically updated as new data becomes available for use. You
    can select which version of the CDR you wish to query in this Workspace.`,
  billingAccount: `Throughout this period of testing and development, your use of the Workbench is
    being funded by the National Institutes of Health. In the future researchers may be required to
    enter billing account information to cover the cost of computing time in the cloud.`,
  researchPurpose: `You  are required to describe your research purpose, or the reason why you are
    conducting this study. This information, along with your name, will be posted on the publicly
    available All of Us website (https://www.researchallofus.org/) to inform our participants and
    other stakeholders about what kind of research their data is being used for.`,
  reviewRequest: `If you are concerned that your research may be stigmatizing to a particular group
    of research participants, you may request a review of your research purpose by the All of Us
    Resource Access Board (RAB). The RAB will provide feedback regarding potential for stigmatizing
    specific groups of participants and, if needed, guidance for modifying your research
    purpose/scope. Even if you request a review, you will be able to create a Workspace and proceed
    with your research.`,
  underservedPopulation: `A primary mission of the All of Us Research Program is to include
    populations that are medically underserved and/or historically underrepresented in biomedical
    research or who, because of systematic social disadvantage, experience disparities in health.
    As a way to understand how much research is being conducted on these populations, All of Us
    requests that you mark all options for underserved populations that will be included in your
    research.`
};


const styles = reactStyles({

  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: '#262262'
  },

  requiredText: {
    fontSize: '13px',
    fontStyle: 'italic',
    fontWeight: 400,
    color: '#4A4A4A',
    marginLeft: '0.2rem'
  },

  text: {
    fontSize: '13px',
    color: '#4A4A4A',
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
  textArea: {
    width: '100%',
    marginTop: '0.5rem',
    height: '10em',
    resize: 'none',
    padding: '0.1rem 0.2rem',
    background: '#fff',
    border: '1px solid #ccc',
    color: '#000',
    borderRadius: '.125rem'
  },
  shortDescription: {
    color: '#262262',
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '24px'
  },
  longDescription : {
    position: 'relative',
    display: 'inline-block',
    minHeight: '1rem',
    cursor: 'pointer',
    lineHeight: '1rem',
    width: '95%'
  },
  categoryRow: {
    display: 'flex',
    flexDirection: 'row',
    padding: '0.6rem 0',
    width: '50%'
  }
});


export const WorkspaceEditSection = (props) => {
  return <div key={props.header}>
    <div style={{display: 'flex', flexDirection: 'row', marginTop: (props.largeHeader ? 48 : 24)}}>
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
    </div>
    {props.subHeader && <div style={{...styles.header, color: '#4A4A4A', fontSize: 14}}>
      {props.subHeader}
    </div>
    }
    <div style={styles.text}>
      {props.text}
    </div>
    <div>
      {props.children}
    </div>
  </div>;
};

export const WorkspaceCategory = (props) => {
  return <div style={...fp.merge(styles.categoryRow, props.style)}>
    <CheckBox style={{height: '.66667rem', marginRight: '.31667rem'}} checked={!!props.value}
      onChange={e => props.onChange(e)}/>
    <div style={{display: 'flex', flexDirection: 'column', marginTop: '-0.2rem'}}>
      <label style={styles.shortDescription}>
        {props.item.shortDescription}
      </label>
      <div>
        <label style={styles.longDescription}>
          {props.item.longDescription}
        </label>
        {props.children}
      </div>
    </div>
  </div>;
};

export enum WorkspaceEditMode { Create = 1, Edit = 2, Clone = 3 }


export interface WorkspaceEditProps {
  routeConfigData: any;
  workspace: Workspace;
  cancel: Function;
}

export interface WorkspaceEditState {
  cdrVersionItems: Array<CdrVersion>;
  workspace: Workspace;
  workspaceCreationConflictError: boolean;
  workspaceCreationError: boolean;
  cloneUserRole: boolean;
  loading: boolean;
}

export const WorkspaceEdit = fp.flow(withRouteConfigData(), withCurrentWorkspace())(
  class extends React.Component<WorkspaceEditProps, WorkspaceEditState> {

    constructor(props: WorkspaceEditProps) {
      super(props);
      this.isEmpty.bind(this);
      this.state = {
        cdrVersionItems: [],
        workspace: {
          name: '',
          description: '',
          dataAccessLevel: DataAccessLevel.Registered,
          namespace: userProfileStore.getValue().profile.freeTierBillingProjectName,
          cdrVersionId: '',
          researchPurpose: {
            diseaseFocusedResearch: false,
            methodsDevelopment: false,
            controlSet: false,
            aggregateAnalysis: false,
            ancestry: false,
            commercialPurpose: false,
            population: false,
            reviewRequested: false,
            containsUnderservedPopulation: false,
            underservedPopulationDetails: []
          }
        },
        workspaceCreationConflictError: false,
        workspaceCreationError: false,
        cloneUserRole: false,
        loading: false
      };
    }

    componentDidMount() {
      if (!this.isMode(WorkspaceEditMode.Create)) {
        this.setState({workspace : this.props.workspace});
        if (this.isMode(WorkspaceEditMode.Clone)) {
          this.setState(fp.set(['workspace', 'name'], 'Duplicate of ' + this.props.workspace.name));
        }
      }
      this.setCdrVersions();
    }

    async setCdrVersions() {
      try {
        const cdrVersions = await cdrVersionsApi().getCdrVersions();
        this.setState({cdrVersionItems: cdrVersions.items});
        if (this.isMode(WorkspaceEditMode.Create)) {
          this.setState(fp.set(['workspace', 'cdrVersionId'], cdrVersions.defaultCdrVersionId));
        }
      } catch (exception) {
        console.log(exception);
      }
    }

    renderHeader() {
      switch (this.props.routeConfigData.mode) {
        case WorkspaceEditMode.Create:
          return 'Create a new Workspace';
        case WorkspaceEditMode.Edit:
          return 'Edit workspace \"' + this.props.workspace.name + '\"';
        case WorkspaceEditMode.Clone:
          return 'Clone workspace \"' +
              this.props.workspace.name + '\"';
      }
    }

    renderButtonText() {
      switch (this.props.routeConfigData.mode) {
        case WorkspaceEditMode.Create: return 'Create Workspace';
        case WorkspaceEditMode.Edit: return 'Update Worspace';
        case WorkspaceEditMode.Clone: return 'Duplicate Workspace';
      }
    }

    disableButton() {
      return this.isEmpty('name') || this.isEmpty('description');
    }

    updateWorkspaceCategory(category, value) {
      this.setState(fp.set(['workspace', 'researchPurpose', category], value));
    }


    updateUnderservedPopulation(populationDetails) {
      this.setState(fp.set(['workspace', 'researchPurpose', 'underservedPopulationDetails'],
        populationDetails));
      this.setState(fp.set(['workspace', 'researchPurpose', 'containsUnderservedPopulation'],
        (populationDetails.length > 0)));
    }

    async saveWorkspace() {
      try {
        this.setState({loading: true});
        let workspace = this.state.workspace;
        if (this.isMode(WorkspaceEditMode.Create)) {
          workspace =
              await workspacesApi().createWorkspace(this.state.workspace);
        } else if (this.isMode(WorkspaceEditMode.Clone)) {
          await workspacesApi().cloneWorkspace(
            this.state.workspace.namespace, this.state.workspace.id,
            {
              includeUserRoles: this.state.cloneUserRole,
              workspace: this.state.workspace
            });
        } else {
          await workspacesApi()
              .updateWorkspace(this.state.workspace.namespace, this.state.workspace.id,
                  {workspace: this.state.workspace});
        }
        navigate(['workspaces', workspace.namespace, workspace.id]);
      } catch (error) {
        this.setState({loading: false});
        if (error.status === 409) {
          this.setState({workspaceCreationConflictError: true});
        } else {
          this.setState({workspaceCreationError: true});
        }
      }
    }

    resetWorkspaceEditor() {
      this.setState({
        workspaceCreationError : false,
        workspaceCreationConflictError : false
      });
    }

    isEmpty(field) {
      const fieldValue = this.state.workspace[field];
      return !fieldValue || fieldValue === '';
    }

    isMode(mode) {
      return this.props.routeConfigData.mode === mode;
    }

    render() {
      return <React.Fragment>
        <div style={{width: '60%'}}>
        <WorkspaceEditSection header={this.renderHeader()} tooltip={toolTipText.header}
                              section={{marginTop: '24px'}} largeHeader required>
          <div style={{display: 'flex', flexDirection: 'row'}}>
            <TextInput type='text' style={styles.textInput} autoFocus placeholder='Workspace Name'
                       value = {this.state.workspace.name}
                       onChange={v => this.setState(fp.set(['workspace', 'name'], v))}/>
            <TooltipTrigger
                content='To use a different dataset version, clone or create a new workspace.'
                disabled={!(this.isMode(WorkspaceEditMode.Edit))}>
              <div style={styles.select}>
                <select style={{borderColor: 'rgb(151, 151, 151)', borderRadius: '6px',
                  height: '1.5rem', width: '12rem'}}
                  value={this.state.workspace.cdrVersionId}
                  onChange={v => this.setState(fp.set(['workspace', 'cdrVersionId'], v))}
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
              <InfoIcon style={{...styles.infoIcon, marginTop: '0.5rem'}}/>
            </TooltipTrigger>
          </div>
        </WorkspaceEditSection>
        {this.isMode(WorkspaceEditMode.Clone) &&
        <div style={{display: 'flex', flexDirection: 'row'}}>
          <CheckBox
                 style={{height: '.66667rem', marginRight: '.31667rem', marginTop: '1.2rem'}}
          onChange={v => this.setState({cloneUserRole: v.value})}/>
          <WorkspaceEditSection header='Copy Original workspace Collaborators'
            text='Share cloned workspace with same collaborators'/>
        </div>
        }
        <WorkspaceEditSection header='Billing Account' subHeader='National Institutes of Health'
            tooltip={toolTipText.billingAccount}
            text='To fulfill program requirements, All of Us requests the following information
            for each workspace.'/>
        <WorkspaceEditSection header='Describe your research purpose'
            tooltip={toolTipText.researchPurpose}
            text={['Please include the ', <strong key='question'>research question</strong>,
              '\, the ', <strong key='plan'> plan for use of the data </strong>,
              'to answer the research question, and the',
              <strong key = 'benefit'> expected outcome/benefit </strong>,
              'of the research information will be posted publicly on the All of Us website to ' +
              'inform the research participants.']} required>
          <TextArea value={this.state.workspace.description}
                    onChange={v => this.setState(fp.set(['workspace', 'description'], v))}/>
        </WorkspaceEditSection>
        <WorkspaceEditSection
            header='Please select all data use categories that apply for your current study'
            text='These are for informational purposes only and do not affect or configure your
            new workspace.'>
          <WorkspaceCategory style={{width: '100%'}}
              item={ResearchPurposeItems.diseaseFocusedResearch}
              value={this.state.workspace.researchPurpose.diseaseFocusedResearch}
              onChange={v => this.updateWorkspaceCategory('diseaseFocusedResearch', v)}>
            <TooltipTrigger
                content='You must select disease focused research to enter a disease of focus'
                disabled={this.state.workspace.researchPurpose.diseaseFocusedResearch}>
              <TextInput value={this.state.workspace.researchPurpose.diseaseOfFocus}
                         style={{
                           width: 'calc(50% - 2rem)',
                           border: '1px solid #9a9a9',
                           borderRadius: '5px'
                         }}
                         placeholder='Name of Disease' onChange={v =>
                  this.setState(fp.set(['workspace', 'researchPurpose', 'diseaseOfFocus'], v))}
                         disabled={!this.state.workspace.researchPurpose.diseaseFocusedResearch}/>
            </TooltipTrigger>
          </WorkspaceCategory>
          <div style={{display: 'inline-block'}}>
            <div style={{display: 'flex'}}>
              <WorkspaceCategory item={ResearchPurposeItems.methodsDevelopment}
                  value={this.state.workspace.researchPurpose.methodsDevelopment}
                  onChange={v => this.updateWorkspaceCategory('methodsDevelopment', v)}/>
              <WorkspaceCategory item={ResearchPurposeItems.aggregateAnalysis}
                  value={!!this.state.workspace.researchPurpose.aggregateAnalysis}
                  onChange={v => this.updateWorkspaceCategory('aggregateAnalysis', v)}/>
            </div>
          </div>
          <div style={{display: 'inline-block'}}>
            <div style={{display: 'flex'}}>
              <WorkspaceCategory item={ResearchPurposeItems.controlSet}
                  value={this.state.workspace.researchPurpose.controlSet}
                  onChange={v => this.updateWorkspaceCategory('controlSet', v)}/>
              <WorkspaceCategory item={ResearchPurposeItems.ancestry}
                  value={this.state.workspace.researchPurpose.ancestry}
                  onChange={v => this.updateWorkspaceCategory('ancestry', v)}/>
            </div>
          </div>
          <div style={{display: 'inline-block'}}>
            <div style={{display: 'flex'}}>
              <WorkspaceCategory item={ResearchPurposeItems.population}
                  value={this.state.workspace.researchPurpose.population}
                  onChange={v => this.updateWorkspaceCategory('population', v)}/>
              <WorkspaceCategory item={ResearchPurposeItems.commercialPurpose}
                  value={this.state.workspace.researchPurpose.commercialPurpose}
                  onChange={v => this.updateWorkspaceCategory('commercialPurpose', v)}/>
            </div>
          </div>
          <WorkspaceUnderservedPopulation
            selectedValues={
              !this.state.workspace.researchPurpose.underservedPopulationDetails ? [] :
                  this.state.workspace.researchPurpose.underservedPopulationDetails}
              onChange={v => this.updateUnderservedPopulation(v)}>
          </WorkspaceUnderservedPopulation>
        </WorkspaceEditSection>
        <WorkspaceEditSection header='Request a review of your research purpose'
                              tooltip={toolTipText.reviewRequest}>
          <div style={{display: 'flex', flexDirection: 'row',
            paddingBottom: '14.4px', paddingTop: '0.3rem'}}>
            <CheckBox style={{height: '.66667rem', marginRight: '.31667rem', marginTop: '0.3rem'}}
              onChange={v => this.setState(
                fp.set(['workspace', 'researchPurpose', 'reviewRequested' ], v.value))}
              checked={this.state.workspace.researchPurpose.reviewRequested}/>
            <label style={styles.text}>
              I am concerned about potential
              <a href='/definitions/stigmatization' target='_blank'> stigmatization </a>
            of research participants. I would like the All of Us Resource Access Board (RAB) to
              review my Research Purpose.
              (This will not prevent you from creating a workspace and proceeding.)
            </label>
          </div>
        </WorkspaceEditSection>
        <div>
          <div style={{display: 'flex', flexDirection: 'row', marginTop: '1rem',
            marginBottom: '1rem'}}>
            <Button type='secondary' style={{marginRight: '1rem'}}
                    onClick = {() => this.props.cancel()}>
              Cancel
            </Button>
            <TooltipTrigger content={[<ul>Missing Required Fields:
              { this.isEmpty('name') && <li> Name </li> }
              { this.isEmpty('description') && <li> Description </li> }
            </ul>]} disabled={!this.disableButton()}>
              <Button type='primary' onClick={() => this.saveWorkspace()}
                      disabled={this.disableButton() || this.state.loading}>
                {this.state.loading && <SpinnerOverlay/>}
                {this.renderButtonText()}
              </Button>
            </TooltipTrigger>
          </div>
        </div>
        {this.state.workspaceCreationError &&
        <Modal>
          <ModalTitle>Error:</ModalTitle>
          <ModalBody>Could not
            {this.props.routeConfigData.mode === WorkspaceEditMode.Create ? 'create' : 'update'}
            workspace.
          </ModalBody>
          <ModalFooter>
            <Button onClick = {() => this.props.cancel()}
                type='secondary' style={{marginRight: '2rem'}}>
              Cancel
              {this.props.routeConfigData.mode === WorkspaceEditMode.Create ? 'Creation' : 'Update'}
                </Button>
            <Button type='primary' onClick={() => this.resetWorkspaceEditor()}>Keep Editing</Button>
          </ModalFooter>
        </Modal>
        }
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
        </div>
      </React.Fragment> ;
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
