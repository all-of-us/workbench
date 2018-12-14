import { async, ComponentFixture, TestBed } from '@angular/core/testing';

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
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
