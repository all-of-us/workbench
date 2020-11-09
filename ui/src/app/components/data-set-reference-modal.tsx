import * as fp from 'lodash/fp';
import * as React from 'react';

import {datasetApi} from 'app/services/swagger-fetch-clients';
import {reactStyles} from 'app/utils';
import {getDisplayName, getId, getType, getTypeString} from 'app/utils/resources';
import {WorkspaceResource} from 'generated/fetch';
import {Button} from './buttons';
import {Modal, ModalBody, ModalTitle} from './modals';

const styles = reactStyles({
  resource: {
    fontWeight: 'bold',
  },
  datasets: {
    fontWeight: 'bold',
  }
});

interface Props {
  referencedResource: WorkspaceResource;
  datasets: string;
  onCancel: () => void;
  deleteResource: () => Promise<void>;
}

class DatasetReferenceModal extends React.Component<Props, {}> {

  constructor(props: Props) {
    super(props);
  }

  async markAndDelete() {
    const {referencedResource, deleteResource} = this.props;
    try {
      await datasetApi().markDirty(
        referencedResource.workspaceNamespace,
        referencedResource.workspaceFirecloudName, {
          id: getId(referencedResource),
          resourceType: getType(referencedResource)
        });
      await deleteResource();
    } catch (ex) {
      console.log(ex);
    }
  }

  render() {
    const {referencedResource, datasets, onCancel} = this.props;

    const resourceName = fp.startCase(getDisplayName(referencedResource));
    const resourceElem = <span style={styles.resource}>{resourceName}</span>;
    const resourceWithTypeElem = <span>{getTypeString(referencedResource)} {resourceElem}</span>;
    const datasetsElem = <span style={styles.datasets}>{datasets}</span>;

    return <Modal>
            <ModalTitle>WARNING</ModalTitle>
            <ModalBody>
                <div style={{paddingBottom: '1rem'}}>
                    The {resourceWithTypeElem} is referenced by the following datasets: {datasetsElem}.
                    Deleting the {resourceWithTypeElem} will make these datasets unavailable for use.
                    Are you sure you want to delete {resourceElem} ?
                </div>
                <div style={{float: 'right'}}>
                    <Button type='secondary' style={{marginRight: '2rem'}} onClick={onCancel}>Cancel</Button>
                    <Button type='primary' onClick={() => this.markAndDelete()}>YES, DELETE</Button>
                </div>
            </ModalBody>
        </Modal>;
  }
}

export {
    DatasetReferenceModal
};
