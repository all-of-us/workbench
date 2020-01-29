import {RenameModal} from 'app/components/rename-modal';
import {Action, ResourceCardTemplate} from 'app/components/resource-card-template';
import {withConfirmDeleteModal, WithConfirmDeleteModalProps} from 'app/components/with-confirm-delete-modal';
import {withErrorModal, WithErrorModalProps} from 'app/components/with-error-modal';
import {withSpinnerOverlay, WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {ExportDataSetModal} from 'app/pages/data/data-set/export-data-set-modal';
import {dataSetApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {formatRecentResourceDisplayDate} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {navigate} from 'app/utils/navigation';
import {toDisplay} from 'app/utils/resourceActions';
import {RecentResource, ResourceType} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';

interface Props extends WithConfirmDeleteModalProps, WithErrorModalProps, WithSpinnerOverlayProps {
  resource: RecentResource;
  existingNameList: string[];
  onUpdate: Function;
}

interface State {
  showRenameModal: boolean;
  showExportToNotebookModal: boolean;
}

export const DatasetResourceCard = fp.flow(
  withErrorModal(),
  withConfirmDeleteModal(),
  withSpinnerOverlay(),
)(class extends React.Component<Props, State> {

  constructor(props: Props) {
    super(props);
    this.state = {
      showRenameModal: false,
      showExportToNotebookModal: false
    };
  }

  get resourceType(): ResourceType {
    return ResourceType.DATASET;
  }

  get displayName(): string {
    return this.props.resource.dataSet.name;
  }

  get canWrite(): boolean {
    return this.props.resource.permission === 'OWNER'
      || this.props.resource.permission === 'WRITER';
  }

  get canDelete(): boolean {
    return this.props.resource.permission === 'OWNER';
  }

  get resourceUrl(): string {
    const {workspaceNamespace, workspaceFirecloudName, dataSet} = this.props.resource;
    const workspacePrefix = `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}`;
    return `${workspacePrefix}/data/data-sets/${dataSet.id}`;
  }

  get actions(): Action[] {
    return [
      {
        icon: 'pencil',
        displayName: 'Rename Dataset',
        onClick: () => {
          AnalyticsTracker.DatasetBuilder.OpenRenameModal();
          this.setState({showRenameModal: true});
        },
        disabled: !this.canWrite
      },
      {
        icon: 'pencil',
        displayName: 'Edit',
        onClick: () => {
          AnalyticsTracker.DatasetBuilder.OpenEditPage('From Card Snowman');
          navigate(['workspaces',
            this.props.resource.workspaceNamespace,
            this.props.resource.workspaceFirecloudName,
            'data', 'data-sets', this.props.resource.dataSet.id]);
        },
        disabled: !this.canWrite
      },
      {
        icon: 'pencil',
        displayName: 'Export to Notebook',
        onClick: () => {
          AnalyticsTracker.DatasetBuilder.OpenExportModal();
          this.setState({showExportToNotebookModal: true});
        },
        disabled: !this.canWrite
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          AnalyticsTracker.DatasetBuilder.OpenDeleteModal();
          this.props.showConfirmDeleteModal(this.displayName,
            this.resourceType, () => this.delete());
        },
        disabled: !this.canDelete
      }
    ];
  }

  delete() {
    AnalyticsTracker.DatasetBuilder.Delete();
    return dataSetApi().deleteDataSet(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      this.props.resource.dataSet.id
    ).then(() => {
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
      conceptSetIds: dataset.conceptSets.map(concept => concept.id),
      cohortIds: dataset.cohorts.map(cohort => cohort.id)
    };

    return dataSetApi().updateDataSet(
      this.props.resource.workspaceNamespace,
      this.props.resource.workspaceFirecloudName,
      dataset.id,
      request
    ).then(() => {
      this.props.onUpdate();
    }).catch(error => console.error(error)
    ).finally(() => {
      this.setState({showRenameModal: false});
    });
  }

  render() {
    return <React.Fragment>
      {this.state.showExportToNotebookModal &&
      <ExportDataSetModal dataSet={this.props.resource.dataSet}
                          workspaceNamespace={this.props.resource.workspaceNamespace}
                          workspaceFirecloudName={this.props.resource.workspaceFirecloudName}
                          closeFunction={() => this.setState({showExportToNotebookModal: false})}/>
      }
      {this.state.showRenameModal &&
      <RenameModal onRename={(name, description) => this.rename(name, description)}
                   resourceType={this.resourceType}
                   onCancel={() => this.setState({showRenameModal: false})}
                   oldDescription={this.props.resource.dataSet.description}
                   oldName={this.displayName}
                   existingNames={this.props.existingNameList}/>
      }

      <ResourceCardTemplate
        actions={this.actions}
        disabled={!this.canWrite}
        resourceUrl={this.resourceUrl}
        displayName={this.displayName}
        description={this.props.resource.dataSet.description}
        displayDate={formatRecentResourceDisplayDate(this.props.resource.modifiedTime)}
        footerText={toDisplay(this.resourceType)}
        footerColor={colors.resourceCardHighlights.dataSet}
      />
    </React.Fragment>;
  }
});
