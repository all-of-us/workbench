import {
  CheckEmailRequest,
  CheckEmailResponse,
  GetInstitutionsResponse,
  GetPublicInstitutionDetailsResponse,
  Institution,
  InstitutionApi,
  InstitutionMembershipRequirement,
  OrganizationType,
} from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';
import { getTierConfig } from 'app/utils/institutions';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export const VUMC: Institution = {
  shortName: 'VUMC',
  displayName: 'Vanderbilt University Medical Center',
  organizationTypeEnum: OrganizationType.HEALTHCENTERNONPROFIT,
  tierConfigs: [
    {
      accessTierShortName: 'registered',
      membershipRequirement: InstitutionMembershipRequirement.DOMAINS,
      eraRequired: true,
      emailDomains: ['vumc.org'],
    },
  ],
  userInstructions: 'Vanderbilt User Instruction',
};

export const BROAD_ADDR_1 = 'contactEmail@broadinstitute.org';
export const BROAD_ADDR_2 = 'broad_institution@broadinstitute.org';
export const BROAD: Institution = {
  shortName: 'Broad',
  displayName: 'Broad Institute',
  organizationTypeEnum: OrganizationType.ACADEMICRESEARCHINSTITUTION,
  tierConfigs: [
    {
      accessTierShortName: 'registered',
      membershipRequirement: InstitutionMembershipRequirement.ADDRESSES,
      eraRequired: true,
      emailAddresses: [BROAD_ADDR_1, BROAD_ADDR_2],
    },
  ],
};

export const VERILY: Institution = {
  shortName: 'Verily',
  displayName: 'Verily LLC',
  organizationTypeEnum: OrganizationType.INDUSTRY,
  tierConfigs: [
    {
      accessTierShortName: 'registered',
      membershipRequirement: InstitutionMembershipRequirement.DOMAINS,
      eraRequired: true,
      emailDomains: ['verily.com', 'google.com'],
    },
    {
      accessTierShortName: 'controlled',
      membershipRequirement: InstitutionMembershipRequirement.ADDRESSES,
      eraRequired: true,
      emailAddresses: ['foo@verily.com'],
    },
  ],
  userInstructions: 'Verily User Instruction',
};

export const VERILY_WITHOUT_CT: Institution = {
  shortName: 'Non CT Access',
  displayName: 'Non CT Access',
  organizationTypeEnum: OrganizationType.INDUSTRY,
  tierConfigs: [
    {
      accessTierShortName: 'registered',
      membershipRequirement: InstitutionMembershipRequirement.DOMAINS,
      eraRequired: true,
      emailDomains: ['verily.com', 'google.com'],
    },
  ],
  userInstructions: 'Verily User Instruction',
};

export const defaultInstitutions: Array<Institution> = [
  VUMC,
  BROAD,
  VERILY,
  VERILY_WITHOUT_CT,
];

export class InstitutionApiStub extends InstitutionApi {
  public institutions: Array<Institution>;

  constructor(institutions: Array<Institution> = defaultInstitutions) {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });

    this.institutions = institutions;
  }

  createInstitution(institution: Institution): Promise<Institution> {
    return new Promise<Institution>((resolve) => {
      this.institutions.push(institution);
      resolve(institution);
    });
  }

  deleteInstitution(shortName: string): Promise<Response> {
    return new Promise<Response>((resolve) => {
      this.institutions = this.institutions.filter((institution) => {
        return institution.shortName !== shortName;
      });
      resolve(new Response());
    });
  }

  getInstitution(shortName: string): Promise<Institution> {
    return new Promise((resolve, reject) => {
      const institution = this.institutions.find(
        (x) => x.shortName === shortName
      );
      if (institution) {
        resolve(institution);
      } else {
        reject(new Response('No institution found', { status: 404 }));
      }
    });
  }

  getInstitutions(): Promise<GetInstitutionsResponse> {
    return new Promise((resolve) => {
      const institution = { institutions: this.institutions };
      resolve(institution);
    });
  }

  getPublicInstitutionDetails(): Promise<GetPublicInstitutionDetailsResponse> {
    return new Promise((resolve) => {
      resolve({
        institutions: this.institutions.map((x) => {
          return {
            shortName: x.shortName,
            displayName: x.displayName,
            organizationTypeEnum: x.organizationTypeEnum,
            registeredTierMembershipRequirement: getTierConfig(
              x,
              AccessTierShortNames.Registered
            ).membershipRequirement,
          };
        }),
      });
    });
  }

  updateInstitution(
    shortName: string,
    institution: Institution
  ): Promise<Institution> {
    return new Promise((resolve) => {
      this.institutions = this.institutions.filter(
        (x) => x.shortName !== shortName
      );
      this.institutions.push(institution);
      resolve(institution);
    });
  }

  async checkEmail(
    shortName: string,
    request: CheckEmailRequest
  ): Promise<CheckEmailResponse> {
    const { contactEmail } = request;
    const domain = contactEmail.substring(contactEmail.lastIndexOf('@') + 1);

    const institution = this.institutions.find(
      (x) => x.shortName === shortName
    );
    if (!institution) {
      throw new Response('No institution found', { status: 404 });
    }

    const response: CheckEmailResponse = {
      isValidMember: false,
    };
    const tierConfig = getTierConfig(
      institution,
      AccessTierShortNames.Registered
    );
    if (
      tierConfig.membershipRequirement ===
        InstitutionMembershipRequirement.ADDRESSES &&
      tierConfig.emailAddresses.includes(contactEmail)
    ) {
      response.isValidMember = true;
    } else if (
      tierConfig.membershipRequirement ===
        InstitutionMembershipRequirement.DOMAINS &&
      tierConfig.emailDomains.includes(domain)
    ) {
      response.isValidMember = true;
    }
    return response;
  }
}
