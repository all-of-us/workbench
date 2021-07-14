import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {currentCohortReviewStore} from 'app/utils/navigation';
import {CohortStatus, FilterColumns, ParticipantCohortStatus, SortOrder} from 'generated/fetch';

export const initialFilterState = {
  global: {
    dateMin: null,
    dateMax: null,
    ageMin: '',
    ageMax: '',
    visits: null
  },
  participants: {
    PARTICIPANTID: '',
    SEXATBIRTH: ['Select All'],
    GENDER: ['Select All'],
    RACE: ['Select All'],
    ETHNICITY: ['Select All'],
    DECEASED: ['1', '0', 'Select All'],
    STATUS: [
      CohortStatus.INCLUDED,
      CohortStatus.EXCLUDED,
      CohortStatus.NEEDSFURTHERREVIEW,
      CohortStatus.NOTREVIEWED,
      'Select All'
    ]
  },
  tabs: {
    ALL_EVENTS: {
      standardCode: '',
      sourceCode: '',
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      domain: ['Select All'],
      sourceName: '',
      standardName: '',
      value: '',
    },
    PROCEDURE: {
      standardCode: '',
      sourceCode: '',
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      sourceName: '',
      standardName: '',
    },
    CONDITION: {
      standardCode: '',
      sourceCode: '',
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      sourceName: '',
      standardName: '',
    },
    DRUG: {
      sourceName: '',
      standardName: '',
      numMentions: '',
      firstMention: '',
      lastMention: '',
    },
    OBSERVATION: {
      standardCode: '',
      sourceCode: '',
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      sourceName: '',
      standardName: '',
    },
    PHYSICAL_MEASUREMENT: {
      standardCode: '',
      sourceCode: '',
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      sourceName: '',
      standardName: '',
      value: '',
    },
    LAB: {
      itemTime: '',
      sourceName: '',
      standardName: '',
      value: ''
    },
    VITAL: {
      itemTime: '',
      sourceName: '',
      standardName: '',
    },
    SURVEY: {
      survey: ''
    }
  },
  vocab: 'standard',
};

const initialPaginationState = {
  page: 0,
  pageSize: 25,
  sortColumn: FilterColumns.PARTICIPANTID,
  sortOrder: SortOrder.Asc
};

export const visitsFilterOptions = new BehaviorSubject<Array<any>>(null);
export const filterStateStore = new BehaviorSubject<any>(JSON.parse(JSON.stringify(initialFilterState)));
export const vocabOptions = new BehaviorSubject<any>(null);
export const participantStore = new BehaviorSubject<ParticipantCohortStatus>(null);
export const queryResultSizeStore = new BehaviorSubject<number>(undefined);
export const reviewPaginationStore = new BehaviorSubject<any>(initialPaginationState);

export function getVocabOptions(workspaceNamespace: string, workspaceId: string, cohortReviewId: number) {
  const vocabFilters = {source: {}, standard: {}};
  try {
    cohortReviewApi().getVocabularies(workspaceNamespace, workspaceId, cohortReviewId)
      .then(response => {
        response.items.forEach(item => {
          const type = item.type.toLowerCase();
          vocabFilters[type][item.domain] = [
            ...(vocabFilters[type][item.domain] || []),
            item.vocabulary
          ];
        });
        vocabOptions.next(vocabFilters);
      });
  } catch (error) {
    vocabOptions.next(vocabFilters);
    console.error(error);
  }
}

export function updateParticipant(participant: ParticipantCohortStatus) {
  const review = currentCohortReviewStore.getValue();
  if (participant && review) {
    const index = review.participantCohortStatuses.findIndex(p => p.participantId === participant.participantId);
    if (index !== -1) {
      review.participantCohortStatuses[index] = participant;
      currentCohortReviewStore.next(review);
    }
  }
}
