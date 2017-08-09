import {Component, DebugElement} from '@angular/core';
import {TestBed, async, fakeAsync, ComponentFixture} from '@angular/core/testing';
import {Title, By} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {updateAndTick, simulateInput} from 'testing/test-helpers';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {CohortsService} from 'generated';
import {UserService} from 'app/services/user.service';
import {RepositoryService} from 'app/services/repository.service';

class Context {
  fixture: ComponentFixture<WorkspaceComponent>;
  cohortsService: CohortsService;
  userService: UserService;
  repositoryService: RepositoryService;
  cohortsTableRows: DebugElement[];
  notebookTableRows: DebugElement[];
  cdrText: DebugElement;
  workspaceDescription: DebugElement;
  loggedOutMessage: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(WorkspaceComponent);
    this.cohortsService = this.fixture.debugElement.injector.get(CohortsService);
    this.userService = this.fixture.debugElement.injector.get(UserService);
    this.repositoryService = this.fixture.debugElement.injector.get(RepositoryService);
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.readPageData();
  }

  readPageData() {
    this.cohortsTableRows = this.fixture.debugElement.queryAll(By.css('.cohort-table-row'));
    this.notebookTableRows = this.fixture.debugElement.queryAll(By.css('.notebook-table-row'));
    this.cdrText = this.fixture.debugElement.query(By.css('#cdr-text'));
    this.workspaceDescription = this.fixture.debugElement.query(By.css('.description-text'));
    this.loggedOutMessage = this.fixture.debugElement.query(By.css('#logged-out-message'));
  }
}


describe('WorkspaceComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule
      ],
      declarations: [
        WorkspaceComponent
      ],
      providers: [
        { provide: CohortsService, useValue: new CohortsServiceStub() },
        { provide: UserService, useValue: new UserService() },
        { provide: RepositoryService, useValue: new RepositoryService() }
      ] }).compileComponents();
  }));


  it('displays correct information in default workspace', fakeAsync(() => {
    const context = new Context(TestBed);
    expect(context.cohortsTableRows.length).toEqual(1);
    expect(context.notebookTableRows.length).toEqual(2);
    expect(context.cdrText.nativeElement.innerText)
      .toMatch('CDR version info goes here.');
    expect(context.workspaceDescription.nativeElement.innerText)
      .toMatch('Default workspace for July, 2017 demo');
  }));

  it('displays correct information when logged out', fakeAsync(() => {
    const context = new Context(TestBed);
    context.userService.logOut();
    context.userService.getLoggedInUser().then((user) => {
      updateAndTick(context.fixture);
      updateAndTick(context.fixture);
      context.fixture.componentRef.instance.ngOnInit();
      updateAndTick(context.fixture);
      updateAndTick(context.fixture);
      context.readPageData();
      expect(context.loggedOutMessage.nativeElement.innerText)
        .toMatch('Log in to view workspace.');
    });
  }));
});
