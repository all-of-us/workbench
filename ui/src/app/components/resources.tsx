import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {switchCase} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActionsReact';

export const ResourceCardMenu: React.FunctionComponent<{
  disabled: boolean, resourceType: ResourceType, onRenameResource?: Function,
  onCopyConceptSet?: Function, canDelete: boolean, onDeleteResource?: Function,
  canEdit: boolean, onEdit?: Function, onExportDataSet: Function,
}> = ({
        disabled, resourceType, onRenameResource = () => {},
        onCopyConceptSet = () => {}, canDelete, onDeleteResource = () => {},
        canEdit, onEdit = () => {}, onExportDataSet = () => {}
      }) => {
  return <PopupTrigger
    data-test-id='resource-card-menu'
    side='bottom'
    closeOnClick
    content={
      switchCase(resourceType,
        ['cohortReview', () => {
          return <React.Fragment>
            <MenuItem icon='note' onClick={onRenameResource} disabled={!canEdit}>Rename</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource} disabled={!canDelete}>
              Delete
            </MenuItem>
          </React.Fragment>;
        }],
        ['conceptSet', () => {
          return <React.Fragment>
            <MenuItem icon='pencil' onClick={onEdit} disabled={!canEdit}>Rename</MenuItem>
            <MenuItem icon='copy' onClick={onCopyConceptSet}>Copy to another workspace</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource} disabled={!canDelete}>
              Delete
            </MenuItem>
          </React.Fragment>;
        }],
        ['dataSet', () => {
          return <React.Fragment>
            <MenuItem icon='pencil' onClick={onRenameResource} disabled={!canEdit}>
              Rename Data Set
            </MenuItem>
            <MenuItem icon='pencil' onClick={onEdit} disabled={!canEdit}>Edit</MenuItem>
            <MenuItem icon='clipboard' onClick={onExportDataSet} disabled={!canEdit}>
              Export to Notebook
            </MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource} disabled={!canDelete}>
              Delete
            </MenuItem>
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
