import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {CheckBox} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import colors from 'app/styles/colors';
import {reactStyles, withWindowSize} from 'app/utils';
import * as React from 'react';
import {Document, Page, pdfjs} from 'react-pdf';

pdfjs.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjs.version}/pdf.worker.js`;

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

export function getPdfPath() {
  return '/assets/documents/terms-of-service.pdf';
}

export interface AccountCreationTosProps {
  onComplete: () => void;
  windowSize: { width: number, height: number };
  pdfPath: string;
}

interface AccountCreationTosState {
  hasReadEntireTos: boolean;
  hasAckedPrivacyStatement: boolean;
  hasAckedTermsOfService: boolean;
  loadingPdf: boolean;
  numPages: number;
}

export const AccountCreationTos = withWindowSize()(
  class extends React.Component<AccountCreationTosProps, AccountCreationTosState> {

    pdfContainerRef: React.RefObject<HTMLDivElement>;
    lastPageLoaded = false;
    lastPage: HTMLElement;

    constructor(props: AccountCreationTosProps) {
      super(props);
      this.state = {
        hasReadEntireTos: false,
        hasAckedPrivacyStatement: false,
        hasAckedTermsOfService: false,
        loadingPdf: true,
        numPages: 0,
      };
      this.pdfContainerRef = React.createRef();
    }

    handleLastPageLoad() {
      if (this.lastPageLoaded) {
        return;
      }
      this.lastPageLoaded = true;
      const intersectionCallback: IntersectionObserverCallback = (
        entries: IntersectionObserverEntry[], unusedObserver: IntersectionObserver) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            this.setState({hasReadEntireTos: true});
          }
        }
      };
      const observer = new IntersectionObserver(intersectionCallback);
      observer.observe(this.lastPage);
    }

    scrollToBottom() {
      if (this.lastPage) {
        this.lastPage.scrollIntoView({block: 'end'});
      } else {
        throw new Error('Last page has not yet loaded.');
      }
    }

    render() {
      const {numPages, loadingPdf, hasReadEntireTos, hasAckedTermsOfService, hasAckedPrivacyStatement} = this.state;

      return <FlexColumn data-test-id='account-creation-tos'
                         style={{flex: 1, padding: '1rem 3rem 0 3rem'}}>
        {/* TODO: all of this PDF rendering stuff should be broken out into a separate component. */}
        <div ref={this.pdfContainerRef} style={{flex: '1 1 0', overflowY: 'auto'}}>
          {loadingPdf && <SpinnerOverlay/>}
          <Document data-test-id='tos-pdf-document' file={this.props.pdfPath}
                    loading=''
                    onLoadSuccess={data => this.setState(
                      {numPages: data.numPages, loadingPdf: false})}
          >
            {
              Array.from(
                new Array(numPages),
                (el, index) => (
                  <Page
                    renderAnnotationLayer={false}
                    renderTextLayer={false}
                    loading=''
                    className='terms-of-service__page'
                    width={Math.max(500, this.props.windowSize.width * .75)}
                    key={`page_${index + 1}`}
                    pageNumber={index + 1}
                    inputRef={index === numPages - 1 ? (ref) => {
                      this.lastPage = ref;
                    } : undefined}
                    onRenderSuccess={index === numPages - 1 ? () => this.handleLastPageLoad() :
                      undefined}
                  />
                ),
              )
            }
          </Document>
        </div>
        <FlexRow
          style={{display: 'inline-flex', padding: '1rem', maxWidth: '1000px', margin: 'auto'}}>
          <div style={{flex: 3}}>
            <div style={{...styles.noticeText, marginBottom: '.5rem', height: '3rem'}}>
              By clicking here and moving to the Registration step, you acknowledge that you
              understand the terms of this agreement and agree to abide by them.
              {!hasReadEntireTos && <span><br/>
              <a data-test-id='scroll-to-bottom'
                 onClick={() => this.scrollToBottom()}
              >Scroll to bottom</a></span>}
            </div>
            <div style={{marginBottom: '.25rem'}}>
              <CheckBox data-test-id='privacy-statement-check'
                        checked={false}
                        disabled={!hasReadEntireTos}
                        onChange={checked => this.setState({hasAckedPrivacyStatement: true})}
                        style={styles.checkbox}
                        labelStyle={hasReadEntireTos ?
                          styles.checkboxLabel :
                          styles.disabledCheckboxLabel}
                        wrapperStyle={{marginBottom: '0.5rem'}}
                        label={<span>
                I have read and understand the <i>All of Us</i> Research Program Privacy Statement.</span>}
              /></div>
            <div>
              <CheckBox data-test-id='terms-of-service-check'
                        checked={false}
                        disabled={!hasReadEntireTos}
                        onChange={checked => this.setState({hasAckedTermsOfService: true})}
                        style={styles.checkbox}
                        labelStyle={hasReadEntireTos ?
                          styles.checkboxLabel :
                          styles.disabledCheckboxLabel}
                        wrapperStyle={{marginBottom: '0.5rem'}}
                        label={<span>
                I have read and understand the <i>All of Us</i> Research Program Terms of Use described above.</span>}
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
  });

export default AccountCreationTos;
