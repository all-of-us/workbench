import {ElementHandle, Page} from 'puppeteer';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import Select from 'app/element/select';
import SelectMenu from 'app/component/select-menu';
import Textbox from 'app/element/textbox';
import WebComponent from 'app/element/web-component';
import AuthenticatedPage from 'app/page/authenticated-page';
import Dialog, {ButtonLabel} from 'app/component/dialog';

const faker = require('faker/locale/en_US');

export const PAGE = {
  TITLE: 'Create Workspace',
};

export const LABEL_ALIAS = {
  SYNTHETIC_DATASET: 'Workspace Name',  // select Synthetic DataSet
  SELECT_BILLING: 'Select account',   // select billing account
  CREATE_WORKSPACE: 'Create Workspace',  // button CREATE WORKSPACE on edit page
  DUPLICATE_WORKSPACE: 'Duplicate Workspace', // button DUPLICATE WORKSPACE on edit page
  CANCEL: `Cancel`,  // button CANCEL on edit page
  WORKSPACE_NAME: 'Workspace Name',  // Workspace name input textbox
  RESEARCH_PURPOSE: 'Research purpose',
  EDUCATION_PURPOSE: 'Educational Purpose',
  FOR_PROFIT_PURPOSE: 'For-Profit Purpose',
  OTHER_PURPOSE: 'Other Purpose',
  DISEASE_FOCUSED_RESEARCH: 'Disease-focused research',
  POPULATION_HEALTH: 'Population Health/Public Health Research',
  METHODS_DEVELOPMENT: 'Methods development/validation study',
  DRUG_THERAPEUTIC_DEVELOPMENT: 'Drug/Therapeutics Development Research',
  RESEARCH_CONTROL: 'Research Control',
  GENETIC_RESEARCH: 'Genetic Research',
  SOCIAL_BEHAVIORAL_RESEARCH: 'Social/Behavioral Research',
  ETHICAL_LEGAL_SOCIAL_IMPLICATIONS: 'Ethical, Legal, and Social Implications (ELSI) Research',
  INTENT_TO_STUDY: 'What are the specific scientific question(s) you intend to study',
  SCIENTIFIC_APPROACHES: 'What are the scientific approaches you plan to use for your study',
  ANTICIPATED_FINDINGS: 'What are the anticipated findings from the study',
  PUBLICATION_IN_JOURNALS: 'Publication in peer-reviewed scientific journals',
  SOCIAL_MEDIA: 'Social media (Facebook, Instagram, Twitter)',
  PRESENTATION_AT_CONFERENCES: 'Presentation at national or international scientific conferences',
  PRESENTATION_AT_COMMUNITY_FORUMS: 'Presentation at community forums or advisory groups',
  PRESS_RELEASE: 'Press release or media article covering scientific publication',
  PUBLICATION_IN_COMMUNITY_JOURNALS: 'Publication in community-based journals or blog',
  PUBLICATION_IN_PERSONAL_BLOG: 'Publication of article in a personal blog',
  OTHER: 'Other',
  INCREASE_WELLNESS: 'This research project seeks to increase wellness and resilience',
  SEEKS_TO_REDUCE_HEALTH_DISPARITIES: 'This research project seeks to reduce health disparities and improve health equity ',
  SEEKS_TO_DEVELOP_RISK_ASSESSMENT: 'This research project seeks to develop improved risk assessment and prevention strategies to preempt disease',
  SEEKS_TO_PROVIDE_EARLIER_ACCURATE_DIAGNOSIS: 'This research project seeks to provide earlier and more accurate diagnosis to decrease illness burden',
  SEEKS_TO_REDUCE_BURDEN: 'This research project seeks to improve health outcomes and reduce disease/illness burden',
  YES_FOCUS_ON_UNDERREPRESENTED_POPULATION: 'Yes, my study will focus on one or more specific underrepresented populations',
  NO_FOCUS_ON_UNDERREPRESENTED_POPULATION: 'No, my study will not center on underrepresented populations',
  RACE_MULTI_ANCESTRY: 'Multi-Ancestry or more than one race',
  AGE_GROUPS_ADOLESCENTS: 'Adolescents',
  SEX_AT_BIRTH: 'Participants who report something other than female or male as their sex at birth',
  GENDER_IDENTITY: 'Participants who identify as gender variant',
  GEOGRAPHY_RURAL: 'Participants who live in a rural or non-metropolitan setting',
  EDUCATION_LEVEL_HIGHSCHOOL: 'Participants with less than a high school degree or equivalent',
  DISABILITY_STATUS_WITH_DISABILITY: 'Participants with a physical and/or cognitive disability',
  NO_REQUEST_REVIEW: 'No, I have no concerns at this time about potential stigmatization',
  YES_REQUEST_REVIEW: 'Yes, I would like to request a review of my research purpose',
  SHARE_WITH_COLLABORATORS: 'Share workspace with the same set of collaborators', // visible when clone workspace
};


export const FIELD = {
  createWorkspaceButton: {
    type: 'button',
    textOption: {text: LABEL_ALIAS.CREATE_WORKSPACE}
  },
  duplicateWorkspaceButton: {
    type: 'button',
    textOption: {text: LABEL_ALIAS.DUPLICATE_WORKSPACE}
  },
  cancelWorkspaceButton: {
    type: 'button',
    textOption: {text: LABEL_ALIAS.CANCEL}
  },
  workspaceNameTextbox: {
    type: 'textbox',
    textOption: {
      text: LABEL_ALIAS.WORKSPACE_NAME,
      ancestorNodeLevel: 2
    }
  },
  dataSetSelect: {
    type: 'select',
    textOption: {text: LABEL_ALIAS.SYNTHETIC_DATASET}
  },
  billingAccountSelect: {
    type: 'select',
    textOption: {text: LABEL_ALIAS.SELECT_BILLING}
  },
  shareWithCollaboratorsCheckbox: {
    type: 'checkbox',
    textOption: {text: LABEL_ALIAS.SHARE_WITH_COLLABORATORS}
  },
  PRIMARY_PURPOSE: { // fields in question #1
    researchPurposeCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.RESEARCH_PURPOSE, ancestorNodeLevel: 2}
    },
    diseaseFocusedResearchCheckbox: {
      type: 'checkbox',
      textOption:  {text: LABEL_ALIAS.DISEASE_FOCUSED_RESEARCH, ancestorNodeLevel: 2},
      affiliated: 'textbox',
    },
    methodsDevelopmentValidationStudyCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.METHODS_DEVELOPMENT, ancestorNodeLevel: 2}
    },
    researchControlCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.RESEARCH_CONTROL, ancestorNodeLevel: 2}
    },
    geneticResearchCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.GENETIC_RESEARCH, ancestorNodeLevel: 2}
    },
    socialBehavioralResearchCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.SOCIAL_BEHAVIORAL_RESEARCH, ancestorNodeLevel: 2}
    },
    populationHealthCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.POPULATION_HEALTH, ancestorNodeLevel: 2}
    },
    ethicalLegalSocialImplicationsResearchCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.ETHICAL_LEGAL_SOCIAL_IMPLICATIONS, ancestorNodeLevel: 2}
    },
    drugTherapeuticsDevelopmentResearchCheckbox: {
      type: 'checkbox',
      textOption:  {text: LABEL_ALIAS.DRUG_THERAPEUTIC_DEVELOPMENT, ancestorNodeLevel: 2}
    },
    educationPurposeCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.EDUCATION_PURPOSE, ancestorNodeLevel: 2}
    },
    forProfitPurposeCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.FOR_PROFIT_PURPOSE, ancestorNodeLevel: 2}
    },
    otherPurposeCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.OTHER_PURPOSE, ancestorNodeLevel: 2},
      affiliated: 'textarea'
    }
  },
  RESEARCH_PURPOSE_SUMMARY: {  // fields in question #2
    scientificQuestionsIntentToStudyTextarea: {
      type: 'textarea',
      textOption: {textContains: LABEL_ALIAS.INTENT_TO_STUDY, ancestorNodeLevel: 3}
    },
    scientificApproachesToUseTextarea: {
      type: 'textarea',
      textOption: {textContains: LABEL_ALIAS.SCIENTIFIC_APPROACHES, ancestorNodeLevel: 3}
    },
    anticipatedFindingsFromStudyTextarea: {
      type: 'textarea',
      textOption: {textContains: LABEL_ALIAS.ANTICIPATED_FINDINGS, ancestorNodeLevel: 3}
    }
  },
  DISSEMINATE_RESEARCH_FINDINGS: {  // fields in question #3
    publicationInScientificJournalsCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PUBLICATION_IN_JOURNALS, ancestorNodeLevel: 2}
    },
    socialMediaCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SOCIAL_MEDIA, ancestorNodeLevel: 2}
    },
    presentationAtScientificConferencesCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PRESENTATION_AT_CONFERENCES, ancestorNodeLevel: 2}
    },
    presentationAtCommunityForumsCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PRESENTATION_AT_COMMUNITY_FORUMS, ancestorNodeLevel: 2}
    },
    pressReleaseCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PRESS_RELEASE, ancestorNodeLevel: 2}
    },
    publicationInCommunityJournalsCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PUBLICATION_IN_COMMUNITY_JOURNALS, ancestorNodeLevel: 2}
    },
    publicationInPersonalBlogCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PUBLICATION_IN_PERSONAL_BLOG, ancestorNodeLevel: 2}
    },
    otherCheckbox: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.OTHER, ancestorNodeLevel: 2},
      affiliated: 'textarea'
    }
  },
  DESCRIBE_ANTICIPATED_OUTCOMES: {  // fields in question #4
    seeksIncreaseWellnessCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.INCREASE_WELLNESS, ancestorNodeLevel: 1}
    },
    seeksToReduceHealthDisparitiesCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEEKS_TO_REDUCE_HEALTH_DISPARITIES, ancestorNodeLevel: 1}
    },
    seeksToDevelopRiskAssessmentCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEEKS_TO_DEVELOP_RISK_ASSESSMENT, ancestorNodeLevel: 1}
    },
    seeksToProvideEarlierDiagnosisCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEEKS_TO_PROVIDE_EARLIER_ACCURATE_DIAGNOSIS, ancestorNodeLevel: 1}
    },
    seeksToReduceBurdenCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEEKS_TO_REDUCE_BURDEN, ancestorNodeLevel: 1}
    }
  },
  POPULATION_OF_INTEREST: {  // fields in question #5
    yesOnUnderrepresentedPopulationRadiobutton: {
      type: 'radiobutton',
      textOption: {textContains: LABEL_ALIAS.YES_FOCUS_ON_UNDERREPRESENTED_POPULATION}
    },
    noUnderrepresentedPopulationRadiobutton: {
      type: 'radiobutton',
      textOption: {textContains: LABEL_ALIAS.NO_FOCUS_ON_UNDERREPRESENTED_POPULATION}
    },
    raceMultiAncestryCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.RACE_MULTI_ANCESTRY, ancestorNodeLevel: 1}
    },
    ageGroupsAdolescentsCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.AGE_GROUPS_ADOLESCENTS, ancestorNodeLevel: 1}
    },
    sexAtBirthCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEX_AT_BIRTH, ancestorNodeLevel: 1}
    },
    genderIdentityCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.GENDER_IDENTITY, ancestorNodeLevel: 1}
    },
    geographyRuralCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.GEOGRAPHY_RURAL, ancestorNodeLevel: 1}
    },
    educationLevelHighSchoolCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.EDUCATION_LEVEL_HIGHSCHOOL, ancestorNodeLevel: 1}
    },
    disabilityStatusWithDisabilityCheckbox: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.DISABILITY_STATUS_WITH_DISABILITY, ancestorNodeLevel: 1}
    }
  },
  REQUEST_FOR_REVIEW: {  // fields in question #6
    yesRequestReviewRadiobutton: {
      type: 'radiobutton',
      textOption: {textContains: LABEL_ALIAS.YES_REQUEST_REVIEW}
    },
    noRequestReviewRadiobutton: {
      type: 'radiobutton',
      textOption: {textContains: LABEL_ALIAS.NO_REQUEST_REVIEW}
    }
  }

};


export default class WorkspaceEditPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitUntilTitleMatch(PAGE.TITLE);
      await this.getWorkspaceNameTextbox();
      await new SelectMenu(this.page, LABEL_ALIAS.SELECT_BILLING).getSelectedValue();
      await this.getCreateWorkspaceButton();
      return true;
    } catch (e) {
      return false;
    }
  }

  async getCreateWorkspaceButton(): Promise<Button> {
    return Button.forLabel(this.page, FIELD.createWorkspaceButton.textOption);
  }

  async getDuplicateWorkspaceButton(): Promise<Button> {
    // Cannot use Button.forLabel because it finds two elements on Duplicate workspace page.
    // Don't change. use this xpath to find the button "DUPLICATE WORKSPACE".
    const elemt = await this.page.waitForXPath(`//*[text()="Duplicate Workspace" and @role="button"]`);
    return new Button(this.page, elemt);
  }

  async getCancelButton(): Promise<Button> {
    return Button.forLabel(this.page, FIELD.cancelWorkspaceButton.textOption);
  }

  async getWorkspaceNameTextbox(): Promise<Textbox> {
    return await Textbox.forLabel(this.page, FIELD.workspaceNameTextbox.textOption);
  }

  question1_researchPurpose(): WebComponent {
    return new WebComponent(this.page, FIELD.PRIMARY_PURPOSE.researchPurposeCheckbox.textOption);
  }

  question1_educationalPurpose(): WebComponent {
    return new WebComponent(this.page, FIELD.PRIMARY_PURPOSE.educationPurposeCheckbox.textOption);
  }

  question1_forProfitPurpose(): WebComponent {
    return new WebComponent(this.page, FIELD.PRIMARY_PURPOSE.forProfitPurposeCheckbox.textOption);
  }

  question1_otherPurpose(): WebComponent {
    return new WebComponent(this.page, FIELD.PRIMARY_PURPOSE.otherPurposeCheckbox.textOption);
  }

  question1_diseaseFocusedResearch(): WebComponent {
    return new WebComponent(this.page, FIELD.PRIMARY_PURPOSE.diseaseFocusedResearchCheckbox.textOption);
  }

  question2_scientificQuestionsIntendToStudy(): WebComponent {
    return new WebComponent(this.page, FIELD.RESEARCH_PURPOSE_SUMMARY.scientificQuestionsIntentToStudyTextarea.textOption);
  }

  question2_scientificApproaches(): WebComponent {
    return new WebComponent(this.page, FIELD.RESEARCH_PURPOSE_SUMMARY.scientificApproachesToUseTextarea.textOption);
  }

  question2_anticipatedFindings(): WebComponent {
    return new WebComponent(this.page, FIELD.RESEARCH_PURPOSE_SUMMARY.anticipatedFindingsFromStudyTextarea.textOption);
  }

  // Question 3. one of many checkboxes
  publicationInJournal(): WebComponent {
    return new WebComponent(this.page, FIELD.DISSEMINATE_RESEARCH_FINDINGS.publicationInScientificJournalsCheckbox.textOption);
  }

  // Question 4. one of many checkboxes
  increaseWellnessResilience(): WebComponent {
    return new WebComponent(this.page, FIELD.DESCRIBE_ANTICIPATED_OUTCOMES.seeksIncreaseWellnessCheckbox.textOption);
  }

  /**
   * Select Synthetic DataSet.
   * @param {string} optionValue: 1 for "Synthetic DataSet 1". 2 for "Synthetic DataSet 2".
   */
  async selectDataSet(optionValue: string = '2') {
    const dataSetSelect = await Select.forLabel(this.page, FIELD.dataSetSelect.textOption);
    await dataSetSelect.selectOption(optionValue);
  }

  /**
   * Select Billing Account
   * @param {string} billingAccount
   */
  async selectBillingAccount(billingAccount: string = 'Use All of Us free credits') {
    const billingAccountSelect = await Select.forLabel(this.page, FIELD.billingAccountSelect.textOption);
    await billingAccountSelect.selectOption(billingAccount);
  }

  /**
   * Assumption: Checked checkbox means to expand the section, hidden questions will become visible.
   * @param {boolean} yesOrNo: True means to check checkbox. False means to uncheck.
   */
  async expandResearchPurposeGroup(yesOrNo: boolean = true) {
    // expand Disease purpose section if needed
    const researchPurpose = this.question1_researchPurpose();
    const researchPurposeCheckbox = await researchPurpose.asCheckBox();
    const is = await researchPurposeCheckbox.isChecked();
    if (yesOrNo !== is) {
      // click checkbox expands or collapses the section, reveal hidden questions contained inside.
      await researchPurposeCheckbox.check();
    }
  }

  /**
   *  Enter value in 'Disease-focused research' textbox
   * @param {string} diseaseName
   */
  async fillOutDiseaseFocusedResearch(diseaseName: string = 'diabetic cataract') {
    const diseaseNameComponent = this.question1_diseaseFocusedResearch();
    await (await diseaseNameComponent.asCheckBox()).check();
    await (await diseaseNameComponent.asTextBox()).type(diseaseName);
    await (await diseaseNameComponent.asTextBox()).tabKey();
  }

  /**
   * Enter value in Other Purpose textarea
   * @param {string} value
   */
  async fillOutOtherPurpose(value?: string) {
    if (value === undefined) {
      value = faker.lorem.paragraph();
    }
    // check Other-Purpose checkbox
    const otherPurpose = this.question1_otherPurpose();
    await (await otherPurpose.asCheckBox()).check(); // enables textarea
    await (await otherPurpose.asTextArea()).type(value);
  }

  /**
   * Question 6. Request for Review of Research Purpose Description
   * @param selected: True means select "Yes, Request Review" radiobutton. False means select "No, Request Review" radiobutton.
   */
  async requestForReviewRadiobutton(selected: boolean) {
    let radioComponent;
    if (selected) {
      radioComponent = new WebComponent(this.page, FIELD.REQUEST_FOR_REVIEW.yesRequestReviewRadiobutton.textOption);
    } else {
      radioComponent = new WebComponent(this.page, FIELD.REQUEST_FOR_REVIEW.noRequestReviewRadiobutton.textOption);
    }
    await (await radioComponent.asRadioButton()).select();
  }

  /**
   * Find and click the CREATE WORKSPACE (FINISH) button
   */
  async clickCreateFinishButton(button: ElementHandle | Button): Promise<void> {
    await button.focus(); // bring into viewport
    await button.click();

    // confirm create in pop-up dialog
    const dialog = new Dialog(this.page);
    await Promise.all([
      dialog.clickButton(ButtonLabel.Confirm),
      dialog.waitUntilDialogIsClosed(),
      this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0'], timeout: 60000}),
    ]);
    await this.waitUntilNoSpinner();
  }

  async clickShareWithCollaboratorsCheckbox() {
    const elemt = await Checkbox.forLabel(this.page, FIELD.shareWithCollaboratorsCheckbox.textOption);
    await elemt.check();
  }

}
