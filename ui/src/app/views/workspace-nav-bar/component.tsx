import {Component, Input} from '@angular/core';

import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {CardMenuIconComponentReact} from 'app/icons/card-menu-icon/component';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {NavStore} from 'app/utils/navigation';

import * as fp from 'lodash/fp';
import * as React from 'react';


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
  menuButton: {
    width: 27, height: 27,
    opacity: 0.65, marginRight: 16
  }
});

const menuIcon = (iconName) =>
  <ClrIcon shape={iconName} style={{marginRight: '.5rem'}} size={15}/>;

const tabs = [
  {name: 'About', link: ''},
  {name: 'Cohorts', link: 'cohorts'},
  {name: 'Concepts', link: 'concepts'},
  {name: 'Notebooks', link: 'notebooks'}
];

const navSeparator = <div style={styles.separator}/>;

const MenuButton = ({disabled = false, children, ...props}) => {
  return <Clickable
    disabled={disabled}
    style={{
      display: 'flex', alignItems: 'center',
      fontSize: 12, minWidth: 125, height: '2rem',
      color: disabled ? colors.gray[2] : undefined,
      padding: '0 1.5rem',
      cursor: disabled ? 'not-allowed' : 'pointer'
    }}
    hover={!disabled ? {backgroundColor: colors.blue[3], fontWeight: 'bold'} : undefined}
    {...props}>
    {children}
  </Clickable>;
};


export const WorkspaceNavBarReact = props => {
  const {shareFunction, deleteFunction, workspace, tabPath} = props;
  const {namespace, id} = workspace;
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

  return <div id='workspace-top-nav-bar'
              className='do-not-print'
              style={styles.container}>
    {activeTabIndex > 0 && navSeparator}
    {fp.map(tab => navTab(tab), tabs)}
    <div style={{flexGrow: 1}}/>
    <PopupTrigger
      side='bottom'
      closeOnClick={true}
      content={
        <React.Fragment>
          <MenuButton onClick={() => NavStore.navigate(['/workspaces', namespace, id, 'clone'])}>
            {menuIcon('copy')}Clone
          </MenuButton>
          <MenuButton onClick={() => NavStore.navigate(['/workspaces', namespace, id, 'edit'])}>
            {menuIcon('pencil')}Edit
          </MenuButton>
          <MenuButton onClick={() => shareFunction()}>
            {menuIcon('share')}Share
          </MenuButton>
          <MenuButton onClick={() => deleteFunction()}>
            {menuIcon('trash')}Delete
          </MenuButton>
        </React.Fragment>
      }>
      <Clickable
        style={styles.menuButton}
        hover={{opacity: 1}}>
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
