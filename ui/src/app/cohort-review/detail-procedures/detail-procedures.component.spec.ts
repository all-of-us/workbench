import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';

import {DetailProceduresComponent} from './detail-procedures.component';

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
  getParticipantProcedures = jasmine
    .createSpy('getParticipantConditions')
    .and
    .callFake((ns, wsid, cid, cdrid, pid, pageRequest) =>
      Observable.of({items: [], count: 0, pageRequest}));
}


describe('DetailProceduresComponent', () => {
  let component: DetailProceduresComponent;
  let fixture: ComponentFixture<DetailProceduresComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ ClarityModule, NgxPopperModule ],
      declarations: [ DetailProceduresComponent ],
      providers: [
        {provide: CohortReviewService, useValue: new ApiSpy()},
        {provide: ActivatedRoute, useClass: StubRoute}
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailProceduresComponent);
    component = fixture.componentInstance;
    component.ngOnInit();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
