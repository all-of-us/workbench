import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {SemiBoldHeader} from 'app/components/headers';
import {TextArea, TextInputWithLabel} from 'app/components/inputs';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {Scroll} from 'app/icons/scroll';
import {MembershipRequirements, OrganizationTypeOptions} from 'app/pages/admin/admin-institution-options';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, UrlParamsProps, withUrlParams} from 'app/utils';
import {AccessTierShortNames} from 'app/utils/access-tiers';
import {convertAPIError} from 'app/utils/errors';
import {
  getControlledTierConfig, getControlledTierEmailAddresses, getControlledTierEmailDomains,
  getRegisteredTierConfig,
  getRegisteredTierEmailAddresses,
  getRegisteredTierEmailDomains
} from 'app/utils/institutions';
import {navigate} from 'app/utils/navigation';
import {
  DuaType,
  Institution,
  InstitutionMembershipRequirement,
  OrganizationType
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';
import * as validate from 'validate.js';
import {RegisteredTierBadge, ControlledTierBadge} from "../../components/icons";
import Switch from "react-switch";

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
    marginBottom: '1.5rem'
  },
  tierConfigContainer: {
    width: '31rem',
    height: '29rem',
    borderRadius: '0.31rem',
    backgroundColor: 'rgba(33,111,180,0.1)'
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
  institutionToEdit: Institution;
  invalidEmailAddress: boolean;
  invalidEmailAddressMsg: string;
  invalidEmailDomain: boolean;
  invalidEmailDomainsMsg: string;
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
      invalidEmailAddress: false,
      invalidEmailAddressMsg: '',
      invalidEmailDomain: false,
      invalidEmailDomainsMsg: '',
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

  // Filter out empty line or empty email addresses like <email1>,,<email2>
  // Confirm each email is a valid email using validate.js
  validateEmailAddresses() {
    const invalidEmailAddress = [];
    const emailAddresses = getRegisteredTierEmailAddresses(this.state.institution);
    const updatedEmailAddress = emailAddresses.filter(
      emailAddress => {
        return emailAddress !== '' || !!emailAddress;
      });

    // TODO(RW-6933): Implement new institution admin UI with CT support.
    this.setState(fp.set(['institution', 'tierEmailAddresses'],
        [{accessTierShortName: AccessTierShortNames.Registered, emailAddresses: updatedEmailAddress}]));
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
    const emailDomains = getRegisteredTierEmailDomains(this.state.institution);
    const emailDomainsWithNoEmptyString =
      emailDomains.filter(emailDomain => emailDomain.trim() !== '');
    // TODO(RW-6933): Implement new institution admin UI with CT support.
    this.setState(fp.set(['institution', 'tierEmailDomains'],
        [{accessTierShortName: AccessTierShortNames.Registered, emailDomains: emailDomainsWithNoEmptyString}]));

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

  setTierRequirement(membershipRequirement) {
    // TODO(RW-6933): Implement new institution admin UI with CT support.
    const rtTierConfig = {
      ...getRegisteredTierConfig(this.state.institution),
      membershipRequirement: membershipRequirement.value,
      eraRequired: true
    };
    // For now, only RT requirement is supported, so fine to set tierEmailAddresses to an single element array.
    this.setState(fp.set(['institution', 'tierConfigs'], [rtTierConfig]));

    // Dual write DuaTypeEnum for now. Because account creation flow still uses that field.
    const duaType = membershipRequirement.value === InstitutionMembershipRequirement.ADDRESSES
        ? DuaType.RESTRICTED : DuaType.MASTER;
    this.setState(fp.set(['institution', 'duaTypeEnum'], duaType));
  }

  setEmails(emailInput) {
    const emailList = emailInput.split(/[,\n]+/);
    const rtTierConfig = {
      ...getRegisteredTierConfig(this.state.institution),
      emailAddresses: emailList.map(email => email.trim()).filter((item) => item !== '')
    };
    // For now, only RT requirement is supported, so fine to set tierEmailAddresses to an single element array.
    this.setState(fp.set(['institution', 'tierConfigs'], [rtTierConfig]));
  }

  setDomains(emailInput) {
    const emailList = emailInput.split(/[,\n]+/);
    const rtTierConfig = {
      ...getRegisteredTierConfig(this.state.institution),
      emailDomains: emailList.map(email => email.trim()).filter((item) => item !== '')
    };
    // For now, only RT requirement is supported, so fine to set tierEmailAddresses to an single element array.
    this.setState(fp.set(['institution', 'tierConfigs'], [rtTierConfig]));
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
      || this.state.invalidEmailAddress || this.state.invalidEmailDomain;
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
        <SemiBoldHeader style={{fontSize: '18px', lineHeight: '22px', marginTop: '4rem'}}>
          Data access tiers
        </SemiBoldHeader>
        <hr style={{border: '1px solid #A9B6CB'}}/>
        <FlexRow style={{gap: '2rem'}}>
          <FlexRow style={styles.tierConfigContainer}>
            <FlexColumn>
              <RegisteredTierBadge style={{marginTop: '0.6rem', marginLeft: '0.3rem'}}/>
            </FlexColumn>
            <FlexColumn style={{marginLeft: '0.4rem'}}>
              <label style={styles.tierLabel}>Registered tier access</label>
              <FlexRow style={{gap: '0.3rem'}}>
                <Switch
                    onChange={(v) => this.setTierRequirement(v)}
                    checked={false}
                    checkedIcon={true}
                    disabled={true}
                    height={18}
                    width={33}
                />
                eRA account required
              </FlexRow>
              <label style={styles.label}>A user is considered part of this institution and eligible
                to access registered tier data if:</label>
              <Dropdown style={{width: '16rem'}} data-test-id='agreement-dropdown'
                        placeholder='Select type'
                        options={MembershipRequirements}
                        value={getRegisteredTierConfig(institution).membershipRequirement}
                        onChange={(v) => this.setTierRequirement(v)}/>
              {getRegisteredTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.ADDRESSES &&
              <FlexColumn data-test-id='emailAddress' style={{width: '16rem'}}>
                <label style={styles.label}>Accepted Email Addresses</label>
                <TextArea value={getRegisteredTierEmailAddresses(institution)
                && getRegisteredTierEmailAddresses(institution).join(',\n')}
                          data-test-id='emailAddressInput'
                          onBlur={(v) => this.validateEmailAddresses()}
                          onChange={(v) => this.setEmails(v)}/>
                {this.state.invalidEmailAddress && <div data-test-id='emailAddressError' style={{color: colors.danger}}>
                  {this.state.invalidEmailAddressMsg}
                </div>}
              </FlexColumn>}
              {getRegisteredTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.DOMAINS
              && <FlexColumn data-test-id='emailDomain' style={{width: '16rem'}}>
                <label style={styles.label}>Accepted Email Domains</label>
                <TextArea value={getRegisteredTierEmailDomains(institution) &&
                getRegisteredTierEmailDomains(institution).join(',\n')} onBlur={(v) => this.validateEmailDomains()}
                          data-test-id='emailDomainInput'
                          onChange={(v) => this.setDomains(v)}/>
                {this.state.invalidEmailDomain && <div data-test-id='emailDomainError' style={{color: colors.danger}}>
                  {this.state.invalidEmailDomainsMsg}
                </div>}
              </FlexColumn>}
              <p style={{color: colors.primary}}>
                Enter one domain per line. <br/>
                Note that subdomains are not included, so “university.edu” <br/>
                matches alice@university.edu but not bob@med.university.edu.
              </p>
            </FlexColumn>
          </FlexRow>
          <FlexColumn style={styles.tierConfigContainer}>
            <label style={styles.label}>Controlled tier access</label>
            <Dropdown style={{width: '16rem'}} data-test-id='agreement-dropdown'
                      placeholder='Select type'
                      options={MembershipRequirements}
                      value={getControlledTierConfig(institution).membershipRequirement}
                      onChange={(v) => this.setTierRequirement(v)}/>
            {getControlledTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.ADDRESSES &&
            <FlexColumn data-test-id='emailAddress' style={{width: '16rem'}}>
              <label style={styles.label}>Accepted Email Addresses</label>
              <TextArea value={getControlledTierEmailAddresses(institution)
              && getControlledTierEmailAddresses(institution).join(',\n')}
                        data-test-id='emailAddressInput'
                        onBlur={(v) => this.validateEmailAddresses()}
                        onChange={(v) => this.setEmails(v)}/>
              {this.state.invalidEmailAddress && <div data-test-id='emailAddressError' style={{color: colors.danger}}>
                {this.state.invalidEmailAddressMsg}
              </div>}
            </FlexColumn>}
            {getControlledTierConfig(institution).membershipRequirement === InstitutionMembershipRequirement.DOMAINS
            && <FlexColumn data-test-id='emailDomain' style={{width: '16rem'}}>
              <label style={styles.label}>Accepted Email Domains</label>
              <TextArea value={getControlledTierEmailDomains(institution) &&
              getControlledTierEmailDomains(institution).join(',\n')} onBlur={(v) => this.validateEmailDomains()}
                        data-test-id='emailDomainInput'
                        onChange={(v) => this.setDomains(v)}/>
              {this.state.invalidEmailDomain && <div data-test-id='emailDomainError' style={{color: colors.danger}}>
                {this.state.invalidEmailDomainsMsg}
              </div>}
            </FlexColumn>}
          </FlexColumn>
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
