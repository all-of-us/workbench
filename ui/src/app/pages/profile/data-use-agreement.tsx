import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {
  BolderHeader,
  styles as headerStyles
} from 'app/components/headers';
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

const SecondHeader = (props) => {
  return <h2 style={{...styles.h2, ...props.style}}>{props.children}</h2>;
};

const indentedListStyles = {
  margin: '0.5rem 0 0.5rem 1.5rem', listStylePosition: 'outside'
};

const IndentedUl = (props) => {
  return <ul style={{...indentedListStyles, ...props.style}}>{props.children}</ul>;
};

const IndentedOl = (props) => {
  return <ol style={{...indentedListStyles, ...props.style}}>{props.children}</ol>;
};

const IndentedLi = (props) => {
  return <li style={{marginTop: '0.5rem', ...props.style}}>{props.children}</li>;
};

const coreValuesUrl = 'https://allofus.nih.gov/about/about-all-us-research-program';

const dataUseAgreementVersion = 1;

{/* NOTE: Make sure to update dataUseAgreementVersion if there is any change to the DUA text. */}
const DataUseAgreementText = () => {
  return <div>
    <BolderHeader>All of Us Research Program - Data Use Agreement</BolderHeader>
    <div>This data use agreement describes how <i>All of Us </i>
      Research Program data can and cannot be used.
    </div>
    <div>This is an agreement between the United States National Institutes of Health and authorized
      users of data from the <i>All of Us</i> Research Program.
    </div>
    <div>An authorized user is a person who is authorized to access and/or work with registered or
      controlled tier data from the <i>All of Us</i> Research Program.
    </div>
    <div>Before they access and/or work with <i>All of Us </i>
      Research Program data, authorized users must:
      <IndentedOl>
        <li>complete the <i>All of Us</i> Research Program research ethics training</li>
        <li>read and sign this data use agreement</li>
      </IndentedOl>
    </div>
    <div>Please read this agreement carefully and completely before signing.</div>
    <SecondHeader>As “Authorized User” of the <i>All of Us</i> Research Program data, I
      will:</SecondHeader>
    <IndentedUl>
      <li>read and adhere to the <i>All of Us</i> Research
        Program <a target='_blank' href={coreValuesUrl}>core values</a>.
      </li>
      <li>know and follow all applicable US federal, state, and local
        (applicable in the area where I am conducting research) laws
        regarding research involving human data and data privacy.
      </li>
      <li>respect the privacy of research participants at all times.
        <IndentedUl>
          <IndentedLi style={{margin: '0.5rem 0'}}>
            I <strong>will <strong>NOT</strong> use</strong> or disclose any
            information that directly identifies one or more participants.
            <br/>
            If I become aware of any information that directly identifies one or more
            participants, I will notify the <i>All of Us</i> Research Program immediately
            using the automatic notification system.
          </IndentedLi>
          <IndentedLi style={{margin: '0.5rem 0'}}>
            I <strong>will <strong>NOT</strong> attempt</strong> to re-identify research
            participants or their relatives.
            <br/>
            If I unintentionally re-identify participants through the process of my work, I will
            contact the <i>All of Us</i> Research Program immediately using the automatic
            notification system.
          </IndentedLi>
          <IndentedLi style={{margin: '0.5rem 0'}}>
            If I become aware of any uses or disclosures of <i>All of Us</i> Research Program data
            that could endanger the security or privacy of research participants, I will contact
            the <i>All of Us</i> Research Program immediately using the automatic
            notification system.
          </IndentedLi>
        </IndentedUl>
      </li>
      <li>provide a meaningful and accurate description of my research purpose every time I create
        an <i>All of Us</i> Research Program workspace.
        <IndentedUl>
          <li>
            Within each workspace, I will use the data for the research purpose I have provided.
          </li>
          <li>If I have a new research purpose, I will create a new workspace and provide a new
            research purpose description.
          </li>
        </IndentedUl>
      </li>
      <li>use a version of the <i>All of Us</i> Research Program database that is current at or
        after the time my analysis begins
        <IndentedUl>
          <li>Archive versions of the database are maintained for the sole purpose of completion of
            existing studies or replication of previous studies. New work may not be initiated on
            archive versions of the database.
          </li>
        </IndentedUl>
      </li>
      <li>honor the contribution of those who take part in <i>All of Us</i> to my work
        <IndentedUl>
          <li>I will acknowledge the <i>All of Us</i> Research Program and its research participants
            in all oral and written presentations, disclosures, and publications resulting from
            any analyses of the data.
            <IndentedUl>
              <li>
                Here is an example acknowledgement statement: “The results reported here are based
                on data contributed by participants to the <i>All of Us</i> Research Program,
                managed by the National Institutes of Health. Information about
                the <i>All of Us</i> Research Program can be found
                at <a href='https://allofus.nih.gov'>https://allofus.nih.gov</a>.”
              </li>
            </IndentedUl>
          </li>

          <li>I will deposit an electronic version of final, peer-reviewed manuscript on National
            Library of Medicine's PubMed Central no later than 3 months after the official date of
            publication.
          </li>
        </IndentedUl>
      </li>
    </IndentedUl>
    <SecondHeader>As “Authorized User” of the <i>All of Us</i> Research Program data, I
      will:</SecondHeader>
    <IndentedUl>
      <li><strong>NOT</strong> share my login information with anyone.
        <IndentedUl>
          <li>I will not share my login information with another authorized user of
            the <i>All of Us</i> Research Program.
          </li>
          <li>I will not create any group or shared accounts.</li>
        </IndentedUl>
      </li>
      <li>
        <strong>NOT</strong> use <i>All of Us</i> Research Program data for research that is
        discriminatory or stigmatizing of individuals, families, or communities. Please review
        the <i>All of Us</i> policy on stigmatizing research here.
        <IndentedUl>
          <li>I will contact the <i>All of Us</i> Research Program Resource Access Board (RAB) for
            further guidance on this point as needed.
          </li>
        </IndentedUl>
      </li>
      <li><strong>NOT</strong> attempt to contact <i>All of Us</i> Research Program participants.
      </li>
      <li><strong>NOT</strong> make copies of or download individual-level data outside of
        the <i>All of Us</i> Research Program environment without approval from RAB.
        <IndentedUl>
          <li>I will not take screenshots or attempt any other way of
            copying individual-level data.
          </li>
        </IndentedUl>
      </li>

      <li><strong>NOT</strong> redistribute or publish registered access or controlled tier <i>All
        of Us</i> Research Program data including aggregate statistics that are more granular
        than buckets of 20 without approval from the RAB.
      </li>
      <li><strong>NOT</strong> attempt to link registered or controlled tier <i>All of
        Us</i> Research Program data at the individual level with data from other
        sources without explicit permission from RAB.
      </li>
      <li><strong>NOT</strong> use <i>All of Us</i> Research Program data for marketing purposes.
        <IndentedUl>
          <li>“Marketing” means a communication about a product or service that encourages
            recipients of the communication to purchase or use the product or service
            (US 45 CFR §164.501).
          </li>
        </IndentedUl>
      </li>
      <li><strong>NOT</strong> represent that the <i>All of Us</i> Research Program endorses or
        approves of my research unless such endorsement is expressly provided by the RAB.
      </li>
    </IndentedUl>

    <SecondHeader>Data Disclaimer:</SecondHeader>
    <div>The <i>All of Us</i> Research Program does not guarantee the accuracy of the data in
      the <i>All of Us</i> Research Program database. The <i>All of Us</i> Research Program
      does not guarantee the performance of the software in the <i>All of Us</i> Research
      Program database. The <i>All of Us</i> Research Program does not warrant or endorse
      the research results obtained by using the <i>All of Us</i> database.
    </div>
  </div>;
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
        <IndentedUl>
          <li>
            <label>The <i>All of Us</i> Research Program office and/or the All of Us IRB may
            implement the following sanctions if it is determined that an investigator has violated
            the Code of Conduct:
            </label>
            <IndentedUl>
              <IndentedLi>
                Determine whether any action by the investigator is required to remedy the
                violation.
              </IndentedLi>
              <IndentedLi>
                Revoke and/or deny access of the violator to all non-public (Registered and
                Controlled tier) <i>All of Us</i> data.
              </IndentedLi>
              <IndentedLi>
                Post the name and affiliation of the violator on a public <i>All of Us</i>
                Research Program webpage.
              </IndentedLi>
              <IndentedLi>
                Revoke extant NIH funding and/or prohibit future funding, either permanently or for
                an explicit period of time
              </IndentedLi>
              <IndentedLi>
                Prosecute the violator for breach of a contract with the federal government
              </IndentedLi>
            </IndentedUl>
          </li>
        </IndentedUl>
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
          presence: {allowEmpty: false}
        },
        initialWork: {
          presence: {allowEmpty: false},
          equality: {
            attribute: 'initialName'
          }
        },
        initialSanctions: {
          presence: {allowEmpty: false},
          equality: {
            attribute: 'initialName'
          }
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
          <TooltipTrigger content={errors && 'All fields must be initialed and all sets of initials must match'}>
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
