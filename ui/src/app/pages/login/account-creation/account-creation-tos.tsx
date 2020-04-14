import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {CheckBox} from 'app/components/inputs';
import {PdfViewer} from 'app/components/pdf-viewer';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as React from 'react';

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
    marginRight: '.31667rem', zoom: '1.5',
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

export interface AccountCreationTosProps {
  // Callback which will be called by this component when the user clicks "Next".
  onComplete: () => void;
  // Path to the Terms of Service PDF file to be displayed.
  pdfPath: string;
}

interface AccountCreationTosState {
  hasReadEntireTos: boolean;
  hasAckedPrivacyStatement: boolean;
  hasAckedTermsOfService: boolean;
  // Whether the PDF document is currently being loaded. A spinner will show while true.
  loadingPdf: boolean;
  // Once the PDF has been loaded, this value contains the number of pages in the PDF document.
  numPages: number;
}

export class AccountCreationTos extends React.Component<
  AccountCreationTosProps,
  AccountCreationTosState
> {
  constructor(props: AccountCreationTosProps) {
    super(props);
    this.state = {
      hasReadEntireTos: false,
      hasAckedPrivacyStatement: false,
      hasAckedTermsOfService: false,
      loadingPdf: true,
      numPages: 0,
    };
  }

  render() {
    const {hasReadEntireTos, hasAckedTermsOfService, hasAckedPrivacyStatement} = this.state;

    return <FlexColumn data-test-id='account-creation-tos'
                       style={{flex: 1, padding: '1rem 3rem 0 3rem'}}>
      <PdfViewer
          onLastPageRender={() => this.setState({hasReadEntireTos: true})}
          pdfPath={this.props.pdfPath}
      />
      <FlexRow
        style={{display: 'inline-flex', padding: '1rem', maxWidth: '1000px', margin: 'auto'}}>
        <div style={{flex: 3}}>
          <div style={{...styles.noticeText, marginBottom: '.5rem', height: '3rem'}}>
            <div>
              Please read through the entire agreement to continue.
            </div>
            <div style={{fontWeight: 400}}>
                By clicking below, or continuing with the registration process or accessing the
                Workbench, You agree to these Terms and make the following certifications:
            </div>
          </div>
          <div style={{marginBottom: '.25rem'}}>
            <CheckBox data-test-id='privacy-statement-check'
                      checked={false}
                      disabled={!hasReadEntireTos}
                      onChange={checked => this.setState({hasAckedPrivacyStatement: checked})}
                      style={styles.checkbox}
                      labelStyle={hasReadEntireTos ?
                        styles.checkboxLabel :
                        styles.disabledCheckboxLabel}
                      wrapperStyle={{marginBottom: '0.5rem'}}
                      label={<span>
              I have read, understand, and agree to the <i>All of Us</i> Program Privacy Statement.</span>}
            /></div>
          <div>
            <CheckBox data-test-id='terms-of-service-check'
                      checked={false}
                      disabled={!hasReadEntireTos}
                      onChange={checked => this.setState({hasAckedTermsOfService: checked})}
                      style={styles.checkbox}
                      labelStyle={hasReadEntireTos ?
                        styles.checkboxLabel :
                        styles.disabledCheckboxLabel}
                      wrapperStyle={{marginBottom: '0.5rem'}}
                      label={<span>
              I have read, understand, and agree to the Terms described above.</span>}
            /></div>
        </div>
        <FlexColumn style={{paddingLeft: '3rem', alignItems: 'center', justifyContent: 'center'}}>
          <Button data-test-id='next-button'
                  style={{width: '5rem', height: '2rem', margin: '.25rem .5rem .25rem 0'}}
                  disabled={!hasReadEntireTos || !hasAckedPrivacyStatement ||
                  !hasAckedTermsOfService}
                  onClick={() => this.props.onComplete()}>
            Next
          </Button>
        </FlexColumn>
      </FlexRow>
    </FlexColumn>;
  }
}
