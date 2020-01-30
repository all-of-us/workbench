import {Component, Input} from '@angular/core';

import {Clickable} from 'app/components/buttons';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {NavStore} from 'app/utils/navigation';

import * as fp from 'lodash/fp';
import * as React from 'react';


const styles = reactStyles({
  container: {
    display: 'flex', alignItems: 'center', backgroundColor: colors.secondary,
    fontWeight: 500, color: 'white', textTransform: 'uppercase',
    height: 60, paddingRight: 16,
    boxShadow: 'inset rgba(0, 0, 0, 0.12) 0px 3px 2px 0px',
    width: 'calc(100% + 1.2rem)',
    marginLeft: '-0.6rem',
    paddingLeft: 80, borderBottom: `5px solid ${colors.accent}`, flex: 'none'
  },
  tab: {
    minWidth: 140, flexGrow: 0, padding: '0 20px',
    color: colors.white,
    alignSelf: 'stretch', display: 'flex', justifyContent: 'center', alignItems: 'center'
  },
  active: {
    backgroundColor: 'rgba(255,255,255,0.15)', color: 'unset',
    borderBottom: `4px solid ${colors.accent}`, fontWeight: 'bold'
  },
  separator: {
    background: 'rgba(255,255,255,0.15)', width: 1, height: 48, flexShrink: 0
  }
});

const tabs = [
  {name: 'Data', link: 'data'},
  {name: 'Analysis', link: 'notebooks'},
  {name: 'About', link: 'about'},
];

const navSeparator = <div style={styles.separator}/>;

export const WorkspaceNavBarReact = fp.flow(
  withCurrentWorkspace(),
  withUrlParams(),
)(props => {
  const {tabPath, urlParams: {ns: namespace, wsid: id}} = props;
  const activeTabIndex = fp.findIndex(['link', tabPath], tabs);


  const navTab = currentTab => {
    const {name, link} = currentTab;
    const selected = tabPath === link;
    const hideSeparator = selected || (activeTabIndex === tabs.indexOf(currentTab) + 1);

    return <React.Fragment key={name}>
      <Clickable
        data-test-id={name}
        aria-selected={selected}
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
  </div>;
});

@Component({
  selector: 'app-workspace-nav-bar',
  template: '<div #root></div>',
})
export class WorkspaceNavBarComponent extends ReactWrapperBase {
  @Input() tabPath;

  constructor() {
    super(WorkspaceNavBarReact, ['tabPath']);
  }
}
