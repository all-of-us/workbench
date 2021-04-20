import {Button, IconButton} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Check, ClrIcon, Times} from 'app/components/icons';
import {Toggle} from 'app/components/inputs';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';
import {AccessModule, AdminTableUser} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';

const styles = reactStyles({
  infoIcon: {
    color: colors.accent,
    cursor: 'pointer',
    marginBottom: '0.5rem',
    height: '16px',
    width: '16px',
    alignSelf: 'center'
  }
});

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
    ...(user.betaAccessBypassTime ? [AccessModule.BETAACCESS] : []),
    ...(user.complianceTrainingBypassTime ? [AccessModule.COMPLIANCETRAINING] : []),
    ...(user.dataUseAgreementBypassTime ? [AccessModule.DATAUSEAGREEMENT] : []),
    ...(user.eraCommonsBypassTime ? [AccessModule.ERACOMMONS] : []),
    ...(user.twoFactorAuthBypassTime ? [AccessModule.TWOFACTORAUTH] : []),
    ...(user.rasLinkLoginGovBypassTime ? [AccessModule.RASLINKLOGINGOV] : []),
  ];
};

export class AdminUserBypass extends React.Component<Props, State> {
  popupRef: React.RefObject<PopupTrigger>;
  constructor(props: Props) {
    super(props);
    const {user} = props;
    this.state = {
      isPopupOpen: false,
      isSaving: false,
      selectedModules: getBypassedModules(user)
    };
    this.popupRef = React.createRef<PopupTrigger>();
  }

  private resetState() {
    this.setState({selectedModules: getBypassedModules(this.props.user)});
  }

  private closePopup() {
    if (this.popupRef && this.popupRef.current) {
      this.popupRef.current.close();
    }
  }

  cancel() {
    this.resetState();
    this.closePopup();
  }

  async save() {
    const {selectedModules} = this.state;
    const {user} = this.props;
    const changedModules = fp.xor(getBypassedModules(user), selectedModules);
    this.setState({isSaving: true});
    try {
      for (const module of changedModules) {
        await profileApi()
          .bypassAccessRequirement(user.userId,
          {isBypassed: selectedModules.includes(module), moduleName: module});
      }
      this.setState({isSaving: false});
      this.resetState();
      this.closePopup();
      if (this.props.onBypassModuleUpdate) {
        this.props.onBypassModuleUpdate();
      }
    } catch (e) {
      // TODO: if we had a toast component, here would be the right place to fire a toast
      // notification that saving failed and the user should retry. Instead, we'll just keep
      // the popup open but remove the spinner.
      this.setState({isSaving: false});
    }
  }

  hasEdited(): boolean {
    return fp.xor(this.state.selectedModules, getBypassedModules(this.props.user)).length !== 0;
  }

  componentDidUpdate(prevProps: Props) {
  // Reset the "default" set of selected modules if the rendered user changes.
    if (prevProps.user.userId !== this.props.user.userId) {
      this.resetState();
    } else if (!fp.isEqual(getBypassedModules(prevProps.user), getBypassedModules(this.props.user))) {
      console.log('User modules changed');
      this.resetState();
    }
  }

  render() {
    const {selectedModules, isPopupOpen, isSaving} = this.state;
    const {enableBetaAccess,
      enableComplianceTraining,
      enableEraCommons,
      enableDataUseAgreement,
      enableRasLoginGovLinking} = serverConfigStore.getValue();
    return <PopupTrigger
        ref={this.popupRef}
        side='bottom'
        onClose={() => { this.setState({isPopupOpen: false}); this.resetState(); }}
        onOpen={() => this.setState({isPopupOpen: true})}
        content={<FlexColumn style={{padding: '1rem'}}>
          {enableBetaAccess && <FlexRow style={{justifyContent: 'space-between'}}>
            <Toggle name='Beta Access'
                    checked={selectedModules.includes(AccessModule.BETAACCESS)}
                    data-test-id='beta-access-toggle'
                    onToggle={() => {this.setState({selectedModules:
                      fp.xor(selectedModules, [AccessModule.BETAACCESS])}); }}
            />
            <TooltipTrigger content={'Grant beta access to a user.  This replaces verify/reject.'}>
              <ClrIcon shape='info' className='is-solid' style={styles.infoIcon}/>
            </TooltipTrigger>
          </FlexRow>}
          {enableBetaAccess && <hr style={{width: '100%', marginBottom: '0.5rem'}}/>}
          {enableComplianceTraining && <Toggle name='Compliance Training'
                  checked={selectedModules.includes(AccessModule.COMPLIANCETRAINING)}
                  data-test-id='compliance-training-toggle'
                  onToggle={() => {this.setState({selectedModules:
                      fp.xor(selectedModules, [AccessModule.COMPLIANCETRAINING])}); }}
          />}
          {enableEraCommons && <Toggle name='eRA Commons Linking'
                  checked={selectedModules.includes(AccessModule.ERACOMMONS)}
                  data-test-id='era-commons-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.ERACOMMONS])}); }}
          />}
          <Toggle name='Two Factor Auth'
                  checked={selectedModules.includes(AccessModule.TWOFACTORAUTH)}
                  data-test-id='two-factor-auth-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.TWOFACTORAUTH])}); }}
          />
          {enableDataUseAgreement && <Toggle name='Data Use Agreement'
                  checked={selectedModules.includes(AccessModule.DATAUSEAGREEMENT)}
                  data-test-id='data-use-agreement-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.DATAUSEAGREEMENT])}); }}
          />}
          {enableRasLoginGovLinking && <Toggle name='RAS Login.gov Link'
                                       checked={selectedModules.includes(AccessModule.RASLINKLOGINGOV)}
                                       data-test-id='ras-link-login-gov-toggle'
                                       onToggle={() => {this.setState({selectedModules:
                                             fp.xor(selectedModules, [AccessModule.RASLINKLOGINGOV])}); }}
          />}
          <div style={{display: 'flex', justifyContent: 'flex-end'}}>
            <IconButton icon={Times}
                        onClick={() => this.cancel()}
                        disabled={!this.hasEdited()}/>
            <IconButton icon={Check}
                        data-test-id='toggle-save'
                        onClick={() => this.save()}
                        disabled={!this.hasEdited()}/>
          </div>
          {isSaving && <SpinnerOverlay/>}
        </FlexColumn>}>
      <Button type='secondaryLight' data-test-id='bypass-popup' style={{height: '40px'}}>
        <ClrIcon shape={isPopupOpen ? 'caret down' : 'caret right'} size={19}
                 style={{color: colors.accent, marginRight: '1px', cursor: 'pointer'}}/>
        Bypass
      </Button>
    </PopupTrigger>;
  }

}
