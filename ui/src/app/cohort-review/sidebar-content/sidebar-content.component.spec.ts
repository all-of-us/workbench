import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {Participant} from '../participant.model';
import {SidebarContentComponent} from './sidebar-content.component';

import {CohortStatus} from 'generated';


const participant = new Participant({
  participantId: 1,
  status: CohortStatus.NOTREVIEWED,
  gender: 'gender',
  race: 'race',
  ethnicity: 'ethnicity',
  birthDate: 519714000,
});


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
    component.participant = participant;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
