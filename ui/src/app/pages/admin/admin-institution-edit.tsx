import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SemiBoldHeader} from 'app/components/headers';
import {TextArea} from 'app/components/inputs';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Scroll} from 'app/icons/scroll';
import {TextInputWithLabel} from 'app/pages/login/account-creation/common';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, UrlParamsProps, withUrlParams} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {DuaType, Institution, OrganizationType} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import * as validate from 'validate.js';

const styles = reactStyles({
  label: {
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '22px',
    color: colors.primary,
    marginTop: '2rem',
    marginBottom: '0.3rem'
  }
});

interface InstitutionEditState {
  apiErrorMsg: string;
  isAddInstitution: boolean;
  institution: Institution;
  invalidEmailAddress: boolean;
  invalidEmailAddressMsg: string;
  invalidEmailDomain: boolean;
  invalidEmailDomainsMsg: string;
  showOtherInstitution: boolean;
  showBackButtonWarning: boolean;
  showApiError: boolean;
}

let title = 'Add new Institution';
let institutionToEdit;

export class AdminInstitutionEditImpl extends React.Component<UrlParamsProps, InstitutionEditState> {
  constructor(props) {
    super(props);
    this.state = {
      apiErrorMsg: '',
      isAddInstitution: true,
      institution: {
        shortName: '',
        displayName: '',
        organizationTypeEnum: null
      },
      invalidEmailAddress: false,
      invalidEmailAddressMsg: '',
      invalidEmailDomain: false,
      invalidEmailDomainsMsg: '',
      showOtherInstitution: false,
      showBackButtonWarning: false,
      showApiError: false
    };
  }

  async componentDidMount() {
    // If institution short Name is passed in the URL get the institution details
    if (this.props.urlParams.institutionId) {
      institutionToEdit = await institutionApi().getInstitution(this.props.urlParams.institutionId);
      title = institutionToEdit.displayName;
      this.setState({
        isAddInstitution: false,
        institution: institutionToEdit,
        showOtherInstitution: institutionToEdit.organizationTypeEnum === OrganizationType.OTHER
      });
    } else {
      title = 'Add new Institution';
      this.setState({isAddInstitution: true});
    }

  }

  get institutionTypeOptions() {
    const options = [
      {label: 'Industry', value: OrganizationType.INDUSTRY},
      {label: 'Academic Research Institution', value: OrganizationType.ACADEMICRESEARCHINSTITUTION},
      {label: 'Educational Institution', value: OrganizationType.EDUCATIONALINSTITUTION},
      {label: 'Health Center non profit', value: OrganizationType.HEALTHCENTERNONPROFIT},
      {label: 'Other', value: OrganizationType.OTHER}
    ];
    return options;
  }

  get institutionAgreementTypeOptions() {
    const options = [
      {label: 'Master', value: DuaType.MASTER},
      {label: 'Individual', value: DuaType.RESTRICTED}
    ];
    return options;
  }

  // Filter out empty line or empty email addresses like <email1>,,<email2>
  // Confirm each email is a valid email using validate.js
  validateEmailAddresses() {
    const invalidEmailAddress = [];
    const {emailAddresses} = this.state.institution;
    this.state.institution.emailAddresses = emailAddresses.filter(
      emailAddress => {
        return emailAddress !== '' || !!emailAddress;
      });

    this.state.institution.emailAddresses.map(emailAddress => {
      const errors = validate({
        emailAddress
      }, {
        emailAddress: {email: true}
      });
      if (errors && errors.emailAddress && errors.emailAddress.length > 0) {
        invalidEmailAddress.push(emailAddress);
      }
    });
    this.setState({invalidEmailAddress: invalidEmailAddress.length > 0});
    if (invalidEmailAddress.length > 0) {
      const errMessage = 'Following Email Addresses are not valid : ' + invalidEmailAddress.join(' , ');
      this.setState({invalidEmailAddressMsg: errMessage});
    }
    return;
  }

  // Filter out empty line or empty email addresses like <email1>,,<email2>
  // Confirm each email domain matches with regex
  validateEmailDomain() {
    const invalidEmailDomain = [];
    const {emailDomains} = this.state.institution;
    this.state.institution.emailDomains =  emailDomains.filter(emailDomain => emailDomain);
    this.state.institution.emailDomains.map(emailDomain => {
      const errors = validate({
        emailDomain
      }, {
        emailDomain: {format: {pattern: /[a-zA-z\-\.]+[.][a-zA-Z]+/i}}
      });
      if (errors && errors.emailDomain && errors.emailDomain.length > 0) {
        invalidEmailDomain.push(emailDomain);
      }
    });
    this.setState({invalidEmailDomain: invalidEmailDomain.length > 0});
    if (invalidEmailDomain.length > 0) {
      const errMessage = 'Following Email Domains are not valid : ' + invalidEmailDomain.join(' , ');
      this.setState({invalidEmailDomainsMsg: errMessage});
    }
    return;
  }

  setEmailDomain(emailDomains, attribute) {
    const emailDomainList = emailDomains.split(/[,\n]+/);
    this.setState(fp.set(['institution', attribute], emailDomainList));
  }

  // Check if the fields have not been edited
  fieldsNotEdited() {
    return (this.state.isAddInstitution && !this.fieldsNotEditedAddInstitution)
        || (institutionToEdit && this.fieldsNotEditedEditInstitution);
  }

  get fieldsNotEditedAddInstitution() {
    const {institution} = this.state;
    return institution.displayName || institution.organizationTypeOtherText ||
        institution.organizationTypeEnum || institution.duaTypeEnum || institution.emailAddresses || institution.emailDomains;
  }

  get fieldsNotEditedEditInstitution() {
    const {institution} = this.state;
    return institution.displayName === institutionToEdit.displayName &&
        institution.organizationTypeEnum === institutionToEdit.organizationTypeEnum &&
        institution.duaTypeEnum === institutionToEdit.duaTypeEnum &&
        institution.emailAddresses === institutionToEdit.emailAddresses &&
        institution.emailDomains === institutionToEdit.emailDomains &&
        institution.userInstructions === institutionToEdit.userInstructions &&
        institution.organizationTypeOtherText === institutionToEdit.organizationTypeOtherText;
  }


  noEmptyRequiredFields() {
    const {institution} = this.state;
    let emailValid = true;
    if (institution.duaTypeEnum) {
      emailValid = institution.duaTypeEnum === DuaType.MASTER ?
          institution.emailDomains !== undefined : institution.emailAddresses !== undefined;
    }
    return !emailValid || !institution.displayName || !institution.organizationTypeEnum ||
      !institution.duaTypeEnum ||
        (institution.organizationTypeEnum === OrganizationType.OTHER &&
            !institution.organizationTypeOtherText);
  }

  // Disable save button if
  // a) No fields were edited or if there are any errors
  // b) email address/Domain are not valid
  // c) Required fields are not empty
  disableSave(errors) {
    return this.noEmptyRequiredFields() || (errors && errors.displayName) || this.fieldsNotEdited()
      || this.state.invalidEmailAddress || this.state.invalidEmailDomain;
  }

  async saveInstitution() {
    const {institution} = this.state;
    if (institution) {
      this.setState({invalidEmailAddress: false});
      if (institution.duaTypeEnum === DuaType.MASTER) {
        institution.emailAddresses = [];
      } else {
        institution.emailDomains = [];
      }
      if (institution.organizationTypeEnum !== OrganizationType.OTHER) {
        institution.organizationTypeOtherText = null;
      }
    }
    if (this.props.urlParams.institutionId) {
      await institutionApi().updateInstitution(this.props.urlParams.institutionId, institution)
        .then(value => this.backNavigate())
        .catch(reason => this.handleError(reason));
    } else {
      await institutionApi().createInstitution(institution)
        .then(value => this.backNavigate())
        .catch(reason => this.handleError(reason));
    }
  }

  handleError(rejectReason) {
    let errorMsg = 'Error while saving Institution. Please try again later';
    if (rejectReason.status === 409) {
      errorMsg  = 'Institution with Name ' + this.state.institution.displayName + ' already exist';
    }
    this.setState({apiErrorMsg: errorMsg, showApiError: true});
  }
  updateInstitutionRole(institutionRole) {
    this.setState({showOtherInstitution: institutionRole === OrganizationType.OTHER});
    this.setState(fp.set(['institution', 'organizationTypeEnum'], institutionRole));
  }

  backButton() {
    if (!this.fieldsNotEdited()) {
      this.setState({showBackButtonWarning: true});
    } else {
      this.backNavigate();
    }
  }

  backNavigate() {
    navigate(['admin/institution']);
  }

  isEmailAddressValid() {
    return this.state.institution.duaTypeEnum === DuaType.RESTRICTED && !this.state.institution.emailAddresses;
  }

  isEmailDomainValid() {
    return this.state.institution.duaTypeEnum === DuaType.MASTER && !this.state.institution.emailDomains;
  }

  get buttonText() {
    return !this.state.isAddInstitution ? 'SAVE' : 'ADD';
  }


  render() {
    const {institution, isAddInstitution, showOtherInstitution} = this.state;
    const {
      displayName, organizationTypeEnum, duaTypeEnum
    } = institution;
    const errors = validate({
      displayName,
      'emailAddresses': !this.isEmailAddressValid(),
      'emailDomain': !this.isEmailDomainValid(),
      organizationTypeEnum,
      duaTypeEnum
    }, {
      displayName: {presence: {allowEmpty: false}, length: {maximum: 80, tooLong: 'must be %{count} characters or less'}},
      organizationTypeEnum: {presence: {allowEmpty: false}},
      duaTypeEnum: {presence: {allowEmpty: false}},
      emailAddresses: {truthiness: true},
      emailDomain: {truthiness: true}
    });
    return <div>
      <FadeBox style={{marginTop: '1rem', marginLeft: '1rem', width: '1239px'}}>
         <FlexRow>
           <Scroll
              dir='left'
              onClick={() => this.backButton()}
              style={{width: '1.2rem', margin: '0.4rem 0.4rem 0rem 0rem'}}
          /> <SemiBoldHeader style={{fontSize: '18px', lineHeight: '22px', marginBottom: '1rem'}}>
          {title}
          </SemiBoldHeader>
        </FlexRow>
        <FlexRow style={{justifyContent: 'flex-end', marginRight: '1rem'}}>
          <div>
            <Button type='secondary' onClick={() => this.backNavigate()} style={{marginRight: '1.5rem'}}>Cancel</Button>
            <TooltipTrigger data-test-id='tooltip' content={
              errors && this.disableSave(errors) && <div>Answer required fields
                <BulletAlignedUnorderedList>
                  {errors.displayName && <li>Display Name should be of at most 80 Characters</li>}
                </BulletAlignedUnorderedList>
              </div>
            } disable={isAddInstitution}>
              <Button type='primary' disabled={this.disableSave(errors)} onClick={() => this.saveInstitution()}>
                {this.buttonText}
              </Button>
            </TooltipTrigger>
          </div>
        </FlexRow>
        <FlexRow>
          <FlexColumn style={{width: '50%'}}>
            <TextInputWithLabel
                value={fp.startCase(institution.displayName)}
                inputId='displayName'
                inputName='displayName'
                placeholder='New Username'
                labelStyle={styles.label}
                inputStyle={{width: '16rem', marginTop: '0.3rem'}}
                labelText='Institution Name'
                onChange={v => this.setState(fp.set(['institution', 'displayName'], v))}
            />
            <div style={{color: colors.danger}} data-test-id='displayNameError'>
              {!isAddInstitution && errors && errors.displayName}
              </div>
            <label style={styles.label}>Institution Type</label>
            <Dropdown style={{width: '16rem'}} data-test-id='role-dropdown'
                      placeholder='Your Role'
                      options={this.institutionTypeOptions}
                      value={institution.organizationTypeEnum}
                      onChange={v => this.updateInstitutionRole(v.value)}/>
            <div style={{color: colors.danger}}>{!isAddInstitution && errors && errors.organizationTypeEnum}</div>

            {showOtherInstitution && <TextInputWithLabel value={institution.organizationTypeOtherText}
               onChange={v => this.setState(fp.set(['institution', 'organizationTypeOtherText'], v))}
               inputStyle={{width: '16rem', marginTop: '0.8rem'}}/>}
            <label style={styles.label}>Agreement Type</label>
            <Dropdown style={{width: '16rem'}} data-test-id='agreement-dropdown'
                      placeholder='Your Agreement'
                      options={this.institutionAgreementTypeOptions}
                      value={institution.duaTypeEnum}
                      onChange={v => this.setState(fp.set(['institution', 'duaTypeEnum'], v.value))}/>
            {institution.duaTypeEnum === DuaType.RESTRICTED && <FlexColumn data-test-id='emailAddress' style={{width: '16rem'}}>
              <label style={styles.label}>Accepted Email Address</label>
              <TextArea value={institution.emailAddresses && institution.emailAddresses.join(',\n')}
                        data-test-id='emailAddressInput'
                        onBlur={(v) => this.validateEmailAddresses()}
                  onChange={(v) => this.setEmailDomain(v, 'emailAddresses')}/>
              {this.state.invalidEmailAddress && <div data-test-id='emailAddressError' style={{color: colors.danger}}>
                {this.state.invalidEmailAddressMsg}
                </div>}
            </FlexColumn>}
            {institution.duaTypeEnum === DuaType.MASTER && <FlexColumn data-test-id='emailDomain' style={{width: '16rem'}}>
              <label style={styles.label}>Accepted Email Domain</label>
              <TextArea value={institution.emailDomains && institution.emailDomains.join(',\n')} onBlur={(v) => this.validateEmailDomain()}
                        data-test-id='emailDomainInput'
                        onChange={(v) => this.setEmailDomain(v, 'emailDomains')}/>
              {this.state.invalidEmailDomain && <div data-test-id='emailDomainError' style={{color: colors.danger}}>
                {this.state.invalidEmailDomainsMsg}
                </div>}
            </FlexColumn>}
          </FlexColumn>
          <FlexColumn style={{width: '50%', marginRight: '1rem'}}>
            <label style={{...styles.label, marginTop: '0rem'}}>User instruction (Optional)</label>
            <TextArea
                id={'areaOfResearch'}
                value={institution.userInstructions}
                onChange={(s: string) => this.setState(fp.set(['institution', 'userInstructions'], s))}
            />
          </FlexColumn>
        </FlexRow>
        {this.state.showBackButtonWarning && <Modal>
          <ModalTitle>Information not saved</ModalTitle>
          <ModalFooter>
            <Button onClick={() => this.setState({showBackButtonWarning: false})}
                    type='secondary' style={{marginRight: '2rem'}}>Finish Saving</Button>
            <Button onClick={() => this.backNavigate()}
                    type='primary'>Yes Continue</Button>
          </ModalFooter>
        </Modal>}
        {this.state.showApiError && <Modal>
          <ModalTitle>Error While Saving Data</ModalTitle>
          <ModalBody>
            <label style={{...styles.label, fontWeight: 100}}>{this.state.apiErrorMsg}</label>
          </ModalBody>
          <ModalFooter>
            <Button onClick={() => this.setState({showApiError: false})}
                    type='secondary'>Close</Button>
          </ModalFooter>
        </Modal>}
      </FadeBox>
      </div>;
  }

}

export const AdminInstitutionEdit = withUrlParams()(AdminInstitutionEditImpl);

@Component({
  template: '<div #root></div>'
})
export class AdminInstitutionEditComponent extends ReactWrapperBase {
  constructor() {
    super(AdminInstitutionEdit, []);
  }
}
