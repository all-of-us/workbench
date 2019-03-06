import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {CreateReviewPage} from 'app/cohort-review/create-review-page/create-review-page';
import {Participant} from 'app/cohort-review/participant.model';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ParticipantStatusComponent} from './participant-status.component';

import {CohortReviewApi, CohortStatus} from 'generated/fetch';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';

const participant: Participant = new Participant({
  participantId: 1,
  status: CohortStatus.NOTREVIEWED,
  birthDate: '12-31-1969'
});

describe('ParticipantStatusComponent', () => {
  let component: ParticipantStatusComponent;
  let fixture: ComponentFixture<ParticipantStatusComponent>;

  beforeEach(fakeAsync(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    TestBed.configureTestingModule({
      declarations: [CreateReviewPage, ParticipantStatusComponent],
      imports: [ReactiveFormsModule, ClarityModule],
      providers: [],
    }).compileComponents().then((resp) => {
      fixture = TestBed.createComponent(ParticipantStatusComponent);

      component = fixture.componentInstance;
      component.participant = participant;
      fixture.detectChanges();
    });
  }));

  it('Should render', () => {
    expect(component).toBeTruthy();
  });
});
