import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {switchCase} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActionsReact';

export const ResourceCardMenu: React.FunctionComponent<{
  disabled: boolean, resourceType: ResourceType, onRenameCohort?: Function,
  onCloneResource?: Function, onDeleteResource?: Function, onEdit?: Function,
  onExportDataSet: Function, onReviewCohort?: Function, onRenameDataSet?: Function
}> = ({
        disabled, resourceType, onRenameCohort = () => {}, onCloneResource = () => {},
        onDeleteResource = () => {}, onEdit = () => {}, onExportDataSet = () => {},
        onReviewCohort = () => {}, onRenameDataSet = () => {}
      }) => {
  return <PopupTrigger
    data-test-id='resource-card-menu'
    side='bottom'
    closeOnClick
    content={
      switchCase(resourceType,
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
               style={{color: disabled ? colorWithWhiteness(colors.dark, 0.6) : colors.accent,
                 marginLeft: -9, cursor: disabled ? 'auto' : 'pointer'}}/>
    </Clickable>
  </PopupTrigger>;
};
