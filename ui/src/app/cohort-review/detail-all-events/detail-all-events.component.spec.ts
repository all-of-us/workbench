import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService, ParticipantDataListResponse} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';

import {DetailTabTableComponent} from '../detail-tab-table/detail-tab-table.component';
import {DetailAllEventsComponent} from './detail-all-events.component';

describe('DetailAllEventsComponent', () => {
  let component: DetailAllEventsComponent;
  let fixture: ComponentFixture<DetailAllEventsComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ DetailAllEventsComponent, DetailTabTableComponent ],
      imports: [ClarityModule, NgxPopperModule],
      providers: [
        {
          provide: CohortReviewService, useValue: {
            getParticipantData: (): Observable<ParticipantDataListResponse> => {
              return Observable.of(<ParticipantDataListResponse> {items: []});
            }
          }
        },
        {
          provide: ActivatedRoute, useValue: {
            data: Observable.of({
              workspace: {cdrVersionId: '1'},
              cohort: {},
              participant: {}
            })
          }
        },
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailAllEventsComponent);
    component = fixture.componentInstance;
    component.columns = [{name: ''}];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
