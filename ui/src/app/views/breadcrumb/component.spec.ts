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
          breadcrumb: {
            value: ':p1',
            intermediate: false
          }
        }
      },
      {
        path: 'child1',
        component: Component,
        data: {
          title: 'child1',
          breadcrumb: {
            value: 'child1',
            intermediate: false
          }
        },
        children: [
          {
            path: 'grandchild',
            component: Component,
            data: {
              title: 'grandchild',
              breadcrumb: {
                value: 'grandchild',
                intermediate: false
              }
            }
          }
        ]
      },
      {
        path: 'child2',
        component: Component,
        data: {
          title: 'child2',
          breadcrumb: {
            value: 'child2',
            intermediate: false
          }
        }
      },
      {
        path: 'workspaceChild',
        component: Component,
        data: {
          title: 'workspaceChild',
          breadcrumb: {
            value: 'Param: Workspace Name',
            intermediate: false
          },
          workspace: {
            name: 'Workspace Name'
          }
        }
      },
      {
        path: 'child3',
        component: Component,
        data: {title: 'child3'}
      },
      {
        path: 'parent',
        component: Component,
        data: {
          title: 'parent',
          breadcrumb: {
            value: 'parent',
          }
        },
        children: [
          {
            path: 'intermediateChild',
            component: Component,
            data: {
              title: 'intermediateChild',
              breadcrumb: {
                value: 'intermediateChild',
                intermediate: true
              }
            },
            children: [
              {
                path: 'grandchild',
                component: Component,
                data: {
                  title: 'grandchild',
                  breadcrumb: {
                    value: 'grandchild',
                  }
                }
              }
            ]
          }
        ]
      },
      {
        path: 'intermediateRoot',
        component: Component,
        data: {
          title: 'intermediateRoot',
          breadcrumb: {
            value: 'intermediateRoot',
            intermediate: true
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

  it('should have populated breadcrumbs for all supported paths', fakeAsync(() => {
    for (const p of supportedPaths) {
      const pathArray = [].concat.apply([], [p]);
      router.navigate(pathArray);
      tick();
      expect(testComponent.breadcrumbs.length).toBe(
        pathArray.length,
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

  it('should populate parameter value in label', fakeAsync(() => {
    router.navigate(['params', 'P1']);
    tick();
    expect(testComponent.breadcrumbs.pop().label)
      .toBe('P1', 'Breadcrumb label should be "P1"');
  }));

  it('should lookup name when there is a workspace name param', fakeAsync(() => {
    router.navigate(['workspaceChild']);
    tick();
    expect(testComponent.breadcrumbs.pop().label).toBe('Workspace Name',
      'Breadcrumb label should be "Workspace Name"');
  }));

  it('should show intermediateBreadcrumbs when they have children', fakeAsync( () => {
    router.navigate(['parent', 'intermediateChild', 'grandchild']);
    tick();
    expect(testComponent.breadcrumbs.length).toBe(3);
    expect(testComponent.breadcrumbs[0].label).toBe('parent');
    expect(testComponent.breadcrumbs[1].label).toBe('intermediateChild');
    expect(testComponent.breadcrumbs[2].label).toBe('grandchild');
  }));

  it('should not show intermediateBreadcrumbs when they are the last child', fakeAsync( () => {
    router.navigate(['parent', 'intermediateChild']);
    tick();
    expect(testComponent.breadcrumbs.length).toBe(1);
    expect(testComponent.breadcrumbs.pop().label).toBe('parent');
  }));

  it('should show intermediateBreadcrumb if it is the root', fakeAsync( () => {
    router.navigate(['intermediateRoot']);
    tick();
    expect(testComponent.breadcrumbs.pop().label).toBe('intermediateRoot');
  }));

});
