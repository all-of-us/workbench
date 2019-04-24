import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';

import {OptionInfoComponent} from 'app/cohort-search/option-info/option-info.component';
import {CohortSearchActions} from 'app/cohort-search/redux';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {SearchBarComponent} from './list-search-bar.component';

describe('SearchBarComponent', () => {
  let component: SearchBarComponent;
  let fixture: ComponentFixture<SearchBarComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [ OptionInfoComponent, SearchBarComponent, SafeHtmlPipe ],
      imports: [
        ClarityModule,
        NgxPopperModule,
        ReactiveFormsModule
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        CohortSearchActions,
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
