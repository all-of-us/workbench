import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from 'clarity-angular';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {ChartsModule} from '../charts/charts.module';
import {CohortSearchActions} from '../redux';
import {OverviewComponent} from './overview.component';

import {CohortBuilderService} from 'generated';

class MockActions {}

describe('OverviewComponent', () => {
  let fixture: ComponentFixture<OverviewComponent>;
  let component: OverviewComponent;

  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          OverviewComponent,
        ],
        imports: [
          ClarityModule,
          ChartsModule,
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
