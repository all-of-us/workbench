import {
  CheckEmailRequest,
  CheckEmailResponse,
  DuaType,
  GetInstitutionsResponse,
  GetPublicInstitutionDetailsResponse,
  Institution,
  InstitutionApi,
  OrganizationType,
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export const defaultInstitutions: Array<Institution> = [{
  shortName: 'VUMC',
  displayName: 'Vanderbilt University Medical Center',
  organizationTypeEnum: OrganizationType.HEALTHCENTERNONPROFIT,
  emailDomains: ['vumc.org'],
  duaTypeEnum: DuaType.MASTER,
  userInstructions: 'Vanderbilt User Instruction'
}, {
  shortName: 'Broad',
  displayName: 'Broad Institute',
  organizationTypeEnum: OrganizationType.ACADEMICRESEARCHINSTITUTION,
  emailDomains: [],
  emailAddresses: ['contactEmail@broadinstitute.org', 'broad_institution@broadinstitute.org'],
  duaTypeEnum: DuaType.RESTRICTED
}, {
  shortName: 'Verily',
  displayName: 'Verily LLC',
  organizationTypeEnum: OrganizationType.INDUSTRY,
  emailDomains: ['verily.com', 'google.com'],
  userInstructions: 'Verily User Instruction'
}];

export class InstitutionApiStub extends InstitutionApi {
  public institutions: Array<Institution>;

  constructor(institutions: Array<Institution> = defaultInstitutions) {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });

    this.institutions = institutions;
  }

  createInstitution(institution: Institution): Promise<Institution> {
    return new Promise<Institution>(resolve => {
      this.institutions.push(institution);
      resolve(institution);
    });
  }

  deleteInstitution(shortName: string): Promise<Response> {
    return new Promise<Response>(resolve => {
      this.institutions = this.institutions.filter(institution => {
        return institution.shortName !== shortName;
      });
      resolve(new Response());
    });
  }

  getInstitution(shortName: string): Promise<Institution> {
    return new Promise((resolve, reject) => {
      const institution = this.institutions.find(x => x.shortName === shortName);
      if (institution) {
        resolve(institution);
      } else {
        reject(new Response('No institution found', {status: 404}));
      }
    });
  }

  getInstitutions(shortName: string): Promise<GetInstitutionsResponse> {
    return new Promise((resolve, reject) => {
      const institution = {institutions: this.institutions};
      resolve(institution);
    });
  }

  getPublicInstitutionDetails(): Promise<GetPublicInstitutionDetailsResponse> {
    return new Promise(resolve => {
      resolve({
        institutions: this.institutions.map(x => {
          return {
            shortName: x.shortName,
            displayName: x.displayName,
            organizationTypeEnum: x.organizationTypeEnum,
            duaTypeEnum: x.duaTypeEnum
          };
        })
      });
    });
  }

  updateInstitution(shortName: string, institution: Institution): Promise<Institution> {
    return new Promise(resolve => {
      this.institutions = this.institutions.filter(x => x.shortName !== shortName);
      this.institutions.push(institution);
      resolve(institution);
    });
  }

  async checkEmail(shortName: string, request: CheckEmailRequest, options?: any): Promise<CheckEmailResponse> {
    const {contactEmail} = request;
    const domain = contactEmail.substring(contactEmail.lastIndexOf('@') + 1);

    const institution = this.institutions.find(x => x.shortName === shortName);
    if (!institution) {
      throw new Response('No institution found', {status: 404});
    }

    const response: CheckEmailResponse = {
      isValidMember: false
    };
    if (institution.emailAddresses && institution.emailAddresses.includes(contactEmail)) {
      response.isValidMember = true;
    } else if (institution.emailDomains && institution.emailDomains.includes(domain)) {
      response.isValidMember = true;
    }
    return response;
  }
}
