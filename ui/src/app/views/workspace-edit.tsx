import {Location} from '@angular/common';
import {Component} from '@angular/core';
import {Button, Link} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon, InfoIcon} from 'app/components/icons';
import {CheckBox, RadioButton, TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {cdrVersionsApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withRouteConfigData} from 'app/utils';
import {currentWorkspaceStore, navigate, userProfileStore} from 'app/utils/navigation';
import {CdrVersion, DataAccessLevel, SpecificPopulationEnum, Workspace} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';


export const ResearchPurposeItems = [
  {
    shortName: 'diseaseFocusedResearch',
    shortDescription: 'Disease-focused research',
    longDescription: 'The primary purpose of the research is to learn more about a particular \
    disease or disorder (for example, type 2 diabetes), a trait (for example, blood pressure), \
    or a set of related conditions (for example, autoimmune diseases, psychiatric disorders).'
  }, {
    shortName: 'methodsDevelopment',
    shortDescription: 'Methods development/validation study',
    longDescription: 'The primary purpose of the use of AoU data is to develop and/or  \
    validate specific methods/tools for analyzing or interpreting data (e.g. statistical  \
    methods for describing data trends, developing more powerful methods to detect \
    gene-environment or other types of interactions in genome-wide association studies).'
  }, {
    shortName: 'controlSet',
    shortDescription: 'Research Control',
    longDescription: 'AoU data will be used as a reference or control dataset \
      for comparison with another dataset from a different resource (e.g. Case-control \
      studies).'
  }, {
    shortName: 'ancestry',
    shortDescription: 'Genetic Research',
    longDescription: 'Research concerning genetics (i.e. the study of genes, genetic variations \
      and heredity) in the context of diseases or ancestry.'
  }, {
    shortName: 'socialBehavioral',
    shortDescription: 'Social/Behavioral Research',
    longDescription: 'The research focuses on the social or behavioral phenomena or determinants \
      of health.'
  }, {
    shortName: 'populationHealth',
    shortDescription: 'Population Health/Public Health Research',
    longDescription: 'The primary purpose of using AoU data is to investigate health behaviors, \
      outcomes, access and disparities in populations.'
  }, {
    shortName: 'drugDevelopment',
    shortDescription: 'Drug/Therapeutics Development Research',
    longDescription: 'Primary focus of the research is drug/therapeutics development. The data \
      will be used to understand treatment-gene interactions or treatment outcomes relevant \
      to the therapeutic(s) of interest.'
  },  {
    shortName: 'commercialPurpose',
    shortDescription: 'For-Profit Purpose',
    longDescription: 'The data will be used by a for-profit entity for research or product \
      or service development (e.g. for understanding drug responses as part of a \
      pharmaceutical company\'s drug development or market research efforts).'
  }, {
    shortName: 'educational',
    shortDescription: 'Educational Purpose',
    longDescription: 'The data will be used for education purposes (e.g. for a college research \
      methods course, to educate students on population-based research approaches).'
  }, {
    shortName: 'otherPurpose',
    shortDescription: 'Other Purpose',
    longDescription: 'If your Purpose of Use is different from the options listed above, please \
      select \"Other Purpose\" and provide details regarding your purpose of data use here \
      (500 character limit).'
  }
];

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
    with your research.`
};

export const researchPurposeQuestions = [
  {
    header: '1. What is the primary purpose of your project?',
    description: ['(Please select as many options below as describe your \
      research purpose)']
  }, {
    header: '2. Provide the reason for choosing All of Us data for your investigation',
    description: ['(Free text; 500 Character limit)']
  }, {
    header: '3. What are the specific scientific question(s) you intend to study?',
    description: ['If you are exploring the data at this stage to formalize a specific research \
      question, please describe the reason for exploring the data, and the scientific \
      question you hope to be able to answer using the data.', <br/>,
      '(Free text; 500 Character limit)']
  }, {
    header: '4. What are your anticipated findings from this study?',
    description: ['(Layperson language; 2000 Character limit)']
  }, {
    header: '5. Will your study or data analysis focus on specific population(s)? \
      Or do you intend to study your phenotype, disease, or condition of interest with \
      a focus on comparative analysis of a specific demographic group (for example \
      a group based on race/ethnicity, gender, or age)?',
    description: ['']
  }
];

export const specificPopulations = [
  {
    label: 'Race/Ethnicity',
    object: SpecificPopulationEnum.RACEETHNICITY
  }, {
    label: 'Age Groups',
    object: SpecificPopulationEnum.AGEGROUPS
  }, {
    label: 'Sex',
    object: SpecificPopulationEnum.SEX
  }, {
    label: 'Gender Identity',
    object: SpecificPopulationEnum.GENDERIDENTITY
  }, {
    label: 'Sexual Orientation',
    object: SpecificPopulationEnum.SEXUALORIENTATION
  }, {
    label: 'Geography (e.g. Rural, urban, suburban, etc.)',
    object: SpecificPopulationEnum.GEOGRAPHY
  }, {
    label: 'Disability status',
    object: SpecificPopulationEnum.DISABILITYSTATUS
  }, {
    label: 'Access to care',
    object: SpecificPopulationEnum.ACCESSTOCARE
  }, {
    label: 'Education level',
    object: SpecificPopulationEnum.EDUCATIONLEVEL
  }, {
    label: 'Income level',
    object: SpecificPopulationEnum.INCOMELEVEL
  }
];


const styles = reactStyles({

  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: colors.purple[0]
  },

  requiredText: {
    fontSize: '13px',
    fontStyle: 'italic',
    fontWeight: 400,
    color: colors.gray[0],
    marginLeft: '0.2rem'
  },

  text: {
    fontSize: '13px',
    color: colors.gray[0],
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
    background: colors.white,
    border: '1px solid #ccc',
    color: colors.black[0],
    borderRadius: '.125rem'
  },
  shortDescription: {
    color: colors.purple[0],
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '24px'
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
    display: 'flex',
    flexDirection: 'row',
    padding: '0.6rem 0',
  },
  checkBoxStyle: {
    marginRight: '.31667rem', zoom: '1.5'
  },
  checkboxRow: {
    display: 'inline-block', padding: '0.2rem 0', marginRight: '1rem'
  }
});


export const WorkspaceEditSection = (props) => {
  return <div key={props.header} style={{marginBottom: '0.5rem'}}>
    <div style={{display: 'flex', flexDirection: 'row', marginBottom: (props.largeHeader ? 12 : 0),
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
    </div>
    {props.subHeader && <div style={{...styles.header, color: colors.gray[0], fontSize: 14}}>
      {props.subHeader}
    </div>
    }
    <div style={styles.text}>
      {props.description}
    </div>
    <div>
      {props.children}
    </div>
  </div>;
};

export const WorkspaceCategory = (props) => {
  return <div style={...fp.merge(styles.categoryRow, props.style)}>
    <CheckBox style={styles.checkBoxStyle} checked={!!props.value}
      onChange={e => props.onChange(e)}/>
    <div style={{display: 'flex', flexDirection: 'column', marginTop: '-0.2rem'}}>
      <label style={styles.shortDescription}>
        {props.shortDescription}
      </label>
      <div>
        <label style={{...styles.longDescription, ...styles.text}}>
          {props.longDescription}
        </label>
        {props.children}
      </div>
    </div>
  </div>;
};

export const LabeledCheckBox = (props) => {
  return <div style={...fp.merge(styles.checkboxRow, props.style)}>
    <CheckBox style={{...styles.checkBoxStyle, verticalAlign: 'middle'}}
              checked={!!props.value} disabled={props.disabled}
              onChange={e => props.onChange(e)}/>
    <label style={styles.text}>{props.label}</label>
  </div>;
};

export enum WorkspaceEditMode { Create = 1, Edit = 2, Duplicate = 3 }


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
  workspaceCreationErrorMessage: string;
  cloneUserRole: boolean;
  loading: boolean;
  showUnderservedPopulationDetails: boolean;
}

export const WorkspaceEdit = fp.flow(withRouteConfigData(), withCurrentWorkspace())(
  class extends React.Component<WorkspaceEditProps, WorkspaceEditState> {

    constructor(props: WorkspaceEditProps) {
      super(props);
      this.state = {
        cdrVersionItems: [],
        workspace: {
          name: '',
          dataAccessLevel: DataAccessLevel.Registered,
          namespace: userProfileStore.getValue().profile.freeTierBillingProjectName,
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
            otherPurpose: false,
            otherPurposeDetails: '',
            population: false,
            populationDetails: [],
            populationHealth: false,
            reviewRequested: false,
            socialBehavioral: false,
            reasonForAllOfUs: '',
          }
        },
        workspaceCreationConflictError: false,
        workspaceCreationError: false,
        workspaceCreationErrorMessage: '',
        cloneUserRole: false,
        loading: false,
        showUnderservedPopulationDetails: false
      };
    }

    componentDidMount() {
      if (!this.isMode(WorkspaceEditMode.Create)) {
        this.setState({workspace : {
          ...this.props.workspace,
            // Replace potential nulls with empty string or empty array
          researchPurpose: {
            ...this.props.workspace.researchPurpose,
            populationDetails: !this.props.workspace.researchPurpose.populationDetails ?
              [] : this.props.workspace.researchPurpose.populationDetails,
            diseaseOfFocus: !this.props.workspace.researchPurpose.diseaseOfFocus ?
              '' : this.props.workspace.researchPurpose.diseaseOfFocus}
        }});
        if (this.isMode(WorkspaceEditMode.Duplicate)) {
          this.setState({workspace: {
            ...this.props.workspace,
            // These are the only fields which are not automatically handled/differentiated
            // on the API level.
            name: 'Duplicate of ' + this.props.workspace.name,
            namespace: userProfileStore.getValue().profile.freeTierBillingProjectName
          }});
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

    categoryIsSelected() {
      const rp = this.state.workspace.researchPurpose;
      return rp.ancestry || rp.commercialPurpose || rp.controlSet || rp.diseaseFocusedResearch ||
        rp.drugDevelopment || rp.educational || rp.methodsDevelopment || rp.otherPurpose ||
        rp.populationHealth || rp.socialBehavioral;
    }

    noSpecificPopulationSelected(): boolean {
      return this.state.workspace.researchPurpose.population &&
        this.state.workspace.researchPurpose.populationDetails.length === 0;
    }

    disableButton(): boolean {
      const rp = this.state.workspace.researchPurpose;
      return this.isEmpty(this.state.workspace, 'name') ||
        this.isEmpty(rp, 'intendedStudy') ||
        this.isEmpty(rp, 'anticipatedFindings') ||
        this.isEmpty(rp, 'reasonForAllOfUs') ||
        !this.categoryIsSelected() || this.noSpecificPopulationSelected();
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

    specificPopulationSelected(populationEnum): boolean {
      return fp.includes(populationEnum, this.state.workspace.researchPurpose.populationDetails);
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
          await workspacesApi()
            .getWorkspace(this.state.workspace.namespace, this.state.workspace.id)
            .then(ws => currentWorkspaceStore.next({
              ...ws.workspace,
              accessLevel: ws.accessLevel
            }));
        }
        navigate(['workspaces', workspace.namespace, workspace.id]);
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

    isEmpty(parent, field) {
      const fieldValue = parent[field];
      return !fieldValue || fieldValue === '';
    }

    isMode(mode) {
      return this.props.routeConfigData.mode === mode;
    }

    sliceLength(obj) {
      return obj.length / 2 + 1;
    }

    render() {
      return <FadeBox  style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <div style={{width: '95%'}}>
        <WorkspaceEditSection header={this.renderHeader()} tooltip={toolTipText.header}
                              section={{marginTop: '24px'}} largeHeader required>
          <div style={{display: 'flex', flexDirection: 'row'}}>
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
        {this.isMode(WorkspaceEditMode.Duplicate) &&
        <div style={{display: 'flex', flexDirection: 'row'}}>
          <CheckBox
                 style={{height: '.66667rem', marginRight: '.31667rem', marginTop: '1.2rem'}}
          onChange={v => this.setState({cloneUserRole: v})}/>
          <WorkspaceEditSection header='Copy Original workspace Collaborators'
            description='Share cloned workspace with same collaborators'/>
        </div>
        }
        <WorkspaceEditSection header='Billing Account' subHeader='National Institutes of Health'
            tooltip={toolTipText.billingAccount}/>
        <WorkspaceEditSection header='Research Use Statement Questions'
            description={['The AoU Research Program requires each user of AoU data to provide a \
             meaningful description of the intended purpose of data use for each Workspace they \
             create. The responses provided below will be posted publicly in the AoU Research Hub \
             website to inform the AoU Research Participants.  Therefore, please provide \
             sufficiently detailed responses at a 5th grade reading level.  Your responses will \
             not be used to make decisions about data access.', <br/>, <br/>,
              <i>Note that you are required to create separate Workspaces for each project
              for which you access AoU data, hence the responses below are expected to be specific
              to the project for which you are creating this particular Workspace.</i>
            ]}/>
        <WorkspaceEditSection header={researchPurposeQuestions[0].header}
            description={researchPurposeQuestions[0].description} required>
          <div style={{display: 'flex', flexDirection: 'row'}}>
            <div style={{display: 'flex', flexDirection: 'column', flex: '1 1 0'}}>
              {ResearchPurposeItems.slice(0, this.sliceLength(ResearchPurposeItems))
                .map((rp, i) =>
                  <WorkspaceCategory shortDescription={rp.shortDescription} key={i}
                    longDescription={rp.longDescription}
                    value={this.state.workspace.researchPurpose[rp.shortName]}
                    onChange={v => this.updateResearchPurpose(rp.shortName, v)}
                    children={rp.shortName === 'diseaseFocusedResearch' ?
                      <TooltipTrigger
                        content='You must select disease focused research to enter
                          a disease of focus'
                        disabled={this.state.workspace.researchPurpose.diseaseFocusedResearch}>
                        <TextInput value={this.state.workspace.researchPurpose.diseaseOfFocus}
                          style={{
                            width: 'calc(50% - 2rem)',
                            border: '1px solid #9a9a9',
                            borderRadius: '5px'
                          }}
                          placeholder='Name of Disease' onChange={v =>
                          this.setState(
                            fp.set(['workspace', 'researchPurpose', 'diseaseOfFocus'], v))}
                          disabled={!this.state.workspace.researchPurpose.diseaseFocusedResearch}/>
                      </TooltipTrigger> : undefined}/>
              )}
            </div>
            <div style={{display: 'flex', flexDirection: 'column', flex: '1 1 0'}}>
              {ResearchPurposeItems.slice(this.sliceLength(ResearchPurposeItems))
                .map((rp, i) =>
                  <WorkspaceCategory shortDescription={rp.shortDescription}
                    longDescription={rp.longDescription} key={i}
                    value={this.state.workspace.researchPurpose[rp.shortName]}
                    onChange={v => this.updateResearchPurpose(rp.shortName, v)}
                    children={rp.shortName === 'otherPurpose' ?
                      <TextArea value={this.state.workspace.researchPurpose.otherPurposeDetails}
                        onChange={v => this.updateResearchPurpose('otherPurposeDetails', v)}
                        disabled={!this.state.workspace.researchPurpose.otherPurpose}
                        style={{marginTop: '0.5rem'}}/> : undefined}/>
                )}
            </div>
          </div>
        </WorkspaceEditSection>
        <WorkspaceEditSection
          header={researchPurposeQuestions[1].header}
          description={researchPurposeQuestions[1].description} required>
          <TextArea value={this.state.workspace.researchPurpose.reasonForAllOfUs}
                    onChange={v => this.updateResearchPurpose('reasonForAllOfUs', v)}
                    style={{marginTop: '0.5rem'}}/>
        </WorkspaceEditSection>
        <WorkspaceEditSection
          header={researchPurposeQuestions[2].header}
          description={researchPurposeQuestions[2].description} required>
          <TextArea value={this.state.workspace.researchPurpose.intendedStudy}
                    onChange={v => this.updateResearchPurpose('intendedStudy', v)}
                    style={{marginTop: '0.5rem'}}/>
        </WorkspaceEditSection>
        <WorkspaceEditSection header={researchPurposeQuestions[3].header}
                              description={researchPurposeQuestions[3].description} required>
          <TextArea value={this.state.workspace.researchPurpose.anticipatedFindings}
                    onChange={v => this.updateResearchPurpose('anticipatedFindings', v)}
                    style={{marginTop: '0.5rem'}}/>
        </WorkspaceEditSection>
        <WorkspaceEditSection required header={researchPurposeQuestions[4].header}>
          <Link onClick={() => this.setState({showUnderservedPopulationDetails:
              !this.state.showUnderservedPopulationDetails})} style={{marginTop: '0.5rem'}}>
            More info on underserved populations
            {this.state.showUnderservedPopulationDetails ? <ClrIcon shape='caret' dir='up'/> :
              <ClrIcon shape='caret' dir='down'/>}
          </Link>
          {this.state.showUnderservedPopulationDetails && <div style={styles.text}>
            A primary mission of the AoU Research Program is to include research participants
            who are medically underserved or are historically underrepresented in Biomedical
            Research, or who, because of systematic social disadvantage, experience health
            disparities.  As a way to assess the research being conducted with a focus on these
            populations, AoU requires that you indicate the demographic categories you intend
            to focus your analysis on.
          </div>}
          <div style={{marginTop: '0.5rem'}}>
            <RadioButton name='population' style={{marginRight: '0.5rem'}}
              onChange={v => this.updateResearchPurpose('population', true)}
              checked={this.state.workspace.researchPurpose.population}/>
            <label style={styles.text}>Yes, I am interested in the focused study of specific
            population(s), either on their own or in comparison to other groups.</label>
          </div>
          <div>
            <RadioButton name='population' style={{marginRight: '0.5rem'}}
              onChange={v => this.updateResearchPurpose('population', false)}
              checked={!this.state.workspace.researchPurpose.population}/>
            <label style={styles.text}>No, I am not interested in focusing on
              specific population(s) in my research.</label>
          </div>
          <div style={{...styles.text, marginLeft: '2rem'}}>
            <strong>If "Yes": </strong> Please specify the demographic category or categories of the
            population(s) that you are interested in exploring in your study.
            Select as many as applicable.
            <div style={{display: 'flex', flexDirection: 'row', flex: '1 1 0',
              marginTop: '0.5rem'}}>
              <div style={{display: 'flex', flexDirection: 'column'}}>
                {specificPopulations.slice(0, specificPopulations.length / 2 + 1).map(i =>
                  <LabeledCheckBox label={i.label} key={i.label}
                                   value={this.specificPopulationSelected(i.object)}
                                   onChange={v => this.updateSpecificPopulation(i.object, v)}
                                   disabled={!this.state.workspace.researchPurpose.population}/>
                )}
              </div>
              <div style={{display: 'flex', flexDirection: 'column'}}>
                {specificPopulations.slice(specificPopulations.length / 2 + 1).map(i =>
                  <LabeledCheckBox label={i.label} key={i.label}
                                   value={this.specificPopulationSelected(i.object)}
                                   onChange={v => this.updateSpecificPopulation(i.object, v)}
                                   disabled={!this.state.workspace.researchPurpose.population}/>
                )}
                <LabeledCheckBox label='Other'
                   value={this.specificPopulationSelected(SpecificPopulationEnum.OTHER)}
                   onChange={v => this.updateSpecificPopulation(SpecificPopulationEnum.OTHER, v)}
                   disabled={!this.state.workspace.researchPurpose.population}/>
                <TextInput type='text' autoFocus placeholder='Please specify'
                           disabled={!this.state.workspace.researchPurpose.populationDetails
                             .includes(SpecificPopulationEnum.OTHER)}/>
              </div>
            </div>
          </div>
        </WorkspaceEditSection>
        <WorkspaceEditSection header='Request a review of your research purpose'
                              tooltip={toolTipText.reviewRequest}>
          <div style={{display: 'flex', flexDirection: 'row',
            paddingBottom: '14.4px', paddingTop: '0.3rem'}}>
            <CheckBox style={{height: '.66667rem', marginRight: '.31667rem', marginTop: '0.3rem'}}
              onChange={v => this.setState(
                fp.set(['workspace', 'researchPurpose', 'reviewRequested' ], v))}
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
              { this.isEmpty(this.state.workspace, 'name') && <li> Name </li> }
              { this.isEmpty(this.state.workspace.researchPurpose, 'intendedStudy') &&
              <li>Field of intended study</li>}
              { this.isEmpty(this.state.workspace.researchPurpose, 'anticipatedFindings') &&
              <li>Anticipated findings</li>}
              { this.isEmpty(this.state.workspace.researchPurpose, 'reasonForAllOfUs') &&
              <li>Reason for choosing AoU</li>}
              { !this.categoryIsSelected() && <li>Research focus</li>}
              { this.noSpecificPopulationSelected() && <li>Population of study</li>}
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
