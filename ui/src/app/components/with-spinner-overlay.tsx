import {SpinnerOverlay, SpinnerOverlayProps} from 'app/components/spinners';
import * as React from 'react';

interface State {
  show: boolean;
}

export interface WithSpinnerOverlayProps {
  showSpinner: () => void;
  hideSpinner: () => void;
}

export const withSpinnerOverlay = (
    initialShowState = false,
    spinnerOverlayProps: SpinnerOverlayProps = {}) => {
  return (WrappedComponent) => {
    return class WithSpinnerOverlay extends React.Component<any, State> {

      constructor(props) {
        super(props);

        this.state = {
          show: initialShowState
        };
      }

      show() {
        this.setState({show: true});
      }

      hide() {
        console.log('Hide spinner');
        this.setState({show: false});
      }

      render() {
        return <React.Fragment>
          {this.state.show && <SpinnerOverlay {...spinnerOverlayProps}/>}
          <WrappedComponent showSpinner={() => this.show()}
                            hideSpinner={() => this.hide()}
                            {...this.props} />
        </React.Fragment>;
      }
    };
  };
};
