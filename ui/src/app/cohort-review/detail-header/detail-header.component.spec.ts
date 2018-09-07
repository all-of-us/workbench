import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute, Router} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService} from 'generated';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';
import {DetailHeaderComponent} from './detail-header.component';

describe('DetailHeaderComponent', () => {
  let component: DetailHeaderComponent;
  let fixture: ComponentFixture<DetailHeaderComponent>;
  const routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ DetailHeaderComponent ],
      imports: [ClarityModule],
      providers: [
        {provide: CohortReviewService, useValue: {}},
        {provide: ReviewStateService, useValue: {}},
        {provide: ActivatedRoute, useValue: {}},
        {provide: Router, useValue: routerSpy},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailHeaderComponent);
    component = fixture.componentInstance;
    component.participant = <Participant> {id: 1};
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
