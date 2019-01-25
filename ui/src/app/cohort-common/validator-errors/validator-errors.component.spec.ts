import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {FormControl, FormGroup} from '@angular/forms';

import { ValidatorErrorsComponent } from './validator-errors.component';

describe('ValidatorErrorsComponent', () => {
  let component: ValidatorErrorsComponent;
  let fixture: ComponentFixture<ValidatorErrorsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ValidatorErrorsComponent ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ValidatorErrorsComponent);
    component = fixture.componentInstance;
    component.form = new FormGroup({testControl: new FormControl()});
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
