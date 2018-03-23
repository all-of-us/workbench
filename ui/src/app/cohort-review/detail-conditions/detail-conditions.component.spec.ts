import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {DetailConditionsComponent} from './detail-conditions.component';

import {
  CohortReviewService,
  PageFilterRequest,
  PageFilterType,
  ParticipantConditionsColumns,
  SortOrder,
} from 'generated';

class StubRoute {
  snapshot = {
    data: {
      participant: {
        participantId: 1,
      }
    },
    parent: {
      data: {
        cohort: {
          id: 1,
        },
        workspace: {
          namespace: 'ns',
          id: 'wsid',
          cdrVersionId: 1,
        }
      }
    }
  };
}

class ApiSpy {
  getParticipantConditions = jasmine
    .createSpy('getParticipantConditions')
    .and
    .returnValue(Observable.of({items: []}));
}


describe('DetailConditionsComponent', () => {
  let component: DetailConditionsComponent;
  let fixture: ComponentFixture<DetailConditionsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ ClarityModule ],
      declarations: [ DetailConditionsComponent ],
      providers: [
        {provide: CohortReviewService, useValue: new ApiSpy()},
        {provide: ActivatedRoute, useClass: StubRoute}
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailConditionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
