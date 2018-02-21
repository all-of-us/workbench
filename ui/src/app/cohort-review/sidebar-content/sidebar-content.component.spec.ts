import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {SidebarContentComponent} from './sidebar-content.component';

describe('SidebarContentComponent', () => {
  let component: SidebarContentComponent;
  let fixture: ComponentFixture<SidebarContentComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [SidebarContentComponent],
      schemas: [NO_ERRORS_SCHEMA],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SidebarContentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
