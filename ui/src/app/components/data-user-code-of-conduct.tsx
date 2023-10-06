import * as React from 'react';
import { CSSProperties } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { validate } from 'validate.js';

import { Profile } from 'generated/fetch';

import { Button, StyledRouterLink } from 'app/components/buttons';
import { FlexColumn, FlexRow, flexStyle } from 'app/components/flex';
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
import {
  DARPageMode,
  DATA_ACCESS_REQUIREMENTS_PATH,
  wasReferredFromRenewal,
} from 'app/utils/access-utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import {
  canRenderSignedDucc,
  getDuccRenderingInfo,
  getLiveDUCCVersion,
} from 'app/utils/code-of-conduct';
import { NavigationProps } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';

const styles = reactStyles({
  dataUserCodeOfConductPage: {
    margin: 'auto',
    color: colors.primary,
    height: '100%',
    // The chrome is minimized on this page to increase scroll space. This has
    // the unwanted side-effect of removing the app padding. Add this same padding back.
    paddingLeft: '.9rem',
    paddingRight: '.9rem',
  },
  dataUserCodeOfConductFooter: {
    backgroundColor: colors.white,
    marginTop: 'auto',
    paddingTop: '1.5rem',
    paddingBottom: '1.5rem',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  smallTopMargin: {
    marginTop: '0.75rem',
  },
  largeTopMargin: {
    marginTop: '2.25rem',
  },
  bold: {
    fontWeight: 600,
  },
  textInput: {
    padding: '0 1ex',
    width: '18rem',
    fontSize: 10,
    borderRadius: 6,
  },
  signature: {
    margin: '0 5.25rem',
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

const SignedVersionFailure = (props: { duccSignedVersion: number }) => {
  const reason = props.duccSignedVersion
    ? 'this user has signed an older version of the Data User Code of Conduct ' +
      `(version id ${props.duccSignedVersion}) which we are unable to display in signed form.`
    : 'this user has not signed a Data User Code of Conduct.';
  return (
    <>
      <div>Error: {reason}</div>
      <div>
        Signing the latest version of the Data User Code of Conduct will grant
        access to a signed copy. This version can be accessed{' '}
        <StyledRouterLink path='/data-code-of-conduct'>here</StyledRouterLink>.
      </div>
    </>
  );
};

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
  props.signatureState === DuccSignatureState.SIGNED ? (
    <SignedText text={props.value} />
  ) : (
    <DuccTextInput {...fp.omit(['signatureState'], props)} disabled={true} />
  );

interface InitialsProps {
  signatureState: DuccSignatureState;
  onChange: (v: string) => void;
  signedInitials?: string;
  children;
}

const InitialsAgreement = (props: InitialsProps) => (
  <div style={{ display: 'flex', marginTop: '0.75rem' }}>
    {props.signatureState === DuccSignatureState.SIGNED ? (
      <SignedText text={props.signedInitials?.toLocaleUpperCase()} />
    ) : (
      <DuccTextInput
        onChange={props.onChange}
        placeholder='INITIALS'
        dataTestId='ducc-initials-input'
        style={{ width: '4rem', textAlign: 'center', padding: 0 }}
      />
    )}
    <div style={{ marginLeft: '0.75rem' }}>{props.children}</div>
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
  const {
    signatureState,
    versionToRender,
    buttonDisabled,
    onLastPage,
    onClick,
  } = props;
  const versionInfo = getDuccRenderingInfo(versionToRender);
  const htmlViewerStyle: CSSProperties = {
    margin: '1.5rem 0 1.5rem',
    height: versionInfo.height,
  };

  return (
    versionInfo && (
      <div data-test-id='ducc-content-page'>
        <HtmlViewer
          ariaLabel='data user code of conduct agreement'
          containerStyles={htmlViewerStyle}
          filePath={versionInfo.path}
          onLastPage={() => onLastPage()}
        />
        {signatureState === DuccSignatureState.UNSIGNED && (
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
              disabled={buttonDisabled}
              onClick={() => onClick()}
            >
              Proceed
            </Button>
          </FlexRow>
        )}
      </div>
    )
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
const DuccSignaturePage = (props: SignatureProps) => {
  const {
    signatureState,
    signedInitials,
    signedDate,
    fullName,
    username,
    errors,
    submitting,
    onChangeMonitoring,
    onChangePublic,
    onChangeAccess,
    onBack,
    onAccept,
  } = props;
  return (
    <div data-test-id='ducc-signature-page' style={styles.signature}>
      <FlexColumn>
        {submitting && <SpinnerOverlay />}
        {signatureState === DuccSignatureState.UNSIGNED && (
          <h1>Accept Data User Code of Conduct</h1>
        )}
        <div style={{ ...styles.bold, ...styles.smallTopMargin }}>
          I,
          <ReadOnlyTextField
            {...{ signatureState }}
            style={{ margin: '0 1ex' }}
            value={fullName}
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
          {...{ signatureState, signedInitials }}
          onChange={(v) => onChangeMonitoring(v)}
        >
          My work, including any external data, files, or software I upload into
          the Researcher Workbench, will be logged and monitored by the <AoU />{' '}
          Research Program to ensure compliance with policies and procedures.
        </InitialsAgreement>
        <InitialsAgreement
          {...{ signatureState, signedInitials }}
          onChange={(v) => onChangePublic(v)}
        >
          My name, affiliation, profile information and research description
          will be made public. My research description will be used by the{' '}
          <AoU /> Research Program to provide participants with meaningful
          information about the research being conducted.
        </InitialsAgreement>
        <InitialsAgreement
          {...{ signatureState, signedInitials }}
          onChange={(v) => onChangeAccess(v)}
        >
          <AoU /> retains the discretion to make decisions about my access,
          including the provision or revocation thereof, at any time that take
          into account any data use violations, data management incidents,
          research misconduct, and legal or regulatory violations related to the
          conduct of research for which I’ve been penalized in the past or that
          I may commit or am found to have committed subsequent to becoming an{' '}
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
          {...{ signatureState }}
          disabled
          dataTestId='ducc-username-input'
          value={fullName}
        />
        <label style={{ ...styles.bold, ...styles.largeTopMargin }}>
          User ID
        </label>
        <ReadOnlyTextField
          {...{ signatureState }}
          dataTestId='ducc-user-id-input'
          value={username}
        />
        <label style={{ ...styles.bold, ...styles.largeTopMargin }}>Date</label>
        <ReadOnlyTextField
          {...{ signatureState }}
          type='text'
          value={
            signatureState === DuccSignatureState.SIGNED
              ? new Date(signedDate).toLocaleDateString()
              : new Date().toLocaleDateString()
          }
        />
      </FlexColumn>
      {signatureState === DuccSignatureState.UNSIGNED && (
        <FlexRow style={styles.dataUserCodeOfConductFooter}>
          <Button
            type='link'
            style={{ marginLeft: 'auto' }}
            onClick={() => onBack()}
          >
            Back
          </Button>
          <TooltipTrigger
            content={
              errors && (
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
              disabled={errors || submitting}
              onClick={() => onAccept()}
            >
              Accept
            </Button>
          </TooltipTrigger>
        </FlexRow>
      )}
    </div>
  );
};

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
        onDismiss: () =>
          this.props.navigate([DATA_ACCESS_REQUIREMENTS_PATH], {
            queryParams: {
              pageMode: DARPageMode.ANNUAL_RENEWAL,
            },
          }),
      }),
      withErrorModal({
        title: 'Your agreement failed to update',
        message: 'Please try submitting the agreement again.',
      })
    )(async (initials) => {
      const profile = await profileApi().submitDUCC(
        getLiveDUCCVersion(),
        initials
      );
      this.props.profileState.updateCache(profile);
    });

    submitDataUserCodeOfConduct(initials) {
      profileApi()
        .submitDUCC(getLiveDUCCVersion(), initials)
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
        // FlexColumn is appropriate styling only for the UNSIGNED case, due to iframe height styling conflicts
        ...(signatureState === DuccSignatureState.UNSIGNED && flexStyle.column),
      };

      const showContentPage =
        signatureState === DuccSignatureState.SIGNED
          ? canRenderSignedDucc(duccSignedVersion)
          : page === DataUserCodeOfConductPage.CONTENT;
      const showSignaturePage =
        signatureState === DuccSignatureState.SIGNED
          ? canRenderSignedDucc(duccSignedVersion)
          : page === DataUserCodeOfConductPage.SIGNATURE;

      return (
        <div style={containerStyle}>
          {signatureState === DuccSignatureState.SIGNED &&
            !canRenderSignedDucc(duccSignedVersion) && (
              <SignedVersionFailure {...{ duccSignedVersion }} />
            )}
          {showContentPage && (
            <DuccContentPage
              {...{ signatureState }}
              versionToRender={
                signatureState === DuccSignatureState.SIGNED
                  ? duccSignedVersion
                  : getLiveDUCCVersion()
              }
              buttonDisabled={proceedDisabled}
              onLastPage={() => this.setState({ proceedDisabled: false })}
              onClick={() =>
                this.setState({ page: DataUserCodeOfConductPage.SIGNATURE })
              }
            />
          )}
          {showSignaturePage && (
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
