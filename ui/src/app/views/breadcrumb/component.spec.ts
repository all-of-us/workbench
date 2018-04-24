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
        }
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
        path: 'child3',
        component: Component,
        data: {title: 'child3'}
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
    expect(testComponent.breadcrumbs.pop().label).toBe('P1', 'Breadcrumb label should be "P1"');
  }));

});
