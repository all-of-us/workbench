import {Button, IconButton} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Toggle} from 'app/components/inputs';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
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
  user: AdminTableUser;
}

interface State {
  selectedModules: AccessModule[];
  open: boolean;
}

const getUserModuleList = (user: AdminTableUser): Array<AccessModule> => {
  return [
    ...(user.betaAccessBypassTime ? [AccessModule.BETAACCESS] : []),
    ...(user.complianceTrainingBypassTime ? [AccessModule.COMPLIANCETRAINING] : []),
    ...(user.dataUseAgreementBypassTime ? [AccessModule.DATAUSEAGREEMENT] : []),
    ...(user.eraCommonsBypassTime ? [AccessModule.ERACOMMONS] : []),
    ...(user.twoFactorAuthBypassTime ? [AccessModule.TWOFACTORAUTH] : []),
  ];
};

export class AdminUserBypass extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    const {user} = props;
    this.state = {
      open: false,
      selectedModules: getUserModuleList(user)
    };
  }

  cancel() {
    this.setState({selectedModules: getUserModuleList(this.props.user)});
  }

  save() {
    const {selectedModules} = this.state;
    const {user} = this.props;
    const changedModules = fp.xor(getUserModuleList(user), selectedModules);
    changedModules.forEach(async module => {
      await profileApi()
        .bypassAccessRequirement(user.userId,
          {isBypassed: selectedModules.includes(module), moduleName: module});
    });
  }

  hasEdited(): boolean {
    return fp.xor(this.state.selectedModules, getUserModuleList(this.props.user)).length !== 0;
  }

  componentDidUpdate(prevProps: Props) {
  // Reset the "default" set of selected modules if the rendered user changes.
    if (prevProps.user.userId !== this.props.user.userId) {
      this.setState({
        selectedModules: getUserModuleList(this.props.user)
      });
    }
  }

  render() {
    const {selectedModules, open} = this.state;
    const {enableBetaAccess,
      enableComplianceTraining,
      enableEraCommons,
      enableDataUseAgreement} = serverConfigStore.getValue();
    return <PopupTrigger
        side='bottom'
        onClose={() => {this.cancel(); this.setState({open: false}); }}
        onOpen={() => this.setState({open: true})}
        content={<FlexColumn style={{padding: '1rem'}}>
          {enableBetaAccess && <FlexRow style={{justifyContent: 'space-between'}}>
            <Toggle name='Beta Access'
                    checked={selectedModules.includes(AccessModule.BETAACCESS)}
                    data-test-id='beta-access-toggle'
                    onToggle={() => {this.setState({selectedModules:
                      fp.xor(selectedModules, [AccessModule.BETAACCESS])}); } }
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
                      fp.xor(selectedModules, [AccessModule.COMPLIANCETRAINING])}); } }
          />}
          {enableEraCommons && <Toggle name='eRA Commons Linking'
                  checked={selectedModules.includes(AccessModule.ERACOMMONS)}
                  data-test-id='era-commons-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.ERACOMMONS])}); } }
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
          <div style={{display: 'flex', justifyContent: 'flex-end'}}>
            <IconButton icon='times'
                        onClick={() => this.cancel()}
                        disabled={!this.hasEdited()}/>
            <IconButton icon='check'
                        data-test-id='toggle-save'
                        onClick={() => this.save()}
                        disabled={!this.hasEdited()}/>
          </div>
        </FlexColumn>}>
      <Button type='secondaryLight' data-test-id='bypass-popup' style={{height: '40px'}}>
        <ClrIcon shape={open ? 'caret down' : 'caret right'} size={19}
                 style={{color: colors.accent, marginRight: '1px', cursor: 'pointer'}}/>
        Bypass
      </Button>
    </PopupTrigger>;
  }

}
