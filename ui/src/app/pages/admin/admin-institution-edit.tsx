import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SemiBoldHeader} from 'app/components/headers';
import {TextArea, TextInputWithLabel} from 'app/components/inputs';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Scroll} from 'app/icons/scroll';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, UrlParamsProps, withUrlParams} from 'app/utils';
import {convertAPIError} from 'app/utils/errors';
import {navigate} from 'app/utils/navigation';
import {DuaType, Institution, OrganizationType} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import * as validate from 'validate.js';
import {DuaTypes, OrganizationTypeOptions} from './admin-institution-options';
import {WithSpinnerOverlayProps} from "../../components/with-spinner-overlay";

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

enum InstitutionMode {
  ADD,
  EDIT
}

interface InstitutionEditState {
  apiErrorMsg: string;
  institutionMode: InstitutionMode;
  institution: Institution;
  invalidEmailAddress: boolean;
  invalidEmailAddressMsg: string;
  invalidEmailDomain: boolean;
  invalidEmailDomainsMsg: string;
  showOtherInstitutionTextBox: boolean;
  showBackButtonWarning: boolean;
  showApiError: boolean;
}

let title = 'Add new Institution';
let institutionToEdit;

interface Props extends UrlParamsProps, WithSpinnerOverlayProps {}

export const AdminInstitutionEdit = withUrlParams()(class extends React.Component<Props, InstitutionEditState> {
  constructor(props) {
    super(props);
    this.state = {
      apiErrorMsg: '',
      institutionMode: InstitutionMode.ADD,
      institution: {
        shortName: '',
        displayName: '',
        organizationTypeEnum: null
      },
      invalidEmailAddress: false,
      invalidEmailAddressMsg: '',
      invalidEmailDomain: false,
      invalidEmailDomainsMsg: '',
      showOtherInstitutionTextBox: false,
      showBackButtonWarning: false,
      showApiError: false
    };
  }

  async componentDidMount() {
    this.props.hideSpinner();
    // If institution short Name is passed in the URL get the institution details
    if (this.props.urlParams.institutionId) {
      institutionToEdit = await institutionApi().getInstitution(this.props.urlParams.institutionId);
      title = institutionToEdit.displayName;
      if (!institutionToEdit.duaTypeEnum) {
        institutionToEdit.duaTypeEnum = DuaType.MASTER;
      }
      this.setState({
        institutionMode: InstitutionMode.EDIT,
        institution: institutionToEdit,
        showOtherInstitutionTextBox: institutionToEdit.organizationTypeEnum === OrganizationType.OTHER
      });
    } else {
      title = 'Add new Institution';
      this.setState({institutionMode: InstitutionMode.ADD});
    }

  }

  // Filter out empty line or empty email addresses like <email1>,,<email2>
  // Confirm each email is a valid email using validate.js
  validateEmailAddresses() {
    const invalidEmailAddress = [];
    const {emailAddresses} = this.state.institution;
    const updatedEmailAddress = emailAddresses.filter(
      emailAddress => {
        return emailAddress !== '' || !!emailAddress;
      });

    this.setState(fp.set(['institution', 'emailAddresses'], updatedEmailAddress));
    updatedEmailAddress.map(emailAddress => {
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
  validateEmailDomains() {
    const invalidEmailDomain = [];
    const {emailDomains} = this.state.institution;
    const emailDomainsWithNoEmptyString =
      emailDomains.filter(emailDomain => emailDomain.trim() !== '');
    this.setState(fp.set(['institution', 'emailDomains'], emailDomainsWithNoEmptyString));

    emailDomainsWithNoEmptyString.map(emailDomain => {
      const testAddress = 'test@' + emailDomain;
      const errors = validate({
        testAddress
      }, {
        testAddress: {email: true}
      });
      if (errors && errors.testAddress && errors.testAddress.length > 0) {
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

  setEmails(emailInput, attribute) {
    const emailList = emailInput.split(/[,\n]+/);
    this.setState(fp.set(['institution', attribute], emailList.map(email => email.trim())));
  }

  // Check if the fields have not been edited
  fieldsNotEdited() {
    return (this.isAddInstitutionMode && !this.fieldsNotEditedAddInstitution)
        || (institutionToEdit && this.fieldsNotEditedEditInstitution);
  }

  get fieldsNotEditedAddInstitution() {
    const {institution} = this.state;
    return institution.displayName || institution.userInstructions ||
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


  validateRequiredFields() {
    const {institution} = this.state;
    let emailValid = false;
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
    return this.validateRequiredFields() || errors || this.fieldsNotEdited()
      || this.state.invalidEmailAddress || this.state.invalidEmailDomain;
  }

  async saveInstitution() {
    const {institution, institutionMode} = this.state;
    if (institution) {
      if (institution.duaTypeEnum === DuaType.MASTER) {
        institution.emailAddresses = [];
      } else {
        institution.emailDomains = [];
      }
      if (institution.organizationTypeEnum !== OrganizationType.OTHER) {
        institution.organizationTypeOtherText = null;
      }
    }
    if (institutionMode === InstitutionMode.EDIT) {
      await institutionApi().updateInstitution(this.props.urlParams.institutionId, institution)
        .then(value => this.backNavigate())
        .catch(reason => this.handleError(reason));
    } else {
      await institutionApi().createInstitution(institution)
        .then(value => this.backNavigate())
        .catch(reason => this.handleError(reason));
    }
  }

  async handleError(rejectReason) {
    let errorMsg = 'Error while saving Institution. Please try again later';
    const error = await convertAPIError(rejectReason);
    if (rejectReason.status === 409) {
      errorMsg  = error.message;
    }
    this.setState({apiErrorMsg: errorMsg, showApiError: true});
  }
  updateInstitutionRole(institutionRole) {
    this.setState({showOtherInstitutionTextBox: institutionRole === OrganizationType.OTHER});
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

  validateEmailAddressPresence() {
    const {institution} = this.state;
    return this.state.institution.duaTypeEnum === DuaType.MASTER ||
      institution.emailAddresses && institution.emailAddresses.length > 0;
  }

  validateEmailDomainPresence() {
    const {institution} = this.state;
    return this.state.institution.duaTypeEnum === DuaType.RESTRICTED ||
      institution.emailDomains && institution.emailDomains.length > 0;
  }

  get buttonText() {
    return !this.isAddInstitutionMode ? 'SAVE' : 'ADD';
  }

  get isAddInstitutionMode() {
    return this.state.institutionMode === InstitutionMode.ADD;
  }

  render() {
    const {institution, showOtherInstitutionTextBox} = this.state;
    const {
      displayName, organizationTypeEnum, duaTypeEnum
    } = institution;
    const errors = validate({
      displayName,
      'emailAddresses': this.validateEmailAddressPresence(),
      'emailDomain': this.validateEmailDomainPresence(),
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
                  {errors.organizationTypeEnum && <li>Organization Type should not be empty</li>}
                  {errors.duaTypeEnum && <li>Agreement Type should not be empty</li>}
                  {!errors.duaTypeEnum && errors.emailDomain && <li>Email Domain should not be empty</li>}
                  {!errors.duaTypeEnum && errors.emailAddresses && <li>Email Address should not be empty</li>}
                </BulletAlignedUnorderedList>
              </div>
            } disable={this.isAddInstitutionMode}>
              <Button type='primary' disabled={this.disableSave(errors)} onClick={() => this.saveInstitution()}>
                {this.buttonText}
              </Button>
            </TooltipTrigger>
          </div>
        </FlexRow>
        <FlexRow>
          <FlexColumn style={{width: '50%'}}>
            <TextInputWithLabel
                value={institution.displayName}
                inputId='displayName'
                inputName='displayName'
                placeholder='Display Name'
                labelStyle={styles.label}
                inputStyle={{width: '16rem', marginTop: '0.3rem'}}
                labelText='Institution Name'
                onChange={v => this.setState(fp.set(['institution', 'displayName'], v))}
                onBlur={v => this.setState(fp.set(['institution', 'displayName'], v.trim()))}
            />
            <div style={{color: colors.danger}} data-test-id='displayNameError'>
              {!this.isAddInstitutionMode && errors && errors.displayName}
              </div>
            <label style={styles.label}>Institution Type</label>
            <Dropdown style={{width: '16rem'}} data-test-id='organization-dropdown'
                      placeholder='Organization Type'
                      options={OrganizationTypeOptions}
                      value={institution.organizationTypeEnum}
                      onChange={v => this.updateInstitutionRole(v.value)}/>
            <div style={{color: colors.danger}}>{!this.isAddInstitutionMode && errors && errors.organizationTypeEnum}</div>
            {showOtherInstitutionTextBox && <TextInputWithLabel
              value={institution.organizationTypeOtherText}
              onChange={v => this.setState(fp.set(['institution', 'organizationTypeOtherText'], v))}
              onBlur={v => this.setState(fp.set(['institution', 'organizationTypeOtherText'], v.trim()))}
              inputStyle={{width: '16rem', marginTop: '0.8rem'}}/>}
            <label style={styles.label}>Agreement Type</label>
            <Dropdown style={{width: '16rem'}} data-test-id='agreement-dropdown'
                      placeholder='Your Agreement'
                      options={DuaTypes}
                      value={institution.duaTypeEnum}
                      onChange={v => this.setState(fp.set(['institution', 'duaTypeEnum'], v.value))}/>
            {institution.duaTypeEnum === DuaType.RESTRICTED && <FlexColumn data-test-id='emailAddress' style={{width: '16rem'}}>
              <label style={styles.label}>Accepted Email Addresses</label>
              <TextArea value={institution.emailAddresses && institution.emailAddresses.join(',\n')}
                        data-test-id='emailAddressInput'
                        onBlur={(v) => this.validateEmailAddresses()}
                  onChange={(v) => this.setEmails(v, 'emailAddresses')}/>
              {this.state.invalidEmailAddress && <div data-test-id='emailAddressError' style={{color: colors.danger}}>
                {this.state.invalidEmailAddressMsg}
                </div>}
            </FlexColumn>}
            {institution.duaTypeEnum === DuaType.MASTER && <FlexColumn data-test-id='emailDomain' style={{width: '16rem'}}>
              <label style={styles.label}>Accepted Email Domains</label>
              <TextArea value={institution.emailDomains && institution.emailDomains.join(',\n')} onBlur={(v) => this.validateEmailDomains()}
                        data-test-id='emailDomainInput'
                        onChange={(v) => this.setEmails(v, 'emailDomains')}/>
              {this.state.invalidEmailDomain && <div data-test-id='emailDomainError' style={{color: colors.danger}}>
                {this.state.invalidEmailDomainsMsg}
                </div>}
            </FlexColumn>}
          </FlexColumn>
          <FlexColumn style={{width: '50%', marginRight: '1rem'}}>
            <label style={{...styles.label, marginTop: '0rem'}}>User Email Instructions Text (Optional)</label>
            <TextArea
                id={'userEmailInstructions'}
                value={institution.userInstructions}
                onChange={(s: string) => this.setState(fp.set(['institution', 'userInstructions'], s))}
            />
          </FlexColumn>
        </FlexRow>
        {this.state.showBackButtonWarning && <Modal>
          <ModalTitle>Institution not saved</ModalTitle>
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
});
