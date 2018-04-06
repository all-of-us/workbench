import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';

import {DetailConditionsComponent} from './detail-conditions.component';

import {CohortReviewService} from 'generated';

const participant = {
  participantId: 1,
};

const cohort = {
  id: 1,
};

const workspace = {
  namespace: 'ws',
  id: 'wsid',
  cdrVersionId: 1,
};

class StubRoute {
  data = Observable.of({participant});
  parent = {data: Observable.of({cohort, workspace})};
}

class ApiSpy {
  getParticipantData = jasmine
    .createSpy('getParticipantData')
    .and
    .callFake((ns, wsid, cid, cdrid, pid, pageRequest) =>
      Observable.of({items: [], count: 0, pageRequest}));
}


describe('DetailConditionsComponent', () => {
  let component: DetailConditionsComponent;
  let fixture: ComponentFixture<DetailConditionsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ ClarityModule, NgxPopperModule ],
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
    component.ngOnInit();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
