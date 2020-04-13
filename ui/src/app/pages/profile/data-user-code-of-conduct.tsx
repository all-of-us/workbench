import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {PdfViewer} from 'app/components/pdf-viewer';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {DataUseAgreementContentV2} from 'app/pages/profile/data-use-agreement-content-v2';
import {getDataUseAgreementWidgetV2} from 'app/pages/profile/data-use-agreement-panel';
import {
  dataUserCodeOfConductStyles,
  DuaTextInput,
  InitialsAgreement
} from 'app/pages/profile/data-user-code-of-conduct-styles';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {serverConfigStore} from 'app/utils/navigation';
import {Profile} from 'generated/fetch';
import * as React from 'react';
import {validate} from 'validate.js';

const styles = reactStyles({
  dataUserCodeOfConductPage: {
    margin: 'auto',
    color: colors.primary,
    height: '100%',
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
  }
});

export enum DataUserCodeOfConductPage {
  CONTENT,
  SIGNATURE
}

interface Props {
  profileState: {
    profile: Profile,
    reload: Function,
    updateCache: Function
  };
}

interface State {
  name: string;
  initialNameV2: string;
  initialWorkV2: string;
  initialSanctionsV2: string;
  initialMonitoring: string;
  initialPublic: string;
  page: DataUserCodeOfConductPage;
  submitting: boolean;
}

export const DataUserCodeOfConduct = withUserProfile()(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        name: '',
        initialNameV2: '',
        initialWorkV2: '',
        initialSanctionsV2: '',
        initialMonitoring: '',
        initialPublic: '',
        page: DataUserCodeOfConductPage.CONTENT,
        submitting: false
      };
    }

    submitDataUserCodeOfConduct(initials) {
      this.setState({submitting: true});
      const dataUseAgreementVersion = serverConfigStore.getValue().enableV3DataUserCodeOfConduct ? 3 : 2;
      profileApi().submitDataUseAgreement(dataUseAgreementVersion, initials).then((profile) => {
        this.props.profileState.updateCache(profile);
        window.history.back();
      });
    }

    render() {
      const {profileState: {profile}} = this.props;
      const {initialNameV2, initialWorkV2, initialSanctionsV2, initialMonitoring, initialPublic, page, submitting} = this.state;
      const errorsV2 = validate({initialNameV2, initialWorkV2, initialSanctionsV2}, {
        initialNameV2: {
          presence: {allowEmpty: false},
          length: {maximum: 6}
        },
        initialWorkV2: {
          presence: {allowEmpty: false},
          equality: {attribute: 'initialNameV2'},
          length: {maximum: 6}
        },
        initialSanctionsV2: {
          presence: {allowEmpty: false},
          equality: {attribute: 'initialNameV2'},
          length: {maximum: 6}
        }
      });
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
      if (serverConfigStore.getValue().enableV3DataUserCodeOfConduct) {
        return <FlexColumn style={styles.dataUserCodeOfConductPage}>
          {
            page === DataUserCodeOfConductPage.CONTENT && <React.Fragment>
              <div style={{marginTop: '2rem', marginBottom: '1rem'}}>
                <PdfViewer
                    pdfPath={'assets/documents/data-user-code-of-conduct-v3.pdf'}
                />
              </div>
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
                    onClick={() => this.setState({page: DataUserCodeOfConductPage.SIGNATURE})}
                >
                  Accept
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
                  <DuaTextInput style={{margin: '0 1ex'}}
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
                   Researcher Workbench, may be logged and monitored by the
                   <i>All of Us</i> Research Program to ensure compliance with policies and
                   procedures.
                </InitialsAgreement>
                <InitialsAgreement onChange={(v) => this.setState({initialPublic: v})}>
                  My name, affiliation, profile information, and research description will be made
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
                   Hub, Researcher Workbench, or the <i>All of Us</i> Research Program data is
                   taken very seriously and other sanctions may be sought.
                </div>
                <label style={{...styles.bold, ...styles.largeTopMargin}}>Authorized Data User Name</label>
                <DuaTextInput
                    disabled
                    data-test-id='ducc-username-input'
                    value={profile.givenName + ' ' + profile.familyName}
                />
                <label style={{...styles.bold, ...styles.largeTopMargin}}>User ID</label>
                <DuaTextInput
                    disabled
                    data-test-id='ducc-user-id-input'
                    value={profile.username}
                />
                <label style={{...styles.bold, ...styles.largeTopMargin}}>Date</label>
                <DuaTextInput type='text' disabled value={new Date().toLocaleDateString()}/>
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
                        AnalyticsTracker.Registration.AcceptDUCC();
                        this.submitDataUserCodeOfConduct(initialMonitoring);
                      }}
                  >
                    Accept
                  </Button>
                </TooltipTrigger>
              </FlexRow>
            </React.Fragment>
          }
        </FlexColumn>;
      } else {
        return <div style={dataUserCodeOfConductStyles.dataUserCodeOfConductPage}>
          <DataUseAgreementContentV2/>
          <div style={{height: '1rem'}}/>
          {getDataUseAgreementWidgetV2.call(this,
            submitting,
            initialWorkV2,
            initialNameV2,
            initialSanctionsV2,
            errorsV2,
            this.props.profileState.profile)}
        </div>;
      }
    }
  });

@Component({
  template: '<div #root style="height: 100%"></div>'
})
export class DataUserCodeOfConductComponent extends ReactWrapperBase {
  constructor() {
    super(DataUserCodeOfConduct, []);
  }
}
