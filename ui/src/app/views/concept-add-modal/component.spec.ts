import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {
   ConceptsService, Domain, WorkspaceResponse,
  WorkspacesService
} from 'generated';
import {ConceptSetsService} from 'generated/api/conceptSets.service';
import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {
  ConceptsServiceStub, ConceptStubVariables
} from 'testing/stubs/concepts-service-stub';
import {
  WorkspacesServiceStub,
  WorkspaceStubVariables
} from 'testing/stubs/workspace-service-stub';
import {
  simulateClick, simulateEvent,
  updateAndTick
} from 'testing/test-helpers';
import {ConceptAddModalComponent} from './component';

class ConceptSetAddPage {
  fixture: ComponentFixture<ConceptAddModalComponent>;
  conceptSetService: ConceptSetsService;
  workspacesService: WorkspacesService;
  workspaceNamespace: string;
  workspaceId: string;
  conceptList: string[] = [];
  conceptSelect: DebugElement;
  save: DebugElement;
  formSections: DebugElement[] = [];

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(ConceptAddModalComponent);
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.conceptSetService = this.fixture.debugElement.injector.get(ConceptSetsService);
    this.workspacesService.getWorkspace(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID).subscribe((response: WorkspaceResponse) => {});
    tick();
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    const de = this.fixture.debugElement;
    this.save = de.query(By.css('.btn-primary'));
    const selects = de.queryAll(By.css('.concept-select'));
    this.conceptSelect = selects[0];
    this.conceptList = [];

    if (selects && selects.length > 0) {
      selects[0].children.forEach((option) => {
        this.conceptList.push(option.nativeNode.childNodes[0].textContent.trim());
      });
    }
    this.formSections = de.queryAll(By.css('.form-section'));

  }
}

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
        ConceptAddModalComponent
      ],
      providers: [
        {provide: ConceptsService, useValue: conceptServiceStub},
        {provide: ConceptSetsService, useValue: conceptSetServiceStub},
        {provide: WorkspacesService, useValue: new WorkspacesServiceStub() }
      ]
    }).compileComponents().then(() => {
      conceptSetAddCreatePage = new ConceptSetAddPage(TestBed);
    });
    tick();
  }));

  it('gets concept set list filtered by domain selected', fakeAsync(() => {
    conceptSetAddCreatePage.fixture.componentInstance.selectedDomain = Domain.CONDITION;
    conceptSetAddCreatePage.fixture.componentInstance.selectedConcepts =
        ConceptStubVariables.STUB_CONCEPTS;
    conceptSetAddCreatePage.fixture.componentInstance.open();
    conceptSetAddCreatePage.readPageData();
    tick();
    expect(conceptSetAddCreatePage.conceptList.length).toBe(2);
  }));

  it('displays option to add to existing concept set if concept set exist', fakeAsync(() => {
    conceptSetAddCreatePage.fixture.componentInstance.selectedDomain = Domain.CONDITION;
    conceptSetAddCreatePage.fixture.componentInstance.selectedConcepts =
        ConceptStubVariables.STUB_CONCEPTS;
    conceptSetAddCreatePage.fixture.componentInstance.open();
    conceptSetAddCreatePage.readPageData();
    tick();
    expect(conceptSetAddCreatePage.formSections.length).toBe(2);
  }));

  it('disables option to add to existing if concept set does not exist',
    fakeAsync(() => {
      conceptSetAddCreatePage.fixture.componentInstance.selectedDomain = Domain.DRUG;
      conceptSetAddCreatePage.fixture.componentInstance.selectedConcepts =
        ConceptStubVariables.STUB_CONCEPTS;
      conceptSetAddCreatePage.fixture.componentInstance.open();
      conceptSetAddCreatePage.readPageData();
      tick();
      expect(conceptSetAddCreatePage.formSections.length).toBe(2);
      expect(conceptSetAddCreatePage.fixture.debugElement
        .query(By.css('#select-add')).properties['disabled']).toBeTruthy();
    }));

  it('selects Add to Existing option by default'
      , fakeAsync(() => {
        conceptSetAddCreatePage.fixture.componentInstance.selectedDomain = Domain.CONDITION;
        conceptSetAddCreatePage.fixture.componentInstance.selectedConcepts =
        ConceptStubVariables.STUB_CONCEPTS;
        conceptSetAddCreatePage.fixture.componentInstance.open();
        conceptSetAddCreatePage.readPageData();
        const selects = conceptSetAddCreatePage.fixture.debugElement
        .queryAll(By.css('#select-add'));
        simulateClick(conceptSetAddCreatePage.fixture, selects[0]);
        conceptSetAddCreatePage.readPageData();
        expect(conceptSetAddCreatePage.conceptSelect.children[0].name).toBe('option');
        expect(conceptSetAddCreatePage.conceptSelect.children[1].name).toBe('option');
      }));

  it('on selecting create new option, only name and description field should be displayed'
      , fakeAsync(() => {
        conceptSetAddCreatePage.fixture.componentInstance.selectedDomain = Domain.CONDITION;
        conceptSetAddCreatePage.fixture.componentInstance.selectedConcepts =
            ConceptStubVariables.STUB_CONCEPTS;
        conceptSetAddCreatePage.fixture.componentInstance.open();
        conceptSetAddCreatePage.readPageData();
        const selects = conceptSetAddCreatePage.fixture.debugElement
            .queryAll(By.css('#select-create'));
        selects[0].triggerEventHandler('change', { target: selects[0].nativeElement });
        simulateEvent(conceptSetAddCreatePage.fixture, selects[0], 'click');
        conceptSetAddCreatePage.readPageData();
        const de = conceptSetAddCreatePage.fixture.debugElement;
        const name = de.queryAll(By.css('.inputName'));
        const description = de.queryAll(By.css('.description'));
        expect(name).not.toBeNull();
        expect(description).not.toBeNull();
      }));
});
