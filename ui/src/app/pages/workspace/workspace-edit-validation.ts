import {
  AIANResearchType,
  DisseminateResearchEnum,
  SpecificPopulationEnum,
  Workspace,
} from 'generated/fetch';

import {
  nonEmptyArray,
  refinedObject,
  refineFields,
  requiredString,
  requiredStringWithMaxLength,
  trueBoolean,
  zodToValidateJS,
} from 'app/utils/zod-validators';
import { z } from 'zod';

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

export const getWorkspaceEditValidator = (askAboutAIAN: boolean) =>
  refinedObject<WorkspaceEditValidationArgs>((data, ctx) => {
    const noop = undefined;
    refineFields(data, ctx, {
      name: requiredStringWithMaxLength(80, 'Name'),
      // The prefix for these lengthMessages require HTML formatting
      // The prefix string is omitted here and included in the React template
      billingAccountName: requiredString(),
      intendedStudy: requiredStringWithMaxLength(1000),
      populationChecked: z.boolean({ required_error: "can't be blank" }),
      anticipatedFindings: requiredStringWithMaxLength(1000),
      reviewRequested: z.boolean({ required_error: "can't be blank" }),
      scientificApproach: requiredStringWithMaxLength(1000),
      researchOutcomeList: nonEmptyArray(z.string(), "can't be blank"),
      disseminateResearchFindingList: nonEmptyArray(
        z.string(),
        "can't be blank"
      ),
      primaryPurpose: trueBoolean(),

      // Optional fields that are required based on conditions
      // old validation allowed for "present" but empty array [] value.
      otherPurposeDetails: data.otherPurpose
        ? requiredStringWithMaxLength(500, 'Other primary purpose')
        : noop,
      populationDetails: data.populationChecked
        ? nonEmptyArray(z.string(), "can't be blank")
        : noop,
      otherPopulationDetails: data.populationDetails?.includes(
        SpecificPopulationEnum.OTHER
      )
        ? requiredStringWithMaxLength(100, 'Other Specific Population')
        : noop,
      diseaseOfFocus: data.diseaseFocusedResearch
        ? requiredStringWithMaxLength(80, 'Disease of Focus')
        : noop,
      otherDisseminateResearchFindings:
        data.disseminateResearchFindingList?.includes(
          DisseminateResearchEnum.OTHER
        )
          ? requiredStringWithMaxLength(
              100,
              'Other methods of disseminating research findings'
            )
          : noop,
      aianResearchType: askAboutAIAN
        ? z.nativeEnum(AIANResearchType, { required_error: "can't be blank" })
        : noop,
      aianResearchDetails: askAboutAIAN
        ? requiredStringWithMaxLength(1000)
        : noop,
    });
    return z.NEVER;
  });

export const validateWorkspaceEdit = (
  data: WorkspaceEditValidationArgs,
  askAboutAIAN: boolean
) => zodToValidateJS(() => getWorkspaceEditValidator(askAboutAIAN).parse(data));
