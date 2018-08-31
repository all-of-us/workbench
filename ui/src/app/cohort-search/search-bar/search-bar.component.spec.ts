import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';

import {CohortSearchActions} from '../redux';
import {SafeHtmlPipe} from '../safe-html.pipe';
import {SearchBarComponent} from './search-bar.component';

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
      declarations: [ SearchBarComponent, SafeHtmlPipe ],
      imports: [
        ClarityModule,
        FormsModule],
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
