import {Component} from '@angular/core';
import * as React from 'react';

import {Clickable} from 'app/components/buttons';
import {Header} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';

const styles = reactStyles({
  navPanel: {
    display: 'flex', flexDirection: 'column', backgroundColor: '#E7E6F0',
    minWidth: '300px', maxWidth: '300px', marginLeft: '-0.6rem', padding: '1rem'
  },
  searchBar: {
    height: '2rem', fontSize: '16px', lineHeight: '19px', paddingLeft: '2rem',
    marginTop: '0.5rem', borderStyle: 'none'
  },
  iconStyling: {
    color: '#262262', marginRight: '0.5rem', marginLeft: '0.5rem'
  },
  menuLink: {
    color: '#216FB4', fontSize: 14, fontWeight: 600, display: 'flex',
    flexDirection: 'row', alignItems: 'center', padding: '0.35rem 0', borderRadius: '3px'
  },
  divider: {
    width: '100%', backgroundColor: '#262262', borderWidth: '0px', height: '1px',
    marginBottom: '0.5rem'
  },
  menuLinkSelected: {
    backgroundColor: '#F3F2F7'
  }
});

const libraryTabEnums = {
  FEATURED_WORKSPACES: {
    title: 'Featured Workspaces',
    icon: 'star'
  },
  PUBLISHED_WORKSPACES: {
    title: 'Published Workspaces',
    icon: 'library'
  }
};

const libraryTabs = [
  libraryTabEnums.FEATURED_WORKSPACES,
  libraryTabEnums.PUBLISHED_WORKSPACES
];

const LibraryTab: React.FunctionComponent<{
  title: string, icon: string, onClick: Function, selected: boolean}> =
  ({title, icon, onClick, selected}) => {
    return <Clickable style={selected ? {...styles.menuLink, ...styles.menuLinkSelected}
    : styles.menuLink}
                      onClick={onClick} hover={styles.menuLinkSelected}>
      <ClrIcon shape={icon} style={styles.iconStyling} class='is-solid' size={24}/>
      {title}
    </Clickable>;
  };

export const WorkspaceLibrary = withUserProfile()
(class extends React.Component<{}, {currentTab: {title: string, icon: string}}> {
  constructor(props) {
    super(props);
    this.state = {
      currentTab: libraryTabEnums.PUBLISHED_WORKSPACES
    };
  }

  render() {
    return <div style={{display: 'flex', flexDirection: 'row', height: '100%'}}>
      <div style={styles.navPanel}>
        <div style={{display: 'flex', flexDirection: 'column'}}>
          {libraryTabs.map((tab, i) => {
            return <React.Fragment key={i}>
              <LibraryTab icon={tab.icon} title={tab.title} selected={this.state.currentTab === tab}
                          onClick={() => this.setState({currentTab: tab})}/>
                {i !== libraryTabs.length - 1 &&
                <hr style={{width: '100%', margin: '0.5rem 0'}}/>}
            </React.Fragment>;
          })}
        </div>
      </div>
      <div style={{padding: '1rem', width: '100%'}}>
        <Header>RESEARCHER WORKBENCH WORKSPACE LIBRARY</Header>
        <div style={{color: '#262262', fontSize: 16, marginTop: '1rem'}}>
          Search through featured and public workspaces.
        </div>
        <div style={{display: 'flex', flexDirection: 'column', marginTop: '2rem'}}>
          <div style={{display: 'flex', flexDirection: 'row'}}>
            <ClrIcon shape={this.state.currentTab.icon} style={styles.iconStyling}
                     class='is-solid' size={24}/>
            <div style={{color: '#262262', fontSize: 18, fontWeight: 600}}>
              {this.state.currentTab.title}
            </div>
          </div>
          <hr style={styles.divider}/>
        </div>
      </div>
    </div>;
  }
});

@Component({
  template: '<div #root style="height: 100%"></div>'
})
export class WorkspaceLibraryComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceLibrary, []);
  }
}
