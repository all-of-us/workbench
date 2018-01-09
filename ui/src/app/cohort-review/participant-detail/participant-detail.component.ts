import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import * as moment from 'moment';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {
  CohortReview,
  CohortReviewService,
  ParticipantCohortStatus
} from 'generated';

const CDR_VERSION = 1;

@Component({
  selector: 'app-participant-detail',
  templateUrl: './participant-detail.component.html',
  styleUrls: ['./participant-detail.component.css']
})
export class ParticipantDetailComponent implements OnInit, OnDestroy {
  participant: Participant;
  isFirstParticipant: boolean;
  isLastParticipant: boolean;
  priorId: number;
  afterId: number;
  subscription: Subscription;

  DUMMY_CONDITIONS = dummyData();

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.subscription = this.state.participant$
      .merge(this.route.data.pluck('participant'))
      .do(participant => this.participant = <Participant>participant)
      .withLatestFrom(this.state.review$)
      .subscribe(([participant, review]: [Participant, CohortReview]) => {
        const statuses = review.participantCohortStatuses;
        const id = participant && participant.participantId;
        const index = statuses.findIndex(({participantId}) => participantId === id);

        // The participant is not on the current page... for now, just log it and ignore it
        // We get here by URL (when a direct link to a detail page is shared, for example)
        if (index < 0) {
          console.log('Participant not on page');
          // For now, disable next / prev entirely
          this.isFirstParticipant = true;
          this.isLastParticipant = true;
          return;
        }

        const totalPages = Math.floor(review.reviewSize / review.pageSize);

        this.isFirstParticipant =
          review.page === 0   // first page
          && index === 0;     // first person on page

        this.isLastParticipant =
          (review.page + 1) === totalPages    // last page
          && (index + 1) === statuses.length; // last person on page

        this.priorId = statuses[index - 1] && statuses[index - 1]['participantId'];
        this.afterId = statuses[index + 1] && statuses[index + 1]['participantId'];
      });
  }

  ngOnDestroy() {
    this.state.sidebarOpen.next(false);
    this.state.participant.next(null);
    this.subscription.unsubscribe();
  }

  toggleSidebar() {
    this.state.sidebarOpen$
      .take(1)
      .subscribe(val => this.state.sidebarOpen.next(!val));
  }

  up() {
    this.router.navigate(['..'], {relativeTo: this.route});
  }

  previous() {
    this._navigate(true);
  }

  next() {
    this._navigate(false);
  }

  private _navigate(left: boolean) {
    const id = left ? this.priorId : this.afterId;
    const hasNext = !(left ? this.isFirstParticipant : this.isLastParticipant);

    if (id !== undefined) {
      this._navigateById(id);
    } else if (hasNext) {
      const statusGetter = (statuses: ParticipantCohortStatus[]) => left
        ? statuses[statuses.length - 1]
        : statuses[0];

      const adjustPage = (page: number) => left
        ? page - 1
        : page + 1;

      this.state.review$
        .take(1)
        .map(({page, pageSize}) => ({page: adjustPage(page), size: pageSize}))
        .mergeMap(({page, size}) => this._callAPI(page, size))
        .subscribe(review => {
          this.state.review.next(review);
          const stat = statusGetter(review.participantCohortStatuses);
          this._navigateById(stat.participantId);
        });
    }
  }

  private _callAPI = (page: number, size: number): Observable<CohortReview> => {
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    return this.reviewAPI.getParticipantCohortStatuses(ns, wsid, cid, CDR_VERSION, page, size);
  }

  private _navigateById = (id: number): void => {
    this.router.navigate(['..', id], {relativeTo: this.route});
  }
}

const dummyData = () => [
    {
        "ageAtEvent": 46,
        "date": moment("2002-04-16"),
        "description": "Low back pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "724.2"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-08-30"),
        "description": "Arthropathy",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "716.90"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-08-30"),
        "description": "Low back pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "724.2"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-09-23"),
        "description": "Multiple joint pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "719.49"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-09-23"),
        "description": "Shoulder joint pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "719.41"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-10-08"),
        "description": "Inflammatory disease of liver",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "573.3"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-10-22"),
        "description": "Chronic hepatitis C",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "070.54"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-10-25"),
        "description": "Shoulder joint pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "719.41"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-10-28"),
        "description": "Neck pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "723.1"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-11-01"),
        "description": "Shoulder joint pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "719.41"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2002-11-08"),
        "description": "Neck pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "723.1"
    },
    {
        "ageAtEvent": 46,
        "date": moment("2003-03-07"),
        "description": "Eruption",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "782.1"
    },
    {
        "ageAtEvent": 47,
        "date": moment("2003-07-12"),
        "description": "Anxiety disorder",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "300.00"
    },
    {
        "ageAtEvent": 47,
        "date": moment("2003-07-12"),
        "description": "Cellulitis and abscess of lower leg",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "682.6"
    },
    {
        "ageAtEvent": 47,
        "date": moment("2003-08-26"),
        "description": "Inflammatory disease of liver",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "573.3"
    },
    {
        "ageAtEvent": 47,
        "date": moment("2004-02-24"),
        "description": "Pain in limb",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "729.5"
    },
    {
        "ageAtEvent": 47,
        "date": moment("2004-03-29"),
        "description": "Radial styloid tenosynovitis",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "727.04"
    },
    {
        "ageAtEvent": 48,
        "date": moment("2004-08-27"),
        "description": "Hand joint pain",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "719.44"
    },
    {
        "ageAtEvent": 48,
        "date": moment("2004-09-13"),
        "description": "Cough",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "786.2"
    },
    {
        "ageAtEvent": 48,
        "date": moment("2004-12-25"),
        "description": "Arthralgia of the upper arm",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "719.42"
    },
    {
        "ageAtEvent": 48,
        "date": moment("2005-01-08"),
        "description": "Benign essential hypertension",
        "source": "ICD9CM",
        "standard": "SNOMED",
        "value": "401.1"
    }
];
