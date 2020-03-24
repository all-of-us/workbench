import * as React from 'react';
import {Document, Page} from 'react-pdf';

import {SpinnerOverlay} from 'app/components/spinners';
import {withWindowSize} from 'app/utils';

export interface Props {
  handleLastPageRender: () => void;
  pdfPath: string;
  setLastPageRef: (ref) => void;
  windowSize: {
    width: number,
    height: number
  };
}

interface State {
  loading: boolean;
  numPages: number;
}

export const PdfViewer = withWindowSize()( class extends React.Component<Props, State> {
  constructor(props) {
    super(props);

    this.state = {
      loading: true,
      numPages: 0
    };
  }

  unusedCallback() {
    return;
  }

  render() {
    const {loading, numPages} = this.state;

    return <div style={{flex: '1 1 0', overflowY: 'auto'}}>
      {loading && <SpinnerOverlay/>}
      <Document data-test-id='tos-pdf-document'
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
                      loading={''}
                      className={'pdf-page'}
                      width={Math.max(500, this.props.windowSize.width * .75)}
                      key={`page_${index + 1}`}
                      pageNumber={index + 1}
                      inputRef={index === numPages - 1
                          ? (ref) => this.props.setLastPageRef(ref)
                          : () => this.unusedCallback()
                      }
                      onRenderSuccess={index === numPages - 1
                          ? () => this.props.handleLastPageRender()
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
