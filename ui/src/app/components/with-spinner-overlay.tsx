import {SpinnerOverlay} from 'app/components/spinners';
import * as React from 'react';

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

      show() {
        this.setState({show: true});
      }

      hide() {
        this.setState({show: false});
      }

      render() {
        return <React.Fragment>
          {this.state.show && <SpinnerOverlay/>}
          <WrappedComponent showSpinner={this.show}
                            hideSpinner={this.hide}
                            {...this.props} />
        </React.Fragment>;
      }
    };
  };
};
