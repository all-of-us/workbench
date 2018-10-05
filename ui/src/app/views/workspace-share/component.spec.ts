import {DebugElement, Type} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';

import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {
  simulateClick,
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

import {
  UserRole,
  UserService,
  WorkspaceAccessLevel,
  WorkspaceResponse,
  WorkspacesService
} from 'generated';
import {UserServiceStub} from '../../../testing/stubs/user-service-stub';

interface UserRoleRow {
  fullName: string;
  email: string;
  role: string;
}

class WorkspaceSharePage {
  fixture: ComponentFixture<WorkspaceShareComponent>;
  workspacesService: WorkspacesService;
  route: UrlSegment[];
  workspaceNamespace: string;
  workspaceId: string;
  roleNamePairsOnPage: Array<UserRoleRow>;
  emailField: DebugElement;
  permissionsField: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(WorkspaceShareComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.fixture.componentRef.instance.sharing = true;

    this.workspacesService.getWorkspace(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID).subscribe((response: WorkspaceResponse) => {
        this.fixture.componentInstance.workspace = response.workspace;
    });
    tick();
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);

    this.workspaceNamespace = this.route[1].path;
    this.workspaceId = this.route[2].path;
    const de = this.fixture.debugElement;
    const setOfUsers = de.queryAll(By.css('.collaborator'));
    this.roleNamePairsOnPage = [];
    setOfUsers.forEach((user) => {
      this.roleNamePairsOnPage.push({
        fullName: user.children[0].nativeElement.innerText,
        email: user.children[1].nativeElement.innerText,
        role: user.children[2].queryAll(By.css('.roles'))[0].properties.value
      });
    });
    this.emailField = de.query(By.css('.no-border'));
    this.fixture.componentRef.instance.input = this.emailField;
    this.permissionsField = de.query(By.css('.permissions-button'));
  }
}

const activatedRouteStub = {
  snapshot: {
    url: [
      { path: 'workspaces'},
      { path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      { path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    }
  }
};

const userValuesStub = {
  workspaceEtag: undefined,
  items: [
    {
      email: 'sampleuser1@fake-research-aou.org',
      role: 'OWNER',
      name: 'Sample User1'
    },
    {
      email: 'sampleuser2@fake-research-aou.org',
      role: 'WRITER',
      name: 'Sample User2'
    },
    {
      email: 'sampleuser3@fake-research-aou.org',
      role: 'READER',
      name: 'Sample User3'
    },
    {
      email: 'sampleuser4@fake-research-aou.org',
      role: 'WRITER',
      name: 'Sample User4'
    }
  ]
};

function convertToUserRoleRow(userRoles: UserRole[]): UserRoleRow[] {
  const roleNamePairs: UserRoleRow[] = [];
  userRoles.forEach((userRole) => {
    roleNamePairs.push({
      fullName: userRole.givenName + ' ' + userRole.familyName,
      email: userRole.email,
      role: userRole.role.toString()
    });
  });

  return roleNamePairs;
}

describe('WorkspaceShareComponent', () => {
  let workspaceSharePage: WorkspaceSharePage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        WorkspaceShareComponent
      ],
      providers: [
        { provide: UserService, useValue: new UserServiceStub()},
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub()},
        { provide: ActivatedRoute, useValue: activatedRouteStub},
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }
      ]
    }).compileComponents().then(() => {
      workspaceSharePage = new WorkspaceSharePage(TestBed);
      workspaceSharePage.fixture.componentInstance.accessLevel = WorkspaceAccessLevel.OWNER;
      workspaceSharePage.fixture.componentInstance.open();
      workspaceSharePage.fixture.componentInstance.profileStorageService.reload();
    });
    tick();
  }));


  it('displays correct information in default workspace sharing', fakeAsync(() => {
    workspaceSharePage.readPageData();
    expect(workspaceSharePage.roleNamePairsOnPage.length).toEqual(3);
    expect(workspaceSharePage.roleNamePairsOnPage[0].email).toBe(userValuesStub.items[0].email);
    expect(workspaceSharePage.roleNamePairsOnPage[0].role)
        .toBe(userValuesStub.items[0].role.toLocaleString());
    expect(workspaceSharePage.roleNamePairsOnPage[0].fullName).toBe(userValuesStub.items[0].name);

    expect(workspaceSharePage.roleNamePairsOnPage[1].email).toBe(userValuesStub.items[1].email);
    expect(workspaceSharePage.roleNamePairsOnPage[1].role)
        .toBe(userValuesStub.items[1].role.toLocaleString());
    expect(workspaceSharePage.roleNamePairsOnPage[1].fullName).toBe(userValuesStub.items[1].name);


    expect(workspaceSharePage.roleNamePairsOnPage[2].email).toBe(userValuesStub.items[2].email);
    expect(workspaceSharePage.roleNamePairsOnPage[2].role)
        .toBe(userValuesStub.items[2].role.toLocaleString());
    expect(workspaceSharePage.roleNamePairsOnPage[2].fullName).toBe(userValuesStub.items[2].name);
  }));

  it('adds users correctly', fakeAsync(() => {
    workspaceSharePage.readPageData();
    simulateInput(workspaceSharePage.fixture, workspaceSharePage.emailField, 'sampleuser4');
    tick(1000);
    workspaceSharePage.fixture.detectChanges();
    simulateClick(workspaceSharePage.fixture,
        workspaceSharePage.fixture.debugElement.query(By.css('.add-button')));
    workspaceSharePage.fixture.detectChanges();

    workspaceSharePage.readPageData();
    expect(workspaceSharePage.roleNamePairsOnPage.length).toBe(4);
    const addedUserRole = workspaceSharePage.roleNamePairsOnPage[0];
    expect(addedUserRole.email).toBe(userValuesStub.items[3].email);
    expect(addedUserRole.fullName).toBe(userValuesStub.items[3].name);
  }));

  it('removes users correctly and does not allow self removal', fakeAsync(() => {
    workspaceSharePage.fixture.componentRef.instance.userEmail =
        'sampleuser1@fake-research-aou.org';
    workspaceSharePage.fixture.componentRef.instance.accessLevel = WorkspaceAccessLevel.OWNER;
    workspaceSharePage.readPageData();
    const de = workspaceSharePage.fixture.debugElement;
    de.queryAll(By.css('.remove-button')).forEach((removeButton) => {
      simulateClick(workspaceSharePage.fixture, removeButton);
    });
    workspaceSharePage.readPageData();
    expect(workspaceSharePage.roleNamePairsOnPage.length).toBe(1);
    expect(workspaceSharePage.roleNamePairsOnPage[0].fullName).toBe(userValuesStub.items[0].name);
    expect(workspaceSharePage.roleNamePairsOnPage[0].role).toEqual(userValuesStub.items[0].role);
  }));
});
