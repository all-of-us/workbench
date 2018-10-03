import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {EditComponent} from 'app/icons/edit/component';
import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptSetDetailsComponent} from 'app/views/concept-set-details/component';
import {ConceptTableComponent} from 'app/views/concept-table/component';
import {SlidingFabComponent} from 'app/views/sliding-fab/component';
import {TopBoxComponent} from 'app/views/top-box/component';

import {HighlightSearchPipe} from 'app/utils/highlight-search.pipe';


import {
  ConceptsService,
  ConceptSet,
  ConceptSetsService,
  Domain,
  StandardConceptFilter,
  WorkspaceAccessLevel,
} from 'generated';

import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {ConceptsServiceStub, ConceptStubVariables, DomainStubVariables} from 'testing/stubs/concepts-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {simulateClick, simulateInput, updateAndTick} from 'testing/test-helpers';

describe('ConceptSetDetailsComponent', () => {
  let fixture: ComponentFixture<ConceptSetDetailsComponent>;
  let routeStub: any;
  beforeEach(fakeAsync(() => {
    routeStub = {
      snapshot: {
        url: [
          {path: 'workspaces'},
          {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
          {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
          {path: 'concepts'},
          {path: '123'}
        ],
        params: {
          'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
          'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
          'csid': 123
        },
        data: {
          workspace: {
            ...WorkspacesServiceStub.stubWorkspace(),
            accessLevel: WorkspaceAccessLevel.OWNER,
          },
          conceptSet: newConceptSet()
        }
      }
    };
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
        EditComponent,
        HighlightSearchPipe,
        SlidingFabComponent,
        TopBoxComponent,
      ],
      providers: [
        { provide: ConceptsService, useValue: new ConceptsServiceStub() },
        { provide: ConceptSetsService, useValue: new ConceptSetsServiceStub() },
        { provide: ActivatedRoute, useFactory: () => routeStub }
      ]}).compileComponents().then(() => {
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
    }
  }

  function setUpComponent(conceptSet?: ConceptSet) {
    if (!conceptSet) {
      conceptSet = newConceptSet();
    }
    routeStub.snapshot.data.conceptSet = conceptSet;

    fixture = TestBed.createComponent(ConceptSetDetailsComponent);
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

  fit('should render concept table', fakeAsync(() => {
    setUpComponent();
    const de = fixture.debugElement;
    expect(de.queryAll(By.css('app-concept-table')).length).toEqual(1);
    expect(findAddConceptsButtons(de).length).toEqual(1);
  }));

  fit('should render empty concepts state', fakeAsync(() => {
    setUpComponent({
      ...newConceptSet(),
      concepts: []
    });
    const de = fixture.debugElement;
    expect(de.queryAll(By.css('app-concept-table')).length).toEqual(0);
    expect(findAddConceptsButtons(de).length).toEqual(2);
  }));
});
