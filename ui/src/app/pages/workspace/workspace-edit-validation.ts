import { validate } from 'validate.js';

import {
  DisseminateResearchEnum,
  SpecificPopulationEnum,
  Workspace,
} from 'generated/fetch';

export type WorkspaceEditValidationArgs = Required<
  Pick<
    Workspace['researchPurpose'],
    | 'aianResearchDetails'
    | 'aianResearchType'
    | 'intendedStudy'
    | 'anticipatedFindings'
    | 'diseaseFocusedResearch'
    | 'populationDetails'
    | 'scientificApproach'
    | 'reviewRequested'
    | 'researchOutcomeList'
    | 'disseminateResearchFindingList'
    | 'otherPurpose'
    | 'otherDisseminateResearchFindings'
    | 'otherPopulationDetails'
    | 'otherPurposeDetails'
    | 'diseaseOfFocus'
  >
> &
  Pick<Workspace, 'name' | 'billingAccountName'> & {
    primaryPurpose: boolean;
    populationChecked: boolean;
  };

export const validateWorkspaceEdit = (
  values: WorkspaceEditValidationArgs,
  askAboutAIAN: boolean
) => {
  const requiredStringWithMaxLength = (maximum: number, prefix = '') => ({
    presence: {
      allowEmpty: false,
      message: `${prefix} cannot be blank`,
    },
    length: {
      maximum,
      tooLong: `${prefix} cannot exceed %{count} characters`,
    },
  });

  // TODO: This validation spec should include error messages which get
  // surfaced directly. Currently these constraints are entirely separate
  // from the user facing error strings we render.
  const constraints: object = {
    name: requiredStringWithMaxLength(80, 'Name'),
    // The prefix for these lengthMessages require HTML formatting
    // The prefix string is omitted here and included in the React template below
    billingAccountName: { presence: true },
    intendedStudy: requiredStringWithMaxLength(1000),
    populationChecked: { presence: true },
    anticipatedFindings: requiredStringWithMaxLength(1000),
    reviewRequested: { presence: true },
    scientificApproach: requiredStringWithMaxLength(1000),
    researchOutcomeList: { presence: { allowEmpty: false } },
    disseminateResearchFindingList: { presence: { allowEmpty: false } },
    primaryPurpose: { truthiness: true },

    // Conditionally include optional fields for validation.
    otherPurposeDetails: values.otherPurpose
      ? requiredStringWithMaxLength(500, 'Other primary purpose')
      : {},
    populationDetails: values.populationChecked ? { presence: true } : {},
    otherPopulationDetails: values.populationDetails?.includes(
      SpecificPopulationEnum.OTHER
    )
      ? requiredStringWithMaxLength(100, 'Other Specific Population')
      : {},
    diseaseOfFocus: values.diseaseFocusedResearch
      ? requiredStringWithMaxLength(80, 'Disease of Focus')
      : {},
    otherDisseminateResearchFindings:
      values.disseminateResearchFindingList?.includes(
        DisseminateResearchEnum.OTHER
      )
        ? requiredStringWithMaxLength(
            100,
            'Other methods of disseminating research findings'
          )
        : {},
    aianResearchType: askAboutAIAN ? { presence: true } : {},
    aianResearchDetails: askAboutAIAN ? requiredStringWithMaxLength(1000) : {},
  };

  return validate(values, constraints, { fullMessages: false });
};
