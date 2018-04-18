import {Component} from '@angular/core';
import {async, ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {Router, Routes} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

import {WorkspacesService} from 'generated';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {WorkspaceResolver} from '../../resolvers/workspace';
import {BreadcrumbComponent} from './component';

// TODO: Would love a better way of reading the routes. This mock seems super icky.
// Pulling in all of the app's routes is more painful.
const routes: Routes = [
  {
    path: 'data-browser/home',
    component: Component,
    data: {title: 'Data Browser'}
  }, {
    path: 'data-browser/browse',
    component: Component,
    data: {title: 'Browse'}
  }, {
    path: 'login',
    component: Component,
    data: {title: 'Sign In'}
  }, {
    path: '',
    component: Component,
    children: [
      {
        path: '',
        component: Component,
        data: { breadcrumb: 'Workspaces' },
        children: [
          {
            path: '',
            component: Component,
            data: {title: 'View Workspaces'}
          },
          {
            path: 'workspace/:ns/:wsid',
            data: {
              title: 'View Workspace Details',
              breadcrumb: ':wsid'
            },
            resolve: {
              workspace: WorkspaceResolver,
            },
            children: [{
              path: '',
              component: Component,
              data: {
                title: 'View Workspace Details',
                breadcrumb: 'View Workspace Details'
              }
            }, {
              path: 'edit',
              component: Component,
              data: {
                title: 'Edit Workspace',
                breadcrumb: 'Edit Workspace'
              }
            }, {
              path: 'clone',
              component: Component,
              data: {
                title: 'Clone Workspace',
                breadcrumb: 'Clone Workspace'
              }
            }, {
              path: 'share',
              component: Component,
              data: {
                title: 'Share Workspace',
                breadcrumb: 'Share Workspace'
              }
            }, {
              path: 'cohorts/build',
              component: Component,
              data: {
                breadcrumb: 'Cohort Builder'
              }
            }, {
              path: 'cohorts/:cid/review',
              component: Component,
              data: {
                breadcrumb: 'Cohort Review'
              }
            }, {
              path: 'cohorts/:cid/edit',
              component: Component,
              data: {
                title: 'Edit Cohort',
                breadcrumb: 'Edit Cohort'
              }
            }],
          }
        ]
      },
      {
        path: 'admin/review-workspace',
        component: Component,
        data: {title: 'Review Workspaces'}
      }, {
        path: 'admin/review-id-verification',
        component: Component,
        data: {title: 'Review ID Verifications'}
      }, {
        path: 'profile',
        component: Component,
        data: {title: 'Profile'}
      }, {
        path: 'workspace/build',
        component: Component,
        data: {title: 'Create Workspace'}
      }
    ]
  }
];

describe('BreadcrumbComponent', () => {

  const ns = WorkspaceStubVariables.DEFAULT_WORKSPACE_NS;
  const wsid = WorkspaceStubVariables.DEFAULT_WORKSPACE_ID;
  const wsPath = ['workspace', ns, wsid];
  const supportedChildPaths = ['edit', 'clone', 'share', ['cohorts', 'build'],
    ['cohorts', ':cid', 'review'], ['cohorts', ':cid', 'edit']];
  const unsupportedPaths = [['data-browser', 'home'], ['data-browser', 'browse'], 'login',
    ['admin', 'review-workspace'], ['admin', 'review-id-verification'], 'profile',
    ['workspace', 'build']];

  let router: Router;
  let fixture: ComponentFixture<BreadcrumbComponent>;
  let testComponent: BreadcrumbComponent;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes(routes)
      ],
      declarations: [
        BreadcrumbComponent
      ],
      providers: [
        WorkspaceResolver,
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() }
      ]
    }).compileComponents().then(() => {
      router = TestBed.get(Router);
      fixture = TestBed.createComponent(BreadcrumbComponent);
      testComponent = fixture.componentInstance;
      fixture.detectChanges();
      router.initialNavigation();
    });

  }));

  // N.B. We cannot directly test breadcrumb status on the root path, '', since both
  // SignedInComponent and WorkspaceListComponent are both served at that same path, and each one
  // has a different breadcrumb state.

  it('should create breadcrumbs component', fakeAsync( () => {
    router.navigate(['']);
    tick();
    expect(testComponent).toBeTruthy('Breadcrumbs should have instantiated');
  }));

  it('should have two breadcrumbs for a destination workspace ID path', fakeAsync(() => {
    router.navigate(wsPath);
    tick();
    expect(testComponent.breadcrumbs.length)
        .toBe(2, '2 Breadcrumbs should exist for the workspace id path');
    expect(testComponent.breadcrumbs.pop().label)
        .toBe(wsid, 'Last element of the breadcrumb should be the workspace ID');
  }));

  it('should have three breadcrumbs for a supported children of destination workspace ID path',
      fakeAsync(() => {
        for (const p of supportedChildPaths) {
          router.navigate(wsPath.concat(p));
          tick();
          expect(testComponent.breadcrumbs.length)
              .toBe(3, '3 Breadcrumbs should exist for the child path: ' + p);
        }
      }));

  it('should not have any breadcrumb elements for unsupported paths', fakeAsync(() => {
    for (const p of unsupportedPaths) {
      // Need to make sure we are passing an flattened array of path elements to router.navigate
      const pathArray = [].concat.apply([], [p]);
      router.navigate(pathArray);
      tick();
      expect(testComponent.breadcrumbs.length)
          .toBe(0, 'Breadcrumbs should be empty for unsupported path: ' + p);
    }
  }));

});
