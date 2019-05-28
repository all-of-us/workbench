import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {switchCase} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActionsReact';
import {environment} from 'environments/environment';

export const ResourceCardMenu: React.FunctionComponent<{
  disabled: boolean, resourceType: ResourceType, onRenameNotebook?: Function,
  onRenameCohort?: Function, onOpenJupyterLabNotebook?: any, onCloneResource?: Function,
  onCopyResource?: Function, onDeleteResource?: Function, onEdit?: Function,
  onExportDataSet: Function, onReviewCohort?: Function, onRenameDataSet?: Function
}> = ({
        disabled, resourceType, onRenameNotebook = () => {}, onRenameCohort = () => {},
        onOpenJupyterLabNotebook = () => {}, onCloneResource = () => {}, onCopyResource = () => {},
        onDeleteResource = () => {}, onEdit = () => {}, onExportDataSet = () => {},
        onReviewCohort = () => {}, onRenameDataSet = () => {}
      }) => {
  return <PopupTrigger
    data-test-id='resource-card-menu'
    side='bottom'
    closeOnClick
    content={
      switchCase(resourceType,
        ['notebook', () => {
          return <React.Fragment>
            <MenuItem icon='pencil' onClick={onRenameNotebook}>Rename</MenuItem>
            <MenuItem icon='copy' onClick={onCloneResource}>Duplicate</MenuItem>
            <MenuItem icon='copy' onClick={onCopyResource}>Copy to another Workspace</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
            {
              environment.enableJupyterLab &&
              /*
               This does not support both playground mode and jupyterLab yet,
               that is a work in progress. We do not need to worry about that
               here, because the menu will not open if you do not have write
               access, and playground mode is currently only enabled if you do
               not have write access.
              */
              <MenuItem icon='grid-view' onClick={onOpenJupyterLabNotebook}>
                Open in Jupyter Lab
              </MenuItem>
            }
          </React.Fragment>;
        }],
        ['cohort', () => {
          return <React.Fragment>
            <MenuItem icon='note' onClick={onRenameCohort}>Rename</MenuItem>
            <MenuItem icon='copy' onClick={onCloneResource}>Duplicate</MenuItem>
            <MenuItem icon='pencil' onClick={onEdit}>Edit</MenuItem>
            <MenuItem icon='grid-view' onClick={onReviewCohort}>Review</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }],
        ['conceptSet', () => {
          return <React.Fragment>
            <MenuItem icon='pencil' onClick={onEdit}>Rename</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }],
        ['dataSet', () => {
          return <React.Fragment>
            <MenuItem icon='pencil' onClick={onRenameDataSet}>Rename Data Set</MenuItem>
            <MenuItem icon='pencil' onClick={onEdit}>Edit</MenuItem>
            <MenuItem icon='clipboard' onClick={onExportDataSet}>Export to Notebook</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }]
      )
    }
  >
    <Clickable disabled={disabled} data-test-id='resource-menu'>
      <ClrIcon shape='ellipsis-vertical' size={21}
               style={{color: disabled ? '#9B9B9B' : '#2691D0', marginLeft: -9,
                 cursor: disabled ? 'auto' : 'pointer'}}/>
    </Clickable>
  </PopupTrigger>;
};
