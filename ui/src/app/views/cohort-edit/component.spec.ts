import {Component, DebugElement} from '@angular/core';
import {TestBed, async, tick, fakeAsync, ComponentFixture} from '@angular/core/testing';
import {Title, By} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {RouterTestingModule} from '@angular/router/testing';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {updateAndTick, simulateInput} from 'testing/test-helpers';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {CohortsService} from 'generated';
import {ClarityModule} from 'clarity-angular';


class CohortEditPage {
  fixture: ComponentFixture<CohortEditComponent>;
  route: UrlSegment[];
  cohortsService: CohortsService;
  nameField: DebugElement;
  descriptionField: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(CohortEditComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.cohortsService = this.fixture.debugElement.injector.get(CohortsService);
    this.nameField = this.fixture.debugElement.query(By.css('.name'));
    this.descriptionField = this.fixture.debugElement.query(By.css('.description'));
  }
}

const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspace'},
      {path: WorkspaceComponent.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceComponent.DEFAULT_WORKSPACE_ID},
      {path: 'cohorts'},
      {path: 'create'}
    ]
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
    cohortEditPage.route[4].path = '1';
    cohortEditPage.route.push(new UrlSegment('edit', {}));
    updateAndTick(cohortEditPage.fixture);
    updateAndTick(cohortEditPage.fixture);
    expect(cohortEditPage.nameField.nativeElement.value).toMatch('sample name');
    expect(cohortEditPage.descriptionField.nativeElement.value).toMatch('sample description');
  }));

  it('adds a new cohort with given name and description', fakeAsync(() => {
    cohortEditPage.route[4].path = 'create';
    updateAndTick(cohortEditPage.fixture);
    simulateInput(cohortEditPage.fixture, cohortEditPage.nameField, 'New Cohort');
    simulateInput(cohortEditPage.fixture, cohortEditPage.descriptionField, 'New Description');
    const addButton = cohortEditPage.fixture.debugElement.query(By.css('.add-button'));
    addButton.triggerEventHandler('click', null);
    updateAndTick(cohortEditPage.fixture);
    cohortEditPage.cohortsService.getCohortsInWorkspace(
    WorkspaceComponent.DEFAULT_WORKSPACE_NS,
    WorkspaceComponent.DEFAULT_WORKSPACE_ID).subscribe((cohorts) => {
      expect(cohorts.items.length).toBe(2);
      expect(cohorts.items[1].name).toBe('New Cohort');
      expect(cohorts.items[1].description).toBe('New Description');
    });
    updateAndTick(cohortEditPage.fixture);
  }));

  it('edits an existing cohort with given name and description',
  fakeAsync(() => {
    cohortEditPage.route[4].path = '1';
    cohortEditPage.route.push(new UrlSegment('edit', {}));
    updateAndTick(cohortEditPage.fixture);
    const saveButton = cohortEditPage.fixture.debugElement.query(By.css('.save-button'));
    simulateInput(cohortEditPage.fixture, cohortEditPage.nameField, 'Edited Cohort');
    simulateInput(cohortEditPage.fixture, cohortEditPage.descriptionField, 'Edited Description');
    saveButton.triggerEventHandler('click', null);
    updateAndTick(cohortEditPage.fixture);
    cohortEditPage.cohortsService.getCohortsInWorkspace(
    WorkspaceComponent.DEFAULT_WORKSPACE_NS,
    WorkspaceComponent.DEFAULT_WORKSPACE_ID).subscribe((cohorts) => {
      expect(cohorts.items.length).toBe(1);
      expect(cohorts.items[0].name).toBe('Edited Cohort');
      expect(cohorts.items[0].description).toBe('Edited Description');
    });
    updateAndTick(cohortEditPage.fixture);
  }));
});
