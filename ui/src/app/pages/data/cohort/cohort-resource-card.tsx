import * as React from 'react';
import * as fp from 'lodash/fp';

import { DataSet } from 'generated/fetch';

import { DataSetReferenceModal } from 'app/components/data-set-reference-modal';
import { RenameModal } from 'app/components/rename-modal';
import {
  Action,
  ResourceActionsMenu,
} from 'app/components/resource-actions-menu';
import { ResourceActionMenuProps } from 'app/components/resources/render-resource-menu';
import {
  withConfirmDeleteModal,
  WithConfirmDeleteModalProps,
} from 'app/components/with-confirm-delete-modal';
import {
  WithErrorModalProps,
  withErrorModalWrapper,
} from 'app/components/with-error-modal-wrapper';
import {
  withSpinnerOverlay,
  WithSpinnerOverlayProps,
} from 'app/components/with-spinner-overlay';
import { dataTabPath } from 'app/routing/utils';
import { cohortsApi, dataSetApi } from 'app/services/swagger-fetch-clients';
import { NavigationProps } from 'app/utils/navigation';
import {
  canDelete,
  canWrite,
  getDescription,
  getDisplayName,
  getId,
  getResourceUrl,
  getType,
} from 'app/utils/resources';
import { withNavigation } from 'app/utils/with-navigation-hoc';

interface Props
  extends ResourceActionMenuProps,
    WithConfirmDeleteModalProps,
    WithErrorModalProps,
    WithSpinnerOverlayProps,
    NavigationProps {}

interface State {
  showRenameModal: boolean;
  referencingDataSets: Array<DataSet>;
}

export const CohortResourceCard = fp.flow(
  withErrorModalWrapper(),
  withConfirmDeleteModal(),
  withSpinnerOverlay(),
  withNavigation
)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        showRenameModal: false,
        referencingDataSets: [],
      };
    }

    get reviewUrlForCohort(): string {
      const { workspaceNamespace, workspaceFirecloudName, cohort } =
        this.props.resource;

      return (
        dataTabPath(workspaceNamespace, workspaceFirecloudName) +
        `/cohorts/${cohort.id}/reviews`
      );
    }

    get actions(): Action[] {
      const { resource } = this.props;
      return [
        {
          icon: 'note',
          displayName: 'Rename',
          onClick: () => {
            this.setState({ showRenameModal: true });
          },
          disabled: !canWrite(resource),
        },
        {
          icon: 'copy',
          displayName: 'Duplicate',
          onClick: () => this.duplicate(),
          disabled: !canWrite(resource),
        },
        {
          icon: 'pencil',
          displayName: 'Edit',
          onClick: () => {
            const urlObj = getResourceUrl(resource);
            this.props.navigateByUrl(urlObj.url, urlObj);
          },
          disabled: !canWrite(resource),
        },
        {
          icon: 'grid-view',
          displayName: 'Review',
          onClick: () => {
            this.props.navigateByUrl(this.reviewUrlForCohort);
          },
          disabled: !canWrite(resource),
        },
        {
          icon: 'trash',
          displayName: 'Delete',
          onClick: () => {
            this.props.showConfirmDeleteModal(
              getDisplayName(resource),
              getType(resource),
              () => this.maybeDelete()
            );
          },
          disabled: !canDelete(resource),
        },
      ];
    }

    // check if there are any referencing data sets, and pop up a modal if so;
    // if not, continue with deletion
    maybeDelete() {
      const { resource } = this.props;
      return dataSetApi()
        .getDataSetByResourceId(
          resource.workspaceNamespace,
          resource.workspaceFirecloudName,
          getId(resource),
          { resourceType: getType(resource) }
        )
        .then((dataSetList) => {
          if (dataSetList && dataSetList.items.length > 0) {
            this.setState({ referencingDataSets: dataSetList.items });
          } else {
            return this.deleteCohort();
          }
        });
    }

    deleteCohort() {
      return cohortsApi()
        .deleteCohort(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          this.props.resource.cohort.id
        )
        .then(() => {
          this.props.onUpdate();
        });
    }

    duplicate() {
      this.props.showSpinner();

      return cohortsApi()
        .duplicateCohort(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          {
            originalCohortId: this.props.resource.cohort.id,
            newName: `Duplicate of ${getDisplayName(this.props.resource)}`,
          }
        )
        .then(() => {
          this.props.onUpdate();
        })
        .catch(() => {
          this.props.showErrorModal(
            'Duplicating Cohort Error',
            'Cohort could not be duplicated. Please try again.'
          );
        })
        .finally(() => {
          this.props.hideSpinner();
        });
    }

    rename(name, description) {
      const request = {
        ...this.props.resource.cohort,
        name: name,
        description: description,
      };

      return cohortsApi()
        .updateCohort(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          this.props.resource.cohort.id,
          request
        )
        .then(() => {
          this.props.onUpdate();
        })
        .catch((error) => console.error(error))
        .finally(() => {
          this.setState({ showRenameModal: false });
        });
    }

    render() {
      const { resource } = this.props;
      return (
        <React.Fragment>
          {this.state.showRenameModal && (
            <RenameModal
              onRename={(name, description) => this.rename(name, description)}
              resourceType={getType(resource)}
              onCancel={() => this.setState({ showRenameModal: false })}
              oldDescription={getDescription(resource)}
              oldName={getDisplayName(resource)}
              existingNames={this.props.existingNameList}
            />
          )}
          {this.state.referencingDataSets.length > 0 && (
            <DataSetReferenceModal
              referencedResource={resource}
              dataSets={fp.join(
                ', ',
                this.state.referencingDataSets.map((data) => data.name)
              )}
              onCancel={() => {
                this.setState({ referencingDataSets: [] });
              }}
              deleteResource={() => {
                this.setState({ referencingDataSets: [] });
                return this.deleteCohort();
              }}
            />
          )}
          <ResourceActionsMenu
            actions={this.actions}
            disabled={resource.adminLocked}
          />
        </React.Fragment>
      );
    }
  }
);
