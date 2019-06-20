import {Clickable, MenuItem} from 'app/components/buttons';
import {PopupTrigger} from 'app/components/popups';
import {CardMenuIconComponentReact} from 'app/icons/card-menu-icon';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {withCurrentWorkspace, withUrlParams} from 'app/utils/index';

import * as fp from 'lodash/fp';
import * as React from 'react';
import {ClrIcon} from "../components/icons";


const styles = reactStyles({
  container: {
    display: 'flex', alignItems: 'center', backgroundColor: colors.blue[1],
    fontWeight: 500, color: 'white', textTransform: 'uppercase',
    height: 60, paddingRight: 16,
    boxShadow: 'inset rgba(0, 0, 0, 0.12) 0px 3px 2px 0px',
    width: 'calc(100% + 1.2rem)',
    marginLeft: '-0.6rem',
    paddingLeft: 80, borderBottom: `5px solid ${colors.blue[0]}`, flex: 'none'
  },
  tab: {
    minWidth: 140, flexGrow: 0, padding: '0 20px',
    color: colors.gray[5],
    alignSelf: 'stretch', display: 'flex', justifyContent: 'center', alignItems: 'center'
  },
  active: {
    backgroundColor: 'rgba(255,255,255,0.15)', color: 'unset',
    borderBottom: `4px solid ${colors.blue[0]}`, fontWeight: 'bold'
  },
  separator: {
    background: 'rgba(255,255,255,0.15)', width: 1, height: 48, flexShrink: 0
  },
  dropdownHeader: {
    fontSize: 12,
    lineHeight: '30px',
    color: '#262262',
    fontWeight: 600,
    paddingLeft: 12,
    width: 160
  },
  menuButtonIcon: {
    width: 27, height: 27,
    opacity: 0.65, marginRight: 16
  }
});

export const NotebookActionsPopup = fp.flow(
  withCurrentWorkspace(),
  withUrlParams(),
)(props => {
  return (
    <PopupTrigger
      side='bottom'
      closeOnClick={true}
      content={
        <React.Fragment>
          <MenuItem>
            Duplicate
          </MenuItem>
          <MenuItem>
            Edit
          </MenuItem>
          <MenuItem>
            Share
          </MenuItem>
          <MenuItem>
            Delete
          </MenuItem>
        </React.Fragment>
      }>
      <Clickable>
        <ClrIcon shape='ellipsis-vertical' size={24}
                 style={{
                   color: '#216FB4',
                   paddingBottom: '3px',
                   cursor: 'pointer'
                 }}/>
      </Clickable>
  </PopupTrigger>);
});