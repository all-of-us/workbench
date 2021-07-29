import * as fp from 'lodash/fp';
import * as React from 'react';

import {DataSetReferenceModal} from 'app/components/data-set-reference-modal';
import {RenameModal} from 'app/components/rename-modal';
import {Action, ResourceActionsMenu} from 'app/components/resource-actions-menu';
import {canDelete, canWrite, ResourceCard} from 'app/components/resource-card';
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from 'app/components/with-confirm-delete-modal';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {withSpinnerOverlay, WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {cohortsApi, dataSetApi} from 'app/services/swagger-fetch-clients';
import {NavigationProps} from 'app/utils/navigation';
import {getDescription, getDisplayName, getId, getResourceUrl, getType} from 'app/utils/resources';
import {DataSet, WorkspaceResource} from 'generated/fetch';
import {withNavigation} from '../../../utils/navigation-wrapper';

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps, NavigationProps {
  resource: WorkspaceResource;
  existingNameList: string[];
  onUpdate: () => Promise<void>;
  menuOnly: boolean;
}

interface State {
  showRenameModal: boolean;
  referencingDataSets: Array<DataSet>;
}

export const CohortResourceCard = fp.flow(
  withErrorModal(),
  withConfirmDeleteModal(),
  withSpinnerOverlay(),
  withNavigation
)(class extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      showRenameModal: false,
      referencingDataSets: [],
    };
  }

  get reviewUrlForCohort(): string {
    const {workspaceNamespace, workspaceFirecloudName, cohort} = this.props.resource;

    return `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}` +
      `/data/cohorts/${cohort.id}/review`;
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
        displayName: 'Duplicate',
        onClick: () => this.duplicate(),
        disabled: !canWrite(resource)
      },
      {
        icon: 'pencil',
        displayName: 'Edit',
        onClick: () => this.props.navigateByUrl(getResourceUrl(resource)),
        disabled: !canWrite(resource)
      },
      {
        icon: 'grid-view',
        displayName: 'Review',
        onClick: () => this.props.navigateByUrl(this.reviewUrlForCohort),
        disabled: !canWrite(resource)
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          this.props.showConfirmDeleteModal(getDisplayName(resource),
            getType(resource), () => this.maybeDelete());
        },
        disabled: !canDelete(resource)
      }
    ];
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
            return this.deleteCohort();
          }
        });
  }

  deleteCohort() {
    return cohortsApi().deleteCohort(
        this.props.resource.workspaceNamespace,
        this.props.resource.workspaceFirecloudName,
        this.props.resource.cohort.id)
        .then(() => {
          this.props.onUpdate();
        });
  }

  duplicate() {
    this.props.showSpinner();

    return cohortsApi().duplicateCohort(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      {
        originalCohortId: this.props.resource.cohort.id,
        newName: `Duplicate of ${getDisplayName(this.props.resource)}`
      }
    ).then(() => {
      this.props.onUpdate();
    }).catch(e => {
      this.props.showErrorModal('Duplicating Cohort Error',
        'Cohort with the same name already exists.');
    }).finally(() => {
      this.props.hideSpinner();
    });
  }

  rename(name, description) {
    const request = {
      ...this.props.resource.cohort,
      name: name,
      description: description
    };

    return cohortsApi().updateCohort(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      this.props.resource.cohort.id,
      request
    ).then(() => {
      this.props.onUpdate();
    }).catch(error => console.error(error)
    ).finally(() => {
      this.setState({showRenameModal: false});
    });
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
      {this.state.referencingDataSets.length > 0 && <DataSetReferenceModal
          referencedResource={resource}
          dataSets={fp.join(', ' , this.state.referencingDataSets.map((data) => data.name))}
          onCancel={() => {
            this.setState({referencingDataSets: []});
          }}
          deleteResource={() => {
            this.setState({referencingDataSets: []});
            return this.deleteCohort();
          }}/>
      }
      {menuOnly ?
          <ResourceActionsMenu actions={this.actions}/> :
          <ResourceCard resource={resource} actions={this.actions}/>}
    </React.Fragment>;
  }
});
