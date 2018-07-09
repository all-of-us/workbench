import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PhysicalMeasurementsComponent } from './pm.component';

describe('PhysicalMeasurementsComponent', () => {
  let component: PhysicalMeasurementsComponent;
  let fixture: ComponentFixture<PhysicalMeasurementsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PhysicalMeasurementsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PhysicalMeasurementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
