import {Component, Input} from '@angular/core';
import {Button, IconButton} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Toggle} from 'app/components/inputs';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase} from 'app/utils/index';
import {AccessModule, Profile} from 'generated/fetch';
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

export class AdminUserBypass extends React.Component<
    { profile: Profile},
    { initialModules: AccessModule[], selectedModules: AccessModule[],
      open: boolean} > {

  constructor(props) {
    super(props);
    const {profile} = props;
    const initialModules = [
      !!profile.betaAccessBypassTime ? AccessModule.BETAACCESS : null,
      !!profile.complianceTrainingBypassTime ? AccessModule.COMPLIANCETRAINING : null,
      !!profile.dataUseAgreementBypassTime ? AccessModule.DATAUSEAGREEMENT : null,
      !!profile.eraCommonsBypassTime ? AccessModule.ERACOMMONS : null,
      !!profile.twoFactorAuthBypassTime ? AccessModule.TWOFACTORAUTH : null,
    ];
    this.state = {
      open: false,
      initialModules: initialModules,
      selectedModules: initialModules
    };
  }

  cancel() {
    const {initialModules} = this.state;
    this.setState({selectedModules: initialModules});
  }

  save() {
    const {initialModules, selectedModules} = this.state;
    const {profile} = this.props;
    const changedModules = fp.xor(initialModules, selectedModules);
    changedModules.forEach(async module => {
      await profileApi()
        .bypassAccessRequirement(profile.userId,
          {isBypassed: selectedModules.includes(module), moduleName: module});
    });

    this.setState({initialModules: selectedModules});
  }

  hasEdited(): boolean {
    return this.state.selectedModules !== this.state.initialModules;
  }

  render() {
    const {selectedModules, open} = this.state;
    return <PopupTrigger
        side='bottom'
        onClose={() => {this.cancel(); this.setState({open: false}); }}
        onOpen={() => this.setState({open: true})}
        content={<div style={{padding: '1rem', display: 'flex', flexDirection: 'column'}}>
          <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
            <Toggle name='Beta Access'
                    enabled={selectedModules.includes(AccessModule.BETAACCESS)}
                    data-test-id='beta-access-toggle'
                    onToggle={() => {this.setState({selectedModules:
                      fp.xor(selectedModules, [AccessModule.BETAACCESS])}); } } />
            <TooltipTrigger content={'Grant beta access to a user.  This replaces verify/reject.'}>
              <ClrIcon shape='info' className='is-solid' style={styles.infoIcon}/>
            </TooltipTrigger>
          </div>
          <hr style={{width: '100%', marginBottom: '0.5rem'}}/>
          <Toggle name='Compliance Training'
                  enabled={selectedModules.includes(AccessModule.COMPLIANCETRAINING)}
                  data-test-id='compliance-training-toggle'
                  onToggle={() => {this.setState({selectedModules:
                      fp.xor(selectedModules, [AccessModule.COMPLIANCETRAINING])}); } }/>
          <Toggle name='eRA Commons Linking'
                  enabled={selectedModules.includes(AccessModule.ERACOMMONS)}
                  data-test-id='era-commons-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.ERACOMMONS])}); } } />
          <Toggle name='Two Factor Auth'
                  enabled={selectedModules.includes(AccessModule.TWOFACTORAUTH)}
                  data-test-id='two-factor-auth-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.TWOFACTORAUTH])}); }}/>
          <Toggle name='Data Use Agreement'
                  enabled={selectedModules.includes(AccessModule.DATAUSEAGREEMENT)}
                  data-test-id='data-use-agreement-toggle'
                  onToggle={() => {this.setState({selectedModules:
                    fp.xor(selectedModules, [AccessModule.DATAUSEAGREEMENT])}); }}/>
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
