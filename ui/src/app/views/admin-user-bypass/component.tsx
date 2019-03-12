import {Component} from '@angular/core';
import {Button, IconButton} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Toggle} from 'app/components/inputs';
import {PopupTrigger} from 'app/components/popups';
import {ReactWrapperBase, withUserProfile} from 'app/utils/index';
import {Profile} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';


export interface AccessModules {
  complianceTraining: boolean;
  betaAccess: boolean;
  eraCommons: boolean;
}

export const AdminUserBypass = withUserProfile()
(class extends React.Component<
    { profileState: { profile: Profile, reload: Function } },
    { modules: AccessModules, editedModules: AccessModules,
      loading: boolean} > {

  constructor(props) {
    super(props);
    this.state = {
      loading: true,
      modules: {
        complianceTraining: true,
        betaAccess: true,
        eraCommons: true
      },
      editedModules: {
        complianceTraining: true,
        betaAccess: true,
        eraCommons: true
      }
    };
  }

  componentDidMount() {

  }

  componentWillUnmount() {

    console.log('unmounting');
    this.cancel();
  }

  save() {
    const {editedModules} = this.state;
    this.setState({modules: editedModules});
  }

  cancel() {
    const {modules} = this.state;
    this.setState({editedModules: modules});
  }

  hasEdited(): boolean {
    return this.state.editedModules !== this.state.modules;
  }

  render() {
    const {editedModules} = this.state;
    return <PopupTrigger
        side='bottom'
        content={<div style={{padding: '1rem', display: 'flex', flexDirection: 'column'}}>
          <Toggle name='Beta Access'
                  enabled={editedModules.betaAccess}
                  onToggle={() => {this.setState({editedModules:
                          fp.set('betaAccess', !editedModules.betaAccess, editedModules)}); } } />
          <Toggle name='Compliance Training'
                  enabled={editedModules.complianceTraining}
                  onToggle={() => {this.setState({editedModules:
                      fp.set('complianceTraining',
                        !editedModules.complianceTraining, editedModules)}); } }/>
          <Toggle name='eRA Commons Linking'
                  enabled={editedModules.eraCommons}
                  onToggle={() => {this.setState({editedModules:
                      fp.set('eraCommons', !editedModules.eraCommons, editedModules)}); } } />
          <div style={{display: 'flex', justifyContent: 'flex-end'}}>

            <IconButton icon='times'
                        onClick={() => this.cancel()}
                        disabled={!this.hasEdited()}/>
            <IconButton icon='check'
                        onClick={() => this.save()}
                        disabled={!this.hasEdited()}/>
          </div>
        </div>}>
      <Button type='secondaryLight'>
        <ClrIcon shape='caret right' size={19}
                 style={{color: '#0077b7', marginRight: '1px', cursor: 'pointer'}}/>
        Bypass
      </Button>
    </PopupTrigger>;
  }

});


@Component({
  selector: 'app-admin-user-bypass',
  template: '<div #root></div>',
})
export class AdminUserBypassComponent extends ReactWrapperBase {

  constructor() {
    super(AdminUserBypass, []);
  }
}
