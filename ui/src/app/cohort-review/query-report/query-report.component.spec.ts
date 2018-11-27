import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { QueryReportComponent } from './query-report.component';

describe('QueryReportComponent', () => {
  let component: QueryReportComponent;
  let fixture: ComponentFixture<QueryReportComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ QueryReportComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
