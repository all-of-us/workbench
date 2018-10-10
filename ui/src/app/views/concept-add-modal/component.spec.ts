import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {
  ConceptSet, ConceptsService, Domain, WorkspaceResponse,
  WorkspacesService
} from '../../../generated';
import {ConceptSetsService} from '../../../generated/api/conceptSets.service';
import {ConceptSetsServiceStub} from '../../../testing/stubs/concept-sets-service-stub';
import {
  ConceptsServiceStub,
  DomainStubVariables
} from '../../../testing/stubs/concepts-service-stub';
import {
  WorkspacesServiceStub,
  WorkspaceStubVariables
} from '../../../testing/stubs/workspace-service-stub';
import {simulateClick, simulateInput, updateAndTick} from '../../../testing/test-helpers';
import {CreateConceptSetModalComponent} from './component';

class ConceptSetAddPage {
  fixture: ComponentFixture<CreateConceptSetModalComponent>;
  route: UrlSegment[];
  conceptSetService: ConceptSetsService;
  workspacesService: WorkspacesService;
  workspaceNamespace: string;
  workspaceId: string;
  conceptList: string[] = [];
  name: DebugElement;
  description: DebugElement;
  conceptSelect: DebugElement;
  save: DebugElement;


  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(CreateConceptSetModalComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.conceptSetService = this.fixture.debugElement.injector.get(ConceptSetsService);
    this.workspacesService.getWorkspace(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID).subscribe((response: WorkspaceResponse) => {
    });
    tick();
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.workspaceNamespace = this.route[1].path;
    this.workspaceId = this.route[2].path;
    const de = this.fixture.debugElement;
    this.name = de.query(By.css('.input-name'));
    this.description = de.query(By.css('.input-description'));
    this.save = de.query(By.css('.btn-primary'));
    const selects = de.queryAll(By.css('.concept-select'));
    this.conceptSelect = selects[0];
    this.conceptList = [];

    if (selects && selects.length > 0) {
      selects[0].children.forEach((option) => {
        this.conceptList.push(option.nativeNode.childNodes[0].textContent.trim());
      });
    }
  }
}
const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspaces'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    }
  }
};

describe('ConceptSetAddComponent', () => {
  let conceptSetAddCreatePage: ConceptSetAddPage;
  const conceptServiceStub = new ConceptsServiceStub();
  const conceptSetServiceStub = new ConceptSetsServiceStub();
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        CreateConceptSetModalComponent
      ],
      providers: [
        {provide: ConceptsService, useValue: conceptServiceStub},
        {provide: ConceptSetsService, useValue: conceptSetServiceStub},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: WorkspacesService, useValue: new WorkspacesServiceStub() }
      ]
    }).compileComponents().then(() => {
      conceptSetAddCreatePage = new ConceptSetAddPage(TestBed);
    });
    tick();
  }));

  it('gets domain list', fakeAsync(() => {
    console.log(this.conceptSetAddCreatePage);
  }));

});

