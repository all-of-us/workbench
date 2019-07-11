import * as React from 'react';
import {SpinnerOverlay} from "app/components/spinners";

interface State {
  show: boolean;
}

export interface WithSpinnerOverlayProps {
  showSpinner: () => void;
  hideSpinner: () => void;
}

export const withSpinnerOverlay = () => {
  return (WrappedComponent) => {
    return class ErrorModalWrapper extends React.Component<any, State> {

      constructor(props) {
        super(props);

        this.state = {
          show: false
        };
      }

      showSpinner() {
        this.setState({show: true});
      }

      hideSpinner() {
        this.setState({show: false});
      }

      render() {
        return <React.Fragment>
          {this.state.show && <SpinnerOverlay/>}
          <WrappedComponent showSpinner={this.showSpinner}
                            hideSpinner={this.hideSpinner}
                            {...this.props} />
        </React.Fragment>
      }
    };
  };
};