import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {CohortBuilderApi} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {ListAttributesPageComponent} from './list-attributes-page.component';

describe('ListAttributesPageComponent', () => {
  let component: ListAttributesPageComponent;
  let fixture: ComponentFixture<ListAttributesPageComponent>;

  beforeEach(async(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    TestBed.configureTestingModule({
      declarations: [ListAttributesPageComponent, ValidatorErrorsComponent],
      imports: [ClarityModule, ReactiveFormsModule],
      providers: [],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListAttributesPageComponent);
    component = fixture.componentInstance;
    component.attrs = {EXISTS: false, NUM: [{operator: 'ANY', operands: []}], CAT: []};
    component.criterion = {
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
    };
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
});
