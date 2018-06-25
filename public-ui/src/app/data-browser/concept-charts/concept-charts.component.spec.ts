import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ConceptChartsComponent } from './concept-charts.component';

describe('ConceptChartsComponent', () => {
  let component: ConceptChartsComponent;
  let fixture: ComponentFixture<ConceptChartsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ConceptChartsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ConceptChartsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
