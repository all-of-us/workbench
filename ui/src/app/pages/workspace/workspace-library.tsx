import {Component} from '@angular/core';
import * as React from 'react';

import {AlertDanger} from 'app/components/alert';
import {Clickable} from 'app/components/buttons';
import {Header} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {WorkspaceCard} from 'app/pages/workspace/workspace-list';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {featuredWorkspacesConfigApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {ErrorResponse, Profile} from 'generated/fetch';

const styles = reactStyles({
  navPanel: {
    display: 'flex', flexDirection: 'column',  padding: '1rem',
    backgroundColor: colorWithWhiteness(colors.primary, 0.85),
    minWidth: '300px', maxWidth: '300px', marginLeft: '-0.6rem'
  },
  searchBar: {
    height: '2rem', fontSize: '16px', lineHeight: '19px', paddingLeft: '2rem',
    marginTop: '0.5rem', borderStyle: 'none'
  },
  iconStyling: {
    color: colors.primary, marginRight: '0.5rem', marginLeft: '0.5rem'
  },
  menuLink: {
    color: colors.accent, fontSize: 14, fontWeight: 600, display: 'flex',
    flexDirection: 'row', alignItems: 'center', padding: '0.35rem 0', borderRadius: '3px'
  },
  divider: {
    width: '100%', backgroundColor: colors.primary, borderWidth: '0px', height: '1px',
    marginBottom: '0.5rem'
  },
  menuLinkSelected: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.95)
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
(class extends React.Component<
  {profileState: { profile: Profile, reload: Function } },
  {currentTab: {title: string, icon: string}, errorText: string,
    workspaceList: WorkspacePermissions[], featuredWorkspaces: WorkspacePermissions[],
    workspacesLoading: boolean}> {
  constructor(props) {
    super(props);
    this.state = {
      currentTab: libraryTabEnums.FEATURED_WORKSPACES,
      errorText: '',
      featuredWorkspaces: [],
      workspaceList: [],
      workspacesLoading: true
    };
  }

  async componentDidMount() {
    this.loadPublishedWorkspaces();
  }

  componentDidUpdate(prevProps, prevState) {
    // Reload libraries when switching tabs
    if (this.state.currentTab !== prevState.currentTab) {
      this.updateListedWorkspaces(this.state.currentTab);
    }
  }

  updateListedWorkspaces(tabEnum) {
    switch (tabEnum) {
      case tabEnum === libraryTabEnums.PUBLISHED_WORKSPACES:
        this.loadPublishedWorkspaces();
        break;
      default:
        this.loadFeaturedWorkspaces();
        break;
    }
  }

  async loadFeaturedWorkspaces() {
    try {
      const resp = await featuredWorkspacesConfigApi().getFeaturedWorkspacesConfig();
      const idToNamespace = new Map();
      resp.featuredWorkspacesList.map(fw => idToNamespace.set(fw.id, fw.namespace));

      this.setState({
        featuredWorkspaces: this.state.workspaceList.filter(ws => idToNamespace.get(ws.workspace.id)
          && idToNamespace.get(ws.workspace.id) === ws.workspace.namespace),
        workspacesLoading: false
      });
    } catch (e) {
      const response = ErrorHandlingService.convertAPIError(e) as unknown as ErrorResponse;
      this.setState({errorText: response.message});
    }
  }

  async loadPublishedWorkspaces() {
    try {
      const workspacesReceived = await workspacesApi().getPublishedWorkspaces();
      workspacesReceived.items.sort(
        (a, b) => a.workspace.name.localeCompare(b.workspace.name));
      this.setState({
        workspaceList: workspacesReceived.items
          .map(w => new WorkspacePermissions(w)),
        workspacesLoading: false
      });
    } catch (e) {
      const response = ErrorHandlingService.convertAPIError(e) as unknown as ErrorResponse;
      this.setState({errorText: response.message});
    }
  }

  render() {
    const {profile: {username}} = this.props.profileState;
    const {currentTab, errorText, featuredWorkspaces, workspaceList,
      workspacesLoading} = this.state;
    return <div style={{display: 'flex', flexDirection: 'row', height: '100%'}}>
      <div style={styles.navPanel}>
        <div style={{display: 'flex', flexDirection: 'column'}}>
          {libraryTabs.map((tab, i) => {
            return <React.Fragment key={i}>
              <LibraryTab icon={tab.icon} title={tab.title} selected={currentTab === tab}
                          onClick={() => this.setState({currentTab: tab})}
                          data-test-id={tab.title}/>
                {i !== libraryTabs.length - 1 &&
                <hr style={{width: '100%', margin: '0.5rem 0'}}/>}
            </React.Fragment>;
          })}
        </div>
      </div>
      <div style={{padding: '1rem', width: '100%'}}>
        <Header style={{textTransform: 'uppercase'}}>Researcher Workbench Workspace Library</Header>
        <div style={{color: colors.primary, fontSize: 16, marginTop: '1rem'}}>
          Search through featured and public workspaces.
        </div>
        <div style={{display: 'flex', flexDirection: 'column', marginTop: '2rem'}}>
          <div style={{display: 'flex', flexDirection: 'row'}}>
            <ClrIcon shape={currentTab.icon} style={styles.iconStyling}
                     class='is-solid' size={24}/>
            <div style={{color: colors.primary, fontSize: 18, fontWeight: 600}}>
              {currentTab.title}
            </div>
          </div>
          <hr style={styles.divider}/>
          {errorText && <AlertDanger>{errorText}</AlertDanger>}

          {currentTab === libraryTabEnums.PUBLISHED_WORKSPACES &&
          <div style={{display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'}}>
            {workspacesLoading ?
              (<Spinner style={{width: '100%', marginTop: '0.5rem'}}/>) :
              (<div style={{display: 'flex', marginTop: '0.5rem', flexWrap: 'wrap'}}>
                {workspaceList.map(wp => {
                  return <WorkspaceCard key={wp.workspace.name}
                                        wp={wp}
                                        userEmail={username}
                                        reload={() => this.loadPublishedWorkspaces()}/>;
                })}
              </div>)}
          </div>}
          {currentTab === libraryTabEnums.FEATURED_WORKSPACES &&
          <div style={{display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'}}>
            {workspacesLoading ?
              (<Spinner style={{width: '100%', marginTop: '0.5rem'}}/>) :
              (<div style={{display: 'flex', marginTop: '0.5rem', flexWrap: 'wrap'}}>
                {featuredWorkspaces
                  .map(wp => {
                    return <WorkspaceCard key={wp.workspace.name}
                                          wp={wp}
                                          userEmail={username}
                                          reload={() => this.loadFeaturedWorkspaces()}/>;
                  })}
              </div>)}
          </div>}
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
