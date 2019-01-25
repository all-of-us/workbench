import {Component, Input} from '@angular/core';

import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Popup} from 'app/components/popups';
import {WorkspaceData} from 'app/resolvers/workspace';
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
    width: 'calc(100% + 1.6rem)',
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
  menuIcon: {
    backgroundImage: 'url("/assets/icons/card-menu-icon.svg")',
    width: 27, height: 27
  }
});

const tabs = [
  {name: 'About', link: ''},
  {name: 'Cohorts', link: 'cohorts'},
  {name: 'Concepts', link: 'concepts'},
  {name: 'Notebooks', link: 'notebooks'}
];


export class WorkspaceNavBarReact extends React.Component<
  // tabPath is a prop so this re-renders when it changes, replace when there's a React router
  {shareFunction: Function, deleteFunction: Function, workspace: WorkspaceData, tabPath: String},
  {isDropdownOpen: boolean}
> {
  constructor(props) {
    super(props);
    this.state = {isDropdownOpen: false};
  }

  render() {
    const {shareFunction, deleteFunction, workspace, tabPath} = this.props;
    const {isDropdownOpen} = this.state;
    const {namespace, id} = workspace;
    const activeTab = fp.find({link: tabPath}, tabs);


    const navSeparator = <div style={styles.separator}/>;

    const navTab = currentTab => {
      const {name, link} = currentTab;
      const selected = tabPath === link;
      const hideSeparator = selected || tabs.indexOf(activeTab) === tabs.indexOf(currentTab) + 1;

      return <React.Fragment>
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
                className='btn-group do-not-print'
                style={styles.container}>
      {activeTab !== tabs[0] && navSeparator}
      {...fp.map(tab => navTab(tab), tabs)}
      <div style={{flexGrow: 1}} />
      <Clickable
        onClick={() => this.setState({isDropdownOpen: true})}
        style={styles.menuIcon}/>
      {isDropdownOpen && <Popup></Popup>}
    </div>;
  }
}

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
