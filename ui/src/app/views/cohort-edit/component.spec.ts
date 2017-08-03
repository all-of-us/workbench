import {TestBed, async, fakeAsync, tick} from '@angular/core/testing';
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
import { Observable } from 'rxjs/Observable';

class Workspace {
  id: string;
  namespace: string;
  cohorts: Cohort[];
}

function simulateInput(fixture: any, element: any, text: string) {
  element.value = text;
  element.dispatchEvent(new Event('input'));
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

  private getMatchingWorkspace(wsNamespace: string, wsId: string): Workspace {
    return this.workspaces.find(function(workspace: Workspace) {
      if (workspace.namespace === wsNamespace && workspace.id === wsId) {
        return true;
      } else {
        return false;
      }
    });
  }

  public getCohort(wsNamespace: string,
                   wsId: string,
                   cId: string): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspace(wsNamespace, wsId);
        if (workspaceMatch !== null) {
          observer.next(workspaceMatch.cohorts.find(function(cohort: Cohort) {
            if (cohort.id === cId) {
              return true;
            }
          }));
          observer.complete();
        }
        observer.error(new Error('Error fetching a cohort. No cohort with \
          id: ' + cId + ' exists in workspace ' + wsNamespace + ', ' + wsId +
          ' in cohort service stub.'));
      }, 0);
    });
    return observable;
  }

  public updateCohort(wsNamespace: string,
                      wsId: string,
                      cId: string,
                      newCohort: Cohort): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspace(wsNamespace, wsId);
        if (workspaceMatch !== null) {
          const index = workspaceMatch.cohorts.findIndex(function(cohort: Cohort) {
            if (cohort.id === cId) {
              return true;
            }
            return false;
          });
          if (index != null) {
            workspaceMatch.cohorts[index] = newCohort;
          }
          observer.complete();
        }
        observer.error(new Error('Error updating a cohort. No cohort with id: '
          + cId + ' exists in workspace ' + wsNamespace + ', ' + wsId +
          ' in cohort service stub.'));
      }, 0);
    });
    return observable;
  }

  public createCohort(wsNamespace: string,
                      wsId: string, newCohort: Cohort): Observable<Cohort> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspace(wsNamespace, wsId);
        if (workspaceMatch !== null) {
          observer.next(workspaceMatch.cohorts.find(function(cohort: Cohort) {
            if (cohort.id === newCohort.id) {
              observer.error(new Error('Error creating a cohort. Cohort with \
                id: ' + cohort.id + ' already exists.'));
              return true;
            }
          }));
          workspaceMatch.cohorts.push(newCohort);
          observer.complete();
        }
        observer.error(new Error('Error creating a cohort. No workspace '
          + wsNamespace + ', ' + wsId + ' in cohort service stub.'));
      }, 0);
    });
    return observable;
  }

  public getCohortsInWorkspace(wsNamespace: string,
                               wsId: string): Observable<CohortListResponse> {
    const observable = new Observable(observer => {
      setTimeout(() => {
        const workspaceMatch = this.getMatchingWorkspace(wsNamespace, wsId);
        if (workspaceMatch != null) {
          observer.next({items: workspaceMatch.cohorts});
          observer.complete();
        }
        observer.error('No workspace ' + wsNamespace + ', ' + wsId + ' found.');
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
    const fixture = TestBed.createComponent(CohortEditComponent);
    fixture.detectChanges();
    const app = fixture.debugElement.componentInstance;
    const nameField = fixture.debugElement.query(By.css('#name'));
    const descriptionField = fixture.debugElement.query(By.css('#description'));
    fixture.detectChanges();
    setTimeout(function(){
      expect(nameField.nativeNode.value).toMatch('');
      expect(descriptionField.nativeNode.value).toMatch('');
    }, 0);
  }));


  it('fetches and displays an existing cohort in the edit pane',
  fakeAsync(() => {
    const fixture = TestBed.createComponent(CohortEditComponent);
    const route = fixture.debugElement.injector.get(ActivatedRoute).snapshot;
    route.url[4].path = '1';
    route.url.push(new UrlSegment('edit', {}));

    updateAndTick(fixture);
    const app = fixture.debugElement.componentInstance;
    const nameField = fixture.debugElement.query(By.css('#name'));
    const descriptionField = fixture.debugElement.query(By.css('#description'));

    updateAndTick(fixture);

    expect(nameField.nativeElement.value).toMatch('sample name');
    expect(descriptionField.nativeElement.value).toMatch('sample description');
  }));

  it('adds a new cohort with given name and description', fakeAsync(() => {
    const fixture = TestBed.createComponent(CohortEditComponent);
    const nameField = fixture.debugElement.query(By.css('#name'));
    const descriptionField = fixture.debugElement.query(By.css('#description'));
    const route = fixture.debugElement.injector.get(ActivatedRoute).snapshot;
    route.url[4].path = 'create';
    updateAndTick(fixture);
    const buttons = fixture.debugElement.queryAll(By.css('button'));
    const addButton = buttons.find(button => {
      if (button.nativeElement.innerText === 'Add Cohort') {
        return true;
      } else {
        return false;
      }
    });
    simulateInput(fixture, nameField.nativeNode, 'New Cohort');
    simulateInput(fixture, descriptionField.nativeNode, 'New Description');
    addButton.triggerEventHandler('click', null);
    updateAndTick(fixture);
    fixture.debugElement.injector.get(CohortsService).getCohortsInWorkspace(
    WorkspaceComponent.DEFAULT_WORKSPACE_NS,
    WorkspaceComponent.DEFAULT_WORKSPACE_ID).subscribe((cohorts) => {
      expect(cohorts.items.length).toBe(2);
      expect(cohorts.items[1].name).toBe('New Cohort');
      expect(cohorts.items[1].description).toBe('New Description');
    });
    updateAndTick(fixture);
  }));

  it('edits an existing cohort with given name and description',
  fakeAsync(() => {
    const fixture = TestBed.createComponent(CohortEditComponent);
    const route = fixture.debugElement.injector.get(ActivatedRoute).snapshot;
    route.url[4].path = '1';
    route.url.push(new UrlSegment('edit', {}));
    updateAndTick(fixture);
    const app = fixture.debugElement.componentInstance;
    const nameField = fixture.debugElement.query(By.css('#name'));
    const descriptionField = fixture.debugElement.query(By.css('#description'));
    updateAndTick(fixture);
    const buttons = fixture.debugElement.queryAll(By.css('button'));
    const addButton = buttons.find(button => {
      if (button.nativeElement.innerText === 'Save Cohort') {
        return true;
      } else {
        return false;
      }
    });
    simulateInput(fixture, nameField.nativeNode, 'Edited Cohort');
    simulateInput(fixture, descriptionField.nativeNode, 'Edited Description');
    addButton.triggerEventHandler('click', null);
    updateAndTick(fixture);
    fixture.debugElement.injector.get(CohortsService).getCohortsInWorkspace(
    WorkspaceComponent.DEFAULT_WORKSPACE_NS,
    WorkspaceComponent.DEFAULT_WORKSPACE_ID).subscribe((cohorts) => {
      expect(cohorts.items.length).toBe(1);
      expect(cohorts.items[0].name).toBe('Edited Cohort');
      expect(cohorts.items[0].description).toBe('Edited Description');
    });
    updateAndTick(fixture);
  }));
});
