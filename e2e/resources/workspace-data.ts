import {makeString} from 'resources/fake-user';
import {FIELD} from 'app/workspace-edit-page';

// Question #1: What is the primary purpose of your project?
export const defaultAnswersPrimaryPurpose = [
  {
    id: FIELD.PRIMARY_PURPOSE.diseaseFocusedResearchCheckbox,
    selected: true,
    value: 'diabetic cataract'
  },
  {
    id: FIELD.PRIMARY_PURPOSE.methodsDevelopmentValidationStudyCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.researchControlCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.geneticResearchCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.socialBehavioralResearchCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.populationHealthCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.ethicalLegalSocialImplicationsResearchCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.drugTherapeuticsDevelopmentResearchCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.educationPurposeCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.forProfitPurposeCheckbox,
    selected: true
  },
  {
    id: FIELD.PRIMARY_PURPOSE.otherPurposeCheckbox,
    selected: true,
    value: makeString(50)
  }
];

// Question #2: Please provide a summary of your research purpose by responding to the question.
export const defaultAnswersResearchPurposeSummary = [
  {
    id: FIELD.RESEARCH_PURPOSE_SUMMARY.scientificQuestionsIntentToStudyTextarea,
    value: makeString(100)
  }, {
    id: FIELD.RESEARCH_PURPOSE_SUMMARY.scientificApproachesToUseTextarea,
    value: makeString(100)
  }, {
    id: FIELD.RESEARCH_PURPOSE_SUMMARY.anticipatedFindingsFromStudyTextarea,
    value: makeString(100)
  }
];

// Question #3: Please tell us how you plan to disseminate your research findings.
export const defaultAnswersDisseminateResearchFindings = [
  {
    id: FIELD.DISSEMINATE_RESEARCH_FINDINGS.publicationInScientificJournalsCheckbox,
    selected: true
  },
  {
    id: FIELD.DISSEMINATE_RESEARCH_FINDINGS.socialMediaCheckbox,
    selected: true
  },
  {
    id: FIELD.DISSEMINATE_RESEARCH_FINDINGS.presentationAtScientificConferencesCheckbox,
    selected: true
  },
  {
    id: FIELD.DISSEMINATE_RESEARCH_FINDINGS.presentationAtCommunityForumsCheckbox,
    selected: true
  },
  {
    id: FIELD.DISSEMINATE_RESEARCH_FINDINGS.pressReleaseCheckbox,
    selected: true
  },
  {
    id: FIELD.DISSEMINATE_RESEARCH_FINDINGS.publicationInCommunityJournalsCheckbox,
    selected: true
  },
  {
    id: FIELD.DISSEMINATE_RESEARCH_FINDINGS.publicationInPersonalBlogCheckbox,
    selected: true
  },
  {
    id: FIELD.DISSEMINATE_RESEARCH_FINDINGS.otherCheckbox,
    selected: true,
    value: makeString(20)
  }
];

// Question #4: Please select all of the statements below that describe the outcomes you anticipate from your research.
export const defaultAnswersAnticipatedOutcomesFromResearch = [
  {
    id: FIELD.DESCRIBE_ANTICIPATED_OUTCOMES.seeksIncreaseWellnessCheckbox,
    selected: true
  },
  {
    id: FIELD.DESCRIBE_ANTICIPATED_OUTCOMES.seeksToReduceHealthDisparitiesCheckbox,
    selected: true
  },
  {
    id: FIELD.DESCRIBE_ANTICIPATED_OUTCOMES.seeksToDevelopRiskAssessmentCheckbox,
    selected: true
  },
  {
    id: FIELD.DESCRIBE_ANTICIPATED_OUTCOMES.seeksToProvideEarlierDiagnosisCheckbox,
    selected: true
  },
  {
    id: FIELD.DESCRIBE_ANTICIPATED_OUTCOMES.seeksToReduceBurdenCheckbox,
    selected: true
  }
];

// Question #5: Population of interest
export const defaultAnswersPopulationOfInterest = [
  {
    id: FIELD.POPULATION_OF_INTEREST.yesOnUnderrepresentedPopulationRadiobutton,
    selected: true
  },
  {
    id: FIELD.POPULATION_OF_INTEREST.raceMultiAncestryCheckbox,
    selected: true
  },
  {
    id: FIELD.POPULATION_OF_INTEREST.ageGroupsAdolescentsCheckbox,
    selected: true
  },
  {
    id: FIELD.POPULATION_OF_INTEREST.sexAtBirthCheckbox,
    selected: true
  },
  {
    id: FIELD.POPULATION_OF_INTEREST.genderIdentityCheckbox,
    selected: true
  },
  {
    id: FIELD.POPULATION_OF_INTEREST.geographyRuralCheckbox,
    selected: true
  },
  {
    id: FIELD.POPULATION_OF_INTEREST.educationLevelHighSchoolCheckbox,
    selected: true
  },
  {
    id: FIELD.POPULATION_OF_INTEREST.disabilityStatusWithDisabilityCheckbox,
    selected: true
  }
];

// Question #6: Request for Review of Research Purpose Description
export const defaultAnswersRequestForReview = [
  {
    id: FIELD.REQUEST_FOR_REVIEW.noRequestReviewRadiobutton,
    selected: true
  }
];
