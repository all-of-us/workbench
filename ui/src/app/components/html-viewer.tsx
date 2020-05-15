import {SpinnerOverlay} from 'app/components/spinners';
import {withWindowSize} from 'app/utils';
import * as React from 'react';


export interface Props {
  containerStyles?: React.CSSProperties;
  onLastPageRender: () => void;
  filePath: string;
  windowSize: {
    width: number,
    height: number
  };
}

interface State {
  hasReadEntireDoc: boolean;
  iframeFailed: boolean;
  loading: boolean;
}

export const HtmlViewer = withWindowSize()( class extends React.Component<Props,  State> {
  iframeRef: React.RefObject<any>;

  constructor(props) {
    super(props);

    this.state = {
      hasReadEntireDoc: false,
      iframeFailed: false,
      loading: true
    };

    this.iframeRef = React.createRef();
  }

  componentDidUpdate({}, {hasReadEntireDoc}) {
    const { onLastPageRender = () => false } = this.props;
    if (!hasReadEntireDoc && this.state.hasReadEntireDoc) {
      onLastPageRender();
    }
  }

  private handleIframeLoaded() {
    try {
      const iframeDocument = this.iframeRef.current.contentDocument;
      const { body } = iframeDocument;
      const openLinksInNewTab = iframeDocument.createElement('base');
      const endOfPage = iframeDocument.createElement('div');

      openLinksInNewTab.setAttribute('target', '_blank');
      body.prepend(openLinksInNewTab);
      body.appendChild(endOfPage);

      const observer = new IntersectionObserver(
        ([{ isIntersecting }]) => isIntersecting && !this.state.hasReadEntireDoc && this.setState({ hasReadEntireDoc: true }),
        { root: null, threshold: 1.0 }
      );
      observer.observe(endOfPage);
    } catch (e) {
      this.setState({iframeFailed: true});
    } finally {
      this.setState({ loading: false });
    }
  }

  render() {
    const { loading, iframeFailed } = this.state;
    const { filePath, containerStyles } = this.props;

    return <div style={{ flex: '1 1 0', position: 'relative', ...containerStyles }}>
      { loading && <SpinnerOverlay/> }
      {iframeFailed ?
      'Content failed to load - please try refreshing the page' :
      <iframe
        style={{
          border: 'none',
          position: 'absolute',
          padding: '0 2rem',
          bottom: 0,
          top: 0,
          height: '100%',
          width: '100%'}}
          src = {filePath}
          ref = {this.iframeRef}
          onLoad={() => this.handleIframeLoaded() }>
      </iframe>}
    </div>;
  }
});
