import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SemiBoldHeader} from 'app/components/headers';
import {ControlledTierBadge, RegisteredTierBadge} from 'app/components/icons';
import {TextArea, TextInputWithLabel} from 'app/components/inputs';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {Scroll} from 'app/icons/scroll';
import {
  MembershipRequirements,
  OrganizationTypeOptions
} from 'app/pages/admin/admin-institution-options';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, UrlParamsProps, withUrlParams} from 'app/utils';
import {convertAPIError} from 'app/utils/errors';
import {
  getControlledTierConfig,
  getControlledTierEmailAddresses,
  getControlledTierEmailDomains,
  getRegisteredTierConfig,
  getRegisteredTierEmailAddresses,
  getRegisteredTierEmailDomains,
  updateCtEmailAddresses, updateCtEmailDomains,
  updateRtEmailAddresses,
  updateRtEmailDomains,
} from 'app/utils/institutions';
import {navigate} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/stores';
import {
  Institution,
  InstitutionMembershipRequirement,
  OrganizationType
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import {InputSwitch} from 'primereact/inputswitch';
import * as React from 'react';
import * as validate from 'validate.js';

const styles = reactStyles({
  label: {
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '20px',
    color: colors.primary,
    marginTop: '1.5rem',
    marginBottom: '0.3rem'
  },
  tierLabel: {
    fontSize: '16px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '17px',
    color: '#333F52',
    marginTop: '0.8rem',
    marginBottom: '1rem'
  },
  tierConfigContainer: {
    width: '31rem',
    height: '20rem',
    borderRadius: '0.31rem',
    backgroundColor: 'rgba(33,111,180,0.1)',
    marginBottom: '1rem'
  },
  switch: {
    width: '2rem',
    height: '1.125rem',
    borderRadius: '0.31rem',
    onColor: '#080',
  },
});

const css = `
  body .p-inputswitch {
    height: 18px;
    width: 33px;
    border-radius: 15px;
    font-size:11px;
  }
  body .p-inputswitch.p-inputswitch-checked .p-inputswitch-slider {
    background-color: #659F3D;
 }
`;

enum InstitutionMode {
  ADD,
  EDIT
}

interface InstitutionEditState {
  apiErrorMsg: string;
  institutionMode: InstitutionMode;
  institution: Institution;
  institutionToEdit: Institution;
  invalidRtEmailAddress: boolean;
  invalidRtEmailAddressMsg: string;
  invalidCtEmailAddress: boolean;
  invalidCtEmailAddressMsg: string;
  invalidRtEmailDomain: boolean;
  invalidRtEmailDomainsMsg: string;
  invalidCtEmailDomain: boolean;
  invalidCtEmailDomainsMsg: string;
  showOtherInstitutionTextBox: boolean;
  showBackButtonWarning: boolean;
  showApiError: boolean;
  title: string;
}

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
      institutionToEdit: null,
      invalidRtEmailAddress: false,
      invalidRtEmailAddressMsg: '',
      invalidCtEmailAddress: false,
      invalidCtEmailAddressMsg: '',
      invalidRtEmailDomain: false,
      invalidRtEmailDomainsMsg: '',
      invalidCtEmailDomain: false,
      invalidCtEmailDomainsMsg: '',
      showOtherInstitutionTextBox: false,
      showBackButtonWarning: false,
      showApiError: false,
      title: '',
    };
  }

  async componentDidMount() {
    this.props.hideSpinner();
    // If institution short Name is passed in the URL get the institution details
    if (this.props.urlParams.institutionId) {
      const loadedInstitution = await institutionApi().getInstitution(this.props.urlParams.institutionId);
      this.setState({
        institutionMode: InstitutionMode.EDIT,
        institution: loadedInstitution,
        institutionToEdit: loadedInstitution,
        showOtherInstitutionTextBox: loadedInstitution.organizationTypeEnum === OrganizationType.OTHER,
        title: loadedInstitution.displayName
      });
    } else {
      this.setState({institutionMode: InstitutionMode.ADD, title: 'Add new Institution'});
    }
  }

  getInvalidEmailAddresses(emailAddresses) {
    const invalidEmailAddress = [];
    emailAddresses.map(emailAddress => {
      const errors = validate({
        emailAddress
      }, {
        emailAddress: {email: true}
      });
      if (errors && errors.emailAddress && errors.emailAddress.length > 0) {
        invalidEmailAddress.push(emailAddress);
      }
    });
    return invalidEmailAddress;
  }

  // Filter out empty line or empty email addresses like <email1>,,<email2> for registered tier
  // Confirm each email is a valid email using validate.js
  validateRtEmailAddresses() {
    const emailAddresses = getRegisteredTierEmailAddresses(this.state.institution);
    const updatedEmailAddress = emailAddresses.filter(
      emailAddress => emailAddress !== '' || !!emailAddress);
    const invalidRtEmailAddress = this.getInvalidEmailAddresses(updatedEmailAddress);

    this.setState(fp.set(['institution', 'tierConfigs'], updateRtEmailAddresses(this.state.institution, updatedEmailAddress)));
    this.setState({invalidRtEmailAddress: invalidRtEmailAddress.length > 0});
    if (invalidRtEmailAddress.length > 0) {
      const errMessage = 'Following Email Addresses are not valid : ' + invalidRtEmailAddress.join(' , ');
      this.setState({invalidRtEmailAddressMsg: errMessage});
    }
    return;
  }

  // Filter out empty line or empty email addresses like <email1>,,<email2> for controlled tier
  // Confirm each email is a valid email using validate.js
  validateCtEmailAddresses() {
    const emailAddresses = getControlledTierEmailAddresses(this.state.institution);
    const updatedEmailAddress = emailAddresses.filter(
      emailAddress => emailAddress !== '' || !!emailAddress);
    const invalidCtEmailAddress = this.getInvalidEmailAddresses(updatedEmailAddress);

    this.setState(fp.set(['institution', 'tierConfigs'], updateCtEmailAddresses(this.state.institution, updatedEmailAddress)));
    this.setState({invalidCtEmailAddress: invalidCtEmailAddress.length > 0});
    if (invalidCtEmailAddress.length > 0) {
      const errMessage = 'Following Email Addresses are not valid : ' + invalidCtEmailAddress.join(' , ');
      this.setState({invalidCtEmailAddressMsg: errMessage});
    }
    return;
  }

  getInvalidEmailDomains(emailDomains) {
    const invalidEmailDomains = [];
    emailDomains.map(emailDomain => {
      const testAddress = 'test@' + emailDomain;
      const errors = validate({
        testAddress
      }, {
        testAddress: {email: true}
      });
      if (errors && errors.testAddress && errors.testAddress.length > 0) {
        invalidEmailDomains.push(emailDomain);
      }
    });
    return invalidEmailDomains;
  }

  // Filter out empty line or empty email addresses like <email1>,,<email2> for registered tier
  // Confirm each email domain matches with regex
  validateRtEmailDomains() {
    const emailDomains = getRegisteredTierEmailDomains(this.state.institution);
    const emailDomainsWithNoEmptyString =
      emailDomains.filter(emailDomain => emailDomain.trim() !== '');
    this.setState(fp.set(['institution', 'tierConfigs'], updateRtEmailDomains(this.state.institution, emailDomainsWithNoEmptyString)));
    const invalidRtEmailDomain = this.getInvalidEmailDomains(emailDomainsWithNoEmptyString);
    this.setState({invalidRtEmailDomain: invalidRtEmailDomain.length > 0});
    if (invalidRtEmailDomain.length > 0) {
      const errMessage = 'Following Email Domains are not valid : ' + invalidRtEmailDomain.join(' , ');
      this.setState({invalidRtEmailDomainsMsg: errMessage});
    }
    return;
  }

  // Filter out empty line or empty email addresses like <email1>,,<email2> for controlled tier
  // Confirm each email domain matches with regex
  validateCtEmailDomains() {
    const emailDomains = getControlledTierEmailDomains(this.state.institution);
    const emailDomainsWithNoEmptyString =
        emailDomains.filter(emailDomain => emailDomain.trim() !== '');
    this.setState(fp.set(['institution', 'tierConfigs'], updateCtEmailDomains(this.state.institution, emailDomainsWithNoEmptyString)));
    const invalidCtEmailDomain = this.getInvalidEmailDomains(emailDomainsWithNoEmptyString);
    this.setState({invalidCtEmailDomain: invalidCtEmailDomain.length > 0});
    if (invalidCtEmailDomain.length > 0) {
      const errMessage = 'Following Email Domains are not valid : ' + invalidCtEmailDomain.join(' , ');
      this.setState({invalidCtEmailDomainsMsg: errMessage});
    }
    return;
  }

  setRegisteredTierRequirement(membershipRequirement) {
    const rtTierConfig = {
      ...getRegisteredTierConfig(this.state.institution),
      membershipRequirement: membershipRequirement.value,
      eraRequired: true
    };
    this.setState(fp.set(['institution', 'tierConfigs'], [rtTierConfig]));
  }

  setControlledTierRequirement(membershipRequirement) {
    const ctTierConfig = {
      ...getControlledTierConfig(this.state.institution),
      membershipRequirement: membershipRequirement.value,
      eraRequired: true
    };
    this.setState(fp.set(['institution', 'tierConfigs'], [getRegisteredTierConfig(this.state.institution), ctTierConfig]));
  }

  setRtRequireEra(eRAEnabled) {
    const rtTierConfig = {
      ...getRegisteredTierConfig(this.state.institution),
      eraRequired: eRAEnabled.value
    };
    this.setState(fp.set(['institution', 'tierConfigs'], [rtTierConfig, getControlledTierConfig(this.state.institution)]));
  }

  setCtRequireEra(eRAEnabled) {
    const ctTierConfig = {
      ...getControlledTierConfig(this.state.institution),
      eraRequired: eRAEnabled.value
    };
    this.setState(fp.set(['institution', 'tierConfigs'], [getRegisteredTierConfig(this.state.institution), ctTierConfig]));
  }

  setEnableControlledTier(enableCtAccess) {
    // When switch from disable to enabled, set tier requirement from NOACCESS to DOMAINS with empty domain list.
    const ctTierConfig = {
      ...getControlledTierConfig(this.state.institution),
      membershipRequirement: enableCtAccess.value === true ?
          InstitutionMembershipRequirement.DOMAINS : InstitutionMembershipRequirement.NOACCESS,
      eraRequired: true
    };
    this.setState(fp.set(['institution', 'tierConfigs'], [getRegisteredTierConfig(this.state.institution), ctTierConfig]));
  }

  setRegisteredTierEmails(emailInput) {
    this.setState(fp.set(['institution', 'tierConfigs'], updateRtEmailAddresses(this.state.institution, this.formatEmail(emailInput))));
  }

  setControlledTierEmails(emailInput) {
    this.setState(fp.set(['institution', 'tierConfigs'], updateCtEmailAddresses(this.state.institution, this.formatEmail(emailInput))));
  }

  setRegisteredTierDomains(emailInput) {
    this.setState(fp.set(['institution', 'tierConfigs'], updateRtEmailDomains(this.state.institution, this.formatEmail(emailInput))));
  }

  setControlledTierDomains(emailInput) {
    this.setState(fp.set(['institution', 'tierConfigs'], updateCtEmailDomains(this.state.institution, this.formatEmail(emailInput))));
  }

  formatEmail(emailInput) {
    return emailInput.split(/[,\n]+/).map(email => email.trim());
  }

  // Check if the fields have not been edited
  fieldsNotEdited() {
    return (this.isAddInstitutionMode && !this.fieldsEditedAddInstitution)
        || (this.state.institutionToEdit && !this.fieldsEditedEditInstitution);
  }

  get fieldsEditedAddInstitution() {
    const {institution} = this.state;
    return institution.displayName || institution.userInstructions ||
      institution.organizationTypeEnum || institution.tierConfigs;
  }

  get fieldsEditedEditInstitution() {
    const {institution, institutionToEdit} = this.state;
    return institution !== institutionToEdit;
  }

  hasInvalidFields() {
    const {institution} = this.state;
    let emailValid = true;
    if (!institution.tierConfigs) {
      // It is not expected for a tier requirement to be empty
      return true;
    }
    const rtConfig = getRegisteredTierConfig(institution);
    if (rtConfig.membershipRequirement !== InstitutionMembershipRequirement.NOACCESS) {
      emailValid = rtConfig.membershipRequirement === InstitutionMembershipRequirement.DOMAINS ?
          rtConfig.emailDomains !== undefined : rtConfig.emailAddresses !== undefined;
    }
    return !emailValid || !institution.displayName || !institution.organizationTypeEnum ||
        (institution.organizationTypeEnum === OrganizationType.OTHER &&
            !institution.organizationTypeOtherText);
  }

  // Disable save button if
  // a) No fields were edited or if there are any errors
  // b) email address/Domain are not valid
  // c) Required fields are not empty
  disableSave(errors) {
    return this.hasInvalidFields() || errors || this.fieldsNotEdited()
      || this.state.invalidRtEmailAddress || this.state.invalidRtEmailDomain
        || this.state.invalidCtEmailAddress || this.state.invalidCtEmailDomain;
  }

  async saveInstitution() {
    const {institution, institutionMode} = this.state;
    const rtConfig = getRegisteredTierConfig(institution);
    if (institution && rtConfig) {
      if (rtConfig.membershipRequirement === InstitutionMembershipRequirement.DOMAINS) {
        rtConfig.emailAddresses = [];
      } else if (rtConfig.membershipRequirement === InstitutionMembershipRequirement.ADDRESSES) {
        rtConfig.emailDomains = [];
      }
    }
    if (institution && institution.organizationTypeEnum !== OrganizationType.OTHER) {
      institution.organizationTypeOtherText = null;
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
    if (getRegisteredTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.ADDRESSES) {
      const rtEmailAddresses = getRegisteredTierEmailAddresses(institution);
      return rtEmailAddresses && rtEmailAddresses.length > 0;
    } else {
      return true;
    }
  }

  validateEmailDomainPresence() {
    const {institution} = this.state;
    if (getRegisteredTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.DOMAINS) {
      const rtEmailDomains = getRegisteredTierEmailDomains(institution);
      return rtEmailDomains && rtEmailDomains.length > 0;
    } else {
      return true;
    }
  }

  get buttonText() {
    return !this.isAddInstitutionMode ? 'SAVE' : 'ADD';
  }

  get isAddInstitutionMode() {
    return this.state.institutionMode === InstitutionMode.ADD;
  }

  render() {
    const {enableRasLoginGovLinking} = serverConfigStore.get().config;
    const {institution, showOtherInstitutionTextBox, title} = this.state;
    const {
      displayName, organizationTypeEnum, tierConfigs
    } = institution;
    const errors = validate({
      displayName,
      'tierEmailAddresses': this.validateEmailAddressPresence(),
      'tierEmailDomain': this.validateEmailDomainPresence(),
      organizationTypeEnum,
      tierConfigs
    }, {
      displayName: {presence: {allowEmpty: false}, length: {maximum: 80, tooLong: 'must be %{count} characters or less'}},
      organizationTypeEnum: {presence: {allowEmpty: false}},
      tierConfigs: {presence: {allowEmpty: false}},
      tierEmailAddresses: {truthiness: true},
      tierEmailDomain: {truthiness: true}
    });
    return <div>
      <style>{css}</style>
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
          </FlexColumn>
              <FlexColumn style={{width: '50%', marginRight: '1rem'}}>
            <label style={{...styles.label, marginTop: '0rem'}}>User Email Instructions Text (Optional)</label>
              <TextArea
                id={'userEmailInstructions'}
                value={institution.userInstructions ? institution.userInstructions : ''}
                onChange={(s: string) => this.setState(fp.set(['institution', 'userInstructions'], s))}
              />
            </FlexColumn>
        </FlexRow>
        <SemiBoldHeader style={{fontSize: '18px', lineHeight: '22px', marginTop: '2rem'}}>
          Data access tiers
        </SemiBoldHeader>
        <hr style={{border: '1px solid #A9B6CB'}}/>
        <FlexRow style={{gap: '2rem'}}>
          <FlexRow style={styles.tierConfigContainer}>
            <FlexColumn>
              <RegisteredTierBadge style={{marginTop: '0.6rem', marginLeft: '0.6rem'}}/>
            </FlexColumn>
            <FlexColumn style={{marginLeft: '0.4rem'}}>
              <label style={styles.tierLabel}>Registered tier access</label>
              <FlexRow style={{gap: '0.3rem'}}>
                <InputSwitch
                    data-test-id='rt-era-required-switch'
                    onChange={(v) => this.setRtRequireEra(v)}
                    checked={getRegisteredTierConfig(institution).eraRequired}
                    disabled={!enableRasLoginGovLinking}
                />
                eRA account required
              </FlexRow>
              <div style={{marginTop: '1rem'}}>
                <label style={styles.label}>A user is considered part of this institution and eligible <br/>
                  to access registered tier data if:</label>
                <Dropdown style={{width: '16rem'}} data-test-id='rt-agreement-dropdown'
                          placeholder='Select type'
                          options={MembershipRequirements}
                          value={getRegisteredTierConfig(institution).membershipRequirement}
                          onChange={(v) => this.setRegisteredTierRequirement(v)}/>
                {getRegisteredTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.ADDRESSES &&
                <FlexColumn data-test-id='rtEmailAddress' style={{width: '16rem'}}>
                  <label style={styles.label}>Accepted Email Addresses</label>
                  <TextArea value={getRegisteredTierEmailAddresses(institution)
                  && getRegisteredTierEmailAddresses(institution).join(',\n')}
                            data-test-id='rtEmailAddressInput'
                            onBlur={(v) => this.validateRtEmailAddresses()}
                            onChange={(v) => this.setRegisteredTierEmails(v)}/>
                  {this.state.invalidRtEmailAddress && <div data-test-id='rtEmailAddressError' style={{color: colors.danger}}>
                    {this.state.invalidRtEmailAddressMsg}
                  </div>}
                </FlexColumn>}
                {getRegisteredTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.DOMAINS
                && <FlexColumn data-test-id='rtEmailDomain' style={{width: '16rem'}}>
                  <label style={styles.label}>Accepted Email Domains</label>
                  <TextArea value={getRegisteredTierEmailDomains(institution) &&
                  getRegisteredTierEmailDomains(institution).join(',\n')} onBlur={(v) => this.validateRtEmailDomains()}
                            data-test-id='rtEmailDomainInput'
                            onChange={(v) => this.setRegisteredTierDomains(v)}/>
                  {this.state.invalidRtEmailDomain && <div data-test-id='rtEmailDomainError' style={{color: colors.danger}}>
                    {this.state.invalidRtEmailDomainsMsg}
                  </div>}
                </FlexColumn>}
                <p style={{color: colors.primary, fontSize: '12px', lineHeight: '18px'}}>
                  Enter one domain per line. <br/>
                  Note that subdomains are not included, so “university.edu” <br/>
                  matches alice@university.edu but not bob@med.university.edu.
                </p>
              </div>
            </FlexColumn>
          </FlexRow>
          <FlexRow style={styles.tierConfigContainer}>
            <FlexColumn>
              <ControlledTierBadge style={{marginTop: '0.6rem', marginLeft: '0.6rem'}}/>
            </FlexColumn>
            <FlexColumn style={{marginLeft: '0.4rem'}}>
              <label style={styles.tierLabel}>Controlled tier access</label>
              <FlexRow style={{gap: '0.3rem'}}>
                <InputSwitch
                    data-test-id='ct-era-required-switch'
                    onChange={(v) => this.setCtRequireEra(v)}
                    checked={getControlledTierConfig(institution).eraRequired}
                    disabled={!enableRasLoginGovLinking}
                />
                eRA account required
                <InputSwitch
                    data-test-id='ct-enabled-switch'
                    onChange={(v) => this.setEnableControlledTier(v)}
                    checked={getControlledTierConfig(institution).membershipRequirement !== InstitutionMembershipRequirement.NOACCESS}
                    disabled={false}
                />
                Controlled tier enabled
              </FlexRow>
              {getControlledTierConfig(institution).membershipRequirement !== InstitutionMembershipRequirement.NOACCESS &&
              <div style={{marginTop: '1rem'}} data-test-id='ct-card-container'>
                <label style={styles.label}>A user is considered part of this institution and eligible <br/>
                  to access registered tier data if:</label>
                <Dropdown style={{width: '16rem'}} data-test-id='ct-agreement-dropdown'
                          placeholder='Select type'
                          options={MembershipRequirements}
                          value={getControlledTierConfig(institution).membershipRequirement}
                          onChange={(v) => this.setControlledTierRequirement(v)}/>
                {getControlledTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.ADDRESSES &&
                <FlexColumn data-test-id='ctEmailAddress' style={{width: '16rem'}}>
                  <label style={styles.label}>Accepted Email Addresses</label>
                  <TextArea value={getControlledTierEmailAddresses(institution)
                  && getControlledTierEmailAddresses(institution).join(',\n')}
                            data-test-id='ctEmailAddressInput'
                            onBlur={(v) => this.validateCtEmailAddresses()}
                            onChange={(v) => this.setControlledTierEmails(v)}/>
                  {this.state.invalidCtEmailAddress && <div data-test-id='ctEmailAddressError' style={{color: colors.danger}}>
                    {this.state.invalidCtEmailAddressMsg}
                  </div>}
                </FlexColumn>}
                {getControlledTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.DOMAINS
                && <FlexColumn data-test-id='ctEmailDomain' style={{width: '16rem'}}>
                  <label style={styles.label}>Accepted Email Domains</label>
                  <TextArea value={getControlledTierEmailDomains(institution) &&
                  getControlledTierEmailDomains(institution).join(',\n')} onBlur={(v) => this.validateCtEmailDomains()}
                            data-test-id='ctEmailDomainInput'
                            onChange={(v) => this.setControlledTierDomains(v)}/>
                  {this.state.invalidCtEmailDomain && <div data-test-id='ctEmailDomainError' style={{color: colors.danger}}>
                    {this.state.invalidCtEmailDomainsMsg}
                  </div>}
                </FlexColumn>}
                <p style={{color: colors.primary, fontSize: '12px', lineHeight: '18px'}}>
                  Enter one domain per line. <br/>
                  Note that subdomains are not included, so “university.edu” <br/>
                  matches alice@university.edu but not bob@med.university.edu.
                </p>
              </div>}
            </FlexColumn>
          </FlexRow>
        </FlexRow>
        <FlexRow style={{justifyContent: 'flex-start', marginRight: '1rem'}}>
          <div>
            <Button type='secondary' onClick={() => this.backNavigate()} style={{marginRight: '1.5rem'}}>Cancel</Button>
            <TooltipTrigger data-test-id='tooltip' content={
              errors && this.disableSave(errors) && <div>Answer required fields
                <BulletAlignedUnorderedList>
                  {errors.displayName && <li>Display Name should be of at most 80 Characters</li>}
                  {errors.organizationTypeEnum && <li>Organization Type should not be empty</li>}
                  {errors.tierRequirements && <li>Agreement Type should not be empty</li>}
                  {!errors.tierRequirements && errors.tierEmailDomains && <li>Email Domains should not be empty</li>}
                  {!errors.tierRequirements && errors.tierEmailAddresses && <li>Email Addresses should not be empty</li>}
                </BulletAlignedUnorderedList>
              </div>
            } disable={this.isAddInstitutionMode}>
              <Button type='primary' disabled={this.disableSave(errors)} onClick={() => this.saveInstitution()}>
                {this.buttonText}
              </Button>
            </TooltipTrigger>
          </div>
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
