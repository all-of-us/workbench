import {Button} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {
  dataUseAgreementStyles,
  IndentedListItem,
  IndentedUnorderedList,
  SecondHeader
} from './data-use-agreement-styles';


export const SanctionModal = (props) => {
  return <Modal width={750}>
    <ModalTitle style={dataUseAgreementStyles.sanctionModalTitle}>
      All of Us Research Program - Sanctions on Violations of the Code of Conduct
    </ModalTitle>
    <ModalBody>
      <label style={dataUseAgreementStyles.modalLabel}>
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

export function getDataUseAgreementWidget(submitting, name, initialWork,
  initialName, initialSanctions, showSanctionModal, errors, profile) {
  return <FlexColumn style={{
    borderRadius: '1rem',
    backgroundColor: colorWithWhiteness(colors.primary, 0.8),
    padding: '1rem', alignItems: 'flex-start', position: 'relative'
  }}>
    {submitting && <SpinnerOverlay/>}
    <SecondHeader style={{marginTop: 0}}>Agreement:</SecondHeader>
    <div style={{marginTop: '0.5rem', fontWeight: 600}}>I
      <DuaTextInput style={{margin: '0 1ex'}}
                    disabled
                    value={profile.givenName + ' ' + profile.familyName}
                    onChange={(v) => this.setState({name: v})} value={name}
                    data-test-id='dua-name-input'/>
      ("Authorized User") have
      personally reviewed this data use agreement. I agree to follow each of the policies
      and procedures it describes.
    </div>
    <div>By entering my initials next to each statement below, I agree to these terms:</div>
    <InitialsAgreement onChange={(v) => this.setState({initialWork: v})} value={initialWork}>
      My work may be logged, monitored, and audited by the <i>All of Us</i> Research Program to ensure
      compliance with policies and procedures.
    </InitialsAgreement>
    <InitialsAgreement onChange={(v) => this.setState({initialName: v})} value={initialName}>
      My name, affiliation, and research description will be made public. My research
      description will be used by the <i>All of Us</i> Research Program to provide
      participants with meaningful information about the research being conducted.
    </InitialsAgreement>
    <InitialsAgreement onChange={(v) => this.setState({initialSanctions: v})}
                       value={initialSanctions}>
      Access granted by the program in accordance with this agreement is exclusively
      for participation in All of Us demonstration projects. At the conclusion of my
      participation in these activities, I understand that my access to the <i>All of Us</i>
      data resources will be revoked and/or subject to customary access policies and procedures.
    </InitialsAgreement>
    <div><strong>I acknowledge that failure to comply with the terms of this agreement may result
      in termination of my All of Us Research Program account and/or other sanctions, including
      but not limited to:</strong>
      <IndentedUnorderedList>
        <IndentedListItem>
          posting my name and affiliation on a publicly-accessible list of violators, and
        </IndentedListItem>
        <IndentedListItem>notifying the National Institutes of Health or other federal agencies of my
          actions.</IndentedListItem>
      </IndentedUnorderedList>
      <div>I understand that failure to comply with these terms may also carry additional financial, legal, or other
        repercussions.
      </div>
    </div>
    <div>
      {showSanctionModal &&
      <SanctionModal onClose={() => this.setState({showSanctionModal: false})}/>}
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
  </FlexColumn>;
}
