import {async as _async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {DebugElement} from '@angular/core';
import {ClarityModule} from 'clarity-angular';
import {NgRedux, dispatch} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {Map, List, fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions} from '../redux';
import {OverviewComponent} from './overview.component';
import {
  ChartsComponent,
  GenderChartComponent,
  RaceChartComponent,
  GoogleChartComponent,
} from '../charts';
import {CohortBuilderService} from 'generated';

class MockActions {
}

describe('OverviewComponent', () => {
  let fixture: ComponentFixture<OverviewComponent>;
  let component: OverviewComponent;

  let mockReduxInst;

  beforeEach(_async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          ChartsComponent,
          GenderChartComponent,
          RaceChartComponent,
          GoogleChartComponent,
          OverviewComponent,
        ],
        imports: [
          ClarityModule,
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
    MockNgRedux.reset();

    fixture = TestBed.createComponent(OverviewComponent);
    component = fixture.componentInstance;

    // Default Inputs for tests
    component.total$ = Observable.of(0);
    component.isRequesting$ = Observable.of(false);

    fixture.detectChanges();
  });

  xit('Should render', () => {
    // TODO(jms) - need to figure out how to mock `google` for GoogleChartComponent
    // Rewrite this & add other tests when the charts are rewritten
    expect(component).toBeTruthy();
  });
});
