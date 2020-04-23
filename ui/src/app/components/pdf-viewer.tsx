import * as React from 'react';
import {Document, Page} from 'react-pdf';

import {SpinnerOverlay} from 'app/components/spinners';
import {withWindowSize} from 'app/utils';

export interface Props {
  containerStyles?: React.CSSProperties;
  onLastPageRender: () => void;
  pdfPath: string;
  windowSize: {
    width: number,
    height: number
  };
}

interface State {
  hasReadEntirePdf: boolean;
  loading: boolean;
  numPages: number;
}

export const PdfViewer = withWindowSize()( class extends React.Component<Props, State> {
  // Tracks whether this component has created an intersection observer to track the last page
  // visibility yet.
  hasCreatedIntersectionObserver = false;
  // Once the last page has been loaded, this contains a reference to the page's DOM element.
  lastPage: HTMLElement;

  constructor(props) {
    super(props);

    this.state = {
      hasReadEntirePdf: false,
      loading: true,
      numPages: 0
    };
  }

  /**
   * Handles the onRenderSuccess callback from the Page element at the end of the document.
   * This sets up the intersection listener which will change state when the user scrolls to the
   * end of the document.
   */
  private handleLastPageRender() {
    if (this.hasCreatedIntersectionObserver) {
      return;
    }
    this.hasCreatedIntersectionObserver = true;
    const intersectionCallback: IntersectionObserverCallback = (
        entries: IntersectionObserverEntry[],
        unusedObserver: IntersectionObserver
    ) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          if (!this.state.hasReadEntirePdf) {
            this.props.onLastPageRender();
          }
          this.setState({hasReadEntirePdf: true});
        }
      }
    };
    const observer = new IntersectionObserver(intersectionCallback);
    observer.observe(this.lastPage);
  }

  private setLastPageRef(ref) {
    this.lastPage = ref;
  }

  unusedCallback() {
    return;
  }

  render() {
    const {loading, numPages} = this.state;

    return <div style={{flex: '1 1 0', overflowY: 'auto', ...this.props.containerStyles}}>
      {loading && <SpinnerOverlay/>}
      <Document data-test-id='pdf-document'
                file={this.props.pdfPath}
                loading=''
                onLoadSuccess={
                  data => this.setState({numPages: data.numPages, loading: false})
                }
      >
        {
          Array.from(
            new Array(numPages),
            (el, index) => (
                  // We can't set inline styles on the react-pdf Page element, so instead we set a
                  // className and specify some style overrides in src/styles.css
                  <Page
                      renderAnnotationLayer={false}
                      renderTextLayer={false}
                      loading=''
                      className='pdf-page'
                      width={Math.max(500, this.props.windowSize.width * .75)}
                      key={`page_${index + 1}`}
                      pageNumber={index + 1}
                      inputRef={index === numPages - 1
                          ? (ref) => this.setLastPageRef(ref)
                          : () => this.unusedCallback()
                      }
                      onRenderSuccess={index === numPages - 1
                          ? () => this.handleLastPageRender()
                          : () => this.unusedCallback()
                      }
                  />
              ),
          )
        }
      </Document>
    </div>;
  }
});
