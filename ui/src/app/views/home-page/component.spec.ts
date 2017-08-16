import {Component, DebugElement} from '@angular/core';
import {TestBed, async, tick, fakeAsync, ComponentFixture} from '@angular/core/testing';
import {Title, By} from '@angular/platform-browser';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {FormsModule} from '@angular/forms';
import {HomePageComponent} from 'app/views/home-page/component';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {updateAndTick, simulateInput} from 'testing/test-helpers';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspacesService} from 'generated';
import {UserService} from 'app/services/user.service';
import {RepositoryService} from 'app/services/repository.service';
import {ClarityModule} from 'clarity-angular';

class HomePage {
  fixture: ComponentFixture<HomePageComponent>;
  workspacesService: WorkspacesService;
  userService: UserService;
  repositoryService: RepositoryService;
  route: UrlSegment[];
  workspaceTableRows: DebugElement[];
  loggedOutMessage: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(HomePageComponent);
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.userService = this.fixture.debugElement.injector.get(UserService);
    this.repositoryService = this.fixture.debugElement.injector.get(RepositoryService);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.workspaceTableRows = this.fixture.debugElement.queryAll(By.css('.workspace-table-row'));
    this.loggedOutMessage = this.fixture.debugElement.query(By.css('.logged-out-message'));
  }
}


describe('HomePageComponent', () => {
  let homePage: HomePage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        HomePageComponent
      ],
      providers: [
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: UserService, useValue: new UserService() },
        { provide: RepositoryService, useValue: new RepositoryService() }
      ] }).compileComponents().then(() => {
        homePage = new HomePage(TestBed);
      });
      tick();
  }));


  it('displays correct number of workspaces in home-page', fakeAsync(() => {
    let expectedWorkspaces: number;
    homePage.workspacesService.getWorkspaces()
      .subscribe(workspaces => {
        console.log(workspaces);
        console.log(workspaces.items);
      expectedWorkspaces = workspaces.items.length;
    });
    tick();
    expect(homePage.workspaceTableRows.length).toEqual(expectedWorkspaces);
  }));

  it('displays login prompt when logged out', fakeAsync(() => {
    homePage.userService.logOut();
    homePage.userService.getLoggedInUser().then((user) => {
      updateAndTick(homePage.fixture);
      updateAndTick(homePage.fixture);
      homePage.fixture.componentRef.instance.ngOnInit();
      homePage.readPageData();
      expect(homePage.loggedOutMessage.nativeElement.innerText)
        .toMatch('Log in to view workspace.');
    });
  }));


});
