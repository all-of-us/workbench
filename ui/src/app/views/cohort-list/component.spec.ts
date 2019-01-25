import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {
  findElementsReact,
  setupModals,
  simulateClickReact,
  simulateInputReact,
  updateAndTick
} from 'testing/test-helpers';

import {SignInService} from 'app/services/sign-in.service';
import {CohortListComponent} from 'app/views/cohort-list/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {ResourceCardComponent, ResourceCardMenuComponent} from 'app/views/resource-card/component';
import {ToolTipComponent} from 'app/views/tooltip/component';
import {TopBoxComponent} from 'app/views/top-box/component';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {CohortsApi} from 'generated/fetch';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';

import {
  CohortsService,
  ConceptSetsService,
  RecentResource,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';
// import {CohortsApi} from "../../../generated/fetch/api";


const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspaces'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID}
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    },
    data: {
      workspace: {
        ...WorkspacesServiceStub.stubWorkspace(),
        accessLevel: WorkspaceAccessLevel.OWNER,
      }
    }
  }
};

class CohortListPage {
  fixture: ComponentFixture<CohortListComponent>;
  route: UrlSegment[];
  form: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(CohortListComponent);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
  }
}

describe('CohortListComponent', () => {
  let cohortListPage: CohortListPage;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule,
        FormsModule,
        ReactiveFormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        CohortListComponent,
        ConfirmDeleteModalComponent,
        ResourceCardComponent,
        ResourceCardMenuComponent,
        ToolTipComponent,
        TopBoxComponent
      ],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub},
        { provide: CohortsService, useValue: new CohortsServiceStub()},
        { provide: ConceptSetsService },
        { provide: SignInService, useValue: new SignInServiceStub()},
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub()}
      ] }).compileComponents().then(() => {
        cohortListPage = new CohortListPage(TestBed);
      });
    registerApiClient(CohortsApi, new CohortsApiStub());
    tick();
  }));

  it('should delete the correct cohort', fakeAsync(() => {
    const fixture = TestBed.createComponent(CohortListComponent);
    setupModals(fixture);
    const app = fixture.debugElement.componentInstance;
    setupModals(fixture);
    updateAndTick(fixture);
    updateAndTick(fixture);
    const firstCohortName = findElementsReact(fixture, '[data-test-id="card-name"]')[0].innerText;
    const deletedResource: RecentResource = app.resourceList.find(
      (r: RecentResource) => r.cohort.name === firstCohortName);
    expect(deletedResource).toBeTruthy();
    simulateClickReact(fixture, '[data-test-id="resource-menu"]');
    updateAndTick(fixture);
    updateAndTick(fixture);
    simulateClickReact(fixture, '[data-test-id="trash"]');
    updateAndTick(fixture);
    simulateClickReact(fixture, '[data-test-id="confirm-delete"]');
    expect(app).toBeTruthy();
    expect(app.resourceList.length).toBe(1);
    expect(app.resourceList).not.toContain(deletedResource);
  }));

  it('updates the page on edit', fakeAsync(() => {
    const fixture = TestBed.createComponent(CohortListComponent);
    setupModals(fixture);
    const app = fixture.debugElement.componentInstance;
    const editValue = 'edited name';
    setupModals(fixture);
    updateAndTick(fixture);
    updateAndTick(fixture);
    const firstCohortName = findElementsReact(fixture, '[data-test-id="card-name"]')[0].innerText;
    simulateClickReact(fixture, '[data-test-id="resource-menu"]');
    updateAndTick(fixture);
    updateAndTick(fixture);
    simulateClickReact(fixture, '[data-test-id="pencil"]');
    updateAndTick(fixture);
    simulateInputReact(fixture, '[data-test-id="edit-name"]', editValue);
    updateAndTick(fixture);
    simulateClickReact(fixture, '[data-test-id="save-edit"]');
    updateAndTick(fixture);
    expect(app).toBeTruthy();
    expect(app.resourceList.length).toBe(2);
    const listOfNames = findElementsReact(fixture, '[data-test-id="card-name"]')
        .map(el => el.innerText);
    expect(listOfNames).toContain(editValue);
    expect(listOfNames).not.toContain(firstCohortName);
  }));
});
