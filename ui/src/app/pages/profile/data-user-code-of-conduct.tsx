import * as fp from 'lodash/fp';
import * as React from 'react';
import {validate} from 'validate.js';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {HtmlViewer} from 'app/components/html-viewer';
import {TextInput} from 'app/components/inputs';
import {withErrorModal, withSuccessModal} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, withUserProfile} from 'app/utils';
import {wasReferredFromRenewal} from 'app/utils/access-utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {getLiveDUCCVersion} from 'app/utils/code-of-conduct';
import {NavigationProps, withNavigation} from 'app/utils/navigation';
import {Profile} from 'generated/fetch';


const styles = reactStyles({
  dataUserCodeOfConductPage: {
    margin: 'auto',
    color: colors.primary,
    height: '100%',
    // The chrome is minimized on this page to increase scroll space. This has
    // the unwanted side-effect of removing the app padding. Add this same padding back.
    paddingLeft: '.6rem',
    paddingRight: '.6rem'
  },
  dataUserCodeOfConductFooter: {
    backgroundColor: colors.white,
    marginTop: 'auto',
    paddingTop: '1rem',
    paddingBottom: '1rem',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  smallTopMargin: {
    marginTop: '0.5rem'
  },
  largeTopMargin: {
    marginTop: '1.5rem'
  },
  bold: {
    fontWeight: 600
  },
  textInput: {
    padding: '0 1ex',
    width: '12rem',
    fontSize: 10,
    borderRadius: 6
  },
});

export enum DataUserCodeOfConductPage {
  CONTENT,
  SIGNATURE
}

const DuccTextInput = (props) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  return <TextInput {...fp.omit(['data-test-id'], props)}
                    style={{
                      ...styles.textInput,
                      ...props.style
                    }}/>;
};

const InitialsAgreement = (props) => {
  return <div style={{display: 'flex', marginTop: '0.5rem'}}>
    <DuccTextInput
        onChange={props.onChange}
        value={props.value}
        placeholder='INITIALS'
        data-test-id='ducc-initials-input'
        style={{width: '4ex', textAlign: 'center', padding: 0}}/>
    <div style={{marginLeft: '0.5rem'}}>{props.children}</div>
  </div>;
};

interface Props extends WithSpinnerOverlayProps, NavigationProps {
  profileState: {
    profile: Profile,
    reload: Function,
    updateCache: Function
  };
}

interface State {
  name: string;
  initialMonitoring: string;
  initialPublic: string;
  page: DataUserCodeOfConductPage;
  submitting: boolean;
  proceedDisabled: boolean;
}

export const DataUserCodeOfConduct = fp.flow(withUserProfile(), withNavigation)(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        name: '',
        initialMonitoring: '',
        initialPublic: '',
        page: DataUserCodeOfConductPage.CONTENT,
        submitting: false,
        proceedDisabled: true
      };
    }

    submitCodeOfConductWithRenewal = fp.flow(
      withSuccessModal({
        title: 'Your agreement has been updated',
        message: 'You will be redirected to the access renewal page upon closing this dialog.',
        onDismiss: () => this.props.navigate(['access-renewal'])
      }),
      withErrorModal({ title: 'Your agreement failed to update', message: 'Please try submitting the agreement again.' })
    )(async(initials) => {
      const duccVersion = getLiveDUCCVersion();
      const profile = await profileApi().submitDataUseAgreement(duccVersion, initials);
      this.props.profileState.updateCache(profile);
    });

    submitDataUserCodeOfConduct(initials) {
      const duccVersion = getLiveDUCCVersion();
      profileApi().submitDataUseAgreement(duccVersion, initials).then((profile) => {
        this.props.profileState.updateCache(profile);
        this.props.navigate(['/']);
      });
    }

    componentDidMount() {
      this.props.hideSpinner();
    }

    render() {
      const {profileState: {profile}} = this.props;
      const {proceedDisabled, initialMonitoring, initialPublic, page, submitting} = this.state;
      const errors = validate({initialMonitoring, initialPublic}, {
        initialMonitoring: {
          presence: {allowEmpty: false},
          length: {maxiumum: 6}
        },
        initialPublic: {
          presence: {allowEmpty: false},
          equality: {attribute: 'initialMonitoring'}
        }
      });
      return <FlexColumn style={styles.dataUserCodeOfConductPage}>
          {
            page === DataUserCodeOfConductPage.CONTENT && <React.Fragment>
              <HtmlViewer
                  ariaLabel='data user code of conduct agreement'
                  containerStyles={{margin: '2rem 0 1rem'}}
                  filePath={'assets/documents/data-user-code-of-conduct.html'}
                  onLastPage={() => this.setState({proceedDisabled: false})}
              />
              <FlexRow style={styles.dataUserCodeOfConductFooter}>
                Please read the above document in its entirety before proceeding to sign the Data User Code of Conduct.
                <Button
                    type={'link'}
                    style={{marginLeft: 'auto'}}
                    onClick={() => history.back()}
                >
                  Back
                </Button>
                <Button
                    data-test-id={'ducc-next-button'}
                    disabled={proceedDisabled}
                    onClick={() => this.setState({page: DataUserCodeOfConductPage.SIGNATURE})}
                >
                  Proceed
                </Button>
              </FlexRow>
            </React.Fragment>
          }
          {
            page === DataUserCodeOfConductPage.SIGNATURE && <React.Fragment>
              <FlexColumn>
                {submitting && <SpinnerOverlay/>}
                <h1>Accept Data User Code of Conduct</h1>
                <div style={{...styles.bold, ...styles.smallTopMargin}}>
                  I
                  <DuccTextInput style={{margin: '0 1ex'}}
                     disabled
                     value={profile.givenName + ' ' + profile.familyName}
                     data-test-id='ducc-name-input'/>
                   ("Authorized Data User") have personally reviewed this Data User Code of Conduct.
                   I agree to follow each of the policies and procedures it describes.
                </div>
                <div style={styles.smallTopMargin}>
                  By entering my initials next to each statement below, I acknowledge that:
                </div>
                <InitialsAgreement onChange={(v) => this.setState({initialMonitoring: v})}>
                  My work, including any external data, files, or software I upload into the
                   Researcher Workbench, will be logged and monitored by the <i>All of Us</i> Research
                   Program to ensure compliance with policies and procedures.
                </InitialsAgreement>
                <InitialsAgreement onChange={(v) => this.setState({initialPublic: v})}>
                  My name, affiliation, profile information and research description will be made
                   public. My research description will be used by the <i>All of Us</i> Research
                   Program to provide participants with meaningful information about the research
                   being conducted.
                </InitialsAgreement>
                <div style={{...styles.bold, ...styles.smallTopMargin}}>
                  I acknowledge that failure to comply with the requirements outlined in this Data
                   User Code of Conduct may result in termination of my <i>All of Us</i> Research
                   Program account and/or other sanctions, including, but not limited to:
                </div>
                <ul style={{...styles.bold, ...styles.smallTopMargin}}>
                  <li>
                    the posting of my name and affiliation on a publicly accessible list of
                     violators, and
                  </li>
                  <li>
                    notification of the National Institutes of Health or other federal agencies as
                     to my actions.
                  </li>
                </ul>
                <div style={{...styles.bold, ...styles.smallTopMargin}}>
                  I understand that failure to comply with these requirements may also carry
                   financial or legal repercussions. Any misuse of the <i>All of Us</i> Research
                   Hub, Researcher Workbench or data resources is taken very seriously, and other
                  sanctions may be sought.
                </div>
                <label style={{...styles.bold, ...styles.largeTopMargin}}>Authorized Data User Name</label>
                <DuccTextInput
                    disabled
                    data-test-id='ducc-username-input'
                    value={profile.givenName + ' ' + profile.familyName}
                />
                <label style={{...styles.bold, ...styles.largeTopMargin}}>User ID</label>
                <DuccTextInput
                    disabled
                    data-test-id='ducc-user-id-input'
                    value={profile.username}
                />
                <label style={{...styles.bold, ...styles.largeTopMargin}}>Date</label>
                <DuccTextInput type='text' disabled value={new Date().toLocaleDateString()}/>
              </FlexColumn>
              <FlexRow style={styles.dataUserCodeOfConductFooter}>
                <Button
                    type={'link'}
                    style={{marginLeft: 'auto'}}
                    onClick={() => this.setState({page: DataUserCodeOfConductPage.CONTENT})}
                >
                  Back
                </Button>
                <TooltipTrigger content={errors && <div>
                  <div>All fields must be initialed</div>
                  <div>All initials must match</div>
                  <div>Initials must be six letters or fewer</div>
                </div>}>
                  <Button
                      data-test-id={'submit-ducc-button'}
                      disabled={errors || submitting}
                      onClick={() => {
                        this.setState({submitting: true});
                        // This may record extra GA events if the user views & accepts the DUCC from their profile. If the additional events
                        // are an issue, we may need further changes, possibly disable the Accept button after initial submit.
                        AnalyticsTracker.Registration.AcceptDUCC();
                        wasReferredFromRenewal()
                          ? this.submitCodeOfConductWithRenewal(initialMonitoring)
                          : this.submitDataUserCodeOfConduct(initialMonitoring);
                        this.setState({submitting: false});
                      }}
                  >
                    Accept
                  </Button>
                </TooltipTrigger>
              </FlexRow>
            </React.Fragment>
          }
        </FlexColumn>;
    }
  });
