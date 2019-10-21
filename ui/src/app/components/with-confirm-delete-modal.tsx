import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {ResourceType} from 'app/utils/resourceActions';
import * as React from 'react';

interface State {
  show: boolean;
  displayName: string;
  resourceType: ResourceType;
  receiveDelete: () => Promise<void>;
}

export interface WithConfirmDeleteModalProps {
  showConfirmDeleteModal: (displayName: string,
                           resourceType: ResourceType,
                           receiveDelete: () => Promise<void>) => void;
}

export const withConfirmDeleteModal = () => {
  return (WrappedComponent) => {
    return class ConfirmDeleteModalWrapper extends React.Component<any, State> {

      constructor(props) {
        super(props);

        this.state = {
          show: false,
          displayName: 'Name',
          resourceType: null,
          receiveDelete: null
        };
      }

      show(displayName, resourceType, receiveDelete) {
        this.setState({
          show: true,
          displayName: displayName,
          resourceType: resourceType,
          receiveDelete: receiveDelete
        });
      }

      hide() {
        this.setState({show: false});
      }

      render() {
        return <React.Fragment>
          {this.state.show && <ConfirmDeleteModal
            resourceName={this.state.displayName}
            resourceType={this.state.resourceType}
            receiveDelete={() => this.state.receiveDelete().then(() => this.hide())}
            closeFunction={() => this.hide()} />
          }

          <WrappedComponent showConfirmDeleteModal={(displayName, resourceType, receiveDelete) =>
            this.show(displayName, resourceType, receiveDelete)}
                            {...this.props} />
        </React.Fragment>;
      }
    };
  };
};
