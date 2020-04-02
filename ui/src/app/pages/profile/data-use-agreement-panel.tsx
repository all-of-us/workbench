import {Button} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {AouTitle} from 'app/components/text-wrappers';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as React from 'react';
import {
  DuaTextInput,
  IndentedListItem,
  IndentedUnorderedList, InitialsAgreement,
  SecondHeader
} from './data-user-code-of-conduct-styles';

export function getDataUseAgreementWidgetV1(submitting, initialWork, initialName,
  initialSanctions, errors, profile) {
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
    <InitialsAgreement onChange={(v) => this.setState({initialWorkV1: v})} value={initialWork}>
      My work will be logged, monitored, and audited by the <i>All of Us</i> Research Program to ensure
      compliance with policies and procedures, as well as the demonstration project charges.
    </InitialsAgreement>
    <InitialsAgreement onChange={(v) => this.setState({initialNameV1: v})} value={initialName}>
      My name, affiliation, profile information, and research description will be made public.
      My research description will be used by the <AouTitle/> to provide
      participants with meaningful information about the research being conducted.
    </InitialsAgreement>
    <InitialsAgreement onChange={(v) => this.setState({initialSanctionsV1: v})}
                       value={initialSanctions}>
      Access granted by the program in accordance with this agreement is exclusively
      for participation in <i>All of Us</i> demonstration projects. At the conclusion of my
      participation in these activities, I understand that my access to the <i>All of Us</i> data
      resources will be revoked and/or subject to customary access policies and procedures.
    </InitialsAgreement>
    <div style={{marginTop: '0.5rem'}}><strong>I acknowledge that failure to comply with the terms of this agreement
      may result in termination of my <AouTitle/> account and/or other sanctions, including
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
    <DuaTextInput disabled data-test-id='dua-username-input' value={this.props.profileState.profile.username}/>
    <label>Contact Email</label>
    <DuaTextInput disabled data-test-id='dua-contact-email-input' value={this.props.profileState.profile.contactEmail}/>
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
        onClick={() => this.submitDataUserCodeOfConduct(this.state.initialWorkV1)}>Submit</Button>
    </TooltipTrigger>
  </FlexColumn>;
}
