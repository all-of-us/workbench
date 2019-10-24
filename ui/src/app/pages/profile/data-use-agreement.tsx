import {Component} from '@angular/core';
import {styles as headerStyles} from 'app/components/headers';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {Profile} from 'generated/fetch';
import * as React from 'react';
import {validate} from 'validate.js';
import {DataUseAgreementContent} from './data-use-agreement-content';
import {getDataUseAgreementWidget} from './data-use-agreement-panel';

export const dataUseAgreementStyles = reactStyles({
  dataUseAgreementPage: {
    paddingTop: '2rem',
    paddingLeft: '3rem',
    paddingBottom: '2rem',
    maxWidth: '50rem',
    height: '100%',
    color: colors.primary,
  },
  h2: {...headerStyles.h2, lineHeight: '24px', fontWeight: 600, fontSize: '16px'},
  sanctionModalTitle: {
    fontFamily: 'Montserrat',
    fontSize: 16,
    fontWeight: 600,
    lineHeight: '19px'
  },
  modalLabel: {
    fontFamily: 'Montserrat',
    fontSize: 14,
    lineHeight: '24px',
    color: colors.primary
  }
});

export const SecondHeader = (props) => {
  return <h2 style={{...dataUseAgreementStyles.h2, ...props.style}}>{props.children}</h2>;
};

export const indentedListStyles = {
  margin: '0.5rem 0 0.5rem 1.5rem', listStylePosition: 'outside'
};

export const IndentedUnorderedList = (props) => {
  return <ul style={{...indentedListStyles, ...props.style}}>{props.children}</ul>;
};

export const IndentedOrderedList = (props) => {
  return <ol style={{...indentedListStyles, ...props.style}}>{props.children}</ol>;
};

export const IndentedListItem = (props) => {
  return <li style={{marginTop: '0.5rem', ...props.style}}>{props.children}</li>;
};

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
