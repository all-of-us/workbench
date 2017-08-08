import {Component, DebugElement} from '@angular/core';
import {TestBed, async, fakeAsync, tick, ComponentFixture} from '@angular/core/testing';
import {Title, By} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {RouterTestingModule} from '@angular/router/testing';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {SignInService} from 'app/services/sign-in.service';
import {WorkspaceComponent} from 'app/views/workspace/component';

import {Cohort, CohortListResponse} from 'generated';
import {CohortsService} from 'generated';
import {Observable} from 'rxjs/Observable';
import {Observer} from 'rxjs/Observer';

// TODO: Replace with Swagger-generated model
class Workspace {
  id: string;
  namespace: string;
  cohorts: Cohort[];
}

class Context {
  fixture: ComponentFixture<CohortEditComponent>;
  route: UrlSegment[];
  cohortsService: CohortsService;
  nameField: DebugElement;
  descriptionField: DebugElement;
  addButton: DebugElement;
  saveButton: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(CohortEditComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.cohortsService = this.fixture.debugElement.injector.get(CohortsService);
    this.nameField = this.fixture.debugElement.query(By.css('#name'));
    this.descriptionField = this.fixture.debugElement.query(By.css('#description'));
  }
}

function simulateInput(
    fixture: ComponentFixture<Component>,
    element: DebugElement,
    text: string) {
  element.nativeNode.value = text;
  element.nativeNode.dispatchEvent(new Event('input'));
  updateAndTick(fixture);
}

function updateAndTick(fixture: any) {
  fixture.detectChanges();
  tick();
}

class CohortsServiceStub {
  constructor() {
    const stubWorkspace = new Workspace();
    stubWorkspace.id = WorkspaceComponent.DEFAULT_WORKSPACE_ID;
    stubWorkspace.namespace = WorkspaceComponent.DEFAULT_WORKSPACE_NS;

    const exampleCohort: Cohort = {id: '',
      name: '', description: '', criteria: '', type: ''};
    exampleCohort.id = '1';
    exampleCohort.name = 'sample name';
    exampleCohort.description = 'sample description';

    stubWorkspace.cohorts = [exampleCohort];
    this.workspaces = [stubWorkspace];
  }
  public workspaces: Workspace[];

  private getMatchingWorkspaceOrSendError(
      wsNamespace: string,
      wsId: string,
      observer: Observer<{}>): Workspace {
    const workspaceFound = this.workspaces.find(function(workspace: Workspace) {
      if (workspace.namespace === wsNamespace && workspace.id === wsId) {
        return true;
      } else {
        return false;
      }
    });
    if (workspaceFound === undefined) {
      observer.error(`Error Searching. No workspace ${wsNamespace}, ${wsId} found `
                    + 'in cohort service stub.');
    }
    return workspaceFound;
  }

  public getCohort(
      wsNamespace: string,
      wsId: string,
      cId: string): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspaceOrSendError(wsNamespace, wsId, observer);
        if (workspaceMatch !== undefined) {
          observer.next(workspaceMatch.cohorts.find(function(cohort: Cohort) {
            if (cohort.id === cId) {
              return true;
            }
          }));
          observer.complete();
        }
      }, 0);
    });
    return observable;
  }

  public updateCohort(
      wsNamespace: string,
      wsId: string,
      cId: string,
      newCohort: Cohort): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspaceOrSendError(wsNamespace, wsId, observer);
        if (workspaceMatch !== undefined) {
          const index = workspaceMatch.cohorts.findIndex(function(cohort: Cohort) {
            if (cohort.id === cId) {
              return true;
            }
            return false;
          });
          if (index !== -1) {
            workspaceMatch.cohorts[index] = newCohort;
            observer.complete();
          } else {
            observer.error(new Error(`Error updating. No cohort with id: ${cId} `
                                    + `exists in workspace ${wsNamespace}, ${wsId} `
                                    + `in cohort service stub`));
          }
        }
      }, 0);
    });
    return observable;
  }

  public createCohort(
      wsNamespace: string,
      wsId: string,
      newCohort: Cohort): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspaceOrSendError(wsNamespace, wsId, observer);
        if (workspaceMatch !== undefined) {
          observer.next(workspaceMatch.cohorts.find(function(cohort: Cohort) {
            if (cohort.id === newCohort.id) {
              observer.error(new Error(`Error creating. Cohort with `
                                      + `id: ${cohort.id} already exists.`));
              return true;
            }
          }));
          workspaceMatch.cohorts.push(newCohort);
          observer.complete();
        }
      }, 0);
    });
    return observable;
  }

  public getCohortsInWorkspace(
      wsNamespace: string,
      wsId: string): Observable<CohortListResponse> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspaceOrSendError(wsNamespace, wsId, observer);
        if (workspaceMatch !== undefined) {
          observer.next({items: workspaceMatch.cohorts});
          observer.complete();
        }
      }, 0);
    });
    return observable;
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
