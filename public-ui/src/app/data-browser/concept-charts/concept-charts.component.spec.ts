import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ChartComponent} from '../chart/chart.component';
import { ConceptChartsComponent } from './concept-charts.component';

describe('ConceptChartsComponent', () => {
  let component: ConceptChartsComponent;
  let fixture: ComponentFixture<ConceptChartsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ConceptChartsComponent, ChartComponent ]
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
