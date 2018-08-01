import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {ClarityModule} from '@clr/angular';
import {fromJS, List, Map} from 'immutable';

import {
  addAttributeForPreview,
  addParameter,
  CohortSearchActions,
  hideAttributesPage,
  requestAttributePreview,
} from '../redux';

import {CohortBuilderService} from 'generated';
import {AttributesPageComponent} from './attributes-page.component';

class MockActions {
  @dispatch() addAttributeForPreview = addAttributeForPreview;
  @dispatch() addParameter = addParameter;
  @dispatch() hideAttributesPage = hideAttributesPage;
  @dispatch() requestAttributePreview = requestAttributePreview;
}

describe('AttributesPageComponent', () => {
  let component: AttributesPageComponent;
  let fixture: ComponentFixture<AttributesPageComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [ AttributesPageComponent ],
      imports: [ClarityModule, FormsModule],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortBuilderService, useValue: {}},
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AttributesPageComponent);
    component = fixture.componentInstance;
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
      predefinedAttributes: null,
      selectable: true,
      subtype: 'HEIGHT',
      type: 'PM'
    });
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the form with title', () => {
    expect(fixture.debugElement.query(By.css('form'))).toBeTruthy();

    const title = fixture.debugElement.query(By.css('div.title')).nativeElement;
    expect(title.textContent.replace(/\s+/g, ' ').trim()).toEqual('Height Detail');
  });

});
