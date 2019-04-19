import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {List} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';

import {ListSearchGroupListComponent} from './list-search-group-list.component';

describe('ListSearchGroupListComponent', () => {
  let fixture: ComponentFixture<ListSearchGroupListComponent>;
  let component: ListSearchGroupListComponent;

  beforeEach(async(() => {
    TestBed
      .configureTestingModule({
        declarations: [
          ListSearchGroupListComponent,
        ],
        imports: [
          ClarityModule,
          NgxPopperModule,
        ],
        schemas: [ NO_ERRORS_SCHEMA ]
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListSearchGroupListComponent);
    component = fixture.componentInstance;

    // Default Inputs for tests
    component.role = 'includes';
    component.groups$ = Observable.of(List());

    fixture.detectChanges();
  });

  it('Should render', () => {
    expect(component).toBeTruthy();
  });
});
