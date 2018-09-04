import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NgxChartsModule} from '@swimlane/ngx-charts';

import {GenderChartComponent} from './gender-chart.component';

describe('GenderChartComponent', () => {
  let component: GenderChartComponent;
  let fixture: ComponentFixture<GenderChartComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [GenderChartComponent],
      imports: [
        BrowserAnimationsModule,
        NgxChartsModule,
      ],
      providers: [],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GenderChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});

