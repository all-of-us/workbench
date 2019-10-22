import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {styles as headerStyles} from 'app/components/headers';
import {TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {Profile} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {validate} from 'validate.js';
import {DataUseAgreementText} from './data-use-agreement-text';

const styles = reactStyles({
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
  return <h2 style={{...styles.h2, ...props.style}}>{props.children}</h2>;
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

const DuaTextInput = (props) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  return <TextInput {...fp.omit(['data-test-id'], props)}
                    style={{
                      padding: '0 1ex',
                      width: '12rem',
                      fontSize: 10,
                      borderRadius: 6,
                      ...props.style
                    }}/>;
};

const InitialsAgreement = (props) => {
  return <div style={{display: 'flex', marginTop: '0.5rem'}}>
    <DuaTextInput onChange={props.onChange} value={props.value}
                  placeholder='INITIALS' data-test-id='dua-initials-input'
                  style={{width: '4ex', textAlign: 'center', padding: 0}}/>
    <div style={{marginLeft: '0.5rem'}}>{props.children}</div>
  </div>;
};

const SanctionModal = (props) => {
  return <Modal width = {750}>
    <ModalTitle style={styles.sanctionModalTitle}>
      All of Us Research Program - Sanctions on Violations of the Code of Conduct
    </ModalTitle>
    <ModalBody>
      <label style={styles.modalLabel}>
        The Resource Access Board (RAB) of the <i>All of Us</i> Research Program determines whether
        an investigator has violated the Code of Conduct outlined in the Data Use Agreement
        signed by each user. The RAB notifies the All of Us Research Program office of the
        violation.
        <IndentedUnorderedList>
          <li>
            <label>The <i>All of Us</i> Research Program office and/or the All of Us IRB may
            implement the following sanctions if it is determined that an investigator has violated
            the Code of Conduct:
            </label>
            <IndentedUnorderedList>
              <IndentedListItem>
                Determine whether any action by the investigator is required to remedy the
                violation.
              </IndentedListItem>
              <IndentedListItem>
                Revoke and/or deny access of the violator to all non-public (Registered and
                Controlled tier) <i>All of Us</i> data.
              </IndentedListItem>
              <IndentedListItem>
                Post the name and affiliation of the violator on a public <i>All of Us</i>
                Research Program webpage.
              </IndentedListItem>
              <IndentedListItem>
                Revoke extant NIH funding and/or prohibit future funding, either permanently or for
                an explicit period of time
              </IndentedListItem>
              <IndentedListItem>
                Prosecute the violator for breach of a contract with the federal government
              </IndentedListItem>
            </IndentedUnorderedList>
          </li>
        </IndentedUnorderedList>
      </label>
    </ModalBody>
    <ModalFooter>
      <Button type='primary' onClick={props.onClose}>Close</Button>
    </ModalFooter>
  </Modal>;
};

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
      return <div style={styles.dataUseAgreementPage}>
        <DataUseAgreementText/>
        <div style={{height: '1rem'}}/>
        <FlexColumn style={{borderRadius: '1rem',
          backgroundColor: colorWithWhiteness(colors.primary, 0.8),
          padding: '1rem', alignItems: 'flex-start', position: 'relative'
        }}>
          {submitting && <SpinnerOverlay/>}
          <SecondHeader style={{marginTop: 0}}>Agreement:</SecondHeader>
          <div style={{marginTop: '0.5rem', fontWeight: 600}}>I
            <DuaTextInput style={{margin: '0 1ex'}}
                          disabled
                          value={this.props.profileState.profile.givenName + ' ' + this.props.profileState.profile.familyName}
                          data-test-id='dua-name-input'/>
            ("Authorized User") have
            personally reviewed this data use agreement. I agree to follow each of the policies
            and procedures it describes.
          </div>
          <div>By entering my initials next to each statement below, I agree to these terms:</div>
          <InitialsAgreement onChange={(v) => this.setState({initialWork: v})} value={initialWork}>
            My work may be logged and monitored by the <i>All of Us</i> Research Program to ensure
            compliance with policies and procedures.
          </InitialsAgreement>
          <InitialsAgreement onChange={(v) => this.setState({initialName: v})} value={initialName}>
            My name, affiliation, and research description will be made public. My research
            description will be used by the <i>All of Us</i> Research Program to provide
            participants with meaningful information about the research being conducted.
          </InitialsAgreement>
          <InitialsAgreement onChange={(v) => this.setState({initialSanctions: v})}
                             value={initialSanctions}>
            I have read and understand the <i>All of Us</i> Research Program Sanctions for Privacy
            and Information Security Violations Policy.
          </InitialsAgreement>
          <div style={{marginTop: '0.5rem', fontWeight: 600}}>
            I acknowledge that failure to comply with the terms of this agreement may result in
            termination of my <i>All of Us</i> Research Program account and/or other sanctions per
            the <a href='#' onClick={(e) => {this.updateSanction(e); }}>
            <i>All of Us</i> Research Program policy on Sanctions for Violation of Code of
            Conduct</a>.
          </div>
          <div>
            {showSanctionModal &&
          <SanctionModal onClose={() => this.setState({showSanctionModal: false})}></SanctionModal>}
          </div>
          <DuaTextInput style={{marginTop: '0.5rem'}}
                        disabled value={this.props.profileState.profile.username}/>
          <DuaTextInput style={{marginTop: '0.5rem'}}
                        disabled value={this.props.profileState.profile.contactEmail}/>
          <DuaTextInput style={{marginTop: '0.5rem'}}
                        type='text' disabled value={new Date().toLocaleDateString()}/>
          <TooltipTrigger content={errors && <div>
            <div>All fields must be initialed</div>
            <div>All initials must match</div>
            <div>Initials must be six letters or fewer</div>
          </div>}>
            <Button
              style={{marginTop: '1rem', cursor: errors && 'not-allowed', padding: '0 1.3rem'}}
              disabled={errors || submitting} data-test-id='submit-dua-button'
              onClick={() => this.submitDataUseAgreement(this.state.initialWork)}>Submit</Button>
          </TooltipTrigger>
        </FlexColumn>
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
