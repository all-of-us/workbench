import {makeString} from 'util/helper';
import {fields} from '../app/workspace-edit-page';


// Question #1: What is the primary purpose of your project?
export const defaultAnswersPrimaryPurpose = [
  {
    id: fields.PRIMARY_PURPOSE.diseaseFocusedResearchCheckbox,
    selected: true,
    value: 'diabetic cataract'
  },
  {
    id: fields.PRIMARY_PURPOSE.methodsDevelopmentValidationStudyCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.researchControlCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.geneticResearchCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.socialBehavioralResearchCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.populationHealthCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.ethicalLegalSocialImplicationsResearchCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.drugTherapeuticsDevelopmentResearchCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.educationPurposeCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.forProfitPurposeCheckbox,
    selected: true
  },
  {
    id: fields.PRIMARY_PURPOSE.otherPurposeCheckbox,
    selected: true,
    value: makeString(50)
  }
];

// Question #2: Please provide a summary of your research purpose by responding to the question.
export const defaultAnswersResearchPurposeSummary = [
  {
    id: fields.RESEARCH_PURPOSE_SUMMARY.scientificQuestionsIntentToStudyTextarea,
    value: makeString(100)
  }, {
    id: fields.RESEARCH_PURPOSE_SUMMARY.scientificApproachesToUseTextarea,
    value: makeString(100)
  }, {
    id: fields.RESEARCH_PURPOSE_SUMMARY.anticipatedFindingsFromStudyTextarea,
    value: makeString(100)
  }
];

// Question #3: Please tell us how you plan to disseminate your research findings.
export const defaultAnswersDisseminateResearchFindings = [
  {
    id: fields.DISSEMINATE_RESEARCH_FINDINGS.publicationInScientificJournalsCheckbox,
    selected: true
  },
  {
    id: fields.DISSEMINATE_RESEARCH_FINDINGS.socialMediaCheckbox,
    selected: true
  },
  {
    id: fields.DISSEMINATE_RESEARCH_FINDINGS.presentationAtScientificConferencesCheckbox,
    selected: true
  },
  {
    id: fields.DISSEMINATE_RESEARCH_FINDINGS.presentationAtCommunityForumsCheckbox,
    selected: true
  },
  {
    id: fields.DISSEMINATE_RESEARCH_FINDINGS.pressReleaseCheckbox,
    selected: true
  },
  {
    id: fields.DISSEMINATE_RESEARCH_FINDINGS.publicationInCommunityJournalsCheckbox,
    selected: true
  },
  {
    id: fields.DISSEMINATE_RESEARCH_FINDINGS.publicationInPersonalBlogCheckbox,
    selected: true
  },
  {
    id: fields.DISSEMINATE_RESEARCH_FINDINGS.otherCheckbox,
    selected: true,
    value: makeString(20)
  }
];

// Question #4: Please select all of the statements below that describe the outcomes you anticipate from your research.
export const defaultAnswersAnticipatedOutcomesFromResearch = [
  {
    id: fields.DESCRIBE_ANTICIPATED_OUTCOMES.seeksIncreaseWellnessCheckbox,
    selected: true
  },
  {
    id: fields.DESCRIBE_ANTICIPATED_OUTCOMES.seeksToReduceHealthDisparitiesCheckbox,
    selected: true
  },
  {
    id: fields.DESCRIBE_ANTICIPATED_OUTCOMES.seeksToDevelopRiskAssessmentCheckbox,
    selected: true
  },
  {
    id: fields.DESCRIBE_ANTICIPATED_OUTCOMES.seeksToProvideEarlierDiagnosisCheckbox,
    selected: true
  },
  {
    id: fields.DESCRIBE_ANTICIPATED_OUTCOMES.seeksToReduceBurdenCheckbox,
    selected: true
  }
];

// Question #5: Population of interest
export const defaultAnswersPopulationOfInterest = [
  {
    id: fields.POPULATION_OF_INTEREST.yesOnUnderrepresentedPopulationRadiobutton,
    selected: true
  },
  {
    id: fields.POPULATION_OF_INTEREST.raceMultiAncestryCheckbox,
    selected: true
  },
  {
    id: fields.POPULATION_OF_INTEREST.ageGroupsAdolescentsCheckbox,
    selected: true
  },
  {
    id: fields.POPULATION_OF_INTEREST.sexAtBirthCheckbox,
    selected: true
  },
  {
    id: fields.POPULATION_OF_INTEREST.genderIdentityCheckbox,
    selected: true
  },
  {
    id: fields.POPULATION_OF_INTEREST.geographyRuralCheckbox,
    selected: true
  },
  {
    id: fields.POPULATION_OF_INTEREST.educationLevelHighSchoolCheckbox,
    selected: true
  },
  {
    id: fields.POPULATION_OF_INTEREST.disabilityStatusWithDisabilityCheckbox,
    selected: true
  }
];

// Question #6: Request for Review of Research Purpose Description
export const defaultAnswersRequestForReview = [
  {
    id: fields.REQUEST_FOR_REVIEW.noRequestReviewRadiobutton,
    selected: true
  }
];
