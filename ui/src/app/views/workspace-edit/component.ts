import {Location} from '@angular/common';
import {Component, OnInit, ViewChild} from '@angular/core';

import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {WorkspaceData, WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {deepCopy, isBlank} from 'app/utils';
import {currentWorkspaceStore, navigate, routeConfigDataStore} from 'app/utils/navigation';

import {ToolTipComponent} from 'app/views/tooltip/component';
import {
  CdrVersion,
  CloneWorkspaceResponse,
  DataAccessLevel,
  UnderservedPopulationEnum,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';

export enum WorkspaceEditMode { Create = 1, Edit = 2, Clone = 3 }

export const ResearchPurposeItems = {
  methodsDevelopment: {
    shortDescription: 'Methods development/validation study',
    longDescription: 'The primary purpose of the research is to develop and/or validate new \
    methods/tools for analyzing or interpreting data (for example, developing more powerful \
    methods to detect epistatic, gene-environment, or other types of complex interactions in \
    genome-wide association studies). Data will be used for developing and/or validating new \
    methods.'
  },
  diseaseFocusedResearch: {
    shortDescription: 'Disease-focused research',
    longDescription: 'The primary purpose of the research is to learn more about a particular \
    disease or disorder (for example, type 2 diabetes), a trait (for example, blood pressure), \
    or a set of related conditions (for example, autoimmune diseases, psychiatric disorders).'
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
    shortDescription: 'Request a review of your research purpose',
    /*
     * The request review description includes a hyperlink, so needs to be coded
     * inside the html, rather than as text here. There are ways to have it render
     * html, but it strips out unsafe content, so was removing the click behavior
     */
    longDescription: 'SEE HTML'
  }
};

@Component({
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/inputs.css'],
  templateUrl: './component.html',
})
export class WorkspaceEditComponent implements OnInit {
  // Defines the supported modes for the workspace edit component.
  // Unfortunately, it is not currently possible to define an enum directly
  // within a class in Typescript, so make do with this type alias.
  Mode = WorkspaceEditMode;

  mode: WorkspaceEditMode;
  workspace: Workspace;
  workspaceId: string;
  oldWorkspaceName: string;
  oldWorkspaceNamespace: string;
  savingWorkspace = false;
  descriptionNotEntered = false;
  nameNotEntered = false;
  notFound = false;
  workspaceCreationError = false;
  workspaceCreationConflictError = false;
  workspaceUpdateError = false;
  workspaceUpdateConflictError = false;
  accessLevel: WorkspaceAccessLevel;
  isBlank = isBlank;
  raceList = {
    'American Indian or Alaska Native': UnderservedPopulationEnum.RACEAMERICANINDIANORALASKANATIVE,
    'Hispanic or Latino': UnderservedPopulationEnum.RACEHISPANICORLATINO,
    'More than one race': UnderservedPopulationEnum.RACEMORETHANONERACE,
    'Asian': UnderservedPopulationEnum.RACEASIAN,
    'Middle Eastern or North African': UnderservedPopulationEnum.RACEMIDDLEEASTERNORNORTHAFRICAN,
    'Black, African or African American':
    UnderservedPopulationEnum.RACEBLACKAFRICANORAFRICANAMERICAN,
    'Native Hawaiian or Pacific Islander':
    UnderservedPopulationEnum.RACENATIVEHAWAIIANORPACIFICISLANDER
  };
  ageList = {
    'Children (0-11)': UnderservedPopulationEnum.AGECHILDREN,
    'Adolescents (12-17)': UnderservedPopulationEnum.AGEADOLESCENTS,
    'Older Adults (65-74)': UnderservedPopulationEnum.AGEOLDERADULTS,
    'Elderly (75+)': UnderservedPopulationEnum.AGEELDERLY
  };
  sexList = {
    'Female': UnderservedPopulationEnum.SEXFEMALE,
    'Intersex': UnderservedPopulationEnum.SEXINTERSEX
  };
  sexualOrientationList = {
    'Gay': UnderservedPopulationEnum.SEXUALORIENTATIONGAY,
    'Lesbian': UnderservedPopulationEnum.SEXUALORIENTATIONLESBIAN,
    'Bisexual': UnderservedPopulationEnum.SEXUALORIENTATIONBISEXUAL,
    'Polysexual, omnisexual, sapiosexual or pansexual':
    UnderservedPopulationEnum.SEXUALORIENTATIONPOLYSEXUALOMNISEXUALSAPIOSEXUALORPANSEXUAL,
    'Asexual': UnderservedPopulationEnum.SEXUALORIENTATIONASEXUAL,
    'Two-Spirit': UnderservedPopulationEnum.SEXUALORIENTATIONTWOSPIRIT,
    'Have not figured out or are in the process of figuring out their sexuality':
    UnderservedPopulationEnum.SEXUALORIENTATIONFIGURINGOUTSEXUALITY,
    'Mostly straight, but sometimes attracted to people of their own sex':
    UnderservedPopulationEnum.SEXUALORIENTATIONMOSTLYSTRAIGHT,
    'Does not think of themselves as having sexuality':
    UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTTHINKOFHAVINGSEXUALITY,
    'Does not use labels to identify themselves':
    UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTUSELABELS,
    'Does not know the answer': UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTKNOWANSWER
  };
  genderIdentityList = {
    'Woman': UnderservedPopulationEnum.GENDERIDENTITYWOMAN,
    'Non-Binary': UnderservedPopulationEnum.GENDERIDENTITYNONBINARY,
    'Trans man/Transgender Man/FTM': UnderservedPopulationEnum.GENDERIDENTITYTRANSMAN,
    'Trans woman/Transgender Woman/MTF': UnderservedPopulationEnum.GENDERIDENTITYTRANSWOMAN,
    'Genderqueer': UnderservedPopulationEnum.GENDERIDENTITYGENDERQUEER,
    'Genderfluid': UnderservedPopulationEnum.GENDERIDENTITYGENDERFLUID,
    'Gender Variant': UnderservedPopulationEnum.GENDERIDENTITYGENDERVARIANT,
    'Questioning or unsure of their identity': UnderservedPopulationEnum.GENDERIDENTITYQUESTIONING
  };
  geographyList = {
    'Urban clusters (2,500-50,000 people)': UnderservedPopulationEnum.GEOGRAPHYURBANCLUSTERS,
    'Rural (All population, housing and territory not included within an urban area)':
    UnderservedPopulationEnum.GEOGRAPHYRURAL
  };
  disabilityList = {
    'Physical Disability': UnderservedPopulationEnum.DISABILITYPHYSICAL,
    'Mental Disability': UnderservedPopulationEnum.DISABILITYMENTAL
  };
  accessToCareList = {
    'Have not had a clinic visit in the past 12 months':
    UnderservedPopulationEnum.ACCESSTOCARENOTPASTTWELVEMONTHS,
    'Cannot easily obtain or pay for medical care':
    UnderservedPopulationEnum.ACCESSTOCARECANNOTOBTAINORPAYFOR
  };
  educationIncomeList = {
    'Less than a high school graduate':
    UnderservedPopulationEnum.EDUCATIONINCOMELESSTHANHIGHSCHOOLGRADUATE,
    'Less than $25,000 for 4 people':
    UnderservedPopulationEnum.EDUCATIONINCOMELESSTHANTWENTYFIVETHOUSANDFORFOURPEOPLE
  };

  underservedCategories = {
    'Race/Ethnicity': this.raceList,
    'Age': this.ageList,
    'Sex': this.sexList,
    'Gender Identity': this.genderIdentityList,
    'Sexual Orientation': this.sexualOrientationList,
    'Geography': this.geographyList,
    'Disability': this.disabilityList,
    'Access to Care': this.accessToCareList,
    'Education/Income': this.educationIncomeList
  };

  researchPurposeItems = ResearchPurposeItems;
  cloneUserRoles = false;
  fillDetailsLater = false;
  hideDetailsLaterOption = true;
  canEditResearchPurpose = true;
  cdrVersions: CdrVersion[] = [];

  @ViewChild(ToolTipComponent)
  toolTip: ToolTipComponent;

  constructor(
    private locationService: Location,
    private workspacesService: WorkspacesService,
    private workspaceStorageService: WorkspaceStorageService,
    private cdrVersionStorageService: CdrVersionStorageService,
    public profileStorageService: ProfileStorageService,
  ) {}

  ngOnInit(): void {
    this.workspace = {
      name: '',
      description: '',
      dataAccessLevel: DataAccessLevel.Registered,
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
      }};
    this.mode = WorkspaceEditMode.Edit;
    const configData = routeConfigDataStore.getValue();
    if (configData.mode) {
      this.mode = configData.mode;
    }

    this.cdrVersionStorageService.cdrVersions$.subscribe(resp => {
      this.cdrVersions = resp.items;
      if (this.mode === WorkspaceEditMode.Create) {
        this.workspace.cdrVersionId = resp.defaultCdrVersionId;
      }
    });
    if (this.mode === WorkspaceEditMode.Create || this.mode === WorkspaceEditMode.Clone) {
      // There is a new workspace to be created via this flow.
      this.accessLevel = WorkspaceAccessLevel.OWNER;
      this.profileStorageService.profile$.subscribe(profile => {
        this.workspace.namespace = profile.freeTierBillingProjectName;
      });
    }
    if (this.mode === WorkspaceEditMode.Edit || this.mode === WorkspaceEditMode.Clone) {
      // There is an existing workspace referenced in this flow.
      const ws = currentWorkspaceStore.getValue();
      this.oldWorkspaceNamespace = ws.namespace;
      this.oldWorkspaceName = ws.id;
      this.loadWorkspace();
    }
  }

  loadWorkspace(): void {
    const wsData: WorkspaceData = deepCopy(currentWorkspaceStore.getValue()) as WorkspaceData;
    if (this.mode === WorkspaceEditMode.Edit) {
      this.workspace = wsData;
      this.accessLevel = wsData.accessLevel;
      if (isBlank(this.workspace.description)) {
        this.fillDetailsLater = true;
        this.canEditResearchPurpose = true;
      } else {
        this.hideDetailsLaterOption = true;
        this.canEditResearchPurpose = false;
      }
    } else if (this.mode === WorkspaceEditMode.Clone) {
      this.workspace.name = 'Duplicate of ' + wsData.name;
      this.workspace.description = wsData.description;
      this.workspace.cdrVersionId = wsData.cdrVersionId;
      const fromPurpose = wsData.researchPurpose;
      this.workspace.researchPurpose = {
        ...fromPurpose,
        // Heuristic for whether the user will want to request a review,
        // assuming minimal changes to the existing research purpose.
        reviewRequested: (fromPurpose.reviewRequested && !fromPurpose.approved),
        timeRequested: null,
        approved: null,
        timeReviewed: null,
        additionalNotes: null
      };
    }
  }

  navigateBack(): void {
    this.locationService.back();
  }


  reloadConflictingWorkspace(): void {
    this.workspaceStorageService.reloadWorkspace(
      this.workspace.namespace,
      this.workspace.id).then((workspace) => {
        this.workspace = workspace;
        this.accessLevel = workspace.accessLevel;
        this.resetWorkspaceEditor();
      });
  }

  resetWorkspaceEditor(): void {
    this.workspaceCreationError = false;
    this.workspaceCreationConflictError = false;
    this.workspaceUpdateError = false;
    this.workspaceUpdateConflictError = false;
    this.savingWorkspace = false;
  }

  validateForm(): boolean {
    if (this.savingWorkspace) {
      return false;
    }
    this.nameNotEntered = isBlank(this.workspace.name);
    if (this.nameNotEntered) {
      return false;
    }
    this.descriptionNotEntered = isBlank(this.workspace.description);
    if (this.descriptionNotEntered && !this.fillDetailsLater) {
      return false;
    }
    return true;
  }

  addWorkspace(): void {
    if (!this.validateForm()) {
      return;
    }
    this.savingWorkspace = true;
    this.workspacesService.createWorkspace(this.workspace).subscribe(
      (workspace) => {
        navigate(['workspaces', workspace.namespace, workspace.id]);
      },
      (error) => {
        if (error.status === 409) {
          this.workspaceCreationConflictError = true;
        } else {
          this.workspaceCreationError = true;
        }
      });
  }

  updateWorkspace(): void {
    if (!this.validateForm()) {
      return;
    }
    this.savingWorkspace = true;
    this.workspacesService.updateWorkspace(
      this.oldWorkspaceNamespace,
      this.oldWorkspaceName,
      {workspace: this.workspace})
      .subscribe(
        () => {
          this.workspaceStorageService.reloadWorkspace(
            this.workspace.namespace,
            this.workspace.id).then(() => {
              this.navigateBack();
            });
        },
        (error) => {
          if (error.status === 409) {
            this.workspaceUpdateConflictError = true;
          } else {
            this.workspaceUpdateError = true;
          }
        });
  }

  cloneWorkspace(): void {
    if (!this.validateForm()) {
      return;
    }
    this.savingWorkspace = true;
    this.workspacesService.cloneWorkspace(
      this.oldWorkspaceNamespace,
      this.oldWorkspaceName, {
        includeUserRoles: this.cloneUserRoles,
        workspace: this.workspace,
      }).subscribe(
        (r: CloneWorkspaceResponse) => {
          navigate(['/workspaces', r.workspace.namespace, r.workspace.id]);
        },
        (error) => {
          this.resetWorkspaceEditor();
          if (error.status === 409) {
            this.workspaceCreationConflictError = true;
          } else {
            this.workspaceUpdateError = true;
          }
        });
  }

  get selectedCdrName(): string {
    const version = this.cdrVersions.find(v => v.cdrVersionId === this.workspace.cdrVersionId);
    if (!version) {
      return '';
    }
    return version.name;
  }

  get hasPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  keys(input: Object): Array<string> {
    return Object.keys(input);
  }

  bucketAsThree(input: Array<string>): Array<Array<string>> {
    const output = [];
    for (let i = 0; i < input.length; i += 3) {
      output.push(input.slice(i, i + 3));
    }
    return output;
  }

  containsUnderserved(enumValue: UnderservedPopulationEnum): boolean {
    return this.workspacePopulationDetails && this.workspacePopulationDetails.includes(enumValue);
  }

  switchUnderservedStatus(enumValue: UnderservedPopulationEnum): void {
    if (!this.workspacePopulationDetails) {
      this.workspacePopulationDetails = [];
    }
    const positionOfValue = this.workspacePopulationDetails.findIndex(item => item === enumValue);
    if (positionOfValue !== -1) {
      this.workspacePopulationDetails.splice(positionOfValue, 1);
    } else {
      this.workspacePopulationDetails.push(enumValue);
    }
  }

  get workspacePopulationDetails() {
    return this.workspace.researchPurpose.underservedPopulationDetails;
  }

  set workspacePopulationDetails(details) {
    this.workspace.researchPurpose.underservedPopulationDetails = details;
  }

  get isValidWorkspace() {
    return !this.missingFields && !this.nameValidationError;
  }

  get missingFields() {
    return isBlank(this.workspace.name) ||
      ((isBlank(this.workspace.description) && !this.fillDetailsLater));
  }

  get nameValidationError() {
    return this.workspace.name && this.workspace.name.length > 80;
  }

  get allowSave() {
    if (this.savingWorkspace) {
      return false;
    }
    return this.isValidWorkspace;
  }

  openStigmatizationLink() {
    const stigmatizationURL = `/definitions/stigmatization`;
    window.open(stigmatizationURL, '_blank');
  }

  clearAllFields() {
    this.workspace.description = '',
      this.workspace.researchPurpose =  {
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
      };
  }
}
