import * as React from 'react';
import * as fp from 'lodash/fp';
import { faDna } from '@fortawesome/free-solid-svg-icons';

import { PrePackagedConceptSetEnum } from 'generated/fetch';

import { RenameModal } from 'app/components/rename-modal';
import {
  Action,
  ResourceActionsMenu,
} from 'app/components/resource-actions-menu';
import { CommonActionMenuProps } from 'app/components/resources/resource-action-menu';
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
import { GenomicExtractionModal } from 'app/pages/data/data-set/genomic-extraction-modal';
import { dataSetApi } from 'app/services/swagger-fetch-clients';
import { AnalyticsTracker } from 'app/utils/analytics';
import { NavigationProps } from 'app/utils/navigation';
import {
  canDelete,
  canWrite,
  getDescription,
  getDisplayName,
  getType,
} from 'app/utils/resources';
import { ACTION_DISABLED_INVALID_BILLING } from 'app/utils/strings';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';

import { ExportDatasetModal } from './export-dataset-modal';

interface Props
  extends CommonActionMenuProps,
    WithConfirmDeleteModalProps,
    WithErrorModalProps,
    WithSpinnerOverlayProps,
    NavigationProps {
  workspace: WorkspaceData;
  inactiveBilling: boolean;
}

interface State {
  showRenameModal: boolean;
  showExportToNotebookModal: boolean;
  showGenomicExtractionModal: boolean;
}

export const DatasetResourceCard = fp.flow(
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
        showExportToNotebookModal: false,
        showGenomicExtractionModal: false,
      };
    }

    get actions(): Action[] {
      const { resource, inactiveBilling } = this.props;
      const enableExtraction = (
        resource.dataSet.prePackagedConceptSet || []
      ).includes(PrePackagedConceptSetEnum.WHOLE_GENOME);
      return [
        {
          icon: 'pencil',
          displayName: 'Rename Dataset',
          onClick: () => {
            AnalyticsTracker.DatasetBuilder.OpenRenameModal();
            this.setState({ showRenameModal: true });
          },
          disabled: !canWrite(resource),
        },
        {
          icon: 'pencil',
          displayName: 'Edit',
          onClick: () => {
            AnalyticsTracker.DatasetBuilder.OpenEditPage('From Card Snowman');
            this.props.navigate([
              'workspaces',
              resource.workspaceNamespace,
              resource.workspaceFirecloudName,
              'data',
              'data-sets',
              resource.dataSet.id,
            ]);
          },
          disabled: !canWrite(resource),
        },
        {
          icon: 'clipboard',
          displayName: 'Export to Notebook',
          onClick: () => {
            AnalyticsTracker.DatasetBuilder.OpenExportModal();
            this.setState({ showExportToNotebookModal: true });
          },
          disabled: inactiveBilling || !canWrite(resource),
          hoverText: inactiveBilling && ACTION_DISABLED_INVALID_BILLING,
        },
        ...(enableExtraction
          ? [
              {
                faIcon: faDna,
                displayName: 'Extract VCF Files',
                onClick: () => {
                  AnalyticsTracker.DatasetBuilder.OpenGenomicExtractionModal(
                    'From Card Snowman'
                  );
                  this.setState({ showGenomicExtractionModal: true });
                },
                disabled: inactiveBilling || !canWrite(resource),
                hoverText: inactiveBilling && ACTION_DISABLED_INVALID_BILLING,
              },
            ]
          : []),
        {
          icon: 'trash',
          displayName: 'Delete',
          onClick: () => {
            AnalyticsTracker.DatasetBuilder.OpenDeleteModal();
            this.props.showConfirmDeleteModal(
              getDisplayName(resource),
              getType(resource),
              () => this.delete()
            );
          },
          disabled: !canDelete(resource),
        },
      ];
    }

    delete() {
      AnalyticsTracker.DatasetBuilder.Delete();
      return dataSetApi()
        .deleteDataSet(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          this.props.resource.dataSet.id
        )
        .then(() => {
          this.props.onUpdate();
        });
    }

    rename(name, description) {
      AnalyticsTracker.DatasetBuilder.Rename();
      const dataset = this.props.resource.dataSet;

      const request = {
        ...dataset,
        name: name,
        description: description,
        domainValuePairs: [],
      };

      return dataSetApi()
        .updateDataSet(
          this.props.resource.workspaceNamespace,
          this.props.resource.workspaceFirecloudName,
          dataset.id,
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
      const { resource, workspace } = this.props;
      return (
        <React.Fragment>
          {this.state.showExportToNotebookModal && (
            <ExportDatasetModal
              {...{ workspace }}
              dataset={resource.dataSet}
              closeFunction={() =>
                this.setState({ showExportToNotebookModal: false })
              }
            />
          )}
          {this.state.showGenomicExtractionModal && (
            <GenomicExtractionModal
              dataSet={resource.dataSet}
              workspaceNamespace={resource.workspaceNamespace}
              workspaceFirecloudName={resource.workspaceFirecloudName}
              closeFunction={() =>
                this.setState({ showGenomicExtractionModal: false })
              }
            />
          )}
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
          <ResourceActionsMenu
            actions={this.actions}
            disabled={resource.adminLocked}
          />
        </React.Fragment>
      );
    }
  }
);
