import {SpinnerOverlay} from 'app/components/spinners';
import {withWindowSize} from 'app/utils';
import { WindowSizeProps } from 'app/utils';
import * as React from 'react';


export interface Props extends WindowSizeProps {
  containerStyles?: React.CSSProperties;
  onLastPage: () => void;
  filePath: string;
  ariaLabel: string;
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
    const { onLastPage = () => false } = this.props;
    if (!hasReadEntireDoc && this.state.hasReadEntireDoc) {
      onLastPage();
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
        // The callback receives a list of entries - since we only have one intersection entry (threshold: 1.0)
        // we can destructure the first item of the array and determine whether it is intersecting or not
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
    const { filePath, containerStyles, ariaLabel } = this.props;

    return <div aria-label={ariaLabel} style={{ flex: '1 1 0', position: 'relative', ...containerStyles }}>
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
          onLoad={() => this.handleIframeLoaded()}>
      </iframe>}
    </div>;
  }
});
