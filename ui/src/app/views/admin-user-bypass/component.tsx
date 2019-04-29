import {Component, Input} from '@angular/core';
import {Button, IconButton} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Toggle} from 'app/components/inputs';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase} from 'app/utils/index';
import {Profile} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';


const styles = reactStyles({
  infoIcon: {
    color: '#2691D0',
    cursor: 'pointer',
    marginBottom: '0.5rem',
    height: '16px',
    width: '16px',
    alignSelf: 'center'
  }
});

export interface AccessModules {
  complianceTraining: boolean;
  betaAccess: boolean;
  eraCommons: boolean;
  twoFactorAuth: boolean;
  dataUseAgreement: boolean;
}

export class AdminUserBypass extends React.Component<
    { profile: Profile},
    { modules: AccessModules, editedModules: AccessModules,
      open: boolean} > {

  constructor(props) {
    super(props);
    this.state = {
      open: false,
      modules: {
        complianceTraining: true,
        betaAccess: true,
        eraCommons: true,
        twoFactorAuth: true,
        dataUseAgreement: true
      },
      editedModules: {
        complianceTraining: true,
        betaAccess: true,
        eraCommons: true,
        twoFactorAuth: true,
        dataUseAgreement: true
      }
    };
  }

  componentDidMount() {
    const {profile} = this.props;
    const currModules = {
      complianceTraining: !!profile.complianceTrainingBypassTime,
      betaAccess: !!profile.betaAccessBypassTime,
      eraCommons: !!profile.eraCommonsBypassTime,
      twoFactorAuth: !!profile.twoFactorAuthBypassTime,
      dataUseAgreement: !! profile.dataUseAgreementBypassTime
    };
    this.setState({modules: currModules, editedModules: currModules});
  }

  cancel() {
    const {modules} = this.state;
    this.setState({editedModules: modules});
  }

  save() {
    const {modules, editedModules} = this.state;
    const {profile} = this.props;
    Object.keys(editedModules).forEach(async m => {
      if (editedModules[m] !== modules[m]) {
        await profileApi()
          .bypassAccessRequirement(profile.userId, m.toString(), {isBypassed: editedModules[m]});
      }
    });

    this.setState({modules: editedModules});
  }

  hasEdited(): boolean {
    return this.state.editedModules !== this.state.modules;
  }

  render() {
    const {editedModules, open} = this.state;
    return <PopupTrigger
        side='bottom'
        onClose={() => {this.cancel(); this.setState({open: false}); }}
        onOpen={() => this.setState({open: true})}
        content={<div style={{padding: '1rem', display: 'flex', flexDirection: 'column'}}>
          <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
            <Toggle name='Beta Access'
                    enabled={editedModules.betaAccess}
                    data-test-id='beta-access-toggle'
                    onToggle={() => {this.setState({editedModules:
                          fp.set('betaAccess', !editedModules.betaAccess, editedModules)}); } } />
            <TooltipTrigger content={'Grant beta access to a user.  This replaces verify/reject.'}>
              <ClrIcon shape='info' className='is-solid' style={styles.infoIcon}/>
            </TooltipTrigger>
          </div>
          <hr style={{width: '100%', marginBottom: '0.5rem'}}/>
          <Toggle name='Compliance Training'
                  enabled={editedModules.complianceTraining}
                  data-test-id='compliance-training-toggle'
                  onToggle={() => {this.setState({editedModules:
                      fp.set('complianceTraining',
                        !editedModules.complianceTraining, editedModules)}); } }/>
          <Toggle name='eRA Commons Linking'
                  enabled={editedModules.eraCommons}
                  data-test-id='era-commons-toggle'
                  onToggle={() => {this.setState({editedModules:
                      fp.set('eraCommons', !editedModules.eraCommons, editedModules)}); } } />
          <Toggle name='Two Factor Auth'
                  enabled={editedModules.twoFactorAuth}
                  data-test-id='two-factor-auth-toggle'
                  onToggle={() => {this.setState({editedModules:
                      fp.set('twoFactorAuth', !editedModules.twoFactorAuth, editedModules)}); }}/>
          <Toggle name='Data Use Agreement'
                  enabled={editedModules.dataUseAgreement}
                  data-test-id='data-use-agreement-toggle'
                  onToggle={() => {this.setState({editedModules:
                      fp.set('dataUseAgreement', !editedModules.dataUseAgreement,
                        editedModules)}); }}/>
          <div style={{display: 'flex', justifyContent: 'flex-end'}}>
            <IconButton icon='times'
                        onClick={() => this.cancel()}
                        disabled={!this.hasEdited()}/>
            <IconButton icon='check'
                        data-test-id='toggle-save'
                        onClick={() => this.save()}
                        disabled={!this.hasEdited()}/>
          </div>
        </div>}>
      <Button type='secondaryLight' data-test-id='bypass-popup'>
        <ClrIcon shape={open ? 'caret down' : 'caret right'} size={19}
                 style={{color: '#0077b7', marginRight: '1px', cursor: 'pointer'}}/>
        Bypass
      </Button>
    </PopupTrigger>;
  }

}


@Component({
  selector: 'app-admin-user-bypass',
  template: '<div #root></div>',
})
export class AdminUserBypassComponent extends ReactWrapperBase {
  @Input('profile') profile: Profile;

  constructor() {
    super(AdminUserBypass, ['profile']);
  }
}
