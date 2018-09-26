import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptHomepageComponent} from 'app/views/concept-homepage/component';
import {ConceptTableComponent} from 'app/views/concept-table/component';
import {TopBoxComponent} from 'app/views/top-box/component';


import {
  ConceptSetsService,
  ConceptsService,
  WorkspaceAccessLevel,
} from 'generated';

import {ConceptsServiceStub} from 'testing/stubs/concept-service-stub';
import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';




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

describe('ConceptHomepageComponent', () => {
  let fixture: ComponentFixture<ConceptHomepageComponent>;
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
        ConceptHomepageComponent,
        ConceptTableComponent,
        TopBoxComponent,
      ],
      providers: [
        { provide: ConceptsService, useValue: new ConceptsServiceStub() },
        { provide: ConceptSetsService, useValue: new ConceptSetsServiceStub() },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]}).compileComponents().then(() => {
        fixture = TestBed.createComponent(ConceptHomepageComponent);
        tick();
        tick();
        tick();
      });
  }));


  it('should render.', fakeAsync(() => {
    expect(fixture).toBeTruthy();
  }));
});
