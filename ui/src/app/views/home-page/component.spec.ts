import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from 'clarity-angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {HomePageComponent} from 'app/views/home-page/component';
import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {updateAndTick} from 'testing/test-helpers';

import {WorkspacesService} from 'generated';

class HomePage {
  fixture: ComponentFixture<HomePageComponent>;
  workspacesService: WorkspacesService;
  route: UrlSegment[];
  workspaceTableRows: DebugElement[];
  loggedOutMessage: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(HomePageComponent);
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
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
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() }
      ] }).compileComponents().then(() => {
        homePage = new HomePage(TestBed);
      });
      tick();
  }));


  it('displays correct number of workspaces in home-page', fakeAsync(() => {
    let expectedWorkspaces: number;
    homePage.workspacesService.getWorkspaces()
      .subscribe(workspaces => {
        expectedWorkspaces = workspaces.items.length;
    });
    tick();
    expect(homePage.workspaceTableRows.length).toEqual(expectedWorkspaces);
  }));

});
