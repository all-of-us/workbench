import {Component} from '@angular/core';
import {async, ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {Router, Routes} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

import {BreadcrumbComponent} from './component';

const testRoutes: Routes = [
  {
    path: '',
    component: Component,
    data: {title: 'root'},
    children: [
      {
        path: 'params/:p1',
        component: Component,
        data: {
          title: 'params',
          breadcrumb: ':p1'
        },
          children: [
            {
              path: 'paramsChild',
              component: Component,
                data: {
                  title: 'paramsChild',
                  breadcrumb: 'paramschild'
                }
            }
          ]
      },
      {
        path: 'child1',
        component: Component,
        data: {
          title: 'child1',
          breadcrumb: 'child1'
        },
        children: [
          {
            path: 'grandchild',
            component: Component,
            data: {
              title: 'grandchild',
              breadcrumb: 'grandchild'
            }
          }
        ]
      },
      {
        path: 'child2',
        component: Component,
        data: {
          title: 'child2',
          breadcrumb: 'child2'
        }
      },
      {
        path: 'workspaceChild',
        component: Component,
        data: {
          title: 'workspaceChild',
          breadcrumb: 'Param: Workspace Name',
          workspace: {
            name: 'Workspace Name'
          }
        },
          children: [
              {
                path: 'workspaceGrandChild',
                component: Component,
                data: {
                  title: 'workspaceGrandChild',
                    breadcrumb: 'workspacegrandchild'
                }
              }
          ]
      },
      {
        path: 'child3',
        component: Component,
        data: {title: 'child3'}
      },
      {
        path: 'conceptSets/:conceptset',
        component: Component,
        data: {
          title: 'conceptSets',
          breadcrumb: 'Param: Concept Set Name',
          conceptSet: {
            name: 'Concept Set Name'
          }
        }
      }
    ]
  }
];

describe('BreadcrumbComponent', () => {

  const supportedPaths = ['child1', 'child2', ['child1', 'grandchild']];
  const unsupportedPaths = ['', 'child3'];

  let router: Router;
  let fixture: ComponentFixture<BreadcrumbComponent>;
  let testComponent: BreadcrumbComponent;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes(testRoutes)
      ],
      declarations: [
        BreadcrumbComponent
      ]
    }).compileComponents().then(() => {
      router = TestBed.get(Router);
      fixture = TestBed.createComponent(BreadcrumbComponent);
      testComponent = fixture.componentInstance;
      fixture.detectChanges();
      router.initialNavigation();
    });

  }));

  it('should create breadcrumbs component', fakeAsync( () => {
    router.navigate(['']);
    tick();
    expect(testComponent).toBeTruthy('Breadcrumbs should have instantiated');
  }));

  it('should have populated breadcrumbs for all supported paths with parents', fakeAsync(() => {
    for (const p of supportedPaths) {
      const pathArray = [].concat.apply([], [p]);
      router.navigate(pathArray);
      tick();
      expect(testComponent.breadcrumbs.length).toBe(
          pathArray.length - 1,
          'Breadcrumbs should exist for each element of the supported path: ' + p);
    }
  }));

  it('should have empty breadcrumbs for unsupported paths', fakeAsync(() => {
    for (const p of unsupportedPaths) {
      router.navigate([].concat.apply([], [p]));
      tick();
      expect(testComponent.breadcrumbs.length)
          .toBe(0, 'Breadcrumbs should be empty for unsupported path: ' + p);
    }
  }));

  it('should have empty breadcrumbs for path without a parent', fakeAsync(() => {
    router.navigate((['child2']));
    tick();
    expect(testComponent.breadcrumbs.length)
        .toBe(0, 'Breadcrumbs should be empty for path without a parent');

  }));

  it('should populate parent value in label', fakeAsync(() => {
    router.navigate(['params', 'P1', 'paramsChild']);
    tick();
    expect(testComponent.breadcrumbs.pop().label)
        .toBe('P1', 'Breadcrumb label should be "P1"');
  }));

  it('should lookup name when there is a workspace name param', fakeAsync(() => {
    router.navigate(['workspaceChild', 'workspaceGrandChild']);
    tick();
    expect(testComponent.breadcrumbs.pop().label).toBe('Workspace Name',
      'Breadcrumb label should be "Workspace Name"');
  }));

  // Test out the exception case
  it('should display the child as a label for Param: Concept Set Name', fakeAsync( () => {
    router.navigate(['conceptSets', 'myConceptSet']);
    tick();
    expect(testComponent.breadcrumbs.pop().label)
        .toBe('Concept Set Name', 'Breadcrumb label should be "Concept Set Name"');
  }))

});
