import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {isBlank} from 'app/utils';

import {
  CloneWorkspaceResponse,
  DataAccessLevel,
  UnderservedPopulationEnum,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResponse,
  WorkspacesService
} from 'generated';

export enum WorkspaceEditMode { Create = 1, Edit = 2, Clone = 3 }

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
  nameNotEntered = false;
  notFound = false;
  workspaceCreationError = false;
  workspaceUpdateError = false;
  workspaceUpdateConflictError = false;
  private accessLevel: WorkspaceAccessLevel;
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
    'Transman/Transgender Man/FTM': UnderservedPopulationEnum.GENDERIDENTITYTRANSMAN,
    'Transwoman/Transgender Woman/MTF': UnderservedPopulationEnum.GENDERIDENTITYTRANSWOMAN,
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

  constructor(
      private locationService: Location,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
      public profileStorageService: ProfileStorageService,
      private router: Router,
  ) {}

  ngOnInit(): void {
    this.workspace = {
      name: '',
      description: '',
      dataAccessLevel: DataAccessLevel.Registered,
      // TODO - please set this properly
      cdrVersionId: '1',
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
    if (this.route.routeConfig.data.mode) {
      this.mode = this.route.routeConfig.data.mode;
    }

    if (this.mode === WorkspaceEditMode.Create || this.mode === WorkspaceEditMode.Clone) {
      // There is a new workspace to be created via this flow.
      this.accessLevel = WorkspaceAccessLevel.OWNER;

      this.profileStorageService.profile$.subscribe(profile => {
        this.workspace.namespace = profile.freeTierBillingProjectName;
      });
    }
    if (this.mode === WorkspaceEditMode.Edit || this.mode === WorkspaceEditMode.Clone) {
      // There is an existing workspace referenced in this flow.
      this.oldWorkspaceNamespace = this.route.snapshot.params['ns'];
      this.oldWorkspaceName = this.route.snapshot.params['wsid'];
      this.loadWorkspace();
    }
  }

  loadWorkspace(): Observable<WorkspaceResponse> {
    const obs: Observable<WorkspaceResponse> = this.workspacesService.getWorkspace(
      this.oldWorkspaceNamespace, this.oldWorkspaceName);
    obs.subscribe(
      (resp) => {
        if (this.mode === WorkspaceEditMode.Edit) {
          this.workspace = resp.workspace;
          this.accessLevel = resp.accessLevel;
        } else if (this.mode === WorkspaceEditMode.Clone) {
          this.workspace.name = 'Clone of ' + resp.workspace.name;
          this.workspace.description = resp.workspace.description;
          const fromPurpose = resp.workspace.researchPurpose;
          this.workspace.researchPurpose = {
            ...fromPurpose,
            // Heuristic for whether the user will want to request a review,
            // assuming minimal changes to the existing research purpose.
            reviewRequested: (
              fromPurpose.reviewRequested && !fromPurpose.approved),
            timeRequested: null,
            approved: null,
            timeReviewed: null,
            additionalNotes: null
          };
        }
      },
      (error) => {
        if (error.status === 404) {
          this.notFound = true;
        }
      }
    );
    return obs;
  }

  navigateBack(): void {
    this.locationService.back();
  }

  reloadConflictingWorkspace(): void {
    this.loadWorkspace().subscribe(() => {
      this.resetWorkspaceEditor();
    });
  }

  resetWorkspaceEditor(): void {
    this.workspaceCreationError = false;
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
    return true;
  }

  addWorkspace(): void {
    if (!this.validateForm()) {
      return;
    }
    this.savingWorkspace = true;
    this.workspacesService.createWorkspace(this.workspace).subscribe(
        () => {
          this.navigateBack();
        },
        (error) => {
          this.workspaceCreationError = true;
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
          this.navigateBack();
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
        workspace: this.workspace,
      }).subscribe(
        (r: CloneWorkspaceResponse) => {
          this.router.navigate(['/workspace', r.workspace.namespace, r.workspace.id]);
        },
        () => {
          // Only expected errors are transient, so allow the user to try again.
          this.resetWorkspaceEditor();
        });
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
    return this.workspacePopulationDetails.includes(enumValue);
  }

  switchUnderservedStatus(enumValue: UnderservedPopulationEnum): void {
    if (this.mode === WorkspaceEditMode.Edit) {
      return;
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

  get allowSave() {
    if (this.savingWorkspace) {
      return false;
    }
    return !isBlank(this.workspace.name);
  }
}
