import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { HtmlViewer } from 'app/components/html-viewer';
import { CheckBox } from 'app/components/inputs';
import { SpinnerOverlay } from 'app/components/spinners';
import { AoU } from 'app/components/text-wrappers';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const LATEST_TOS_VERSION = 1;
const baseCheckboxLabelStyle = {
  color: colors.primary,
  fontFamily: 'Montserrat',
  fontSize: '14px',
  fontWeight: 400,
  paddingLeft: '0.25rem',
  paddingRight: '0.5rem',
};

const styles = reactStyles({
  checkbox: {
    marginRight: '.31667rem',
    zoom: '1.5',
  },
  checkboxLabel: baseCheckboxLabelStyle,
  // We add opacity to make disabled controls even more obvious (classic CSS trick).
  disabledCheckboxLabel: {
    ...baseCheckboxLabelStyle,
    opacity: '0.5',
  },
  noticeText: {
    fontSize: 14,
    fontWeight: 600,
    color: colors.primary,
  },
});

export interface TosProps {
  showReAcceptNotification: boolean;
  // Callback which will be called by this component when the user clicks "Next".
  onComplete: (tosVersion) => void;
  // Path to the Terms of Service file to be displayed.
  filePath: string;
  // Coming from Institution page
  afterPrev: boolean;
  style?: React.CSSProperties;
}

interface TosState {
  hasReadEntireAgreement: boolean;
  hasAckedAgreement: boolean;
  hasClickedNext: boolean;
}

export class TermsOfService extends React.Component<TosProps, TosState> {
  constructor(props: TosProps) {
    super(props);
    this.state = {
      hasReadEntireAgreement: props.afterPrev,
      hasAckedAgreement: props.afterPrev,
      hasClickedNext: false,
    };
    // This bypasses the scroll detection. A better implementation may be to provide a method
    // on HtmlViewer to force the scroll, thus exercising the scroll detection.
    (window as any).forceHasReadEntireAgreement = () => {
      this.setState({ hasReadEntireAgreement: true });
    };
  }

  render() {
    const { hasReadEntireAgreement, hasAckedAgreement, hasClickedNext } =
      this.state;

    return (
      <FlexColumn
        data-test-id='terms-of-service'
        style={{ flex: 1, padding: '1rem 3rem 0 3rem', ...this.props.style }}
      >
        {this.props.showReAcceptNotification && (
          <div
            style={{
              marginBottom: '0.5rem',
              padding: '0.75rem',
              backgroundColor: 'aliceblue',
              borderRadius: 4,
              boxShadow: '1px 1px 6px gray',
            }}
          >
            <h3 style={{ marginTop: 0, fontWeight: 'bold' }}>
              Please review and re-accept the <AoU /> Research Program
              Researcher Workbench Terms of Use and Privacy Statement
            </h3>
            <p className='h-color' style={{ marginTop: '0.25rem' }}>
              The Terra Platform terms, which are incorporated by reference into
              the <AoU /> terms, have been updated. This update replaces
              references to specific federal datasets with a more broad
              reference to federal datasets as a type of data.
            </p>
          </div>
        )}
        <HtmlViewer
          ariaLabel='terms of use and privacy statement'
          containerStyles={{ backgroundColor: colors.white }}
          onLastPage={() => this.setState({ hasReadEntireAgreement: true })}
          filePath={this.props.filePath}
        />
        {hasClickedNext && <SpinnerOverlay />}
        <FlexRow
          style={{
            display: 'inline-flex',
            padding: '1rem',
            maxWidth: '1000px',
            margin: 'auto',
          }}
        >
          <div style={{ flex: 3 }}>
            <div
              style={{
                ...styles.noticeText,
                marginBottom: '.5rem',
                height: '3rem',
              }}
            >
              <div>Please read through the entire agreement to continue.</div>
              <div style={{ fontWeight: 400 }}>
                By clicking below, or continuing with the registration process
                or accessing the Researcher Workbench, you agree to these terms
                and make the following certification:
              </div>
            </div>
            <div>
              <CheckBox
                aria-label='Acknowledge Terms'
                data-test-id='agreement-check'
                checked={hasAckedAgreement}
                disabled={!hasReadEntireAgreement}
                onChange={(checked) =>
                  this.setState({ hasAckedAgreement: checked })
                }
                style={styles.checkbox}
                labelStyle={
                  hasReadEntireAgreement
                    ? styles.checkboxLabel
                    : styles.disabledCheckboxLabel
                }
                wrapperStyle={{ marginBottom: '0.5rem', display: 'flex' }}
                label={
                  <span>
                    I have read, understand, and agree to the <AoU /> Terms of
                    Use and Privacy Statement described above.
                  </span>
                }
              />
            </div>
          </div>
          <FlexColumn
            style={{
              paddingLeft: '3rem',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Button
              aria-label='Next'
              data-test-id='next-button'
              style={{
                width: '5rem',
                height: '2rem',
                margin: '.25rem .5rem .25rem 0',
              }}
              disabled={
                !hasReadEntireAgreement || !hasAckedAgreement || hasClickedNext
              }
              onClick={() => {
                this.setState({ hasClickedNext: true });
                this.props.onComplete(LATEST_TOS_VERSION);
              }}
            >
              Next
            </Button>
          </FlexColumn>
        </FlexRow>
      </FlexColumn>
    );
  }
}
