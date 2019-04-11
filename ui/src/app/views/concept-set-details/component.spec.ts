import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import { HighlightSearchComponent } from 'app/highlight-search/highlight-search.component';
import {EditComponent} from 'app/icons/edit/component';
import {currentConceptSetStore, currentWorkspaceStore, NavStore, urlParamsStore} from 'app/utils/navigation';
import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptSetDetailsComponent} from 'app/views/concept-set-details/component';
import {ConceptTableComponent} from 'app/views/concept-table/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {SlidingFabComponent} from 'app/views/sliding-fab/component';
import {TopBoxComponent} from 'app/views/top-box/component';

import {
  ConceptSet,
  ConceptSetsService,
  Domain,
  WorkspaceAccessLevel,
} from 'generated';
import {ConceptSet as FetchConceptSet} from 'generated/fetch';

import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {ConceptStubVariables} from 'testing/stubs/concepts-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {
  findElements,
  simulateClick as simulateClickReact,
  simulateClickNthElement
} from 'testing/react-test-helpers';

import {
  setupModals,
  simulateClick,
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

describe('ConceptSetDetailsComponent', () => {
  let fixture: ComponentFixture<ConceptSetDetailsComponent>;
  let conceptSetsStub: ConceptSetsServiceStub;
  beforeEach(fakeAsync(() => {
    conceptSetsStub = new ConceptSetsServiceStub([]);
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
        ConceptSetDetailsComponent,
        ConceptTableComponent,
        ConfirmDeleteModalComponent,
        EditComponent,
        HighlightSearchComponent,
        SlidingFabComponent,
        TopBoxComponent,
      ],
      providers: [
        { provide: ConceptSetsService, useValue: conceptSetsStub },
      ]}).compileComponents();
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      csid: 123
    });
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  }));

  function newConceptSet(): ConceptSet {
    return {
      id: 123,
      name: 'Concept Set 1',
      domain: Domain.CONDITION,
      description: 'Lorem ipsum',
      creator: 'calbach',
      creationTime: 0,
      lastModifiedTime: 0,
      concepts: ConceptStubVariables.STUB_CONCEPTS
    };
  }

  function setUpComponent(conceptSet?: ConceptSet) {
    if (!conceptSet) {
      conceptSet = newConceptSet();
    }
    currentConceptSetStore.next(conceptSet as unknown as FetchConceptSet);
    if (!conceptSetsStub.conceptSets.length) {
      conceptSetsStub.conceptSets = [conceptSet];
    }

    fixture = TestBed.createComponent(ConceptSetDetailsComponent);
    setupModals(fixture);
    // This tick initializes the component.
    tick();
    // This finishes the API calls.
    updateAndTick(fixture);
    // This finishes the page reloading.
    updateAndTick(fixture);
  }

  function findAddConceptsButtons(de: DebugElement) {
    return de.queryAll(By.css('.nav-button'))
      .filter(b => b.nativeElement.textContent.includes('Add concepts'));
  }

  it('should render concept table', fakeAsync(() => {
    setUpComponent();
    const de = fixture.debugElement;
    expect(de.queryAll(By.css('app-concept-table')).length).toEqual(1);
    expect(findAddConceptsButtons(de).length).toEqual(1);
  }));

  it('should render empty concepts state', fakeAsync(() => {
    setUpComponent({
      ...newConceptSet(),
      concepts: []
    });
    const de = fixture.debugElement;
    expect(de.queryAll(By.css('app-concept-table')).length).toEqual(0);
    expect(findAddConceptsButtons(de).length).toEqual(2);
  }));

  it('should allow validLength edits', fakeAsync(() => {
    setUpComponent();
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.edit-icon')));

    const newName = 'cool new name';
    const newDesc = 'cool new description';
    simulateInput(fixture, de.query(By.css('.edit-name')), newName);
    simulateInput(fixture, de.query(By.css('.edit-description')), newDesc);
    simulateClick(fixture, de.query(By.css('.submit-edit-button')));
    updateAndTick(fixture);

    expect(de.query(By.css('.concept-set-name')).nativeElement.textContent)
      .toContain(newName);
    expect(de.query(By.css('.concept-set-details')).nativeElement.textContent)
      .toContain(newDesc);

    expect(conceptSetsStub.conceptSets[0].name).toEqual(newName);
    expect(conceptSetsStub.conceptSets[0].description).toEqual(newDesc);
  }));

  it('should disallow empty name edit', fakeAsync(() => {
    const cs = newConceptSet();
    setUpComponent(cs);
    const originalName = cs.name;

    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.edit-icon')));

    simulateInput(fixture, de.query(By.css('.edit-name')), '');
    simulateClick(fixture, de.query(By.css('.submit-edit-button')));
    updateAndTick(fixture);

    // Edit button is still shown.
    expect(de.queryAll(By.css('.submit-edit-button')).length).toEqual(1);
    expect(de.query(By.css('.edit-buttons')).nativeElement.textContent)
      .toContain('name is required');
    // Name didn't change.
    expect(conceptSetsStub.conceptSets[0].name).toEqual(originalName);
  }));

  it('should not edit on cancel', fakeAsync(() => {
    const cs = newConceptSet();
    setUpComponent(cs);
    const originalName = cs.name;
    const originalDesc = cs.description;

    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.edit-icon')));

    simulateInput(fixture, de.query(By.css('.edit-name')), 'falco');
    simulateInput(fixture, de.query(By.css('.edit-description')), 'lombardi');
    simulateClick(fixture, de.query(By.css('.cancel-edit-button')));
    updateAndTick(fixture);

    // Edit button is no longer shown.
    expect(de.queryAll(By.css('.submit-edit-button')).length).toEqual(0);
    // Content didn't change.
    expect(conceptSetsStub.conceptSets[0].name).toEqual(originalName);
    expect(conceptSetsStub.conceptSets[0].description).toEqual(originalDesc);
  }));

  it('should allow edits via the action menu', fakeAsync(() => {
    setUpComponent();

    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.dropdown-toggle')));
    simulateClick(fixture, de.query(By.css('.action-edit')));

    const newName = 'cool new name';
    simulateInput(fixture, de.query(By.css('.edit-name')), newName);
    simulateClick(fixture, de.query(By.css('.submit-edit-button')));
    updateAndTick(fixture);

    expect(de.query(By.css('.concept-set-name')).nativeElement.textContent)
      .toContain(newName);

    expect(conceptSetsStub.conceptSets[0].name).toEqual(newName);
  }));

  it('should delete via action menu', fakeAsync(() => {
    NavStore.navigate = jasmine.createSpy('navigate');
    setUpComponent();

    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.dropdown-toggle')));
    simulateClick(fixture, de.query(By.css('.action-delete')));
    simulateClickReact(fixture, '[data-test-id="confirm-delete"]');

    expect(NavStore.navigate).toHaveBeenCalled();
    expect(conceptSetsStub.conceptSets).toEqual([]);
  }));

  it('should remove concepts', fakeAsync(() => {
    // Start with 3 concepts, delete two.
    const origConcepts = ConceptStubVariables.STUB_CONCEPTS.slice(0, 3);
    setUpComponent({
      ...newConceptSet(),
      concepts: origConcepts.slice()
    });

    const de = fixture.debugElement;
    simulateClickNthElement(fixture, 'span.p-checkbox-icon.p-clickable', 1);
    updateAndTick(fixture);
    simulateClickNthElement(fixture, 'span.p-checkbox-icon.p-clickable', 3);
    simulateClickReact(fixture, '[data-test-id="sliding-button"]');
    simulateClick(fixture, de.query(By.css('.confirm-remove-btn')));
    updateAndTick(fixture);
    const tableRows = findElements(fixture, 'tr');
    // This includes the header and the row itseld
    expect(tableRows.length).toEqual(2);
    // Just the middle concept should remain.
    const wantConcepts = [origConcepts[1]];
    expect(tableRows[1].childNodes[2].textContent).toEqual(
      wantConcepts[0].conceptSynonyms.join(', '));
  }));
});
