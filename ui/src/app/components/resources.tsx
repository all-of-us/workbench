import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {switchCase} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActionsReact';

export const ResourceCardMenu: React.FunctionComponent<{
  disabled: boolean, resourceType: ResourceType, onRenameResource?: Function,
  onCloneResource?: Function, onCopyConceptSet?: Function, canDelete: boolean,
  onDeleteResource?: Function, canEdit: boolean, onEdit?: Function,
  onExportDataSet: Function, onReviewCohort?: Function,
}> = ({
        disabled, resourceType, onRenameResource = () => {}, onCloneResource = () => {},
        onCopyConceptSet = () => {}, canDelete, onDeleteResource = () => {},
        canEdit, onEdit = () => {}, onExportDataSet = () => {}, onReviewCohort = () => {}
      }) => {
  return <PopupTrigger
    data-test-id='resource-card-menu'
    side='bottom'
    closeOnClick
    content={
      switchCase(resourceType,
        ['cohort', () => {
          return <React.Fragment>
            <MenuItem icon='note' onClick={onRenameResource}>Rename</MenuItem>
            <MenuItem icon='copy' onClick={onCloneResource}>Duplicate</MenuItem>
            <MenuItem icon='pencil' onClick={onEdit}>Edit</MenuItem>
            <MenuItem icon='grid-view' onClick={onReviewCohort}>Review</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }],
        ['cohortReview', () => {
          return <React.Fragment>
            <MenuItem icon='note' onClick={onRenameResource}>Rename</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
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
            <MenuItem icon='pencil' onClick={onRenameResource}>Rename Data Set</MenuItem>
            <MenuItem icon='pencil' onClick={onEdit}>Edit</MenuItem>
            <MenuItem icon='clipboard' onClick={onExportDataSet}>Export to Notebook</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }]
      )
    }
    disabled={resourceType !== ResourceType.CONCEPT_SET || !canEdit}
  >
    <Clickable disabled={disabled} data-test-id='resource-menu'>
      <ClrIcon shape='ellipsis-vertical' size={21}
               style={{color: disabled ? colorWithWhiteness(colors.dark, 0.6) : colors.accent,
                 marginLeft: -9, cursor: disabled ? 'auto' : 'pointer'}}/>
    </Clickable>
  </PopupTrigger>;
};
