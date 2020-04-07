import {ElementHandle, Page} from 'puppeteer';
import Button from './aou-elements/button';
import Select from './aou-elements/select';
import SelectComponent from './aou-elements/select-component';
import Textbox from './aou-elements/textbox';
import WebComponent from './aou-elements/web-component';
import {findButton} from './aou-elements/xpath-finder';
import AuthenticatedPage from './authenticated-page';

const faker = require('faker/locale/en_US');

export const PAGE = {
  TITLE: 'Create Workspace',
};

export const LABEL_ALIAS = {
  SYNTHETIC_DATASET: 'Workspace Name',  // select Synthetic DataSet
  SELECT_BILLING: 'Select account',   // select billing account
  CREATE_WORKSPACE: 'Create Workspace',  // button CREATE WORKSPACE
  CANCEL: `Cancel`,  // button CANCEL
  NEW_WORKSPACE_NAME: 'Create a new Workspace',  // Workspace name input textbox
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
};


export const FIELD_FINDER = {
  CREATE_WORKSPACE_BUTTON: {
    type: 'button',
    textOption: {text: LABEL_ALIAS.CREATE_WORKSPACE}
  },
  CANCEL_WORKSPACE_BUTTON: {
    type: 'button',
    textOption: {text: LABEL_ALIAS.CANCEL}
  },
  WORKSPACE_NAME: {
    type: 'textbox',
    textOption: {
      text: LABEL_ALIAS.NEW_WORKSPACE_NAME,
      ancestorNodeLevel: 2
    }
  },
  DATASET: {
    type: 'select',
    textOption: {text: LABEL_ALIAS.SYNTHETIC_DATASET}
  },
  PRIMARY_PURPOSE: { // fields in question #1
    RESEARCH_PURPOSE: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.RESEARCH_PURPOSE, ancestorNodeLevel: 2}
    },
    DISEASE_FOCUSED_RESEARCH: {
      type: 'checkbox',
      textOption:  {text: LABEL_ALIAS.DISEASE_FOCUSED_RESEARCH, ancestorNodeLevel: 2},
      affiliated: 'textbox',
    },
    METHODS_DEVELOPMENT_VALIDATION_STUDY: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.METHODS_DEVELOPMENT, ancestorNodeLevel: 2}
    },
    RESEARCH_CONTROL: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.RESEARCH_CONTROL, ancestorNodeLevel: 2}
    },
    GENETIC_RESEARCH: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.GENETIC_RESEARCH, ancestorNodeLevel: 2}
    },
    SOCIAL_BEHAVIORAL_RESEARCH: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.SOCIAL_BEHAVIORAL_RESEARCH, ancestorNodeLevel: 2}
    },
    POPULATION_HEALTH: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.POPULATION_HEALTH, ancestorNodeLevel: 2}
    },
    ETHICAL_LEGAL_SOCIAL_IMPLICATIONS_RESEARCH: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.ETHICAL_LEGAL_SOCIAL_IMPLICATIONS, ancestorNodeLevel: 2}
    },
    DRUG_THERAPEUTICS_DEVELOPMENT_RESEARCH: {
      type: 'checkbox',
      textOption:  {text: LABEL_ALIAS.DRUG_THERAPEUTIC_DEVELOPMENT, ancestorNodeLevel: 2}
    },
    EDUCATION_PURPOSE: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.EDUCATION_PURPOSE, ancestorNodeLevel: 2}
    },
    FOR_PROFIT_PURPOSE: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.FOR_PROFIT_PURPOSE, ancestorNodeLevel: 2}
    },
    OTHER_PURPOSE: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.OTHER_PURPOSE, ancestorNodeLevel: 2},
      affiliated: 'textarea'
    }
  },
  RESEARCH_PURPOSE_SUMMARY: {
    SCIENTIFIC_QUESTIONS_INTENT_TO_STUDY: {
      type: 'textarea',
      textOption: {textContains: LABEL_ALIAS.INTENT_TO_STUDY, ancestorNodeLevel: 3}
    },
    SCIENTIFIC_APPROACHES_TO_USE: {
      type: 'textarea',
      textOption: {textContains: LABEL_ALIAS.SCIENTIFIC_APPROACHES, ancestorNodeLevel: 3}
    },
    ANTICIPATED_FINDINGS_FROM_STUDY: {
      type: 'textarea',
      textOption: {textContains: LABEL_ALIAS.ANTICIPATED_FINDINGS, ancestorNodeLevel: 3}
    }
  },
  DISSEMINATE_RESEARCH_FINDINGS: {
    PUBLICATION_IN_SCIENTIFC_JOURNALS: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PUBLICATION_IN_JOURNALS, ancestorNodeLevel: 2}
    },
    SOCIAL_MEDIA: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SOCIAL_MEDIA, ancestorNodeLevel: 2}
    },
    PRESENTATION_AT_SCIENTIFIC_CONFERENCES: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PRESENTATION_AT_CONFERENCES, ancestorNodeLevel: 2}
    },
    PRESENTATION_AT_COMMUNITY_FORUMS: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PRESENTATION_AT_COMMUNITY_FORUMS, ancestorNodeLevel: 2}
    },
    PRESS_RELEASE: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PRESS_RELEASE, ancestorNodeLevel: 2}
    },
    PUBLICATION_IN_COMMUNITY_JOURNALS: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PUBLICATION_IN_COMMUNITY_JOURNALS, ancestorNodeLevel: 2}
    },
    PUBLICATION_IN_PERSONAL_BLOG: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.PUBLICATION_IN_PERSONAL_BLOG, ancestorNodeLevel: 2}
    },
    OTHER: {
      type: 'checkbox',
      textOption: {text: LABEL_ALIAS.OTHER, ancestorNodeLevel: 2},
      affiliated: 'textarea'
    }
  },
  DESCRIBE_ANTICIPATED_OUTCOMES: {
    SEEKS_INCREASE_WELLNESS: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.INCREASE_WELLNESS, ancestorNodeLevel: 1}
    },
    SEEKS_TO_REDUCE_HEALTH_DISPARITIES: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEEKS_TO_REDUCE_HEALTH_DISPARITIES, ancestorNodeLevel: 1}
    },
    SEEKS_TO_DEVELOP_RISK_ASSESSMENT: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEEKS_TO_DEVELOP_RISK_ASSESSMENT, ancestorNodeLevel: 1}
    },
    SEEKS_TO_PROVIDE_EARLIER_ACCURATE_DIAGNOSIS: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEEKS_TO_PROVIDE_EARLIER_ACCURATE_DIAGNOSIS, ancestorNodeLevel: 1}
    },
    SEEKS_TO_REDUCE_BURDEN: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEEKS_TO_REDUCE_BURDEN, ancestorNodeLevel: 1}
    }
  },
  POPULATION_OF_INTEREST: {
    YES: {
      type: 'radiobutton',
      textOption: {textContains: LABEL_ALIAS.YES_FOCUS_ON_UNDERREPRESENTED_POPULATION}
    },
    NO: {
      type: 'radiobutton',
      textOption: {textContains: LABEL_ALIAS.NO_FOCUS_ON_UNDERREPRESENTED_POPULATION}
    },
    RACE_MULTI_ANCESTRY: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.RACE_MULTI_ANCESTRY, ancestorNodeLevel: 1}
    },
    AGE_GROUPS_ADOLESCENTS: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.AGE_GROUPS_ADOLESCENTS, ancestorNodeLevel: 1}
    },
    SEX_AT_BIRTH: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.SEX_AT_BIRTH, ancestorNodeLevel: 1}
    },
    GENDER_IDENTITY: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.GENDER_IDENTITY, ancestorNodeLevel: 1}
    },
    GEOGRAPHY_RURAL: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.GEOGRAPHY_RURAL, ancestorNodeLevel: 1}
    },
    EDUCATION_LEVEL_HIGHSCHOOL: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.EDUCATION_LEVEL_HIGHSCHOOL, ancestorNodeLevel: 1}
    },
    DISABILITY_STATUS_WITH_DISABILITY: {
      type: 'checkbox',
      textOption: {textContains: LABEL_ALIAS.DISABILITY_STATUS_WITH_DISABILITY, ancestorNodeLevel: 1}
    }
  },
  REQUEST_FOR_REVIEW: {
    YES_REQUEST_REVIEW: {
      type: 'radiobutton',
      textOption: {textContains: LABEL_ALIAS.YES_REQUEST_REVIEW}
    },
    NO_REQUEST_REVIEW: {
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
      await this.getDataSetSelectOption();
      await new SelectComponent(this.page, LABEL_ALIAS.SELECT_BILLING).getSelectedValue();
      await this.getCreateWorkspaceButton();
      return true;
    } catch (e) {
      return false;
    }
  }

  async getCreateWorkspaceButton(): Promise<Button> {
    return Button.forLabel(this.page, {text: LABEL_ALIAS.CREATE_WORKSPACE}, {visible: true});
  }

  async getCancelButton(): Promise<ElementHandle> {
    return findButton(this.page, {text: LABEL_ALIAS.CANCEL}, {visible: true});
  }

  async getWorkspaceNameTextbox(): Promise<Textbox> {
    return await Textbox.forLabel(this.page, {text: LABEL_ALIAS.NEW_WORKSPACE_NAME, ancestorNodeLevel: 2});
  }

  getDataSetSelectOption(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.SYNTHETIC_DATASET});
  }

  question1_researchPurpose(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.RESEARCH_PURPOSE, ancestorNodeLevel: 2});
  }

  question1_educationalPurpose(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.EDUCATION_PURPOSE, ancestorNodeLevel: 2});
  }

  question1_forProfitPurpose(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.FOR_PROFIT_PURPOSE, ancestorNodeLevel: 2});
  }

  question1_otherPurpose(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.OTHER_PURPOSE, ancestorNodeLevel: 2});
  }

  question1_diseaseFocusedResearch(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.DISEASE_FOCUSED_RESEARCH, ancestorNodeLevel: 2});
  }

  question1_populationHealth(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.POPULATION_HEALTH, ancestorNodeLevel: 2});
  }

  question1_methodsDevelopmentValidationStudy(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.METHODS_DEVELOPMENT, ancestorNodeLevel: 2});
  }

  question1_drugTherapeuticsDevelopmentResearch(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.DRUG_THERAPEUTIC_DEVELOPMENT, ancestorNodeLevel: 2});
  }

  question1_researchControl(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.RESEARCH_CONTROL, ancestorNodeLevel: 2});
  }

  question1_geneticResearch(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.GENETIC_RESEARCH, ancestorNodeLevel: 2});
  }

  question1_socialBehavioralResearch(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.SOCIAL_BEHAVIORAL_RESEARCH, ancestorNodeLevel: 2});
  }

  question1_ethicalLegalSocialImplicationsResearch(): WebComponent {
    return new WebComponent(this.page, {text: LABEL_ALIAS.ETHICAL_LEGAL_SOCIAL_IMPLICATIONS, ancestorNodeLevel: 2});
  }

  question2_scientificQuestionsIntendToStudy(): WebComponent {
    return new WebComponent(this.page, {textContains: LABEL_ALIAS.INTENT_TO_STUDY, ancestorNodeLevel: 3});
  }

  question2_scientificApproaches(): WebComponent {
    return new WebComponent(this.page, {textContains: LABEL_ALIAS.SCIENTIFIC_APPROACHES, ancestorNodeLevel: 3});
  }

  question2_anticipatedFindings(): WebComponent {
    return new WebComponent(this.page, {textContains: LABEL_ALIAS.ANTICIPATED_FINDINGS, ancestorNodeLevel: 3});
  }

   // Question 3. one of many checkboxes
  publicationInJournal(): WebComponent {
    return new WebComponent(this.page, {textContains: LABEL_ALIAS.PUBLICATION_IN_JOURNALS, ancestorNodeLevel: 2});
  }

   // Question 4. one of many checkboxes
  increaseWellnessResilience(): WebComponent {
    return new WebComponent(this.page, {textContains: LABEL_ALIAS.INCREASE_WELLNESS, ancestorNodeLevel: 1});
  }

   /**
    * Select Data Set.
    * @param optionValue: 1 for selecting Data Set 1. 2 for Data Set 2.
    */
  async selectDataSet(optionValue: string) {
    const dataSetSelect = await Select.forLabel(this.page, {text: LABEL_ALIAS.SYNTHETIC_DATASET});
    await dataSetSelect.selectOption(optionValue);
  }

   /**
    * Select Billing Account
    */
  async selectBillingAccount(account: string) {
    const billingAccountSelect = await Select.forLabel(this.page, {text: LABEL_ALIAS.SELECT_BILLING});
    await billingAccountSelect.selectOption(account);
  }

   /**
    * Assumption: Checked checkbox means to expand the section, hidden questions will become visible.
    * @param yesOrNo: True means to check checkbox. False means to uncheck.
    */
  async expandResearchPurposeGroup(yesOrNo?: boolean) {
    if (yesOrNo === undefined) {
      yesOrNo = true;
    }
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
    * @param diseaseName
    */
  async fillOutDiseaseFocusedResearch(diseaseName?: string) {
    if (diseaseName === undefined) {
      diseaseName = 'diabetes';
    }
    const diseaseNameComponent = this.question1_diseaseFocusedResearch();
    await (await diseaseNameComponent.asCheckBox()).check();
    await (await diseaseNameComponent.asTextBox()).type(diseaseName);
    await (await diseaseNameComponent.asTextBox()).pressKeyboard('Tab', {delay: 10});
  }

   /**
    * Enter value in Other Purpose textarea
    * @param value
    */
  async fillOutOtherPurpose(value?: string) {
    if (value === undefined) {
      value = faker.lorem.word();
    }
      // check Other-Purpose checkbox
    const otherPurpose = this.question1_otherPurpose();
    await (await otherPurpose.asCheckBox()).check(); // enables textarea
    await (await otherPurpose.asTextArea()).type(value);
  }

   /**
    * Question 6. Request for Review of Research Purpose Description
    * @param yesOrNo: True means select "Yes, Request Review" radiobutton. False means select "No, Request Review" radiobutton.
    */
  async requestForReviewRadiobutton(yesOrNo: boolean) {
    let radioComponent;
    if (yesOrNo) {
      radioComponent = new WebComponent(this.page, {textContains: LABEL_ALIAS.YES_REQUEST_REVIEW});
    } else {
      radioComponent = new WebComponent(this.page, {textContains: LABEL_ALIAS.NO_REQUEST_REVIEW});
    }
    await (await radioComponent.asRadioButton()).select();
  }

   /**
    * Find and click the CREATE WORKSPACE (FINISH) button
    */
  async clickCreateFinishButton(): Promise<void> {
    const createButton = await this.getCreateWorkspaceButton();
    await createButton.focus(); // bring into viewport
    await this.clickAndWait(createButton);
    await this.waitUntilNoSpinner();
  }

}
