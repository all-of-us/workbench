import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptSetListComponent} from 'app/views/concept-set-list/component';
import {CreateConceptSetModalComponent} from 'app/views/conceptset-create-modal/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {ResourceCardComponent, ResourceCardMenuComponent} from 'app/views/resource-card/component';
import {TopBoxComponent} from 'app/views/top-box/component';



import {
  CohortsService,
  ConceptSetsService,
  ConceptsService,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {ConceptsServiceStub} from 'testing/stubs/concepts-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {
  findElementsReact,
  setupModals,
  simulateClickReact,
  simulateInputReact,
  updateAndTick
} from 'testing/test-helpers';

import {SignInService} from 'app/services/sign-in.service';
import {ToolTipComponent} from 'app/views/tooltip/component';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ConceptsApi, ConceptSetsApi} from 'generated/fetch';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {ConceptsApiStub} from 'testing/stubs/concepts-api-stub';


const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspaces'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
      {path: 'concepts'}
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

describe('ConceptSetListComponent', () => {
  let fixture: ComponentFixture<ConceptSetListComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        ConceptAddModalComponent,
        ConceptSetListComponent,
        ConfirmDeleteModalComponent,
        CreateConceptSetModalComponent,
        ResourceCardComponent,
        ResourceCardMenuComponent,
        ToolTipComponent,
        TopBoxComponent,
      ],
      providers: [
        {provide: CohortsService},
        {provide: WorkspacesService},
        {provide: SignInService},
        {provide: ConceptsService, useValue: new ConceptsServiceStub()},
        {provide: ConceptSetsService, useValue: new ConceptSetsServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub}
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(ConceptSetListComponent);
      registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
      registerApiClient(ConceptsApi, new ConceptsApiStub());
      setupModals(fixture);
      // This tick initializes the component.
      tick();
      // This finishes the API calls.
      updateAndTick(fixture);
      // This finishes the page reloading.
      updateAndTick(fixture);
    });
  }));


  it('should render.', fakeAsync(() => {
    expect(fixture).toBeTruthy();
  }));

  it('displays correct concept sets', fakeAsync(() => {
    const conceptCards = findElementsReact(fixture, '[data-test-id="card-name"]')
        .map(el => el.innerText);
    expect(conceptCards.length).toEqual(3);
    expect(conceptCards[0]).toMatch('Mock Concept Set');
  }));

  it('displays correct information when concept set renamed', fakeAsync(() => {
    simulateClickReact(fixture, '[data-test-id="resource-menu"]');
    tick();
    simulateClickReact(fixture, '[data-test-id="pencil"]');
    updateAndTick(fixture);
    simulateInputReact(fixture, '[data-test-id="edit-name"]', 'testMockConcept');
    simulateClickReact(fixture, '[data-test-id="save-edit"]');
    tick(1000);
    updateAndTick(fixture);
    const conceptCards = findElementsReact(fixture, '[data-test-id="card-name"]')
        .map(el => el.innerText);
    expect(conceptCards[0]).toMatch('testMockConcept');
  }));

  it('displays correct information when concept set deleted', fakeAsync(() => {
    simulateClickReact(fixture, '[data-test-id="resource-menu"]');
    simulateClickReact(fixture, '[data-test-id="trash"]');
    updateAndTick(fixture);
    simulateClickReact(fixture, '[data-test-id="confirm-delete"]');
    updateAndTick(fixture);
    const conceptCards = findElementsReact(fixture, '[data-test-id="card-name"]');
    expect(conceptCards.length).toBe(2);
  }));
});
