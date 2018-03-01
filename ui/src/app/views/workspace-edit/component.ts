import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {isBlank} from 'app/utils';

import {
  CloneWorkspaceResponse,
  DataAccessLevel,
  ProfileService,
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
  underservedList = {
    race: {
      americanIndianOrAlaskaNative: {value: UnderservedPopulationEnum.RACEAMERICANINDIANORALASKANATIVE, included: false},
      asian: {value: UnderservedPopulationEnum.RACEASIAN, included: false},
      blackAfricanOrAfricanAmerican: {value: UnderservedPopulationEnum.RACEBLACKAFRICANORAFRICANAMERICAN, included: false},
      hispanicOrLatino: {value: UnderservedPopulationEnum.RACEHISPANICORLATINO, included: false},
      middleEasternOrNorthAfrican: {value: UnderservedPopulationEnum.RACEMIDDLEEASTERNORNORTHAFRICAN, included: false},
      nativeHawaiianOrPacificIslander: {value: UnderservedPopulationEnum.RACENATIVEHAWAIIANORPACIFICISLANDER, included: false},
      moreThanOneRace: {value: UnderservedPopulationEnum.RACEMORETHANONERACE, included: false}
    },
    age: {
      children: {value: UnderservedPopulationEnum.AGECHILDREN, included: false},
      adolescents: {value: UnderservedPopulationEnum.AGEADOLESCENTS, included: false},
      olderAdults: {value: UnderservedPopulationEnum.AGEOLDERADULTS, included: false},
      elderly: {value: UnderservedPopulationEnum.AGEELDERLY, included: false}
    },
    sex: {
      female: {value: UnderservedPopulationEnum.SEXFEMALE, included: false},
      intersex: {value: UnderservedPopulationEnum.SEXINTERSEX, included: false}
    },
    sexualOrientation: {
      gay: {value: UnderservedPopulationEnum.SEXUALORIENTATIONGAY, included: false},
      lesbian: {value: UnderservedPopulationEnum.SEXUALORIENTATIONLESBIAN, included: false},
      bisexual: {value: UnderservedPopulationEnum.SEXUALORIENTATIONBISEXUAL, included: false},
      polysexualOmnisexualSapiosexualOrPansexual: {
          value: UnderservedPopulationEnum.SEXUALORIENTATIONPOLYSEXUALOMNISEXUALSAPIOSEXUALORPANSEXUAL,
          included: false
        },
      asexual: {value: UnderservedPopulationEnum.SEXUALORIENTATIONASEXUAL, included: false},
      twoSpirit: {value: UnderservedPopulationEnum.SEXUALORIENTATIONTWOSPIRIT, included: false},
      figuringOutSexuality: {value: UnderservedPopulationEnum.SEXUALORIENTATIONFIGURINGOUTSEXUALITY, included: false},
      mostlyStraight: {value: UnderservedPopulationEnum.SEXUALORIENTATIONMOSTLYSTRAIGHT, included: false},
      doesNotThinkOfHavingSexuality: {value: UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTTHINKOFHAVINGSEXUALITY, included: false},
      doesNotUseLabels: {value: UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTUSELABELS, included: false},
      doesNotKnowAnswer: {value: UnderservedPopulationEnum.SEXUALORIENTATIONDOESNOTKNOWANSWER, included: false}
    },
    genderIdentity: {
      woman: {value: UnderservedPopulationEnum.GENDERIDENTITYWOMAN, included: false},
      nonBinary: {value: UnderservedPopulationEnum.GENDERIDENTITYNONBINARY, included: false},
      transman: {value: UnderservedPopulationEnum.GENDERIDENTITYTRANSMAN, included: false},
      transwoman: {value: UnderservedPopulationEnum.GENDERIDENTITYTRANSWOMAN, included: false},
      genderqueer: {value: UnderservedPopulationEnum.GENDERIDENTITYGENDERQUEER, included: false},
      genderfluid: {value: UnderservedPopulationEnum.GENDERIDENTITYGENDERFLUID, included: false},
      genderVariant: {value: UnderservedPopulationEnum.GENDERIDENTITYGENDERVARIANT, included: false},
      questioning: {value: UnderservedPopulationEnum.GENDERIDENTITYQUESTIONING, included: false}
    },
    geography: {
      urbanClusters: {value: UnderservedPopulationEnum.GEOGRAPHYURBANCLUSTERS, included: false},
      rural: {value: UnderservedPopulationEnum.GEOGRAPHYRURAL, included: false}
    },
    disability: {
      physical: {value: UnderservedPopulationEnum.DISABILITYPHYSICAL, included: false},
      mental: {value: UnderservedPopulationEnum.DISABILITYMENTAL, included: false}
    },
    accessToCare: {
      notPastTwelveMonths: {value: UnderservedPopulationEnum.ACCESSTOCARENOTPASTTWELVEMONTHS, included: false},
      cannotObtainOrPayFor: {value: UnderservedPopulationEnum.ACCESSTOCARECANNOTOBTAINORPAYFOR, included: false}
    },
    educationIncome: {
      lessThanHighSchoolGraduate: {value: UnderservedPopulationEnum.EDUCATIONINCOMELESSTHANHIGHSCHOOLGRADUATE, included: false},
      lessThanTwentyFiveThousandForFourPeople: {value: UnderservedPopulationEnum.EDUCATIONINCOMELESSTHANTWENTYFIVETHOUSANDFORFOURPEOPLE, included: false}
    }
  }
  constructor(
      private locationService: Location,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
      private profileService: ProfileService,
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
        underservedPopulation: false
      }};
    this.mode = WorkspaceEditMode.Edit;
    if (this.route.routeConfig.data.mode) {
      this.mode = this.route.routeConfig.data.mode;
    }

    if (this.mode === WorkspaceEditMode.Create || this.mode === WorkspaceEditMode.Clone) {
      // There is a new workspace to be created via this flow.
      this.accessLevel = WorkspaceAccessLevel.OWNER;
      this.profileService.getMe().subscribe(profile => {
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
    if (this.workspace.researchPurpose.underservedPopulation) {
      this.workspace.researchPurpose.underservedPopulationDetails = [];
      Object.values(this.underservedList).forEach((category) => {
        this.workspace.researchPurpose.underservedPopulationDetails.concat(
            Object.values(category).filter(item => item.included === true));
      });
    }

    // if (!this.validateForm()) {
    //   return;
    // }
    // this.savingWorkspace = true;
    // this.workspacesService.createWorkspace(this.workspace).subscribe(
    //     () => {
    //       this.navigateBack();
    //     },
    //     (error) => {
    //       this.workspaceCreationError = true;
    //     });
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
    if (this.workspace.researchPurpose.underservedPopulation) {
      this.workspace.researchPurpose.underservedPopulationDetails = [];
      Object.values(this.underservedList).forEach((category) => {
        this.workspace.researchPurpose.underservedPopulationDetails.concat(
            Object.values(category).filter(item => item.included === true));
      });
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
}
