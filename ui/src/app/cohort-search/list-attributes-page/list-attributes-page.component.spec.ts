import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';

import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {
  addParameter,
  CohortSearchActions,
  hideAttributesPage,
  requestAttributePreview,
} from 'app/cohort-search/redux';

import {CohortBuilderService} from 'generated';
import {ListAttributesPageComponent} from './list-attributes-page.component';

class MockActions {
  @dispatch() addParameter = addParameter;
  @dispatch() hideAttributesPage = hideAttributesPage;
  @dispatch() requestAttributePreview = requestAttributePreview;
}

describe('ListAttributesPageComponent', () => {
  let component: ListAttributesPageComponent;
  let fixture: ComponentFixture<ListAttributesPageComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [ListAttributesPageComponent, ValidatorErrorsComponent],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortBuilderService, useValue: {}},
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListAttributesPageComponent);
    component = fixture.componentInstance;
    component.attrs = {EXISTS: false, NUM: [{operator: 'ANY', operands: []}], CAT: []};
    component.node = fromJS({
      code: '',
      conceptId: 903133,
      count: 0,
      domainId: 'Measurement',
      group: false,
      hasAttributes: true,
      id: 316305,
      name: 'Height Detail',
      parentId: 0,
      selectable: true,
      subtype: 'HEIGHT',
      type: 'PM'
    });
    component.form = new FormGroup({
      NUM: new FormGroup({
        num0: new FormGroup({
          operator: new FormControl(),
          valueA: new FormControl(),
          valueB: new FormControl(),
        })
      })
    });
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // it('should display the form with title', () => {
  //   expect(fixture.debugElement.query(By.css('form'))).toBeTruthy();
  //
  //   const title = fixture.debugElement.query(By.css('div.title')).nativeElement;
  //   expect(title.textContent.replace(/\s+/g, ' ').trim()).toEqual('Height Detail');
  // });

});
