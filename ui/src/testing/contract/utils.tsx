import {
  BillingStatus,
  SpecificPopulationEnum,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';

import { CdrVersionsStubVariables } from 'testing/stubs/cdr-versions-api-stub';
import { WorkspaceStubVariables } from 'testing/stubs/workspaces';

export const buildWorkspaceTemplate = (suffix: string) => {
  return {
    name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME + suffix,
    id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID + suffix,
    namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + suffix,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    accessTierShortName: AccessTierShortNames.Registered,
    creationTime: new Date('2023-02-03 00:00:00Z').getTime(),
    lastModifiedTime: new Date('2023-02-03 00:00:00Z').getTime(),
    researchPurpose: {
      ancestry: false,
      anticipatedFindings: '',
      commercialPurpose: false,
      controlSet: false,
      disseminateResearchFindingList: [],
      diseaseFocusedResearch: false,
      drugDevelopment: true,
      educational: true,
      intendedStudy: '',
      scientificApproach: '',
      methodsDevelopment: false,
      otherPurpose: false,
      otherPurposeDetails: '',
      populationDetails: [
        SpecificPopulationEnum.AGEOLDERMORETHAN75,
        SpecificPopulationEnum.RACENHPI,
      ],
      populationHealth: true,
      researchOutcomeList: [],
      ethics: true,
      reviewRequested: false,
      socialBehavioral: false,
      reasonForAllOfUs: '',
      needsReviewPrompt: false,
      additionalNotes: 'additional notes',
      approved: false,
      diseaseOfFocus: 'cancer',
      otherPopulationDetails: '',
      timeRequested: new Date('2023-02-03 00:00:00Z').getTime(),
      timeReviewed: null,
      otherDisseminateResearchFindings: '',
    },
    published: false,
    billingAccountName: 'billing-account',
    billingStatus: BillingStatus.ACTIVE,
    adminLocked: false,
    etag: '"0"',
    creator: '',
    googleBucketName: '',
    lastModifiedBy: '',
    adminLockedReason: '',
    googleProject: '',
  };
};

export function buildWorkspaceResponseTemplate(suffixes: string[]) {
  return suffixes.map((suffix) => {
    return {
      workspace: buildWorkspaceTemplate(suffix),
      accessLevel: WorkspaceAccessLevel.WRITER,
    };
  });
}
