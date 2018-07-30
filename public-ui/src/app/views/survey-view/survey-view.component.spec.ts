import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SurveyViewComponent } from './survey-view.component';

describe('SurveyViewComponent', () => {
  let component: SurveyViewComponent;
  let fixture: ComponentFixture<SurveyViewComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SurveyViewComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SurveyViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
