import {Button} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {TextInput} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {
  AoUTitle,
  IndentedListItem,
  IndentedUnorderedList,
  SecondHeader
} from './data-use-agreement-styles';

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

export function getDataUseAgreementWidget(submitting, initialWork,
  initialName, initialSanctions, errors, profile) {
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
                    data-test-id='dua-name-input'/>
      ("Authorized Demonstration User") have
      personally reviewed this data use agreement. I agree to follow each of the policies
      and procedures it describes.
    </div>
    <div>By entering my initials next to each statement below, I agree to these terms:</div>
    <InitialsAgreement onChange={(v) => this.setState({initialWork: v})} value={initialWork}>
      My work will be logged, monitored, and audited by the <i>All of Us</i> Research Program to ensure
      compliance with policies and procedures, as well as the demonstration project charges.
    </InitialsAgreement>
    <InitialsAgreement onChange={(v) => this.setState({initialName: v})} value={initialName}>
      My name, affiliation, profile information, and research description will be made public.
      My research description will be used by the <AoUTitle/> to provide
      participants with meaningful information about the research being conducted.
    </InitialsAgreement>
    <InitialsAgreement onChange={(v) => this.setState({initialSanctions: v})}
                       value={initialSanctions}>
      Access granted by the program in accordance with this agreement is exclusively
      for participation in <i>All of Us</i> demonstration projects. At the conclusion of my
      participation in these activities, I understand that my access to the <i>All of Us</i> data
      resources will be revoked and/or subject to customary access policies and procedures.
    </InitialsAgreement>
    <div style={{marginTop: '0.5rem'}}><strong>I acknowledge that failure to comply with the terms of this agreement
      may result in termination of my <AoUTitle/> account and/or other sanctions, including
      but not limited to:</strong>
      <IndentedUnorderedList>
        <IndentedListItem>
          <strong>posting my name and affiliation on a publicly-accessible list of violators, and</strong>
        </IndentedListItem>
        <IndentedListItem><strong>notifying the National Institutes of Health or other federal agencies of my
          actions.</strong></IndentedListItem>
      </IndentedUnorderedList>
      <div><strong>I understand that failure to comply with these terms may also carry additional financial, legal, or other
        repercussions.</strong>
      </div>
    </div>
    <label>Authorized Demonstration User Name</label>
    <DuaTextInput disabled value={this.props.profileState.profile.username}/>
    <label>Contact Email</label>
    <DuaTextInput disabled value={this.props.profileState.profile.contactEmail}/>
    <label>Date</label>
    <DuaTextInput type='text' disabled value={new Date().toLocaleDateString()}/>
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
