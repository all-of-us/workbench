import * as React from 'react';
import * as fp from 'lodash/fp';

import { AccessModule, AdminTableUser } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { Toggle } from 'app/components/inputs';
import { PopupTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { orderedAccessModules } from 'app/pages/admin/user/admin-user-common';
import { userAdminApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { getAccessModuleConfig } from 'app/utils/access-utils';

interface Props {
  // The user to render the bypass panel for.
  user: AdminTableUser;
  // Callback prop called when the bypass panel has changed one of the user's module bypass
  // statuses, requiring a data refresh.
  onBypassModuleUpdate?: Function;
}

interface State {
  // Whether the PopupTrigger is currently open. The PopupTrigger is an uncontrolled component,
  // so we do not directly control its open state. See popupRef which allows us to call methods
  // on the PopupTrigger wheere needed (e.g. to close the component after saving).
  isPopupOpen: boolean;
  // Whether the dialog is currently saving bypass data to the server, during which time
  // a spinner should be shown.
  isSaving: boolean;
  // The current set of bypassed access modules in the widget.
  selectedModules: AccessModule[];
}

const getBypassedModules = (user: AdminTableUser): Array<AccessModule> => {
  return [
    ...(user.complianceTrainingBypassTime
      ? [AccessModule.COMPLIANCE_TRAINING]
      : []),
    ...(user.ctComplianceTrainingBypassTime
      ? [AccessModule.CT_COMPLIANCE_TRAINING]
      : []),
    ...(user.duccBypassTime ? [AccessModule.DATA_USER_CODE_OF_CONDUCT] : []),
    ...(user.eraCommonsBypassTime ? [AccessModule.ERA_COMMONS] : []),
    ...(user.twoFactorAuthBypassTime ? [AccessModule.TWO_FACTOR_AUTH] : []),
    ...(user.identityBypassTime ? [AccessModule.IDENTITY] : []),
    ...(user.profileConfirmationBypassTime
      ? [AccessModule.PROFILE_CONFIRMATION]
      : []),
    ...(user.publicationConfirmationBypassTime
      ? [AccessModule.PUBLICATION_CONFIRMATION]
      : []),
  ];
};

const moduleToToggleProps: Record<
  AccessModule,
  { name: string; 'data-test-id': string }
> = {
  [AccessModule.COMPLIANCE_TRAINING]: {
    name: 'RT Compliance Training',
    'data-test-id': 'rt-compliance-training-toggle',
  },
  [AccessModule.CT_COMPLIANCE_TRAINING]: {
    name: 'CT Compliance Training',
    'data-test-id': 'ct-compliance-training-toggle',
  },
  [AccessModule.DATA_USER_CODE_OF_CONDUCT]: {
    name: 'Data User Code of Conduct',
    'data-test-id': 'ducc-toggle',
  },
  [AccessModule.ERA_COMMONS]: {
    name: 'eRA Commons Linking',
    'data-test-id': 'era-commons-toggle',
  },
  [AccessModule.TWO_FACTOR_AUTH]: {
    name: 'Two Factor Auth',
    'data-test-id': 'two-factor-auth-toggle',
  },
  [AccessModule.RAS_LINK_LOGIN_GOV]: {
    name: 'RAS Login.gov Link',
    'data-test-id': 'ras-link-login-gov-toggle',
  },
  [AccessModule.IDENTITY]: {
    name: 'Identity Verification',
    'data-test-id': 'identity-toggle',
  },
  [AccessModule.PROFILE_CONFIRMATION]: {
    name: 'Profile Confirmation',
    'data-test-id': 'profile-confirmation-toggle',
  },
  [AccessModule.PUBLICATION_CONFIRMATION]: {
    name: 'Publication Confirmation',
    'data-test-id': 'publication-confirmation-toggle',
  },
  [AccessModule.RAS_LINK_ID_ME]: {
    name: 'RAS ID.ME Link',
    'data-test-id': 'ras-link-id-me-toggle',
  },
};

export class AdminUserBypass extends React.Component<Props, State> {
  popupRef: React.RefObject<PopupTrigger>;
  constructor(props: Props) {
    super(props);
    const { user } = props;
    this.state = {
      isPopupOpen: false,
      isSaving: false,
      selectedModules: getBypassedModules(user),
    };
    this.popupRef = React.createRef<PopupTrigger>();
  }

  private resetState() {
    this.setState({ selectedModules: getBypassedModules(this.props.user) });
  }

  private closePopup() {
    if (this.popupRef?.current) {
      this.popupRef.current.close();
    }
  }

  cancel() {
    this.resetState();
    this.closePopup();
  }

  async save() {
    const { selectedModules } = this.state;
    const {
      user,
      user: { username },
    } = this.props;
    const changedModules = fp.xor(getBypassedModules(user), selectedModules);
    this.setState({ isSaving: true });
    try {
      await userAdminApi().updateAccountProperties({
        username,
        accessBypassRequests: changedModules.map((moduleName) => ({
          moduleName,
          bypassed: selectedModules.includes(moduleName),
        })),
      });
      this.setState({ isSaving: false });
      this.resetState();
      this.closePopup();
      if (this.props.onBypassModuleUpdate) {
        this.props.onBypassModuleUpdate();
      }
    } catch (e) {
      // TODO: if we had a toast component, here would be the right place to fire a toast
      // notification that saving failed and the user should retry. Instead, we'll just keep
      // the popup open but remove the spinner.
      this.setState({ isSaving: false });
    }
  }

  hasEdited(): boolean {
    return (
      fp.xor(this.state.selectedModules, getBypassedModules(this.props.user))
        .length !== 0
    );
  }

  componentDidUpdate(prevProps: Props) {
    // Reset the "default" set of selected modules if the rendered user changes.
    if (prevProps.user.userId !== this.props.user.userId) {
      this.resetState();
    } else if (
      !fp.isEqual(
        getBypassedModules(prevProps.user),
        getBypassedModules(this.props.user)
      )
    ) {
      console.log('User modules changed');
      this.resetState();
    }
  }

  render() {
    const { selectedModules, isPopupOpen, isSaving } = this.state;

    const bypassToggleProps = orderedAccessModules
      .filter((module) => getAccessModuleConfig(module).isEnabledInEnvironment)
      .map((module) => {
        return {
          ...moduleToToggleProps[module],
          module: module,
        };
      });

    return (
      <PopupTrigger
        ref={this.popupRef}
        side='bottom'
        onClose={() => {
          this.setState({ isPopupOpen: false });
          this.resetState();
        }}
        onOpen={() => this.setState({ isPopupOpen: true })}
        content={
          <FlexColumn style={{ padding: '1.5rem' }}>
            {bypassToggleProps.map((m) => (
              <Toggle
                key={m.name}
                name={m.name}
                checked={selectedModules.includes(m.module)}
                dataTestId={m['data-test-id']}
                onToggle={() => {
                  this.setState({
                    selectedModules: fp.xor(selectedModules, [m.module]),
                  });
                }}
              />
            ))}
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                type='secondary'
                onClick={() => this.cancel()}
                disabled={!this.hasEdited()}
              >
                Cancel
              </Button>
              <Button
                data-test-id='toggle-save'
                onClick={() => this.save()}
                disabled={!this.hasEdited()}
              >
                Save
              </Button>
            </div>
            {isSaving && <SpinnerOverlay />}
          </FlexColumn>
        }
      >
        <Button
          type='secondaryLight'
          data-test-id='bypass-popup'
          style={{ height: '40px' }}
        >
          <ClrIcon
            shape={isPopupOpen ? 'caret down' : 'caret right'}
            size={19}
            style={{
              color: colors.accent,
              marginRight: '1px',
              cursor: 'pointer',
            }}
          />
          Bypass
        </Button>
      </PopupTrigger>
    );
  }
}
