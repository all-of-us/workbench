import * as React from 'react';

import {Clickable, MenuItem} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {switchCase} from 'app/utils';
import {ResourceType} from 'app/utils/resourceActionsReact';
import {environment} from 'environments/environment';
import {ConceptSet} from 'generated/fetch';

export const ResourceCardMenu: React.FunctionComponent<{
  disabled: boolean, resourceType: ResourceType, onRenameNotebook?: Function,
  onOpenJupyterLabNotebook?: any, onCloneResource?: Function, onDeleteResource?: Function,
  onEditCohort?: Function, onReviewCohort?: Function, onEditConceptSet?: Function
}> = ({
        disabled, resourceType, onRenameNotebook = () => {}, onOpenJupyterLabNotebook = () => {},
        onCloneResource = () => {}, onDeleteResource = () => {}, onEditCohort = () => {},
        onReviewCohort = () => {}, onEditConceptSet = () => {}
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
            <MenuItem icon='copy' onClick={onCloneResource}>Duplicate</MenuItem>
            <MenuItem icon='pencil' onClick={onEditCohort}>Edit</MenuItem>
            <MenuItem icon='grid-view' onClick={onReviewCohort}>Review</MenuItem>
            <MenuItem icon='trash' onClick={onDeleteResource}>Delete</MenuItem>
          </React.Fragment>;
        }],
        ['conceptSet', () => {
          return <React.Fragment>
            <MenuItem icon='pencil' onClick={onEditConceptSet}>Edit</MenuItem>
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

export const ResourceListItem: React.FunctionComponent <
  {conceptSet: ConceptSet, openConfirmDelete: Function, edit: Function, onSelect: Function}
    > = ({conceptSet, openConfirmDelete, edit, onSelect}) => {
      return<div style={{border: '0.5px solid #C3C3C3', margin: '.4rem',
        height: '1.5rem', display: 'flex'}}>
      <div style={{width: '.75rem', paddingTop: 5, paddingLeft: 10}}>
        <ResourceCardMenu disabled={false}
                          resourceType={ResourceType.CONCEPT_SET}
                          onDeleteResource={openConfirmDelete}
                          onEditConceptSet={edit}/>
      </div>
      <input type='checkbox' value={conceptSet.name} onClick={() => onSelect}
             style={{height: 17, width: 17, marginLeft: 10, marginTop: 10,
               marginRight: 10, backgroundColor: '#7CC79B'}}/>
      <div style={{lineHeight: '1.5rem'}}>{conceptSet.name}</div>
    </div>;
    };
