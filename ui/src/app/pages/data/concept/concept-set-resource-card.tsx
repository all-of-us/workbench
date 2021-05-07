import * as fp from 'lodash/fp';
import * as React from 'react';

import {CopyModal} from 'app/components/copy-modal';
import {DataSetReferenceModal} from 'app/components/data-set-reference-modal';
import {RenameModal} from 'app/components/rename-modal';
import {Action, ResourceActionsMenu} from 'app/components/resource-actions-menu';
import {canDelete, canWrite, ResourceCard} from 'app/components/resource-card';
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from 'app/components/with-confirm-delete-modal';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {withSpinnerOverlay, WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {conceptSetsApi, dataSetApi} from 'app/services/swagger-fetch-clients';
import {getDescription, getDisplayName, getId, getType} from 'app/utils/resources';
import {CopyRequest, DataSet, WorkspaceResource} from 'generated/fetch';

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
  menuOnly: boolean;
}

interface State {
  showRenameModal: boolean;
  showCopyModal: boolean;
  referencingDataSets: Array<DataSet>;
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
      showCopyModal: false,
      referencingDataSets: []
    };
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
        onClick: () => this.setState({showCopyModal: true}),
        disabled: !canDelete(resource)
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          this.props.showConfirmDeleteModal(getDisplayName(resource),
            getType(resource), () => this.maybeDelete());
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

  // check if there are any referencing data sets, and pop up a modal if so;
  // if not, continue with deletion
  maybeDelete() {
    const {resource} = this.props;
    return dataSetApi().getDataSetByResourceId(
      resource.workspaceNamespace,
      resource.workspaceFirecloudName,
      getType(resource),
      getId(resource))
      .then(dataSetList => {
        if (dataSetList && dataSetList.items.length > 0) {
          this.setState({referencingDataSets: dataSetList.items});
        } else {
          return this.deleteConceptSet();
        }
      });
  }

  deleteConceptSet() {
    const {resource} = this.props;
    return conceptSetsApi().deleteConceptSet(
        resource.workspaceNamespace,
        resource.workspaceFirecloudName,
        resource.conceptSet.id)
        .then(() => {
          this.props.onUpdate();
        });
  }

  async copy(copyRequest: CopyRequest) {
    const {resource} = this.props;
    return conceptSetsApi().copyConceptSet(resource.workspaceNamespace,
      resource.workspaceFirecloudName,
      resource.conceptSet.id.toString(), copyRequest);
  }

  render() {
    const {resource, menuOnly} = this.props;
    return <React.Fragment>
      {this.state.showRenameModal &&
        <RenameModal onRename={(name, description) => this.rename(name, description)}
                   resourceType={getType(resource)}
                   onCancel={() => this.setState({showRenameModal: false})}
                   oldDescription={getDescription(resource)}
                   oldName={getDisplayName(resource)}
                   existingNames={this.props.existingNameList}/>
      }
      {this.state.showCopyModal && <CopyModal
          fromWorkspaceNamespace={resource.workspaceNamespace}
          fromWorkspaceFirecloudName={resource.workspaceFirecloudName}
          fromResourceName={resource.conceptSet.name}
          fromCdrVersionId={resource.cdrVersionId}
          fromAccessTierShortName={resource.accessTierShortName}
          resourceType={getType(resource)}
          onClose={() => this.setState({showCopyModal: false})}
          onCopy={() => this.props.onUpdate()}
          saveFunction={(copyRequest: CopyRequest) => this.copy(copyRequest)}/>
      }
      {this.state.referencingDataSets.length > 0 && <DataSetReferenceModal
          referencedResource={resource}
          dataSets={fp.join(', ' , this.state.referencingDataSets.map((data) => data.name))}
          onCancel={() => {
            this.setState({referencingDataSets: []});
          }}
          deleteResource={() => {
            this.setState({referencingDataSets: []});
            return this.deleteConceptSet();
          }}/>
      }
      {menuOnly ?
          <ResourceActionsMenu actions={this.actions}/> :
          <ResourceCard resource={resource} actions={this.actions}/>}
    </React.Fragment>;
  }
});
