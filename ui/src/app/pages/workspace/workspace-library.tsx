import * as React from 'react';

import {AlertDanger} from 'app/components/alert';
import {Clickable} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header} from 'app/components/headers';
import {Spinner} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {WorkspaceCard} from 'app/pages/workspace/workspace-card';
import {featuredWorkspacesConfigApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {convertAPIError} from 'app/utils/errors';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {environment} from 'environments/environment';
import {FeaturedWorkspace, FeaturedWorkspaceCategory} from 'generated/fetch';

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
    color: colors.primary, marginRight: '0.5rem', marginLeft: '0.5rem', height: 24, width: 24
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
  },
  libraryTabDivider: {
    width: '100%',
    margin: '0.5rem 0',
    backgroundColor: colorWithWhiteness(colors.dark, .5),
    border: '0 none',
    height: 1
  }
});

const libraryTabs = {
  PUBLISHED_WORKSPACES: {
    title: 'Published Workspaces',
    // TODO: Find the right icon when we intend to release this.
    icon: '/assets/icons/phenotype-library.svg',
    filter: (workspaceList: WorkspacePermissions[], featuredWorkspaces: FeaturedWorkspace[]) => {
      return workspaceList.filter(workspace => !featuredWorkspaces.find(featuredWorkspace =>
        workspace.workspace.id === featuredWorkspace.id && workspace.workspace.namespace === featuredWorkspace.namespace));
    }
  },
  PHENOTYPE_LIBRARY: {
    title: 'Phenotype Library',
    description: <div>These workspaces demonstrate how computable electronic phenotypes can be implemented within
      the <i>All of Us</i> dataset using examples of previously published phenotype algorithms. You can open the
      workspaces to view them or “duplicate” the workspaces to edit and execute the algorithms.</div>,
    icon: '/assets/icons/phenotype-library.svg',
    filter: (workspaceList: WorkspacePermissions[], featuredWorkspaces: FeaturedWorkspace[]) => {
      return workspaceList.filter(workspace => !!featuredWorkspaces.find(featuredWorkspace =>
        workspace.workspace.id === featuredWorkspace.id &&
        workspace.workspace.namespace === featuredWorkspace.namespace &&
        featuredWorkspace.category === FeaturedWorkspaceCategory.PHENOTYPELIBRARY));
    }
  },
  TUTORIAL_WORKSPACES: {
    title: 'Tutorial Workspaces',
    description: <div>These workspaces provide instructions for key Researcher Workbench components and representations
      of the <i>All of Us</i> dataset. You can open the workspaces to view them or “duplicate” the workspaces to edit
      and execute them.</div>,
    icon: '/assets/icons/tutorial-workspaces.svg',
    filter: (workspaceList: WorkspacePermissions[], featuredWorkspaces: FeaturedWorkspace[]) => {
      return workspaceList.filter(workspace => !!featuredWorkspaces.find(featuredWorkspace =>
        workspace.workspace.id === featuredWorkspace.id &&
        workspace.workspace.namespace === featuredWorkspace.namespace &&
        featuredWorkspace.category === FeaturedWorkspaceCategory.TUTORIALWORKSPACES));
    }
  },
  DEMO_PROJECTS: {
    title: 'Demonstration Projects',
    description: <div>Workspaces included here are meant to demonstrate end-to-end analyses that can be performed
      using <AoU/> Data. The current set of research example analyses were developed as part of <AoU/> Demonstration
      Projects. They are aimed at demonstrating the quality, utility, and diversity of <AoU/> data by replicating
      previously published studies. You can open the workspaces to view them or “duplicate” the workspaces to edit
      and execute them.</div>,
    icon: '/assets/icons/demonstration.svg',
    filter: (workspaceList: WorkspacePermissions[], featuredWorkspaces: FeaturedWorkspace[]) => {
      return workspaceList.filter(workspace => !!featuredWorkspaces.find(featuredWorkspace =>
        workspace.workspace.id === featuredWorkspace.id &&
        workspace.workspace.namespace === featuredWorkspace.namespace &&
        featuredWorkspace.category === FeaturedWorkspaceCategory.DEMOPROJECTS));
    }
  }
};

const LibraryTab: React.FunctionComponent<{
  title: string, icon: string, onClick: Function, selected: boolean}> =
  ({title, icon, onClick, selected}) => {
    return <Clickable style={selected ? {...styles.menuLink, ...styles.menuLinkSelected}
    : styles.menuLink}
                      onClick={onClick} hover={styles.menuLinkSelected}>
      <img src={icon} style={styles.iconStyling}/>
      {title}
    </Clickable>;
  };

interface CurrentTab {
  description?: JSX.Element;
  filter: (workspaceList: WorkspacePermissions[], featuredWorkspaces: FeaturedWorkspace[]) => WorkspacePermissions[];
  icon: string;
  title: string;
}

interface Props extends WithSpinnerOverlayProps {
  enablePublishedWorkspaces: boolean;
}

interface State {
  currentTab: CurrentTab;
  errorText: string;
  workspaceList: WorkspacePermissions[];
  featuredWorkspaces: FeaturedWorkspace[];
  pendingWorkspaceRequests: number;
}

export const WorkspaceLibrary = (class extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      currentTab: libraryTabs.PHENOTYPE_LIBRARY,
      errorText: '',
      featuredWorkspaces: [],
      workspaceList: [],
      pendingWorkspaceRequests: 0
    };
  }

  // Defaulting to the environment variable value here, but allowing it to be overridden by props.
  // This is to make our testing easier. We don't use environment variable injection anywhere in
  // our unit testing framework, so we have to pass it in via props. We also don't create this
  // component in any way except for legacy Angular magic, so we have to get this information via
  // environment variable.
  libraryTabs = (this.props.enablePublishedWorkspaces || environment.enablePublishedWorkspaces)
    ? [
      libraryTabs.PUBLISHED_WORKSPACES,
      libraryTabs.PHENOTYPE_LIBRARY,
      libraryTabs.TUTORIAL_WORKSPACES,
      libraryTabs.DEMO_PROJECTS
    ]
    : [
      libraryTabs.PHENOTYPE_LIBRARY,
      libraryTabs.TUTORIAL_WORKSPACES,
      libraryTabs.DEMO_PROJECTS
    ];

  async componentDidMount() {
    this.props.hideSpinner();
    this.updateWorkspaces();
  }

  areWorkspacesLoading() {
    return this.state.pendingWorkspaceRequests > 0;
  }

  componentDidUpdate(prevProps, prevState) {
    // Reload libraries when switching tabs
    if (this.state.currentTab !== prevState.currentTab) {
      this.updateWorkspaces();
    }
  }

  async updateWorkspaces() {
    await this.getAllPublishedWorkspaces();
    await this.getFeaturedWorkspaces();
  }

  // Gets all published workspaces, including those configured as 'featured'
  async getAllPublishedWorkspaces() {
    this.setState((previousState) => ({
      pendingWorkspaceRequests: previousState.pendingWorkspaceRequests + 1
    }));

    try {
      const workspacesReceived = await workspacesApi().getPublishedWorkspaces();
      workspacesReceived.items.sort(
        (a, b) => a.workspace.name.localeCompare(b.workspace.name));

      this.setState((previousState) => ({
        workspaceList: workspacesReceived.items.map(w => new WorkspacePermissions(w)),
        pendingWorkspaceRequests: previousState.pendingWorkspaceRequests - 1
      }));
    } catch (e) {
      const response = await convertAPIError(e);
      this.setState({errorText: response.message});
    }
  }

  // Gets the 'featured workspaces' config and filters the list of published workspaces to
  // find the 'featured' ones
  async getFeaturedWorkspaces() {
    this.setState((previousState) => ({
      pendingWorkspaceRequests: previousState.pendingWorkspaceRequests + 1
    }));

    try {
      const resp = await featuredWorkspacesConfigApi().getFeaturedWorkspacesConfig();

      this.setState((previousState) => ({
        featuredWorkspaces: resp.featuredWorkspacesList,
        pendingWorkspaceRequests: previousState.pendingWorkspaceRequests - 1
      }));
    } catch (e) {
      const response = await convertAPIError(e);
      this.setState({errorText: response.message});
    }
  }

  render() {
    const {
      currentTab,
      errorText
    } = this.state;
    return <FlexRow style={{height: '100%'}}>
      <div style={styles.navPanel}>
        <FlexColumn>
          {this.libraryTabs.map((tab, i) => {
            return <React.Fragment key={i}>
              <LibraryTab icon={tab.icon} title={tab.title} selected={currentTab === tab}
                          onClick={() => this.setState({currentTab: tab})}
                          data-test-id={tab.title}/>
                {i !== this.libraryTabs.length - 1 &&
                <hr style={styles.libraryTabDivider}/>}
            </React.Fragment>;
          })}
        </FlexColumn>
      </div>
      <div style={{padding: '1rem', width: '100%'}}>
        <Header style={{textTransform: 'uppercase'}}>Researcher Workbench Workspace Library</Header>
        <FlexColumn style={{marginTop: '2rem'}}>
          <FlexRow>
            <img src={currentTab.icon} style={styles.iconStyling}/>
            <div style={{color: colors.primary, fontSize: 18, fontWeight: 600}}>
              {currentTab.title}
              {currentTab.description && <div style={{color: colors.primary, fontWeight: 400,
                fontSize: 14, paddingTop: '0.25rem'}}>{currentTab.description}</div>}
            </div>
          </FlexRow>

          <hr style={styles.divider}/>
          {errorText && <AlertDanger>{errorText}</AlertDanger>}

          <div style={{display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'}}>
            {this.areWorkspacesLoading() ?
              (<Spinner style={{width: '100%', marginTop: '0.5rem'}}/>) :
              (<div style={{display: 'flex', marginTop: '0.5rem', flexWrap: 'wrap'}}>
                {currentTab.filter(this.state.workspaceList, this.state.featuredWorkspaces).map(wp => {
                  return <WorkspaceCard key={wp.workspace.name}
                                        workspace={wp.workspace}
                                        accessLevel={wp.accessLevel}
                                        reload={() => this.updateWorkspaces()}/>;
                })}
              </div>)}
          </div>

        </FlexColumn>
      </div>
    </FlexRow>;
  }
});
