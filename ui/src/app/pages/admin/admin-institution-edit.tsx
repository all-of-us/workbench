import * as React from 'react';
import { CSSProperties } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';
import validate from 'validate.js';

import {
  Institution,
  InstitutionMembershipRequirement,
  InstitutionTierConfig,
  OrganizationType,
} from 'generated/fetch';

import { CommonToggle } from 'app/components/admin/common-toggle';
import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { SemiBoldHeader } from 'app/components/headers';
import { TextArea, TextInputWithLabel } from 'app/components/inputs';
import { BulletAlignedUnorderedList } from 'app/components/lists';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { TierBadge } from 'app/components/tier-badge';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { Scroll } from 'app/icons/scroll';
import {
  MembershipRequirements,
  OrganizationTypeOptions,
} from 'app/pages/admin/admin-institution-options';
import { institutionApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { switchCase } from '@terra-ui-packages/core-utils';
import {
  AccessTierShortNames,
  displayNameForTier,
  orderedAccessTierShortNames,
} from 'app/utils/access-tiers';
import { convertAPIError } from 'app/utils/errors';
import {
  defaultTierConfig,
  getTierConfig,
  getTierEmailAddresses,
  getTierEmailDomains,
  updateEnableControlledTier,
  updateMembershipRequirement,
  updateRequireEra,
  updateTierEmailAddresses,
  updateTierEmailDomains,
} from 'app/utils/institutions';
import { NavigationProps } from 'app/utils/navigation';
import { MatchParams, serverConfigStore, useStore } from 'app/utils/stores';
import { canonicalizeUrl } from 'app/utils/urls';
import { withNavigation } from 'app/utils/with-navigation-hoc';

const styles = reactStyles({
  label: {
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '20px',
    color: colors.primary,
    marginTop: '2.25rem',
    marginBottom: '0.45rem',
  },
  tierLabel: {
    fontSize: '16px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '17px',
    color: '#333F52',
    marginTop: '1.2rem',
    marginBottom: '1.5rem',
  },
  tierConfigContainer: {
    width: '46.5rem',
    height: '30rem',
    borderRadius: '0.465rem',
    backgroundColor: 'rgba(33,111,180,0.1)',
    marginBottom: '1.5rem',
  },
  switch: {
    width: '3rem',
    height: '1.6875rem',
    borderRadius: '0.465rem',
    onColor: '#080',
  },
  saveButton: {
    textTransform: 'uppercase',
  },
});

enum InstitutionMode {
  ADD,
  EDIT,
}

const isAddressInvalid = (emailAddress: string): boolean => {
  const errors = validate({ emailAddress }, { emailAddress: { email: true } });
  return errors?.emailAddress && errors.emailAddress.length > 0;
};

const isDomainInvalid = (emailDomain: string): boolean =>
  isAddressInvalid(`test@${emailDomain}`);

const getInvalidEmailAddresses = (
  emailAddresses: Array<string>
): Array<string> => emailAddresses.filter(isAddressInvalid);

const getInvalidEmailDomains = (emailDomains: Array<string>): Array<string> =>
  emailDomains.filter(isDomainInvalid);

const nonEmpty = (item: string): boolean => item && !!item.trim();

const EraRequiredSwitch = (props: {
  tierConfig: InstitutionTierConfig;
  onToggle: (boolean) => void;
}) => {
  const { tierConfig, onToggle } = props;
  const {
    config: { enableRasLoginGovLinking },
  } = useStore(serverConfigStore);
  return (
    <CommonToggle
      name='eRA account required'
      dataTestId={`${tierConfig.accessTierShortName}-era-required-switch`}
      onToggle={(e) => onToggle(e)}
      checked={tierConfig.eraRequired}
      disabled={
        !enableRasLoginGovLinking ||
        tierConfig.membershipRequirement ===
          InstitutionMembershipRequirement.NOACCESS
      }
    />
  );
};

const EnableCtSwitch = (props: {
  institution: Institution;
  onToggle: (boolean) => void;
}) => {
  const { institution, onToggle } = props;
  return (
    <CommonToggle
      name='Controlled tier enabled'
      dataTestId='controlled-enabled-switch'
      onToggle={(e) => onToggle(e)}
      checked={
        getTierConfig(institution, AccessTierShortNames.Controlled)
          ?.membershipRequirement !== InstitutionMembershipRequirement.NOACCESS
      }
      disabled={false} // TODO
    />
  );
};

const RequirementDropdown = (props: {
  tierConfig: InstitutionTierConfig;
  onChange: (InstitutionMembershipRequirement) => void;
}) => {
  const { tierConfig, onChange } = props;
  return (
    <Dropdown
      appendTo='self'
      style={{ width: '24rem' }}
      data-test-id={`${tierConfig.accessTierShortName}-agreement-dropdown`}
      placeholder='Select type'
      options={MembershipRequirements}
      value={tierConfig.membershipRequirement}
      onChange={(v) => onChange(v.value)}
    />
  );
};

const AddressTextArea = (props: {
  accessTierShortName: string;
  emailAddresses: string[];
  onBlur: Function;
  onChange: (string) => void;
}) => {
  const { accessTierShortName, emailAddresses, onBlur, onChange } = props;
  return (
    <TextArea
      value={emailAddresses?.join(',\n')}
      data-test-id={`${accessTierShortName}-email-address-input`}
      onBlur={onBlur}
      onChange={onChange}
    />
  );
};

const DomainTextArea = (props: {
  accessTierShortName: string;
  emailDomains: string[];
  onBlur: Function;
  onChange: (string) => void;
}) => {
  const { accessTierShortName, emailDomains, onBlur, onChange } = props;
  return (
    <TextArea
      value={emailDomains?.join(',\n')}
      data-test-id={`${accessTierShortName}-email-domain-input`}
      onBlur={onBlur}
      onChange={onChange}
    />
  );
};

interface TierConfigProps {
  institution: Institution;
  accessTierShortName: string;
  setEnableControlledTier?: (boolean) => void;
  setEraRequired: (boolean) => void;
  setTierRequirement: (InstitutionMembershipRequirement) => void;
  filterEmptyAddresses: Function;
  setTierAddresses: (string) => void;
  filterEmptyDomains: Function;
  setTierDomains: (string) => void;
}
const TierConfig = (props: TierConfigProps) => {
  const {
    institution,
    accessTierShortName,
    setEnableControlledTier,
    setEraRequired,
    setTierRequirement,
    filterEmptyAddresses,
    setTierAddresses,
    filterEmptyDomains,
    setTierDomains,
  } = props;

  const tierBadgeStyle: CSSProperties = {
    marginTop: '0.9rem',
    marginLeft: '0.9rem',
  };

  const tierConfig = getTierConfig(institution, accessTierShortName);
  const { emailAddresses, emailDomains } = tierConfig;

  return (
    <FlexRow style={styles.tierConfigContainer}>
      <div>
        <TierBadge {...{ accessTierShortName }} style={tierBadgeStyle} />
      </div>
      <FlexColumn style={{ marginLeft: '0.6rem' }}>
        <label style={styles.tierLabel}>
          {displayNameForTier(accessTierShortName)} access
        </label>
        <FlexRow style={{ gap: '0.45rem' }}>
          <EraRequiredSwitch
            tierConfig={tierConfig}
            onToggle={setEraRequired}
          />
          {accessTierShortName === AccessTierShortNames.Controlled && (
            <EnableCtSwitch
              institution={institution}
              onToggle={setEnableControlledTier}
            />
          )}
        </FlexRow>
        {tierConfig.membershipRequirement !==
          InstitutionMembershipRequirement.NOACCESS && (
          <div
            style={{ marginTop: '1.5rem' }}
            data-test-id={`${accessTierShortName}-card-details`}
          >
            <label style={styles.label}>
              A user is considered part of this institution and eligible <br />
              to access {displayNameForTier(accessTierShortName)} data if:
            </label>
            <RequirementDropdown
              tierConfig={tierConfig}
              onChange={setTierRequirement}
            />
            {tierConfig.membershipRequirement ===
              InstitutionMembershipRequirement.ADDRESSES && (
              <FlexColumn
                data-test-id={`${accessTierShortName}-email-address`}
                style={{ width: '24rem' }}
              >
                <label style={styles.label}>Accepted Email Addresses</label>
                <AddressTextArea
                  accessTierShortName={accessTierShortName}
                  emailAddresses={emailAddresses}
                  onBlur={filterEmptyAddresses}
                  onChange={setTierAddresses}
                />
                <p
                  style={{
                    color: colors.primary,
                    fontSize: '12px',
                    lineHeight: '18px',
                  }}
                >
                  Enter one email address per line. <br />
                </p>
              </FlexColumn>
            )}
            {tierConfig.membershipRequirement ===
              InstitutionMembershipRequirement.DOMAINS && (
              <FlexColumn
                data-test-id={`${accessTierShortName}-email-domain`}
                style={{ width: '24rem' }}
              >
                <label style={styles.label}>Accepted Email Domains</label>
                <DomainTextArea
                  accessTierShortName={accessTierShortName}
                  emailDomains={emailDomains}
                  onBlur={filterEmptyDomains}
                  onChange={setTierDomains}
                />
                <p
                  style={{
                    color: colors.primary,
                    fontSize: '12px',
                    lineHeight: '18px',
                  }}
                >
                  Enter one domain per line. <br />
                  Note that subdomains are not included, so “university.edu”{' '}
                  <br />
                  matches alice@university.edu but not bob@med.university.edu.
                </p>
              </FlexColumn>
            )}
          </div>
        )}
      </FlexColumn>
    </FlexRow>
  );
};

const PendingChangesModal = (props: {
  onFinish: Function;
  onContinue: Function;
}) => {
  const { onFinish, onContinue } = props;
  return (
    <Modal>
      <ModalTitle>Institution not saved</ModalTitle>
      <ModalBody>Are you sure you want to leave this page?</ModalBody>
      <ModalFooter>
        <Button
          onClick={onFinish}
          type='secondary'
          style={{ marginRight: '3rem' }}
        >
          Keep Editing
        </Button>
        <Button onClick={onContinue} type='primary'>
          Yes, Leave
        </Button>
      </ModalFooter>
    </Modal>
  );
};

const ApiErrorModal = (props: { errorMsg: string; onClose: Function }) => {
  const { errorMsg, onClose } = props;
  return (
    <Modal>
      <ModalTitle>Error While Saving Data</ModalTitle>
      <ModalBody>
        <label style={{ ...styles.label, fontWeight: 100 }}>{errorMsg}</label>
      </ModalBody>
      <ModalFooter>
        <Button onClick={onClose} type='secondary'>
          Close
        </Button>
      </ModalFooter>
    </Modal>
  );
};

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps<MatchParams> {}

interface InstitutionEditState {
  apiErrorMsg: string;
  institutionMode: InstitutionMode;
  institution: Institution;
  institutionBeforeEdits: Institution;
  showOtherInstitutionTextBox: boolean;
  showBackButtonWarning: boolean;
  showApiError: boolean;
  title: string;
}

export const AdminInstitutionEdit = fp.flow(
  withNavigation,
  withRouter
)(
  class extends React.Component<Props, InstitutionEditState> {
    constructor(props) {
      super(props);
      this.state = {
        apiErrorMsg: '',
        institutionMode: InstitutionMode.ADD,
        // properly initialized by initEditMode() / initAddMode()
        institution: {
          shortName: '',
          displayName: '',
          organizationTypeEnum: null,
        },
        institutionBeforeEdits: null,
        showOtherInstitutionTextBox: false,
        showBackButtonWarning: false,
        showApiError: false,
        title: '',
      };
    }

    initEditMode(loadedInstitution: Institution) {
      this.setState({
        institutionMode: InstitutionMode.EDIT,
        institution: loadedInstitution,
        institutionBeforeEdits: loadedInstitution,
        showOtherInstitutionTextBox:
          loadedInstitution.organizationTypeEnum === OrganizationType.OTHER,
        title: loadedInstitution.displayName,
      });
    }

    initAddMode() {
      this.setState({
        institutionMode: InstitutionMode.ADD,
        title: 'Add new Institution',
        institution: {
          shortName: '',
          displayName: '',
          organizationTypeEnum: null,
          tierConfigs: [
            {
              ...defaultTierConfig(AccessTierShortNames.Registered),
              membershipRequirement:
                InstitutionMembershipRequirement.UNINITIALIZED,
            },
          ],
        },
      });
    }

    async componentDidMount() {
      this.props.hideSpinner();
      // If institution short Name is passed in the URL get the institution details
      if (this.props.match.params.institutionId) {
        const loadedInstitution = await institutionApi().getInstitution(
          this.props.match.params.institutionId
        );
        this.initEditMode(loadedInstitution);
      } else {
        this.initAddMode();
      }
    }

    private setTierConfigs(tierConfigs: Array<InstitutionTierConfig>) {
      this.setState(fp.set(['institution', 'tierConfigs'], tierConfigs));
    }

    private filterEmptyAddresses(accessTierShortName: string) {
      const { tierConfigs } = this.state.institution;
      const nonEmptyAddrs = getTierEmailAddresses(
        tierConfigs,
        accessTierShortName
      ).filter(nonEmpty);
      this.setTierConfigs(
        updateTierEmailAddresses(
          tierConfigs,
          accessTierShortName,
          nonEmptyAddrs
        )
      );
    }

    private filterEmptyDomains(accessTierShortName: string) {
      const { tierConfigs } = this.state.institution;
      const nonEmptyDomains = getTierEmailDomains(
        tierConfigs,
        accessTierShortName
      ).filter(nonEmpty);
      this.setTierConfigs(
        updateTierEmailDomains(
          tierConfigs,
          accessTierShortName,
          nonEmptyDomains
        )
      );
    }

    private setMembershipRequirement(
      accessTierShortName: string,
      membershipRequirement: InstitutionMembershipRequirement
    ) {
      const { tierConfigs } = this.state.institution;
      this.setTierConfigs(
        updateMembershipRequirement(
          tierConfigs,
          accessTierShortName,
          membershipRequirement
        )
      );
    }

    private setRequireEra(accessTierShortName: string, requireEra: boolean) {
      const { tierConfigs } = this.state.institution;
      this.setTierConfigs(
        updateRequireEra(tierConfigs, accessTierShortName, requireEra)
      );
    }

    private setEnableControlledTier(enableControlled: boolean) {
      const { institution, institutionBeforeEdits } = this.state;
      this.setTierConfigs(
        updateEnableControlledTier(
          institution.tierConfigs,
          institutionBeforeEdits?.tierConfigs,
          enableControlled
        )
      );
    }

    trimEmails(emails: string): Array<string> {
      return emails.split(/[,\n]+/).map((email) => email.trim());
    }

    private setTierAddresses(
      accessTierShortName: string,
      emailAddresses: string
    ) {
      const { tierConfigs } = this.state.institution;
      this.setTierConfigs(
        updateTierEmailAddresses(
          tierConfigs,
          accessTierShortName,
          this.trimEmails(emailAddresses)
        )
      );
    }

    private setTierDomains(accessTierShortName: string, emailDomains: string) {
      const { tierConfigs } = this.state.institution;
      this.setTierConfigs(
        updateTierEmailDomains(
          tierConfigs,
          accessTierShortName,
          this.trimEmails(emailDomains)
        )
      );
    }

    // Check if the fields have not been edited
    fieldsNotEdited() {
      return (
        (this.isAddInstitutionMode && !this.fieldsEditedAddInstitution) ||
        (this.isEditInstitutionMode && !this.fieldsEditedEditInstitution)
      );
    }

    get fieldsEditedAddInstitution() {
      const { institution } = this.state;
      return (
        institution.displayName ||
        institution.userInstructions ||
        institution.organizationTypeEnum ||
        institution.tierConfigs
      );
    }

    get fieldsEditedEditInstitution() {
      const { institution, institutionBeforeEdits } = this.state;
      return institution !== institutionBeforeEdits;
    }

    hasInvalidFields() {
      const { institution } = this.state;
      if (!institution.tierConfigs) {
        // It is not expected for a tier requirement to be empty
        return true;
      }
    }

    // Disable save button if
    // a) there are any errors
    // b) any fields are invalid
    // c) No fields were edited
    disableSave(errors) {
      return errors || this.hasInvalidFields() || this.fieldsNotEdited();
    }

    backNavigate() {
      this.props.navigate(['admin', 'institution']);
    }

    cleanConfigForSaving(
      tierConfig: InstitutionTierConfig
    ): InstitutionTierConfig[] {
      return switchCase(
        tierConfig.membershipRequirement,
        // DOMAINS -> clear emailAddresses
        [
          InstitutionMembershipRequirement.DOMAINS,
          () => [
            {
              ...tierConfig,
              emailAddresses: [],
            },
          ],
        ],
        // ADDRESSES -> clear emailDomains
        [
          InstitutionMembershipRequirement.ADDRESSES,
          () => [
            {
              ...tierConfig,
              emailDomains: [],
            },
          ],
        ],
        // NOACCESS -> remove completely
        [InstitutionMembershipRequirement.NOACCESS, () => []]
      );
    }

    async saveInstitution() {
      const { institution, institutionMode } = this.state;

      const institutionToSave: Institution = {
        ...institution,
        tierConfigs: fp.flatMap(
          (tierConfig) => this.cleanConfigForSaving(tierConfig),
          institution.tierConfigs
        ),
      };

      if (institution.organizationTypeEnum !== OrganizationType.OTHER) {
        institutionToSave.organizationTypeOtherText = null;
      }

      if (institutionMode === InstitutionMode.EDIT) {
        await institutionApi()
          .updateInstitution(
            this.props.match.params.institutionId,
            institutionToSave
          )
          .then((updatedInstitution) => this.initEditMode(updatedInstitution))
          .catch((reason) => this.handleError(reason));
      } else {
        await institutionApi()
          .createInstitution(institutionToSave)
          .then(() => this.backNavigate())
          .catch((reason) => this.handleError(reason));
      }
    }

    async handleError(rejectReason) {
      let errorMsg = 'Error while saving Institution. Please try again later';
      const error = await convertAPIError(rejectReason);
      if (rejectReason.status === 409) {
        errorMsg = error.message;
      }
      this.setState({ apiErrorMsg: errorMsg, showApiError: true });
    }

    updateInstitutionRole(institutionRole) {
      this.setState({
        showOtherInstitutionTextBox: institutionRole === OrganizationType.OTHER,
      });
      this.setState(
        fp.set(['institution', 'organizationTypeEnum'], institutionRole)
      );
    }

    backButton() {
      if (!this.fieldsNotEdited()) {
        this.setState({ showBackButtonWarning: true });
      } else {
        this.backNavigate();
      }
    }

    get buttonText() {
      return !this.isAddInstitutionMode ? 'Save' : 'Add';
    }

    get isAddInstitutionMode() {
      return this.state.institutionMode === InstitutionMode.ADD;
    }

    get isEditInstitutionMode() {
      return this.state.institutionMode === InstitutionMode.EDIT;
    }

    render() {
      const { institution, showOtherInstitutionTextBox, title } = this.state;
      const {
        displayName,
        organizationTypeEnum,
        organizationTypeOtherText,
        requestAccessUrl,
      } = institution;

      validate.validators.customEmailAddresses = (
        accessTierShortName: string
      ) => {
        const tierConfig = getTierConfig(
          this.state.institution,
          accessTierShortName
        );
        if (
          tierConfig.membershipRequirement ===
          InstitutionMembershipRequirement.ADDRESSES
        ) {
          const addresses = (tierConfig.emailAddresses || []).filter(nonEmpty);
          if (addresses.length === 0) {
            return 'should not be empty';
          }
          const invalid = getInvalidEmailAddresses(addresses);
          if (invalid.length > 0) {
            return 'are not valid: ' + invalid.join(', ');
          }
        }
      };

      validate.validators.customEmailDomains = (
        accessTierShortName: string
      ) => {
        const tierConfig = getTierConfig(
          this.state.institution,
          accessTierShortName
        );
        if (
          tierConfig.membershipRequirement ===
          InstitutionMembershipRequirement.DOMAINS
        ) {
          const domains = (tierConfig.emailDomains || []).filter(nonEmpty);
          if (domains.length === 0) {
            return 'should not be empty';
          }
          const invalid = getInvalidEmailDomains(domains);
          if (invalid.length > 0) {
            return 'are not valid: ' + invalid.join(', ');
          }
        }
      };

      const errors = validate(
        {
          displayName,
          organizationTypeEnum,
          organizationTypeOtherText:
            organizationTypeEnum !== OrganizationType.OTHER ||
            organizationTypeOtherText,
          registeredTierEmailAddresses: AccessTierShortNames.Registered,
          controlledTierEmailAddresses: AccessTierShortNames.Controlled,
          registeredTierEmailDomains: AccessTierShortNames.Registered,
          controlledTierEmailDomains: AccessTierShortNames.Controlled,
          requestAccessUrl: requestAccessUrl
            ? canonicalizeUrl(requestAccessUrl)
            : null,
        },
        {
          displayName: {
            presence: { allowEmpty: false },
            length: {
              maximum: 80,
              tooLong: 'must be %{count} characters or less',
            },
          },
          organizationTypeEnum: { presence: { allowEmpty: false } },
          organizationTypeOtherText: { truthiness: true },
          registeredTierEmailAddresses: { customEmailAddresses: {} },
          controlledTierEmailAddresses: { customEmailAddresses: {} },
          registeredTierEmailDomains: { customEmailDomains: {} },
          controlledTierEmailDomains: { customEmailDomains: {} },
          requestAccessUrl: {
            url: {
              message: `^Request access URL is not a valid URL`,
            },

            length: {
              maximum: 2000,
              tooLong: '^Request access URL must be 2000 characters or fewer',
            },
          },
        }
      );

      return (
        <div>
          <FadeBox
            style={{
              marginTop: '1.5rem',
              marginLeft: '1.5rem',
              width: '1239px',
            }}
          >
            <FlexRow>
              <Scroll
                dir='left'
                onClick={() => this.backButton()}
                style={{ width: '1.8rem', margin: '0.6rem 0.6rem 0rem 0rem' }}
              />{' '}
              <SemiBoldHeader
                style={{
                  fontSize: '18px',
                  lineHeight: '22px',
                  marginBottom: '1.5rem',
                }}
              >
                {title}
              </SemiBoldHeader>
            </FlexRow>
            <FlexRow>
              <FlexColumn style={{ width: '50%' }}>
                <TextInputWithLabel
                  value={institution.displayName}
                  inputId='displayName'
                  inputName='displayName'
                  placeholder='Display Name'
                  labelStyle={styles.label}
                  inputStyle={{ width: '24rem', marginTop: '0.45rem' }}
                  labelText='Institution Name'
                  onChange={(v) =>
                    this.setState(fp.set(['institution', 'displayName'], v))
                  }
                  onBlur={(v) =>
                    this.setState(
                      fp.set(['institution', 'displayName'], v.trim())
                    )
                  }
                />
                <div
                  style={{ color: colors.danger }}
                  data-test-id='displayNameError'
                >
                  {institution.displayName && errors?.displayName}
                </div>
                <label style={styles.label}>Institution Type</label>
                <Dropdown
                  appendTo='self'
                  style={{ width: '24rem' }}
                  data-test-id='organization-dropdown'
                  placeholder='Organization Type'
                  options={OrganizationTypeOptions}
                  value={institution.organizationTypeEnum}
                  onChange={(v) => this.updateInstitutionRole(v.value)}
                />
                {showOtherInstitutionTextBox && (
                  <TextInputWithLabel
                    value={institution.organizationTypeOtherText}
                    onChange={(v) =>
                      this.setState(
                        fp.set(['institution', 'organizationTypeOtherText'], v)
                      )
                    }
                    onBlur={(v) =>
                      this.setState(
                        fp.set(
                          ['institution', 'organizationTypeOtherText'],
                          v.trim()
                        )
                      )
                    }
                    inputStyle={{ width: '24rem', marginTop: '1.2rem' }}
                  />
                )}
                <TextInputWithLabel
                  value={requestAccessUrl ?? ''}
                  inputId='requestAccessUrl'
                  inputName='requestAccessUrl'
                  placeholder='Request Access URL'
                  containerStyle={{ marginTop: '2.25rem' }}
                  labelStyle={styles.label}
                  inputStyle={{ width: '24rem', marginTop: '0.45rem' }}
                  labelText='Custom URL for “Request Access” Links'
                  onChange={(v) =>
                    this.setState(
                      fp.set(['institution', 'requestAccessUrl'], v)
                    )
                  }
                  onBlur={(v) =>
                    this.setState(
                      fp.set(['institution', 'requestAccessUrl'], v.trim())
                    )
                  }
                />

                <p
                  style={{
                    color: colors.primary,
                    fontSize: 12,
                    width: '24rem',
                  }}
                >
                  If provided, users who select this institution but are not
                  allowed according to the data access tier requirements below
                  will be guided to the custom URL rather than the standard ones
                  (initial registration, request for Controlled Tier access).
                </p>
              </FlexColumn>
              <FlexColumn style={{ width: '50%', marginRight: '1.5rem' }}>
                <label style={{ ...styles.label, marginTop: '0rem' }}>
                  User Email Instructions Text (Optional)
                </label>
                <TextArea
                  id={'userEmailInstructions'}
                  value={
                    institution.userInstructions
                      ? institution.userInstructions
                      : ''
                  }
                  onChange={(s: string) =>
                    this.setState(
                      fp.set(['institution', 'userInstructions'], s)
                    )
                  }
                />
              </FlexColumn>
            </FlexRow>
            <SemiBoldHeader
              style={{
                fontSize: '18px',
                lineHeight: '22px',
                marginTop: '3rem',
              }}
            >
              Data access tiers
            </SemiBoldHeader>
            <hr style={{ border: '1px solid #A9B6CB' }} />
            <FlexRow style={{ gap: '3rem' }}>
              {orderedAccessTierShortNames.map((accessTierShortName) => (
                <TierConfig
                  key={accessTierShortName}
                  accessTierShortName={accessTierShortName}
                  institution={institution}
                  setEnableControlledTier={(value) =>
                    this.setEnableControlledTier(value)
                  }
                  setEraRequired={(value) =>
                    this.setRequireEra(accessTierShortName, value)
                  }
                  setTierRequirement={(requirement) =>
                    this.setMembershipRequirement(
                      accessTierShortName,
                      requirement
                    )
                  }
                  filterEmptyAddresses={() =>
                    this.filterEmptyAddresses(accessTierShortName)
                  }
                  setTierAddresses={(addrs) =>
                    this.setTierAddresses(accessTierShortName, addrs)
                  }
                  filterEmptyDomains={() =>
                    this.filterEmptyDomains(accessTierShortName)
                  }
                  setTierDomains={(domains) =>
                    this.setTierDomains(accessTierShortName, domains)
                  }
                />
              ))}
            </FlexRow>
            <FlexRow
              style={{ justifyContent: 'flex-start', marginRight: '1.5rem' }}
            >
              <div>
                <Button
                  type='secondary'
                  path='/admin/institution'
                  style={{ marginRight: '2.225rem' }}
                >
                  Cancel
                </Button>
                <TooltipTrigger
                  data-test-id='tooltip'
                  content={
                    errors &&
                    this.disableSave(errors) && (
                      <div>
                        Please correct the following errors
                        <BulletAlignedUnorderedList>
                          {[
                            errors.displayName,
                            errors.organizationTypeEnum,
                            errors.registeredTierEmailAddresses,
                            errors.registeredTierEmailDomains,
                            errors.controlledTierEmailAddresses,
                            errors.controlledTierEmailDomains,
                          ].map((e) => e && <li key={e}>{e}</li>)}
                          {errors.organizationTypeOtherText && (
                            <li>
                              Organization Type 'Other' requires additional
                              information
                            </li>
                          )}
                          {errors.requestAccessUrl?.map(
                            (e) => e && <li key={e}>{e}</li>
                          )}
                        </BulletAlignedUnorderedList>
                      </div>
                    )
                  }
                  disable={this.isAddInstitutionMode}
                >
                  <Button
                    type='primary'
                    data-test-id='save-institution-button'
                    style={styles.saveButton}
                    disabled={this.disableSave(errors)}
                    onClick={async () => {
                      this.props.showSpinner();
                      await this.saveInstitution();
                      this.props.hideSpinner();
                    }}
                  >
                    {this.buttonText}
                  </Button>
                </TooltipTrigger>
              </div>
            </FlexRow>
            {this.state.showBackButtonWarning && (
              <PendingChangesModal
                onFinish={() => this.setState({ showBackButtonWarning: false })}
                onContinue={() => this.backNavigate()}
              />
            )}
            {this.state.showApiError && (
              <ApiErrorModal
                errorMsg={this.state.apiErrorMsg}
                onClose={() => this.setState({ showApiError: false })}
              />
            )}
          </FadeBox>
        </div>
      );
    }
  }
);
