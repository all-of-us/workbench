import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {MultiSelectComponent} from 'app/cohort-search/multi-select/multi-select.component';
import {fromJS} from 'immutable';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

import {
  activeParameterList,
  CohortSearchActions,
  demoCriteriaChildren,
} from 'app/cohort-search/redux';

import {CohortBuilderService} from 'generated';
import {ListDemographicsComponent} from './list-demographics.component';

class MockActions {
  @dispatch() activeParameterList = activeParameterList;
  @dispatch() demoCriteriaChildren = demoCriteriaChildren;
}

describe('ListDemographicsComponent', () => {
  let component: ListDemographicsComponent;
  let fixture: ComponentFixture<ListDemographicsComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        ListDemographicsComponent,
        MultiSelectComponent,
      ],
      imports: [
        ClarityModule,
        NouisliderModule,
        NgxPopperModule,
        ReactiveFormsModule,
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortBuilderService, useValue: {}},
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListDemographicsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
