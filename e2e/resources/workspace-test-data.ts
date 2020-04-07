import {makeString} from '../util/helper';
import {FIELD_FINDER as EDIT_FIELD_FINDER} from '../app/workspace-edit-page';


// Question #1: What is the primary purpose of your project?
export const defaultPrimaryPurposeAnswers = [
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.DISEASE_FOCUSED_RESEARCH,
      selected: true,
      value: 'diabetic cataract'
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.METHODS_DEVELOPMENT_VALIDATION_STUDY,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.RESEARCH_CONTROL,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.GENETIC_RESEARCH,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.SOCIAL_BEHAVIORAL_RESEARCH,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.POPULATION_HEALTH,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.ETHICAL_LEGAL_SOCIAL_IMPLICATIONS_RESEARCH,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.DRUG_THERAPEUTICS_DEVELOPMENT_RESEARCH,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.EDUCATION_PURPOSE,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.FOR_PROFIT_PURPOSE,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.PRIMARY_PURPOSE.OTHER_PURPOSE,
      selected: true,
      value: makeString(50)
   }
];

// Question #2: Please provide a summary of your research purpose by responding to the question.
export const defaultResearchPurposeSummaryAnswers = [
   {
      id: EDIT_FIELD_FINDER.RESEARCH_PURPOSE_SUMMARY.SCIENTIFIC_QUESTIONS_INTENT_TO_STUDY,
      value: makeString(100)
   }, {
      id: EDIT_FIELD_FINDER.RESEARCH_PURPOSE_SUMMARY.SCIENTIFIC_APPROACHES_TO_USE,
      value: makeString(100)
   }, {
      id: EDIT_FIELD_FINDER.RESEARCH_PURPOSE_SUMMARY.ANTICIPATED_FINDINGS_FROM_STUDY,
      value: makeString(100)
   }
];

// Question #3: Please tell us how you plan to disseminate your research findings.
export const defaultDisseminateResearchFindingsAnswers = [
   {
      id: EDIT_FIELD_FINDER.DISSEMINATE_RESEARCH_FINDINGS.PUBLICATION_IN_SCIENTIFC_JOURNALS,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DISSEMINATE_RESEARCH_FINDINGS.SOCIAL_MEDIA,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DISSEMINATE_RESEARCH_FINDINGS.PRESENTATION_AT_SCIENTIFIC_CONFERENCES,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DISSEMINATE_RESEARCH_FINDINGS.PRESENTATION_AT_COMMUNITY_FORUMS,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DISSEMINATE_RESEARCH_FINDINGS.PRESS_RELEASE,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DISSEMINATE_RESEARCH_FINDINGS.PUBLICATION_IN_COMMUNITY_JOURNALS,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DISSEMINATE_RESEARCH_FINDINGS.PUBLICATION_IN_PERSONAL_BLOG,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DISSEMINATE_RESEARCH_FINDINGS.OTHER,
      selected: true,
      value: makeString(20)
   }
];

// Question #4: Please select all of the statements below that describe the outcomes you anticipate from your research.
export const defaultAnticipatedOutcomesFromResearchAnswers = [
   {
      id: EDIT_FIELD_FINDER.DESCRIBE_ANTICIPATED_OUTCOMES.SEEKS_INCREASE_WELLNESS,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DESCRIBE_ANTICIPATED_OUTCOMES.SEEKS_TO_REDUCE_HEALTH_DISPARITIES,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DESCRIBE_ANTICIPATED_OUTCOMES.SEEKS_TO_DEVELOP_RISK_ASSESSMENT,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DESCRIBE_ANTICIPATED_OUTCOMES.SEEKS_TO_PROVIDE_EARLIER_ACCURATE_DIAGNOSIS,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.DESCRIBE_ANTICIPATED_OUTCOMES.SEEKS_TO_REDUCE_BURDEN,
      selected: true
   }
];

// Question #5: Population of interest
export const defaultPopulationOfInterestAnswers = [
   {
      id: EDIT_FIELD_FINDER.POPULATION_OF_INTEREST.YES,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.POPULATION_OF_INTEREST.RACE_MULTI_ANCESTRY,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.POPULATION_OF_INTEREST.AGE_GROUPS_ADOLESCENTS,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.POPULATION_OF_INTEREST.SEX_AT_BIRTH,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.POPULATION_OF_INTEREST.GENDER_IDENTITY,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.POPULATION_OF_INTEREST.GEOGRAPHY_RURAL,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.POPULATION_OF_INTEREST.EDUCATION_LEVEL_HIGHSCHOOL,
      selected: true
   },
   {
      id: EDIT_FIELD_FINDER.POPULATION_OF_INTEREST.DISABILITY_STATUS_WITH_DISABILITY,
      selected: true
   }
];

// Question #6: Request for Review of Research Purpose Description
export const defaultRequestForReviewAnswers = [
   {
      id: EDIT_FIELD_FINDER.REQUEST_FOR_REVIEW.NO_REQUEST_REVIEW,
      selected: true
   }
];
