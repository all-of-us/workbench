import * as React from 'react';
import { ReactNode, useEffect, useState } from 'react';

import { FeaturedWorkspaceCategory, WorkspaceResponse } from 'generated/fetch';

import { AlertDanger } from 'app/components/alert';
import { Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header } from 'app/components/headers';
import { Spinner } from 'app/components/spinners';
import { AoU } from 'app/components/text-wrappers';
import { WorkspaceCard } from 'app/pages/workspace/workspace-card';
import { featuredWorkspaceApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { hasTierAccess } from 'app/utils/access-tiers';
import { convertAPIError } from 'app/utils/errors';
import { profileStore } from 'app/utils/stores';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';
import community from 'assets/icons/community.svg';
import demonstration from 'assets/icons/demonstration.svg';
import phenotypeLibrary from 'assets/icons/phenotype-library.svg';
import tutorialWorkspaces from 'assets/icons/tutorial-workspaces.svg';

const styles = reactStyles({
  navPanel: {
    display: 'flex',
    flexDirection: 'column',
    padding: '1.5rem',
    backgroundColor: colorWithWhiteness(colors.primary, 0.85),
    minWidth: '300px',
    maxWidth: '300px',
    marginLeft: '-0.9rem',
  },
  searchBar: {
    height: '3rem',
    fontSize: '16px',
    lineHeight: '19px',
    paddingLeft: '3rem',
    marginTop: '0.75rem',
    borderStyle: 'none',
  },
  iconStyling: {
    color: colors.primary,
    marginRight: '0.75rem',
    marginLeft: '0.75rem',
    height: 24,
    width: 24,
  },
  menuLink: {
    color: colors.accent,
    fontSize: 14,
    fontWeight: 600,
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    padding: '0.525rem 0',
    borderRadius: '3px',
  },
  divider: {
    width: '100%',
    backgroundColor: colors.primary,
    borderWidth: '0px',
    height: '1px',
    marginBottom: '0.75rem',
  },
  menuLinkSelected: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.95),
  },
  libraryTabDivider: {
    width: '100%',
    margin: '0.75rem 0',
    backgroundColor: colorWithWhiteness(colors.dark, 0.5),
    border: '0 none',
    height: 1,
  },
});

interface LibraryTabProps {
  title: string;
  description: ReactNode;
  icon: string;
  category: FeaturedWorkspaceCategory;
}

const createTab = (title, description, icon, category): LibraryTabProps => ({
  title,
  description,
  icon,
  category,
});

const libraryTabs: Record<string, LibraryTabProps> = {
  PHENOTYPE_LIBRARY: createTab(
    'Phenotype Library',
    <div>
      These workspaces demonstrate how computable electronic phenotypes can be
      implemented within the <AoU /> dataset using examples of previously
      published phenotype algorithms. You can open the workspaces to view them
      or “duplicate” the workspaces to edit and execute the algorithms.
    </div>,
    phenotypeLibrary,
    FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY
  ),
  TUTORIAL_WORKSPACES: createTab(
    'Tutorial Workspaces',
    <div>
      These workspaces provide instructions for key Researcher Workbench
      components and representations of the <AoU /> dataset. You can open the
      workspaces to view them or “duplicate” the workspaces to edit and execute
      them.
    </div>,
    tutorialWorkspaces,
    FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES
  ),
  DEMO_PROJECTS: createTab(
    'Demonstration Projects',
    <div>
      Workspaces included here are meant to demonstrate end-to-end analyses that
      can be performed using <AoU /> Data. The current set of research example
      analyses were developed as part of <AoU /> Demonstration Projects. They
      are aimed at demonstrating the quality, utility, and diversity of <AoU />{' '}
      data by replicating previously published studies. You can open the
      workspaces to view them or “duplicate” the workspaces to edit and execute
      them.
    </div>,
    demonstration,
    FeaturedWorkspaceCategory.DEMO_PROJECTS
  ),
  COMMUNITY: createTab(
    'Community Workspaces',
    <div>
      Community workspaces are curated and maintained by researchers using the
      Research Workbench and are intended for use by the Research Workbench
      community. Please be sure to review the 'About' section of the workspace
      for how to use and cite the workspace in your own research and who to
      contact with any questions about the workspace.
    </div>,
    community,
    FeaturedWorkspaceCategory.COMMUNITY
  ),
};

interface TabProps {
  title: string;
  icon: string;
  onClick: () => void;
  selected: boolean;
}

const LibraryTab = ({ title, icon, onClick, selected }: TabProps) => {
  return (
    <Clickable
      style={
        selected
          ? { ...styles.menuLink, ...styles.menuLinkSelected }
          : styles.menuLink
      }
      onClick={onClick}
      hover={styles.menuLinkSelected}
    >
      <img src={icon} style={styles.iconStyling} />
      {title}
    </Clickable>
  );
};

export const FeaturedWorkspaces = (props) => {
  const [currentTab, setCurrentTab] = useState<LibraryTabProps>(
    libraryTabs.TUTORIAL_WORKSPACES
  );
  const [errorText, setErrorText] = useState<string>('');

  const [workspaceList, setWorkspaceList] = useState<Array<WorkspaceResponse>>(
    []
  );
  const [pendingWorkspaceRequests, setPendingWorkspaceRequests] =
    useState<boolean>(false);

  const libraryTabsArray: LibraryTabProps[] = [
    libraryTabs.TUTORIAL_WORKSPACES,
    libraryTabs.DEMO_PROJECTS,
    libraryTabs.PHENOTYPE_LIBRARY,
    libraryTabs.COMMUNITY,
  ];

  const getAllPublishedWorkspaces = async () => {
    setPendingWorkspaceRequests(true);

    try {
      const workspacesReceived =
        await featuredWorkspaceApi().getFeaturedWorkspacesByCategory(
          currentTab.category
        );
      setWorkspaceList(
        workspacesReceived.items
          .sort((a, b) => a.workspace.name.localeCompare(b.workspace.name))
          .map((w) => new WorkspacePermissions(w))
      );
      setPendingWorkspaceRequests(false);
    } catch (e) {
      const response = await convertAPIError(e);
      setErrorText(response.message);
    }
  };

  useEffect(() => {
    props.hideSpinner();
    getAllPublishedWorkspaces();
  }, [currentTab]);

  const areWorkspacesLoading = () => {
    return pendingWorkspaceRequests;
  };

  const { profile } = profileStore.get();

  return (
    <FlexRow style={{ height: '100%' }}>
      <div style={styles.navPanel}>
        <FlexColumn>
          {libraryTabsArray.map((tab, index) => {
            return (
              <React.Fragment key={index}>
                <LibraryTab
                  icon={tab.icon}
                  title={tab.title}
                  selected={currentTab === tab}
                  onClick={() => setCurrentTab(tab)}
                  data-test-id={tab.title}
                />
                {index !== libraryTabsArray.length - 1 && (
                  <hr style={styles.libraryTabDivider} />
                )}
              </React.Fragment>
            );
          })}
        </FlexColumn>
      </div>
      <div style={{ padding: '1.5rem', width: '100%' }}>
        <Header style={{ textTransform: 'uppercase' }}>
          Researcher Workbench Workspace Library
        </Header>
        <FlexColumn style={{ marginTop: '3rem' }}>
          <FlexRow>
            <img src={currentTab.icon} style={styles.iconStyling} />
            <div
              style={{
                color: colors.primary,
                fontSize: 18,
                fontWeight: 600,
              }}
            >
              {currentTab.title}
              {currentTab.description && (
                <div
                  style={{
                    color: colors.primary,
                    fontWeight: 400,
                    fontSize: 14,
                    paddingTop: '0.375rem',
                  }}
                >
                  {currentTab.description}
                </div>
              )}
            </div>
          </FlexRow>

          <hr style={styles.divider} />
          {errorText && <AlertDanger>{errorText}</AlertDanger>}

          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-start',
              flexWrap: 'wrap',
            }}
          >
            {areWorkspacesLoading() ? (
              <Spinner style={{ width: '100%', marginTop: '0.75rem' }} />
            ) : (
              <div
                style={{
                  display: 'flex',
                  marginTop: '0.75rem',
                  flexWrap: 'wrap',
                }}
              >
                {workspaceList.map((wp) => {
                  return (
                    <WorkspaceCard
                      isOriginFeaturedWorkspace
                      key={wp.workspace.name}
                      workspace={wp.workspace}
                      accessLevel={wp.accessLevel}
                      reload={() => getAllPublishedWorkspaces()}
                      tierAccessDisabled={
                        !hasTierAccess(
                          profile,
                          wp.workspace.accessTierShortName
                        )
                      }
                    />
                  );
                })}
              </div>
            )}
          </div>
        </FlexColumn>
      </div>
    </FlexRow>
  );
};
