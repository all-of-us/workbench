import * as fp from 'lodash/fp';
import * as React from 'react';

import {CopyModal} from 'app/components/copy-modal';
import {DataSetReferenceModal} from 'app/components/data-set-reference-modal';
import {RenameModal} from 'app/components/rename-modal';
import {Action, canDelete, canWrite, ResourceCardTemplate} from 'app/components/resource-card-template';
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from 'app/components/with-confirm-delete-modal';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {withSpinnerOverlay, WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {conceptSetsApi, dataSetApi} from 'app/services/swagger-fetch-clients';
import {getDescription, getDisplayName, getId, getType} from 'app/utils/resources';
import {CopyRequest, DataSet, WorkspaceResource} from 'generated/fetch';

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: Function;
}

interface State {
  showRenameModal: boolean;
  copyingConceptSet: boolean;
  dataSetByResourceIdList: Array<DataSet>;
}

export const ConceptSetResourceCard = fp.flow(
  withErrorModal(),
  withConfirmDeleteModal(),
  withSpinnerOverlay(),
)(class extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      showRenameModal: false,
      copyingConceptSet: false,
      dataSetByResourceIdList: []
    };
  }

  async getDataSetByResourceId() {
    const {resource} = this.props;
    if (this.state.dataSetByResourceIdList.length === 0) {
      const dataSetList = await dataSetApi().getDataSetByResourceId(
        resource.workspaceNamespace,
        resource.workspaceFirecloudName,
        getType(resource),
        getId(resource));
      return dataSetList.items;
    } else {
      this.setState({dataSetByResourceIdList: []});
    }
    return false;
  }

  get actions(): Action[] {
    const {resource} = this.props;
    return [
      {
        icon: 'note',
        displayName: 'Rename',
        onClick: () => {
          this.setState({showRenameModal: true});
        },
        disabled: !canWrite(resource)
      },
      {
        icon: 'copy',
        displayName: 'Copy to another workspace',
        onClick: () => this.setState({copyingConceptSet: true}),
        disabled: !canDelete(resource)
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          this.props.showConfirmDeleteModal(getDisplayName(resource),
            getType(resource), () => this.delete());
        },
        disabled: !canDelete(resource)
      },
    ];
  }

  rename(name, description) {
    const {resource} = this.props;
    const request = {
      ...resource.conceptSet,
      name: name,
      description: description
    };
    conceptSetsApi().updateConceptSet(
        resource.workspaceNamespace,
        resource.workspaceFirecloudName,
        resource.conceptSet.id,
        request
    ).then(() => {
      this.props.onUpdate();
    }).catch(error => console.error(error)
    ).finally(() => {
      this.setState({showRenameModal: false});
    });
  }

  async delete() {
    const {resource} = this.props;
    const dataSetByResourceIdList = await this.getDataSetByResourceId();
    if (dataSetByResourceIdList && dataSetByResourceIdList.length > 0) {
      this.setState({dataSetByResourceIdList: dataSetByResourceIdList});
      return;
    }
    conceptSetsApi().deleteConceptSet(
        resource.workspaceNamespace,
        resource.workspaceFirecloudName,
        resource.conceptSet.id)
        .then(() => {
          this.props.onUpdate();
        });
  }

  async copyConceptSet(copyRequest: CopyRequest) {
    const {resource} = this.props;
    return conceptSetsApi().copyConceptSet(resource.workspaceNamespace,
      resource.workspaceFirecloudName,
      resource.conceptSet.id.toString(), copyRequest);
  }

  render() {
    const {resource} = this.props;
    return <React.Fragment>
      {this.state.showRenameModal &&
        <RenameModal onRename={(name, description) => this.rename(name, description)}
                   resourceType={getType(resource)}
                   onCancel={() => this.setState({showRenameModal: false})}
                   oldDescription={getDescription(resource)}
                   oldName={getDisplayName(resource)}
                   existingNames={this.props.existingNameList}/>
      }
      {this.state.copyingConceptSet && <CopyModal
          fromWorkspaceNamespace={resource.workspaceNamespace}
          fromWorkspaceFirecloudName={resource.workspaceFirecloudName}
          fromResourceName={resource.conceptSet.name}
          fromCdrVersionId={resource.cdrVersionId}
          resourceType={getType(resource)}
          onClose={() => this.setState({copyingConceptSet: false})}
          onCopy={() => this.props.onUpdate()}
          saveFunction={(copyRequest: CopyRequest) => this.copyConceptSet(copyRequest)}/>
      }
      {this.state.dataSetByResourceIdList.length > 0 && <DataSetReferenceModal
          referencedResource={resource}
          dataSets={fp.join(', ' , this.state.dataSetByResourceIdList.map((data) => data.name))}
          onCancel={() => {
            this.setState({dataSetByResourceIdList: []});
          }}
          deleteResource={() => this.delete()}/>
      }

     <ResourceCardTemplate
        actions={this.actions}
        disabled={!canWrite(resource)}
        resource={resource}
      />
    </React.Fragment>;
  }
});
