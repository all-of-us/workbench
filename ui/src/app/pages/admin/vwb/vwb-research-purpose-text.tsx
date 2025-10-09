export const RESEARCH_PURPOSE_MAPPING = [
  {
    sectionHeader: 'Primary purpose of project',
    items: [
      {
        title: 'Research Purpose',
        subItems: [
          {
            title: 'Disease-focused research',
            field: 'purposeDisease',
            valueField: 'purposeDiseaseText',
          },
          {
            title: 'Methods development/validation study',
            field: 'purposeMethods',
            value:
              'The primary purpose of the use of All of Us data is to develop and/or validate specific methods/tools for analyzing or ' +
              'interpreting data (e.g. statistical methods for describing data trends, developing more powerful methods to detect ' +
              'gene-environment, or other types of interactions in genome-wide association studies).',
          },
          {
            title: 'Research Control',
            field: 'purposeResearchControl',
            value:
              'All of Us data will be used as a reference or control dataset for comparison with another dataset from a different ' +
              'resource (e.g. Case-control studies).',
          },
          {
            title: 'Genetic Research',
            field: 'purposeGeneticResearch',
            value:
              'Research concerning genetics (i.e. the study of genes, genetic variations, and heredity) in the context of diseases or ' +
              'ancestry.',
          },
          {
            title: 'Social/Behavioral Research',
            field: 'purposeSocialResearch',
            value:
              'The research focuses on the social or behavioral phenomena or determinants of health.',
          },
          {
            field: 'purposePopulationResearch',
            title: 'Population Health/Public Health Research',
            value:
              'The primary purpose of using All of Us data is to investigate health behaviors, outcomes, access, and disparities in ' +
              'populations.',
          },
          {
            title: 'Ethical, Legal, and Social Implications (ELSI) Research',
            field: 'purposeElsi',
            value:
              'This research focuses on ethical, legal, and social implications (ELSI) of or related to design, conduct, and translation ' +
              'of research.',
          },
          {
            title: 'Drug/Therapeutics Development Research',
            field: 'purposeDrug',
            value:
              'The primary focus of the research is drug/therapeutics development. The data will be used to understand treatment-gene ' +
              'interactions or treatment outcomes relevant to the therapeutic(s) of interest.',
          },
        ],
      },
      {
        title: 'Educational Purpose',
        field: 'purposeEducation',
        value:
          'The data will be used for education purposes (e.g. for a college research methods course, to educate students on ' +
          'population-based research approaches).',
      },
      {
        title: 'For-Profit Purpose',
        field: 'purposeForProfit',
        value:
          'The data will be used by a for-profit entity for research or product or service development (e.g. for understanding drug ' +
          "responses as part of a pharmaceutical company's drug development or market research efforts).",
      },
      {
        title: 'Other Purpose',
        field: 'purposeOther',
        valueField: 'purposeOtherText',
      },
    ],
  },
  {
    sectionHeader: 'Summary of research purpose',
    items: [
      {
        title:
          'What are the specific scientific question(s) you intend to study, and why is the question important (i.e. relevance to ' +
          'science or public health)?',
        field: 'scientificQuestions',
      },
      {
        title:
          'What are the scientific approaches you plan to use for your study? Describe the datasets, research methods, and tools you ' +
          'will use to answer your scientific question(s).',
        field: 'scientificApproaches',
      },
      {
        title:
          'What are the anticipated findings from the study? How would your findings contribute to the body of scientific knowledge in ' +
          'the field?',
        field: 'anticipatedFindings',
      },
    ],
  },
  {
    sectionHeader: 'Findings will be disseminated via:',
    items: [
      {
        field: 'disseminatePressRelease',
        value: 'Press release or media article covering scientific publication',
      },
      {
        field: 'disseminateCommunityJournal',
        value: 'Publication in community-based journals or blog',
      },
      {
        field: 'disseminateOther',
        value: 'Other',
        valueField: 'disseminateOtherText',
      },
      {
        field: 'disseminatePresentationConferences',
        value:
          'Presentation at national or international scientific conferences',
      },
      {
        field: 'disseminateJournal',
        value: 'Publication in peer-reviewed scientific journals',
      },
      {
        field: 'disseminateSocialMedia',
        value: 'Social media (Facebook, Instagram, Twitter)',
      },
      {
        field: 'disseminateCommunityForum',
        value:
          'Presentation at community forums or advisory groups (such as town halls, advocacy group meetings, or community advisory ' +
          'boards)',
      },
      {
        field: 'disseminatePersonalBlog',
        value: 'Publication of article in a personal blog',
      },
    ],
  },
  {
    sectionHeader: 'Outcomes anticipated from the research:',
    items: [
      {
        field: 'fitWellness',
        value:
          'This research project seeks to increase wellness and resilience, and promote healthy living',
      },
      {
        field: 'fitEquity',
        value:
          'This research project seeks to reduce health disparities and improve health equity in underrepresented in biomedical ' +
          'research (UBR) populations',
      },
      {
        field: 'fitDiagnosis',
        value:
          'This research project seeks to provide earlier and more accurate diagnosis to decrease illness burden',
      },
      {
        field: 'fitOutcome',
        value:
          'This research project seeks to improve health outcomes and reduce disease/illness burden through improved treatment and ' +
          'development of precision intervention',
      },
      {
        field: 'fitPrevention',
        value:
          'This research project seeks to develop improved risk assessment and prevention strategies to preempt disease',
      },
      {
        field: 'fitNone',
        value: 'None',
      },
    ],
  },
  {
    sectionHeader: 'Population of interest',
    items: [
      {
        title: 'Race/Ethnicity',
        items: [
          {
            field: 'populationAsian',
            value: 'Asian',
          },
          {
            field: 'populationBlackAfricanAfricanAmerican',
            value: 'Black, African, or African American',
          },
          {
            field: 'populationHispanicLatinoSpanish',
            value: 'Hispanic, Latino, or Spanish',
          },
          {
            field: 'populationAian',
            value: 'American Indian or Alaska Native (AIAN)',
          },
          {
            field: 'populationMena',
            value: 'Middle Eastern or North African (MENA)',
          },
          {
            field: 'populationNhpi',
            value: 'Native Hawaiian or Pacific Islander (NHPI)',
          },
          {
            field: 'populationMultiAncestry',
            value: 'Multi-Ancestry or more than one race',
          },
        ],
      },
      {
        title: 'Age Groups',
        items: [
          {
            field: 'populationChildren',
            value: 'Children (0-11)',
          },
          {
            field: 'populationAdolescents',
            value: 'Adolescents (12-17)',
          },
          {
            field: 'populationOlderAdults65',
            value: 'Older adults (65-74)',
          },
          {
            field: 'populationOlderAdults75',
            value: 'Older adults (75+)',
          },
        ],
      },
      {
        title: 'Sex at Birth',
        items: [
          {
            field: 'populationSexOther',
            value:
              'Participants who report something other than female or male as their sex at birth (e.g. intersex)\n',
          },
        ],
      },
      {
        title: 'Gender Identity',
        items: [
          {
            field: 'populationGenderIdentity',
            value:
              'Participants who identify as gender variant, non-binary, transgender, or something else other than man or woman',
          },
        ],
      },
      {
        title: 'Sexual Orientation',
        items: [
          {
            field: 'populationSexualOrientation',
            value:
              'Participants who identify as asexual, bisexual, gay or lesbian, or something else other than straight',
          },
        ],
      },
      {
        title: 'Geography (e.g. Rural, urban, suburban, etc.)y',
        items: [
          {
            field: 'populationGeography',
            value:
              'Participants who live in a rural or non-metropolitan setting',
          },
        ],
      },
      {
        title: 'Disability status',
        items: [
          {
            field: 'populationDisability',
            value: 'Participants with a physical and/or cognitive disability',
          },
        ],
      },
      {
        title: 'Access to care',
        items: [
          {
            field: 'populationCare',
            value:
              'Participants who cannot easily obtain or access medical care',
          },
        ],
      },
      {
        title: 'Education level',
        items: [
          {
            field: 'populationEducation',
            value:
              'Participants with less than a high school degree or equivalent',
          },
        ],
      },
      {
        title: 'Income level',
        items: [
          {
            field: 'populationIncome',
            value:
              'Participants with household incomes equal to or below 200% of the Federal Poverty Level',
          },
        ],
      },
      {
        field: 'populationOther',
        title: 'Other',
        valueField: 'populationOtherText',
      },
    ],
  },
];
