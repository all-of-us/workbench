import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {DetailTabsComponent} from './detail-tabs.component';


describe('DetailTabsComponent', () => {
  let component: DetailTabsComponent;
  let fixture: ComponentFixture<DetailTabsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [DetailTabsComponent],
      schemas: [NO_ERRORS_SCHEMA],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailTabsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
