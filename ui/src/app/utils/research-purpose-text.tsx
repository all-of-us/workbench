import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  AIANResearchType,
  DisseminateResearchEnum,
  ResearchOutcomeEnum,
  SpecificPopulationEnum,
} from 'generated/fetch';

import { StyledExternalLink } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { AoU, AouTitle } from 'app/components/text-wrappers';
import colors from 'app/styles/colors';

export const ResearchPurposeDescription = (
  <div style={{ display: 'inline' }}>
    The <AouTitle /> requires each data user of the <AoU /> data to provide a
    meaningful description of the intended purpose of data use for each
    workspace they create. To provide transparency to <AouTitle /> participants,
    &nbsp;your answers below will be made available publicly in the{' '}
    <a
      target='_blank'
      href='https://www.researchallofus.org/research-projects-directory/'
    >
      Research Hub Directory{' '}
    </a>{' '}
    on our public website.{' '}
    <b>Your responses will not be used to make decisions about data access.</b>
    <hr />
    <i>
      Note that you are required to create separate workspaces for each project
      for which you access{' '}
    </i>{' '}
    All of Us{' '}
    <i>
      data, hence the responses below are expected to be specific to the project
      for which you are creating this particular workspace.
    </i>
  </div>
);

export interface ResearchPurposeItem {
  shortName: string;
  shortDescription: string;
  longDescription: React.ReactNode;
  uniqueId?: string;
}

export const ResearchPurposeItems: Array<ResearchPurposeItem> = [
  {
    shortName: 'diseaseFocusedResearch',
    shortDescription: 'Disease-focused research',
    longDescription: (
      <div>
        The primary purpose of the research is to learn more about a particular
        disease or disorder (e.g. type 2 diabetes), a trait (e.g. blood
        pressure), or a set of related conditions (e.g. autoimmune diseases,
        psychiatric disorders).
      </div>
    ),
  },
  {
    shortName: 'methodsDevelopment',
    shortDescription: 'Methods development/validation study',
    longDescription: (
      <div>
        The primary purpose of the use of <AoU /> data is to develop and/or
        validate specific methods/tools for analyzing or interpreting data (e.g.
        statistical methods for describing data trends, developing more powerful
        methods to detect gene-environment, or other types of interactions in
        genome-wide association studies).
      </div>
    ),
  },
  {
    shortName: 'controlSet',
    shortDescription: 'Research Control',
    longDescription: (
      <div>
        <AoU /> data will be used as a reference or control dataset for
        comparison with another dataset from a different resource (e.g.
        Case-control studies).
      </div>
    ),
  },
  {
    shortName: 'ancestry',
    shortDescription: 'Genetic Research',
    longDescription: (
      <div>
        Research concerning genetics (i.e. the study of genes, genetic
        variations, and heredity) in the context of diseases or ancestry.
      </div>
    ),
  },
  {
    shortName: 'socialBehavioral',
    shortDescription: 'Social/Behavioral Research',
    longDescription: (
      <div>
        The research focuses on the social or behavioral phenomena or
        determinants of health.
      </div>
    ),
  },
  {
    shortName: 'populationHealth',
    shortDescription: 'Population Health/Public Health Research',
    longDescription: (
      <div>
        The primary purpose of using <AoU /> data is to investigate health
        behaviors, outcomes, access, and disparities in populations.
      </div>
    ),
  },
  {
    shortName: 'ethics',
    shortDescription: 'Ethical, Legal, and Social Implications (ELSI) Research',
    longDescription: (
      <div>
        This research focuses on ethical, legal, and social implications (ELSI)
        of or related to design, conduct, and translation of research.
      </div>
    ),
  },
  {
    shortName: 'drugDevelopment',
    shortDescription: 'Drug/Therapeutics Development Research',
    longDescription: (
      <div>
        The primary focus of the research is drug/therapeutics development. The
        data will be used to understand treatment-gene interactions or treatment
        outcomes relevant to the therapeutic(s) of interest.
      </div>
    ),
  },
];
ResearchPurposeItems.forEach((item) => {
  item.uniqueId = fp.uniqueId('research-purpose');
});

export const PrimaryPurposeItems: Array<ResearchPurposeItem> = [
  {
    shortName: 'educational',
    shortDescription: 'Educational Purpose',
    uniqueId: 'education-purpose',
    longDescription: (
      <div>
        The data will be used for education purposes (e.g. for a college
        research methods course, to educate students on population-based
        research approaches).
      </div>
    ),
  },
  {
    shortName: 'commercialPurpose',
    shortDescription: 'For-Profit Purpose',
    longDescription: (
      <div>
        The data will be used by a for-profit entity for research or product or
        service development (e.g. for understanding drug responses as part of a
        pharmaceutical company's drug development or market research efforts).
      </div>
    ),
  },
  {
    shortName: 'otherPurpose',
    shortDescription: 'Other Purpose',
    uniqueId: 'other-purpose',
    longDescription: (
      <div>
        If your purpose of use is different from the options listed above,
        please select "Other Purpose" and provide details regarding your purpose
        of data use here (500 character limit).
      </div>
    ),
  },
];

export interface ResearchPurposeQuestion {
  header: React.ReactNode;
  description: React.ReactNode;
}

export const researchPurposeQuestions: Array<ResearchPurposeQuestion> = [
  {
    header: ' What is the primary purpose of your project?',
    description: '',
  },
  {
    header: (
      <div>
        {' '}
        Please provide a summary of your research purpose by responding to the
        questions below.
      </div>
    ),
    description: (
      <div>
        Your responses should cover the major components of a research summary:
        hypothesis, methods, and anticipated findings. Therefore, please provide
        sufficiently detailed responses in plain language (without jargon),
        using as few technical terms as possible.{' '}
      </div>
    ),
  },
  {
    header:
      'What are the specific scientific question(s) you intend to study, and why is the ' +
      'question important (i.e. relevance to science or public health)? \n',
    description: (
      <div>
        If you are exploring the data at this stage to formalize a specific
        research question, please describe the reason for exploring the data,
        and the scientific question you hope to be able to answer using the
        data. <br />
        (Free text; 1000 character limit)
      </div>
    ),
  },
  {
    header:
      'What are the scientific approaches you plan to use for your study? Describe the ' +
      'datasets, research methods, and tools you will use to answer your scientific question(s). \n',
    description: <div>(Free text; 1000 character limit)</div>,
  },
  {
    header:
      'What are the anticipated findings from the study? How would your findings ' +
      'contribute to the body of scientific knowledge in the field?',
    description: <div>(Free text; 1000 character limit)</div>,
  },
  {
    header: (
      <div>
        The <AoU /> Research Program encourages researchers to disseminate their
        research findings to both scientific and lay/community audiences, to
        maximize the value of the resource and to recognize the contributions of
        participant partners. Please tell us how you plan to disseminate your
        research findings. Choose as many options below as applicable, and
        specify details in the text box, if available.{' '}
      </div>
    ),
    description: '',
  },
  {
    header: (
      <div>
        The <AoU /> Research Program would like to understand how (or if) your
        research outcome may fit into the <AoU /> Research Program Scientific
        Framework. Please select all of the statements below that describe the
        outcomes you anticipate from your research.
      </div>
    ),
    description: '',
  },
  {
    header: 'Population of interest',
    description: (
      <div>
        A primary aim of <AoU /> is to engage communities that have been
        historically underrepresented in biomedical research. The next set of
        questions is designed to help us assess how well we are serving
        underrepresented communities—and to protect against potentially
        stigmatizing research findings, to which these groups are more
        vulnerable. <AoU /> supports well-designed and responsible research that
        addresses questions relevant to underrepresented communities, as well as
        research that compares different groups and populations. At the same
        time, we recognize that research with underrepresented populations can
        unintentionally result in harm, including the exacerbation of stigma. We
        encourage researchers to consider whether their research design,
        methods, and analyses could potentially cause harm to individuals,
        groups, and communities.
      </div>
    ),
  },
  {
    header: 'Request for Review of Research Purpose Description',
    description: (
      <span>
        <div>
          Any research that focuses on certain population characteristics or
        </div>
        <TooltipTrigger content={toolTipTextDemographic}>
          <div style={{ color: colors.secondary }}>
            {' '}
            uses demographic variables{' '}
          </div>
        </TooltipTrigger>
        <div>
          in analyses can result, often unintentionally, in findings that may be
          misinterpreted or misused by others to foster stigma. While it may not
          be possible to completely prevent misuse of research for stigmatizing
          purposes, data users can take important steps to minimize the risk of
          this happening–taking this step is a condition of your
        </div>
        <TooltipTrigger content={toolTipTextDucc}>
          <div> Data User Code of Conduct agreement. </div>
        </TooltipTrigger>
        <div>
          If you are concerned that your research could inadvertently stigmatize
          participants or communities, or if you are unsure, let us know. We
          encourage you to request a review of your research purpose statement
          by the <i> All of Us</i> Resource Access Board (RAB) as a precaution.
          The RAB will provide feedback and, if needed, guidance for modifying
          your research purpose or scope.To learn more, please refer to the{' '}
          <i> All of Us </i>
          Stigmatizing Research Policy.If you request a review, you can expect
          to receive an initial response within five business days. During the
          RAB’s review, you may begin working in your workspace.
        </div>
      </span>
    ),
  },
  {
    header: (
      <div>
        <AoU /> has a{' '}
        <StyledExternalLink
          href='http://researchallofus.org/PolicyRespectfulAIANResearch'
          target='_blank'
        >
          {' '}
          policy on respectful research involving American Indian and Alaska
          Native (AI/AN) populations
        </StyledExternalLink>
        . The following questions are intended to assess the relevance of the
        policy to your research.
      </div>
    ),
    description: '',
  },
  {
    header:
      'Does your research plan require any of the following with respect to individuals who ' +
      'self-identify as American Indian or Alaska Native (AI/AN) or who are genetically similar to ' +
      'populations with inferred Indigenous American genetic ancestry? Select the option that ' +
      'best describes your plans.',
    description: '',
  },
  {
    header:
      'Please explain your response by sharing specific details about your study design.',
    description: <div>(Free text; 1000 character limit)</div>,
  },
];

export interface SpecificPopulationItem {
  label: string;
  shortName: SpecificPopulationEnum;
  ubrLabel: string;
  ubrDescription: string;
  subCategory: Array<{ label: string; shortName: SpecificPopulationEnum }>;
}

export const SpecificPopulationItems: Array<SpecificPopulationItem> = [
  {
    label: 'Race/Ethnicity',
    shortName: SpecificPopulationEnum.RACE_ETHNICITY,
    ubrLabel: 'Ancestry (Race/Ethnicity)',
    ubrDescription:
      'American Indian and Alaska Native (AIAN); Black, African American, or ' +
      'African; Middle Eastern or North African (MENA); Native Hawaiian or Other Pacific ' +
      'Islander (NHPI); Hispanic, Latino, or Spanish (H/L/S); Multi-Ancestry (2+ Races)',
    subCategory: [
      { label: 'Asian', shortName: SpecificPopulationEnum.RACE_ASIAN },
      {
        label: 'Black, African, or African American',
        shortName: SpecificPopulationEnum.RACE_AA,
      },
      {
        label: 'Hispanic, Latino, or Spanish',
        shortName: SpecificPopulationEnum.RACE_HISPANIC,
      },
      {
        label: 'American Indian or Alaska Native (AIAN)',
        shortName: SpecificPopulationEnum.RACE_AIAN,
      },
      {
        label: 'Middle Eastern or North African (MENA)',
        shortName: SpecificPopulationEnum.RACE_MENA,
      },
      {
        label: 'Native Hawaiian or Pacific Islander (NHPI)',
        shortName: SpecificPopulationEnum.RACE_NHPI,
      },
      {
        label: 'Multi-Ancestry or more than one race',
        shortName: SpecificPopulationEnum.RACE_MORE_THAN_ONE,
      },
    ],
  },
  {
    label: 'Age Groups',
    shortName: SpecificPopulationEnum.AGE_GROUPS,
    ubrLabel: 'Age',
    ubrDescription:
      'Children (0-11); Adolescents (12-17); Older Adults (65-74); Older ' +
      'Adults (75+)',
    subCategory: [
      {
        label: 'Children (0-11)',
        shortName: SpecificPopulationEnum.AGE_CHILDREN,
      },
      {
        label: 'Adolescents (12-17) ',
        shortName: SpecificPopulationEnum.AGE_ADOLESCENTS,
      },
      {
        label: 'Older adults (65-74)',
        shortName: SpecificPopulationEnum.AGE_OLDER,
      },
      {
        label: 'Older adults (75+) ',
        shortName: SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75,
      },
    ],
  },
  {
    label: 'Sex at Birth',
    shortName: SpecificPopulationEnum.SEX,
    ubrLabel: 'Sex at Birth',
    ubrDescription: 'Intersex',
    subCategory: [
      {
        label:
          'Participants who report something other than female or male as their sex at birth ' +
          '(e.g. intersex)',
        shortName: SpecificPopulationEnum.SEX,
      },
    ],
  },
  {
    label: 'Gender Identity',
    shortName: SpecificPopulationEnum.GENDER_IDENTITY,
    ubrLabel: 'Gender Identity (GI)',
    ubrDescription: 'Nonbinary; Transgender; or Other Gender Identity Choices',
    subCategory: [
      {
        label:
          'Participants who identify as gender variant, non-binary, transgender, or something ' +
          'else other than man or woman ',
        shortName: SpecificPopulationEnum.GENDER_IDENTITY,
      },
    ],
  },
  {
    label: 'Sexual Orientation',
    shortName: SpecificPopulationEnum.SEXUAL_ORIENTATION,
    ubrLabel: 'Sexual Orientation (SO)',
    ubrDescription:
      'Gay; Lesbian; Bisexual; Queer; Other Sexual Orientation Choices',
    subCategory: [
      {
        label:
          'Participants who identify as asexual, bisexual, gay or lesbian, or something else ' +
          'other than straight ',
        shortName: SpecificPopulationEnum.SEXUAL_ORIENTATION,
      },
    ],
  },
  {
    label: 'Geography (e.g. Rural, urban, suburban, etc.)',
    shortName: SpecificPopulationEnum.GEOGRAPHY,
    ubrLabel: 'Geography',
    ubrDescription: 'Rural and Non-Metropolitan Zip codes',
    subCategory: [
      {
        label: 'Participants who live in a rural or non-metropolitan setting',
        shortName: SpecificPopulationEnum.GEOGRAPHY,
      },
    ],
  },
  {
    label: 'Disability status',
    shortName: SpecificPopulationEnum.DISABILITY_STATUS,
    ubrLabel: 'Disability Status',
    ubrDescription: 'Physical and Cognitive Disabilities',
    subCategory: [
      {
        label: 'Participants with a physical and/or cognitive disability',
        shortName: SpecificPopulationEnum.DISABILITY_STATUS,
      },
    ],
  },
  {
    label: 'Access to care',
    shortName: SpecificPopulationEnum.ACCESS_TO_CARE,
    ubrLabel: 'Access to Care',
    ubrDescription:
      'Limited access to care; Cannot easily obtain or access medical care',
    subCategory: [
      {
        label: 'Participants who cannot easily obtain or access medical care',
        shortName: SpecificPopulationEnum.ACCESS_TO_CARE,
      },
    ],
  },
  {
    label: 'Education level',
    shortName: SpecificPopulationEnum.EDUCATION_LEVEL,
    ubrLabel: 'Educational Attainment',
    ubrDescription:
      'Less than high school graduate or General Education Development (GED)',
    subCategory: [
      {
        label: 'Participants with less than a high school degree or equivalent',
        shortName: SpecificPopulationEnum.EDUCATION_LEVEL,
      },
    ],
  },
  {
    label: 'Income level',
    shortName: SpecificPopulationEnum.INCOME_LEVEL,
    ubrLabel: 'Income Level',
    ubrDescription: 'Less than USD 25,000 [for a family of four]',
    subCategory: [
      {
        label:
          'Participants with household incomes equal to or below 200% of the Federal Poverty Level',
        shortName: SpecificPopulationEnum.INCOME_LEVEL,
      },
    ],
  },
];

export const disseminateFindings = [
  {
    label: 'Publication in peer-reviewed scientific journals',
    shortName: DisseminateResearchEnum.PUBLICATION_PEER_REVIEWED_JOURNALS,
  },
  {
    label: 'Social media (Facebook, Instagram, Twitter)',
    shortName: DisseminateResearchEnum.SOCIAL_MEDIA,
  },
  {
    label: 'Presentation at national or international scientific conferences',
    shortName: DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES,
  },
  {
    label:
      'Presentation at community forums or advisory groups (such as town halls, advocacy group ' +
      'meetings, or community advisory boards)',
    shortName: DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS,
  },
  {
    label: 'Press release or media article covering scientific publication',
    shortName: DisseminateResearchEnum.PRESS_RELEASE,
  },
  {
    label: 'Publication in community-based journals or blog',
    shortName: DisseminateResearchEnum.PUBLICATION_COMMUNITY_BASED_BLOG,
  },
  {
    label: 'Publication of article in a personal blog',
    shortName: DisseminateResearchEnum.PUBLICATION_PERSONAL_BLOG,
  },
  { label: 'Other', shortName: DisseminateResearchEnum.OTHER },
];

export const researchOutcomes = [
  {
    label:
      'This research project seeks to increase wellness and resilience, and promote ' +
      'healthy living',
    shortName: ResearchOutcomeEnum.PROMOTE_HEALTHY_LIVING,
  },
  {
    label:
      'This research project seeks to reduce health disparities and improve health equity ' +
      'in underrepresented in biomedical research (UBR) populations',
    shortName: ResearchOutcomeEnum.IMPROVE_HEALTH_EQUALITY_UBR_POPULATIONS,
  },
  {
    label:
      'This research project seeks to develop improved risk assessment and prevention ' +
      'strategies to preempt disease',
    shortName: ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT,
  },
  {
    label:
      'This research project seeks to provide earlier and more accurate diagnosis to ' +
      'decrease illness burden',
    shortName: ResearchOutcomeEnum.DECREASE_ILLNESS_BURDEN,
  },
  {
    label:
      'This research project seeks to improve health outcomes and reduce disease/illness burden' +
      ' through improved treatment and development of precision intervention',
    shortName: ResearchOutcomeEnum.PRECISION_INTERVENTION,
  },
  {
    label: 'None of these statements apply to this research project',
    shortName: ResearchOutcomeEnum.NONE_APPLY,
  },
];

export const aianResearchTypeMap: Map<AIANResearchType, string> = new Map([
  [
    AIANResearchType.EXCLUSIVE_AI_AN_POPULATION,
    'I am planning to conduct my study using a study population exclusively including individuals ' +
      'who self-identify as AI/AN and/or who exhibit genetic similarity to populations with inferred ' +
      'Indigenous American genetic ancestry.',
  ],
  [
    AIANResearchType.CASE_CONTROL_AI_AN,
    'I am planning to conduct a case/control study where either the “case” or the “control” ' +
      'population consists exclusively of individuals who self-identify as AI/AN and/or who exhibit ' +
      'genetic similarity to populations with inferred Indigenous American genetic ancestry.',
  ],
  [
    AIANResearchType.FINDINGS_BY_AI_AN,
    'I am planning to break down my findings by race/ethnicity and/or ancestral genetic similarity ' +
      'in such a way that may raise findings that are specific to individuals who self-identify as ' +
      'AI/AN and/or who exhibit genetic similarity to populations with inferred Indigenous American ' +
      'genetic ancestry.',
  ],
  [
    AIANResearchType.NO_AI_AN_ANALYSIS,
    'I will not conduct any analyses that would yield findings specific to AI/AN populations and/or ' +
      'populations who exhibit genetic similarity to populations with inferred Indigenous American ' +
      'genetic ancestry. If that changes, I will immediately update my responses on this form to ' +
      'reflect that change. ',
  ],
]);
