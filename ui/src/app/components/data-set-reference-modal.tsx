import * as fp from 'lodash/fp';
import * as React from 'react';

import {dataSetApi} from 'app/services/swagger-fetch-clients';
import {getDisplayName, getId, getType, getTypeString} from 'app/utils/resources';
import {WorkspaceResource} from 'generated/fetch';
import {Button} from './buttons';
import {Modal, ModalBody, ModalTitle} from './modals';

interface Props {
  referencedResource: WorkspaceResource;
  dataSets: string;
  onCancel: Function;
  deleteResource: Function;
}

class DataSetReferenceModal extends React.Component<Props, {}> {

  constructor(props: Props) {
    super(props);
    this.state = {
      showRenameModal: false,
      copyingConceptSet: false,
      dataSetByResourceIdList: []
    };
  }

  async markDataSetDirty() {
    const {referencedResource, deleteResource} = this.props;
    try {
      await dataSetApi().markDirty(
        referencedResource.workspaceNamespace,
        referencedResource.workspaceFirecloudName, {
          id: getId(referencedResource),
          resourceType: getType(referencedResource)
        });
      deleteResource();
    } catch (ex) {
      console.log(ex);
    }
  }

  render() {
    const {referencedResource, dataSets, onCancel} = this.props;
    return <Modal>
            <ModalTitle>WARNING</ModalTitle>
            <ModalBody>
                <div style={{paddingBottom: '1rem'}}>
                    The {getTypeString(referencedResource)}
                    <b>{fp.startCase(getDisplayName(referencedResource))}&nbsp;</b>
                    is referenced by the following datasets:
                    <b>&nbsp;{dataSets}</b>.
                    Deleting the {getTypeString(referencedResource)}
                    <b>{fp.startCase(getDisplayName(referencedResource))} </b> will make these datasets unavailable for
                    use.
                    Are you sure you want to delete <b>{fp.startCase(getDisplayName(referencedResource))}</b> ?
                </div>
                <div style={{float: 'right'}}>
                    <Button type='secondary' style={{marginRight: '2rem'}} onClick={onCancel}>
                        Cancel
                    </Button>
                    <Button type='primary' onClick={() => this.markDataSetDirty()}>YES, DELETE</Button>
                </div>
            </ModalBody>
        </Modal>;
  }
}

export {
    DataSetReferenceModal
};
