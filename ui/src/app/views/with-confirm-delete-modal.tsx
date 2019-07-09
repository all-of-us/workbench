import * as React from 'react';
import {ConfirmDeleteModal} from "app/views/confirm-delete-modal";

interface State {
  show: boolean;
  displayName: string;
  resourceType: string;
  receiveDelete: Function;
}

export const withConfirmDeleteModal = () => {
  return (WrappedComponent) => {
    return class ConfirmDeleteModalWrapper extends React.Component<any, State> {

      constructor(props) {
        super(props);

        this.state = {
          show: false,
          displayName: 'Name',
          resourceType: 'Type',
          receiveDelete: null
        };
      }

      showConfirmDeleteModal(displayName: string, resourceType: string, receiveDelete: Function) {
        this.setState({
          show: true,
          displayName: displayName,
          resourceType: resourceType,
          receiveDelete: receiveDelete
        });
      }

      close() {
        this.setState({show: false});
      }

      render() {
        return <React.Fragment>
          {this.state.show && <ConfirmDeleteModal
            resourceName={this.state.displayName}
            resourceType={this.state.resourceType}
            receiveDelete={() => this.state.receiveDelete(() => this.close())}
            closeFunction={() => this.close()} />
          }

          <WrappedComponent showConfirmDeleteModal={(displayName, resourceType, receiveDelete) => {
            this.showConfirmDeleteModal(displayName, resourceType, receiveDelete)}}
                            {...this.props} />
        </React.Fragment>
      }
    };
  };
};