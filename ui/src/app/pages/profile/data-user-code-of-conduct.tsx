import {Component} from '@angular/core';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {Profile} from 'generated/fetch';
import * as React from 'react';
import {validate} from 'validate.js';
import {DataUseAgreementContentV1} from 'app/pages/profile/data-use-agreement-content-v1';
import {getDataUseAgreementWidgetV1} from 'app/pages/profile/data-use-agreement-panel';
import {
  dataUseAgreementStyles,
  DuaTextInput,
  InitialsAgreement
} from 'app/pages/profile/data-use-agreement-styles';
import {serverConfigStore} from "app/utils/navigation";
import {FlexColumn, FlexRow} from "../../components/flex";
import colors from "../../styles/colors";
import {Button} from "../../components/buttons";
import {PdfViewer} from "../../components/pdf-viewer";
import {SpinnerOverlay} from "../../components/spinners";

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

enum DataUserCodeOfConductPage {
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
  initialName: string;
  initialWork: string;
  initialSanctions: string;
  page: DataUserCodeOfConductPage;
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
        page: DataUserCodeOfConductPage.CONTENT,
        submitting: false
      };
    }

    submitDataUseAgreement(initials) {
      this.setState({submitting: true});
      const dataUseAgreementVersion = serverConfigStore.getValue().enableV2DataUserCodeOfConduct ? 3 : 2;
      profileApi().submitDataUseAgreement(dataUseAgreementVersion, initials).then((profile) => {
        this.props.profileState.updateCache(profile);
        window.history.back();
      });
    }

    render() {
      const {profileState: {profile}} = this.props;
      const {initialName, initialWork, initialSanctions, page, submitting} = this.state;
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
      if (serverConfigStore.getValue().enableV2DataUserCodeOfConduct) {
        return <FlexColumn style={styles.dataUserCodeOfConductPage}>
          {
            page == DataUserCodeOfConductPage.CONTENT && <React.Fragment>
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
                    onClick={() => {
                      this.setState({page: DataUserCodeOfConductPage.SIGNATURE})}
                    }
                >
                  Accept
                </Button>
              </FlexRow>
            </React.Fragment>
          }
          {
            page == DataUserCodeOfConductPage.SIGNATURE && <React.Fragment>
              <FlexColumn>
                {submitting && <SpinnerOverlay/>}
                <h1>Accept Data User Code of Conduct</h1>
                <div style={{...styles.bold, ...styles.smallTopMargin}}>
                  I
                  <DuaTextInput style={{margin: '0 1ex'}}
                     disabled
                     value={profile.givenName + ' ' + profile.familyName}
                     data-test-id='dua-name-input'/>
                   ("Authorized Data User") have personally reviewed this Data User Code of Conduct.
                   I agree to follow each of the policies and prodedures it describes.
                </div>
                <div style={styles.smallTopMargin}>
                  By entering my initials next to each statement below, I acknowledge that:
                </div>
                <InitialsAgreement>
                  My work, including any external data, files, or software I upload into the Researcher Workbench, may be logged and monitored by the <i>All of Us</i> Research Program to ensure compliance with policies and procedures.
                </InitialsAgreement>
                <InitialsAgreement>
                  My name, affiliation, profile information, and research description will be made public. My research description will be used by the <i>All of Us</i> Research Program to provide participants with meaningful information about the research being conducted.
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
                    notification of the National Institute of Health or other federal agencies as
                     to my actions.
                  </li>
                </ul>
                <div style={{...styles.bold, ...styles.smallTopMargin}}>
                  I understand that failure to comply with these requirements may also carry
                   financial or legal repercussions. Any misuse of the <i>All of Us</i> Research
                   Hub, Researcher Workbench, or the <i>All of Us</i> Research Program data is
                   taken very seriously and other sanctions may be sought.
                </div>
                <label style={{...styles.bold, ...styles.largeTopMargin}}>Authorized Data user Name</label>
                <DuaTextInput
                    disabled
                    data-test-id='dua-username-input'
                    value={profile.givenName + ' ' + profile.familyName}
                />
                <label style={{...styles.bold, ...styles.largeTopMargin}}>User ID</label>
                <DuaTextInput
                    disabled
                    data-test-id='dua-user-id-input'
                    value={profile.username}
                />
                <label style={{...styles.bold, ...styles.largeTopMargin}}>Date</label>
                <DuaTextInput type='text' disabled value={new Date().toLocaleDateString()}/>
              </FlexColumn>
              <FlexRow style={styles.dataUserCodeOfConductFooter}>
                <Button
                    type={'link'}
                    style={{marginLeft: 'auto'}}
                    onClick={() => {this.setState({page: DataUserCodeOfConductPage.CONTENT})}}
                >
                  Back
                </Button>
                <Button
                    onClick={() => {
                      this.submitDataUseAgreement(this.state.initialWork)
                    }}
                >
                  Accept
                </Button>
              </FlexRow>
            </React.Fragment>
          }
        </FlexColumn>
      } else {
        return <div style={dataUseAgreementStyles.dataUseAgreementPage}>
          <DataUseAgreementContentV1/>
          <div style={{height: '1rem'}}/>
          {getDataUseAgreementWidgetV1.call(this,
              submitting,
              initialWork,
              initialName,
              initialSanctions,
              errors,
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
    super(DataUseAgreement, []);
  }
}
