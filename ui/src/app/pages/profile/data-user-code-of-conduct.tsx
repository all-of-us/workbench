import * as React from 'react';
import { CSSProperties } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { validate } from 'validate.js';

import { Profile } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { flexStyle } from 'app/components/flex';
import { HtmlViewer } from 'app/components/html-viewer';
import { TextInput } from 'app/components/inputs';
import { withErrorModal, withSuccessModal } from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { AoU } from 'app/components/text-wrappers';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles, withUserProfile } from 'app/utils';
import { wasReferredFromRenewal } from 'app/utils/access-utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { getLiveDUCCVersion, getVersionInfo } from 'app/utils/code-of-conduct';
import { NavigationProps } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';

const styles = reactStyles({
  dataUserCodeOfConductPage: {
    margin: 'auto',
    color: colors.primary,
    height: '100%',
    // The chrome is minimized on this page to increase scroll space. This has
    // the unwanted side-effect of removing the app padding. Add this same padding back.
    paddingLeft: '.6rem',
    paddingRight: '.6rem',
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
    marginTop: '0.5rem',
  },
  largeTopMargin: {
    marginTop: '1.5rem',
  },
  bold: {
    fontWeight: 600,
  },
  textInput: {
    padding: '0 1ex',
    width: '12rem',
    fontSize: 10,
    borderRadius: 6,
  },
  signedText: {
    margin: '0 1ex',
    fontWeight: 'bold',
    fontStyle: 'italic',
    textDecoration: 'underline',
  },
});

export enum DataUserCodeOfConductPage {
  CONTENT,
  SIGNATURE,
}

export const enum DuccSignatureState {
  UNSIGNED,
  SIGNED,
}

interface DuccTextInputProps {
  value?: string;
  placeholder?: string;
  onChange?: (v: string) => void;
  dataTestId?: string;
  style?: React.CSSProperties;
  disabled?: boolean;
  type?: string;
}
const DuccTextInput = (props: DuccTextInputProps) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  return (
    <TextInput
      {...fp.omit(['data-test-id', 'dataTestId'], props)}
      data-test-id={props.dataTestId}
      style={{
        ...styles.textInput,
        ...props.style,
      }}
    />
  );
};

const SignedText = (props: { text: string }) => (
  // add 2 spaces before and after to simulate a signature line
  <span style={styles.signedText}>&nbsp;&nbsp;{props.text}&nbsp;&nbsp;</span>
);

interface ReadOnlyProps extends DuccTextInputProps {
  signatureState: DuccSignatureState;
}
const ReadOnlyTextField = (props: ReadOnlyProps) =>
  props.signatureState === DuccSignatureState.UNSIGNED ? (
    <DuccTextInput {...fp.omit(['signatureState'], props)} disabled={true} />
  ) : (
    <SignedText text={props.value} />
  );

interface InitialsProps {
  signatureState: DuccSignatureState;
  onChange: (v: string) => void;
  signedValue?: string;
  children;
}

const InitialsAgreement = (props: InitialsProps) => (
  <div style={{ display: 'flex', marginTop: '0.5rem' }}>
    {props.signatureState === DuccSignatureState.SIGNED && (
      <SignedText text={props.signedValue?.toLocaleUpperCase()} />
    )}
    {props.signatureState === DuccSignatureState.UNSIGNED && (
      <DuccTextInput
        onChange={props.onChange}
        placeholder='INITIALS'
        dataTestId='ducc-initials-input'
        style={{ width: '4ex', textAlign: 'center', padding: 0 }}
      />
    )}
    <div style={{ marginLeft: '0.5rem' }}>{props.children}</div>
  </div>
);

interface ContentProps {
  signatureState: DuccSignatureState;
  versionToRender: number;
  buttonDisabled: boolean;
  onLastPage: () => void;
  onClick: () => void;
}
const DuccContentPage = (props: ContentProps) => {
  const versionInfo = getVersionInfo(props.versionToRender);

  return versionInfo ? (
    <>
      <HtmlViewer
        ariaLabel='data user code of conduct agreement'
        containerStyles={{ margin: '1rem 0 1rem', height: versionInfo.height }}
        filePath={versionInfo.path}
        onLastPage={() => props.onLastPage()}
      />
      {props.signatureState === DuccSignatureState.UNSIGNED && (
        <FlexRow style={styles.dataUserCodeOfConductFooter}>
          Please read the above document in its entirety before proceeding to
          sign the Data User Code of Conduct.
          <Button
            type={'link'}
            style={{ marginLeft: 'auto' }}
            onClick={() => history.back()}
          >
            Back
          </Button>
          <Button
            data-test-id={'ducc-next-button'}
            disabled={props.buttonDisabled}
            onClick={() => props.onClick()}
          >
            Proceed
          </Button>
        </FlexRow>
      )}
    </>
  ) : (
    <div>
      Error: cannot render Data User Code of Conduct version '
      {props.versionToRender}'
    </div>
  );
};

interface SignatureProps {
  signatureState: DuccSignatureState;
  signedInitials?: string;
  signedDate?: number;
  errors;
  submitting: boolean;
  fullName: string;
  username: string;
  onChangeMonitoring: (v: string) => void;
  onChangePublic: (v: string) => void;
  onChangeAccess: (v: string) => void;
  onBack: () => void;
  onAccept: () => void;
}
const DuccSignaturePage = (props: SignatureProps) => (
  <>
    <FlexColumn>
      {props.submitting && <SpinnerOverlay />}
      {props.signatureState === DuccSignatureState.UNSIGNED && (
        <h1>Accept Data User Code of Conduct</h1>
      )}
      <div style={{ ...styles.bold, ...styles.smallTopMargin }}>
        I,
        <ReadOnlyTextField
          signatureState={props.signatureState}
          style={{ margin: '0 1ex' }}
          value={props.fullName}
          dataTestId='ducc-name-input'
        />
        ("Authorized Data User"), have personally reviewed this Data User Code
        of Conduct. I agree to follow each of the policies and procedures it
        describes.
      </div>
      <div style={styles.smallTopMargin}>
        By entering my initials next to each statement below, I acknowledge
        that:
      </div>
      <InitialsAgreement
        onChange={(v) => props.onChangeMonitoring(v)}
        signatureState={props.signatureState}
        signedValue={props.signedInitials}
      >
        My work, including any external data, files, or software I upload into
        the Researcher Workbench, will be logged and monitored by the <AoU />{' '}
        Research Program to ensure compliance with policies and procedures.
      </InitialsAgreement>
      <InitialsAgreement
        onChange={(v) => props.onChangePublic(v)}
        signatureState={props.signatureState}
        signedValue={props.signedInitials}
      >
        My name, affiliation, profile information and research description will
        be made public. My research description will be used by the <AoU />{' '}
        Research Program to provide participants with meaningful information
        about the research being conducted.
      </InitialsAgreement>
      <InitialsAgreement
        onChange={(v) => props.onChangeAccess(v)}
        signatureState={props.signatureState}
        signedValue={props.signedInitials}
      >
        <AoU /> retains the discretion to make decisions about my access,
        including the provision or revocation thereof, at any time that take
        into account any data use violations, data management incidents,
        research misconduct, and legal or regulatory violations related to the
        conduct of research for which I’ve been penalized in the past or that I
        may commit or am found to have committed subsequent to becoming an{' '}
        <AoU /> Authorized Data User.
      </InitialsAgreement>
      <div style={{ ...styles.bold, ...styles.smallTopMargin }}>
        I acknowledge that failure to comply with the requirements outlined in
        this Data User Code of Conduct may result in termination of my <AoU />{' '}
        Research Program account and/or other sanctions, including, but not
        limited to:
      </div>
      <ul style={{ ...styles.bold, ...styles.smallTopMargin }}>
        <li>
          the posting of my name and affiliation on a publicly accessible list
          of violators, and
        </li>
        <li>
          notification of the National Institutes of Health or other federal
          agencies as to my actions.
        </li>
      </ul>
      <div style={{ ...styles.bold, ...styles.smallTopMargin }}>
        I understand that failure to comply with these requirements may also
        carry financial or legal repercussions. Any misuse of the <AoU />{' '}
        Research Hub, Researcher Workbench or data resources is taken very
        seriously, and other sanctions may be sought.
      </div>
      <label style={{ ...styles.bold, ...styles.largeTopMargin }}>
        Authorized Data User Name
      </label>
      <ReadOnlyTextField
        signatureState={props.signatureState}
        disabled
        dataTestId='ducc-username-input'
        value={props.fullName}
      />
      <label style={{ ...styles.bold, ...styles.largeTopMargin }}>
        User ID
      </label>
      <ReadOnlyTextField
        signatureState={props.signatureState}
        dataTestId='ducc-user-id-input'
        value={props.username}
      />
      <label style={{ ...styles.bold, ...styles.largeTopMargin }}>Date</label>
      <ReadOnlyTextField
        signatureState={props.signatureState}
        type='text'
        value={
          props.signedDate
            ? new Date(props.signedDate).toLocaleDateString()
            : new Date().toLocaleDateString()
        }
      />
    </FlexColumn>
    {props.signatureState === DuccSignatureState.UNSIGNED && (
      <FlexRow style={styles.dataUserCodeOfConductFooter}>
        <Button
          type='link'
          style={{ marginLeft: 'auto' }}
          onClick={() => props.onBack()}
        >
          Back
        </Button>
        <TooltipTrigger
          content={
            props.errors && (
              <div>
                <div>All fields must be initialed</div>
                <div>All initials must match</div>
                <div>Initials must be six letters or fewer</div>
              </div>
            )
          }
        >
          <Button
            data-test-id='submit-ducc-button'
            disabled={props.errors || props.submitting}
            onClick={() => props.onAccept()}
          >
            Accept
          </Button>
        </TooltipTrigger>
      </FlexRow>
    )}
  </>
);

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps {
  profileState: {
    profile: Profile;
    reload: Function;
    updateCache: Function;
  };
  signatureState: DuccSignatureState;
}

interface State {
  name: string;
  initialMonitoring: string;
  initialPublic: string;
  initialAccess: string;
  page: DataUserCodeOfConductPage;
  submitting: boolean;
  proceedDisabled: boolean;
}

export const DataUserCodeOfConduct = fp.flow(
  withUserProfile(),
  withNavigation,
  withRouter
)(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        name: '',
        initialMonitoring: '',
        initialPublic: '',
        initialAccess: '',
        page: DataUserCodeOfConductPage.CONTENT,
        submitting: false,
        proceedDisabled: true,
      };
    }

    submitCodeOfConductWithRenewal = fp.flow(
      withSuccessModal({
        title: 'Your agreement has been updated',
        message:
          'You will be redirected to the access renewal page upon closing this dialog.',
        onDismiss: () => this.props.navigate(['access-renewal']),
      }),
      withErrorModal({
        title: 'Your agreement failed to update',
        message: 'Please try submitting the agreement again.',
      })
    )(async (initials) => {
      const duccVersion = getLiveDUCCVersion();
      const profile = await profileApi().submitDUCC(duccVersion, initials);
      this.props.profileState.updateCache(profile);
    });

    submitDataUserCodeOfConduct(initials) {
      const duccVersion = getLiveDUCCVersion();
      profileApi()
        .submitDUCC(duccVersion, initials)
        .then((profile) => {
          this.props.profileState.updateCache(profile);
          history.back();
        });
    }

    componentDidMount() {
      this.props.hideSpinner();
    }

    render() {
      const {
        profileState: {
          profile: {
            username,
            givenName,
            familyName,
            duccSignedInitials,
            duccSignedVersion,
            duccCompletionTimeEpochMillis,
          },
        },
        signatureState,
      } = this.props;
      const {
        proceedDisabled,
        initialMonitoring,
        initialPublic,
        initialAccess,
        page,
        submitting,
      } = this.state;
      const errors = validate(
        { initialMonitoring, initialPublic, initialAccess },
        {
          initialMonitoring: {
            presence: { allowEmpty: false },
            length: { maxiumum: 6 },
          },
          initialPublic: {
            presence: { allowEmpty: false },
            equality: { attribute: 'initialMonitoring' },
          },
          initialAccess: {
            presence: { allowEmpty: false },
            equality: { attribute: 'initialMonitoring' },
          },
        }
      );

      const containerStyle: CSSProperties = {
        ...styles.dataUserCodeOfConductPage,
        ...(signatureState === DuccSignatureState.UNSIGNED &&
          // FlexColumn is appropriate styling only for the UNSIGNED case, due to iframe height styling conflicts
          flexStyle.column),
      };
      return (
        <div style={containerStyle}>
          {(signatureState === DuccSignatureState.SIGNED ||
            page === DataUserCodeOfConductPage.CONTENT) && (
            <DuccContentPage
              {...{ signatureState }}
              versionToRender={3}
              //              versionToRender={duccSignedVersion ?? getLiveDUCCVersion()}
              buttonDisabled={proceedDisabled}
              onLastPage={() => this.setState({ proceedDisabled: false })}
              onClick={() =>
                this.setState({ page: DataUserCodeOfConductPage.SIGNATURE })
              }
            />
          )}
          {(signatureState === DuccSignatureState.SIGNED ||
            page === DataUserCodeOfConductPage.SIGNATURE) && (
            <DuccSignaturePage
              {...{ errors, submitting, signatureState, username }}
              fullName={givenName + ' ' + familyName}
              signedInitials={duccSignedInitials}
              signedDate={duccCompletionTimeEpochMillis}
              onChangeMonitoring={(v) =>
                this.setState({ initialMonitoring: v })
              }
              onChangePublic={(v) => this.setState({ initialPublic: v })}
              onChangeAccess={(v) => this.setState({ initialAccess: v })}
              onBack={() =>
                this.setState({ page: DataUserCodeOfConductPage.CONTENT })
              }
              onAccept={() => {
                this.setState({ submitting: true });
                // This may record extra GA events if the user views & accepts the DUCC from their profile. If the additional events
                // are an issue, we may need further changes, possibly disable the Accept button after initial submit.
                AnalyticsTracker.Registration.AcceptDUCC();
                wasReferredFromRenewal(this.props.location.search)
                  ? this.submitCodeOfConductWithRenewal(initialMonitoring)
                  : this.submitDataUserCodeOfConduct(initialMonitoring);
                this.setState({ submitting: false });
              }}
            />
          )}
        </div>
      );
    }
  }
);
