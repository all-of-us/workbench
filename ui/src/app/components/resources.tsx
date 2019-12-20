import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {SnowmanIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {switchCase} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {ResourceType} from "generated/fetch";

export interface ResourceCardMenuProps {
  resourceType: ResourceType;
  onRenameResource?: Function;
  onCopyConceptSet?: Function;
  canDelete: boolean;
  onDeleteResource?: Function;
  canEdit: boolean;
  onEdit?: Function;
  onExportDataSet: Function;
}

export class ResourceCardMenu extends React.Component<ResourceCardMenuProps> {
  render() {
    return <PopupTrigger
      data-test-id='resource-card-menu'
      side='bottom'
      closeOnClick
      content={
        switchCase(this.props.resourceType,
          [ResourceType.COHORTREVIEW, () => {
            return <React.Fragment>
              <MenuItem icon='note'
                        onClick={this.props.onRenameResource}
                        disabled={!this.props.canEdit}
              >
                Rename
              </MenuItem>
              <MenuItem icon='trash'
                        onClick={this.props.onDeleteResource}
                        disabled={!this.props.canDelete}
              >
                Delete
              </MenuItem>
            </React.Fragment>;
          }],
          [ResourceType.CONCEPTSET, () => {
            return <React.Fragment>
              <MenuItem icon='pencil'
                        onClick={this.props.onEdit}
                        disabled={!this.props.canEdit}
              >
                Rename
              </MenuItem>
              <MenuItem icon='copy'
                        onClick={this.props.onCopyConceptSet}
              >
                Copy to another workspace
              </MenuItem>
              <MenuItem icon='trash'
                        onClick={this.props.onDeleteResource}
                        disabled={!this.props.canDelete}
              >
                Delete
              </MenuItem>
            </React.Fragment>;
          }],
          [ResourceType.DATASET, () => {
            return <React.Fragment>
              <MenuItem icon='pencil'
                        onClick={() => {
                          AnalyticsTracker.DatasetBuilder.OpenRenameModal();
                          this.props.onRenameResource();
                        }}
                        disabled={!this.props.canEdit}
              >
                Rename Dataset
              </MenuItem>
              <MenuItem icon='pencil'
                        onClick={() => {
                          AnalyticsTracker.DatasetBuilder.OpenEditPage('From Card Snowman');
                          this.props.onEdit();
                        }}
                        disabled={!this.props.canEdit}
              >
                Edit
              </MenuItem>
              <MenuItem icon='clipboard'
                        onClick={() => {
                          AnalyticsTracker.DatasetBuilder.OpenExportModal();
                          this.props.onExportDataSet();
                        }}
                        disabled={!this.props.canEdit}
              >
                Export to Notebook
              </MenuItem>
              <MenuItem icon='trash'
                        onClick={() => {
                          AnalyticsTracker.DatasetBuilder.OpenDeleteModal();
                          this.props.onDeleteResource();
                        }}
                        disabled={!this.props.canDelete}
              >
                Delete
              </MenuItem>
            </React.Fragment>;
          }]
        )
      }
    >
      <Clickable data-test-id='resource-menu'>
        <SnowmanIcon />
      </Clickable>
    </PopupTrigger>;
  }
}
