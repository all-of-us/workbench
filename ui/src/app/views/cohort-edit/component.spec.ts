import {DebugElement} from '@angular/core';
import {async, ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from 'clarity-angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {CohortsServiceStub, DEFAULT_COHORT_ID} from 'testing/stubs/cohort-service-stub';
import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {
  queryByCss,
  simulateClick,
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

import {CohortsService} from 'generated';

class CohortEditPage {
  fixture: ComponentFixture<CohortEditComponent>;
  route: ActivatedRoute;
  cohortsService: CohortsService;
  nameField: DebugElement;
  descriptionField: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(CohortEditComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute);
    this.cohortsService = this.fixture.debugElement.injector.get(CohortsService);
    this.nameField = queryByCss(this.fixture, '.name');
    this.descriptionField = queryByCss(this.fixture, '.description');
  }
}

const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspace'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
      {path: 'cohorts'},
      {path: 'create'}
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      'cid': DEFAULT_COHORT_ID
    },
  },
  routeConfig: {
    data: {
      title: 'Create Cohort',
      adding: true
    }
  }
};

describe('CohortEditComponent', () => {
  let cohortEditPage: CohortEditPage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        CohortEditComponent
      ],
      providers: [
        { provide: CohortsService, useValue: new CohortsServiceStub() },
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ] }).compileComponents().then(() => {
        cohortEditPage = new CohortEditPage(TestBed);
      });
      tick();
  }));



  it('displays blank input fields when creating a new cohort', async(() => {
    cohortEditPage.fixture.detectChanges();
    expect(cohortEditPage.nameField.nativeNode.value).toMatch('');
    expect(cohortEditPage.descriptionField.nativeNode.value).toMatch('');
  }));


  it('fetches and displays an existing cohort in the edit pane',
  fakeAsync(() => {
    cohortEditPage.route.snapshot.url[4].path = '1';
    cohortEditPage.route.snapshot.url.push(new UrlSegment('edit', {}));
    cohortEditPage.route.routeConfig.data.adding = false;
    updateAndTick(cohortEditPage.fixture);
    updateAndTick(cohortEditPage.fixture);
    expect(cohortEditPage.nameField.nativeElement.value).toMatch('sample name');
    expect(cohortEditPage.descriptionField.nativeElement.value).toMatch('sample description');
  }));

  it('adds a new cohort with given name and description', fakeAsync(() => {
    cohortEditPage.route.snapshot.url[4].path = 'create';
    cohortEditPage.route.routeConfig.data.adding = true;
    updateAndTick(cohortEditPage.fixture);
    simulateInput(cohortEditPage.fixture, cohortEditPage.nameField, 'New Cohort');
    simulateInput(cohortEditPage.fixture, cohortEditPage.descriptionField, 'New Description');
    simulateClick(cohortEditPage.fixture, queryByCss(cohortEditPage.fixture, '.add-button'));
    updateAndTick(cohortEditPage.fixture);
    cohortEditPage.cohortsService.getCohortsInWorkspace(
    WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
    WorkspaceStubVariables.DEFAULT_WORKSPACE_ID).subscribe((cohorts) => {
      expect(cohorts.items.length).toBe(2);
      expect(cohorts.items[1].name).toBe('New Cohort');
      expect(cohorts.items[1].description).toBe('New Description');
    });
    updateAndTick(cohortEditPage.fixture);
  }));

  it('edits an existing cohort with given name and description',
  fakeAsync(() => {
    cohortEditPage.route.snapshot.url[4].path = '1';
    cohortEditPage.route.snapshot.url.push(new UrlSegment('edit', {}));
    cohortEditPage.route.routeConfig.data.adding = false;
    updateAndTick(cohortEditPage.fixture);
    simulateInput(cohortEditPage.fixture, cohortEditPage.nameField, 'Edited Cohort');
    simulateInput(cohortEditPage.fixture, cohortEditPage.descriptionField, 'Edited Description');
    simulateClick(cohortEditPage.fixture, queryByCss(cohortEditPage.fixture, '.save-button'));
    updateAndTick(cohortEditPage.fixture);
    cohortEditPage.cohortsService.getCohortsInWorkspace(
    WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
    WorkspaceStubVariables.DEFAULT_WORKSPACE_ID).subscribe((cohorts) => {
      expect(cohorts.items.length).toBe(1);
      expect(cohorts.items[0].name).toBe('Edited Cohort');
      expect(cohorts.items[0].description).toBe('Edited Description');
    });
    updateAndTick(cohortEditPage.fixture);
  }));
});
