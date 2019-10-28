import {Component} from '@angular/core';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withUserProfile} from 'app/utils';
import {Profile} from 'generated/fetch';
import * as React from 'react';
import {validate} from 'validate.js';
import {DataUseAgreementContent} from './data-use-agreement-content';
import {getDataUseAgreementWidget} from './data-use-agreement-panel';
import {dataUseAgreementStyles} from './data-use-agreement-styles';


const dataUseAgreementVersion = 2;

interface Props {
  profileState: {
    profile: Profile,
    reload: Function,
    updateCache: Function
  };
}

interface State {
  name: string;
  initialName: string;
  initialWork: string;
  initialSanctions: string;
  showSanctionModal: boolean;
  submitting: boolean;
}

export const DataUseAgreement = withUserProfile()(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        name: '',
        initialName: '',
        initialWork: '',
        initialSanctions: '',
        showSanctionModal: false,
        submitting: false
      };
    }

    submitDataUseAgreement(initials) {
      this.setState({submitting: true});
      profileApi().submitDataUseAgreement(dataUseAgreementVersion, initials).then((profile) => {
        this.props.profileState.updateCache(profile);
        window.history.back();
      });
    }

    updateSanction(event) {
      event.preventDefault();
      this.setState({showSanctionModal: true});
    }

    render() {
      const {initialName, initialWork, initialSanctions, showSanctionModal,
        submitting} = this.state;
      const errors = validate({initialName, initialWork, initialSanctions}, {
        initialName: {
          presence: {allowEmpty: false},
          length: {maximum: 6}
        },
        initialWork: {
          presence: {allowEmpty: false},
          equality: {attribute: 'initialName'},
          length: {maximum: 6}
        },
        initialSanctions: {
          presence: {allowEmpty: false},
          equality: {attribute: 'initialName'},
          length: {maximum: 6}
        }
      });
      return <div style={dataUseAgreementStyles.dataUseAgreementPage}>
        <DataUseAgreementContent/>
        <div style={{height: '1rem'}}/>
        {getDataUseAgreementWidget.call(this,
          submitting,
          name,
          initialWork,
          initialName,
          initialSanctions,
          showSanctionModal,
          errors,
          this.props.profileState.profile)
        }
      </div>; }
  });

@Component({
  template: '<div #root></div>'
})
export class DataUseAgreementComponent extends ReactWrapperBase {
  constructor() {
    super(DataUseAgreement, []);
  }
}
