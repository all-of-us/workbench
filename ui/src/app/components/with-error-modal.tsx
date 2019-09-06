import {TextModal} from 'app/components/text-modal';
import * as React from 'react';

interface State {
  show: boolean;
  title: string;
  body: string;
}

export interface WithErrorModalProps {
  showErrorModal: (title: string, body: string) => void;
}

export const withErrorModal = () => {
  return (WrappedComponent) => {
    return class ErrorModalWrapper extends React.Component<any, State> {

      constructor(props) {
        super(props);

        this.state = {
          show: false,
          title: 'Title',
          body: 'Body'
        };
      }

      show(title: string, body: string) {
        this.setState({
          show: true,
          title: title,
          body: body
        });
      }

      render() {
        return <React.Fragment>
          {this.state.show && <TextModal title={this.state.title}
                                         body={this.state.body}
                                         closeFunction={() => this.setState({show: false})}/>
          }
          <WrappedComponent showErrorModal={(tile, body) => this.show(tile, body)}
                            {...this.props} />
        </React.Fragment>;
      }
    };
  };
};
