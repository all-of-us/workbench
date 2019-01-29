import {Component, Input} from '@angular/core';

import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {CardMenuIconComponentReact} from 'app/icons/card-menu-icon/component';
import {WorkspaceData} from 'app/resolvers/workspace';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {NavStore} from 'app/utils/navigation';
import {WorkspaceAccessLevel} from 'generated';

import * as fp from 'lodash/fp';
import * as React from 'react';

// probably move to shared location once needed elsewhere
const colors = {
  blue: [
    '#2691d0',
    '#5aa6da',
    '#85bde4',
    '#afd3ed',
    '#d7eaf6',
    '#eaf4fb'
  ],
  gray: [
    '#4a4a4a',
    '#6e6e6e',
    '#929292',
    '#b7b7b7',
    '#dbdbdb',
    '#ededed'
  ],
};

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
    color: colors.gray[4],
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

const tabs = [
  {name: 'About', link: ''},
  {name: 'Cohorts', link: 'cohorts'},
  {name: 'Concepts', link: 'concepts'},
  {name: 'Notebooks', link: 'notebooks'}
];

const navSeparator = <div style={styles.separator}/>;

// probably move to shared location once needed elsewhere
const MenuButton = ({disabled = false, children, ...props}) => {
  return <TooltipTrigger side='left' content={disabled && 'Requires owner permission'}>
    <Clickable
      disabled={disabled}
      style={{
        display: 'flex', alignItems: 'center', justifyContent: 'start',
        fontSize: 12, minWidth: 125, height: 32,
        color: disabled ? colors.gray[2] : 'black',
        padding: '0 12px',
        cursor: disabled ? 'not-allowed' : 'pointer'
      }}
      hover={!disabled ? {backgroundColor: colors.blue[3], fontWeight: 'bold'} : undefined}
      {...props}
    >
      {children}
    </Clickable>
  </TooltipTrigger>;
};

// probably move to shared location once needed elsewhere
const menuIcon = (iconName) =>
  <ClrIcon shape={iconName} style={{marginRight: 8}} size={15}/>;

export interface WorkspaceNavBarReactProps extends React.FunctionComponent {
  shareFunction: Function;
  deleteFunction: Function;
  workspace: WorkspaceData;
  tabPath: string;
}

export const WorkspaceNavBarReact = (props: WorkspaceNavBarReactProps): JSX.Element => {
  const {shareFunction, deleteFunction, workspace, tabPath} = props;
  const {namespace, id, accessLevel} = workspace;
  const isNotOwner = accessLevel !== WorkspaceAccessLevel.OWNER;
  const activeTabIndex = fp.findIndex(['link', tabPath], tabs);


  const navTab = currentTab => {
    const {name, link} = currentTab;
    const selected = tabPath === link;
    const hideSeparator = selected || (activeTabIndex === tabs.indexOf(currentTab) + 1);

    return <React.Fragment key={name}>
      <Clickable
        style={{...styles.tab, ...(selected ? styles.active : {})}}
        hover={{color: styles.active.color}}
        onClick={() => NavStore.navigate(fp.compact(['/workspaces', namespace, id, link]))}
      >
        {name}
      </Clickable>
      {!hideSeparator && navSeparator}
    </React.Fragment>;
  };

  return <div id='workspace-top-nav-bar' className='do-not-print' style={styles.container}>
    {activeTabIndex > 0 && navSeparator}
    {fp.map(tab => navTab(tab), tabs)}
    <div style={{flexGrow: 1}}/>
    <PopupTrigger
      side='bottom'
      closeOnClick={true}
      content={
        <React.Fragment>
          <div style={styles.dropdownHeader}>Workspace Actions</div>
          <MenuButton onClick={() => NavStore.navigate(['/workspaces', namespace, id, 'clone'])}>
            {menuIcon('copy')}Clone
          </MenuButton>
          <MenuButton disabled={isNotOwner}
                      onClick={() => NavStore.navigate(['/workspaces', namespace, id, 'edit'])}
          >
            {menuIcon('pencil')}Edit
          </MenuButton>
          <MenuButton disabled={isNotOwner} onClick={() => shareFunction()}>
            {menuIcon('share')}Share
          </MenuButton>
          <MenuButton disabled={isNotOwner} onClick={() => deleteFunction()}>
            {menuIcon('trash')}Delete
          </MenuButton>
        </React.Fragment>
      }>
      <Clickable
        style={styles.menuButtonIcon}
        hover={{opacity: 1}}
      >
        <CardMenuIconComponentReact/>
      </Clickable>
    </PopupTrigger>
  </div>;
};

@Component({
  selector: 'app-workspace-nav-bar',
  styleUrls: ['./component.css'],
  template: '<div #root></div>',
})
export class WorkspaceNavBarComponent extends ReactWrapperBase {
  @Input() shareFunction;
  @Input() deleteFunction;
  @Input() workspace;
  @Input() tabPath;

  constructor() {
    super(WorkspaceNavBarReact, ['shareFunction', 'deleteFunction', 'workspace', 'tabPath']);
  }
}
