import {Component, DebugElement} from '@angular/core';
import {TestBed, async, fakeAsync, ComponentFixture} from '@angular/core/testing';
import {Title, By} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {RouterTestingModule} from '@angular/router/testing';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {WorkspaceComponent} from 'app/views/workspace/component';
<<<<<<< HEAD
import {updateAndTick, simulateInput} from 'testing/test-helpers';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
=======
import {updateAndTick, simulateInput} from 'test-files/test-helpers';
import {CohortsServiceStub} from 'test-files/stubs/cohort-service-stub';
>>>>>>> 89527a5940036d67d4f870c2d884c9e5dbb9d3d9
import {CohortsService} from 'generated';


class Context {
  fixture: ComponentFixture<CohortEditComponent>;
  route: UrlSegment[];
  cohortsService: CohortsService;
  nameField: DebugElement;
  descriptionField: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(CohortEditComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.cohortsService = this.fixture.debugElement.injector.get(CohortsService);
    this.nameField = this.fixture.debugElement.query(By.css('#name'));
    this.descriptionField = this.fixture.debugElement.query(By.css('#description'));
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

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule
      ],
      declarations: [
        CohortEditComponent
      ],
      providers: [
        { provide: CohortsService, useValue: new CohortsServiceStub() },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ] }).compileComponents();
  }));



  it('displays blank input fields when creating a new cohort', async(() => {
    const context = new Context(TestBed);
    context.fixture.detectChanges();
    expect(context.nameField.nativeNode.value).toMatch('');
    expect(context.descriptionField.nativeNode.value).toMatch('');
  }));


  it('fetches and displays an existing cohort in the edit pane',
  fakeAsync(() => {
    const context = new Context(TestBed);
    context.route[4].path = '1';
    context.route.push(new UrlSegment('edit', {}));
    updateAndTick(context.fixture);
    updateAndTick(context.fixture);
    expect(context.nameField.nativeElement.value).toMatch('sample name');
    expect(context.descriptionField.nativeElement.value).toMatch('sample description');
  }));

  it('adds a new cohort with given name and description', fakeAsync(() => {
    const context = new Context(TestBed);
    context.route[4].path = 'create';
    updateAndTick(context.fixture);
    simulateInput(context.fixture, context.nameField, 'New Cohort');
    simulateInput(context.fixture, context.descriptionField, 'New Description');
    const addButton = context.fixture.debugElement.query(By.css('#add'));
    addButton.triggerEventHandler('click', null);
    updateAndTick(context.fixture);
    context.cohortsService.getCohortsInWorkspace(
    WorkspaceComponent.DEFAULT_WORKSPACE_NS,
    WorkspaceComponent.DEFAULT_WORKSPACE_ID).subscribe((cohorts) => {
      expect(cohorts.items.length).toBe(2);
      expect(cohorts.items[1].name).toBe('New Cohort');
      expect(cohorts.items[1].description).toBe('New Description');
    });
    updateAndTick(context.fixture);
  }));

  it('edits an existing cohort with given name and description',
  fakeAsync(() => {
    const context = new Context(TestBed);
    context.route[4].path = '1';
    context.route.push(new UrlSegment('edit', {}));
    updateAndTick(context.fixture);
    const saveButton = context.fixture.debugElement.query(By.css('#save'));
    simulateInput(context.fixture, context.nameField, 'Edited Cohort');
    simulateInput(context.fixture, context.descriptionField, 'Edited Description');
    saveButton.triggerEventHandler('click', null);
    updateAndTick(context.fixture);
    context.cohortsService.getCohortsInWorkspace(
    WorkspaceComponent.DEFAULT_WORKSPACE_NS,
    WorkspaceComponent.DEFAULT_WORKSPACE_ID).subscribe((cohorts) => {
      expect(cohorts.items.length).toBe(1);
      expect(cohorts.items[0].name).toBe('Edited Cohort');
      expect(cohorts.items[0].description).toBe('Edited Description');
    });
    updateAndTick(context.fixture);
  }));
});
