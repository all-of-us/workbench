import * as React from 'react';
import { useEffect, useState } from 'react';

import { ResourceType, WorkspaceAccessLevel } from 'generated/fetch';

import {
  CardButton,
  StyledExternalLink,
  TabButton,
} from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { ClrIcon } from 'app/components/icons';
import { CheckBox } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { ResourceList } from 'app/components/resources/resource-list';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { useNavigation } from 'app/utils/navigation';
import { profileStore, serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import cohortImg from 'assets/images/cohort-diagram.svg';
import dataSetImg from 'assets/images/dataset-diagram.svg';

const styles = {
  cardButtonArea: {
    display: 'flex',
    alignItems: 'center',
    width: '100%',
  },
  cardHeader: {
    display: 'flex',
    alignItems: 'baseline',
  },
  resourceTypeButton: {
    width: '33%',
    justifyContent: 'flex-start',
    maxWidth: 'none',
    margin: '2.85rem 1.5rem 0 0',
    minHeight: '325px',
    maxHeight: '325px',
  },
  resourceTypeButtonLast: {
    marginRight: '0rem',
  },
  cardHeaderText: (disabled) => {
    return {
      color: disabled ? colorWithWhiteness(colors.dark, 0.4) : colors.accent,
      fontSize: '20px',
      marginRight: '0.75rem',
      marginTop: '0.75rem',
    };
  },
  cardText: {
    color: colors.primary,
    fontSize: '14px',
    lineHeight: '22px',
  },
  tabContainer: {
    display: 'flex',
    justifyContent: 'flex-start',
    alignItems: 'center',
    width: '100%',
    marginBottom: '0.75rem',
  },
};

enum Tabs {
  SHOWALL = 'SHOW ALL',
  DATASETS = 'DATASETS',
  COHORTS = 'COHORTS',
  COHORTREVIEWS = 'COHORT REVIEWS',
  CONCEPTSETS = 'CONCEPT SETS',
}

const descriptions = {
  datasets: `A dataset is a table containing data about a cohort that can
  be exported for analysis. `,
  cohorts: 'A cohort is a group of participants based on specific criteria.',
};

const VWB_USER_SUPPORT_MIGRATION_URL =
  'https://support.researchallofus.org/hc/en-us/articles/48266066855188';
const VWB_USER_SUPPORT_BILLING_URL =
  'https://support.researchallofus.org/hc/en-us/articles/48266066855188';

const resourceTypesToFetch = [
  ResourceType.COHORT.toString(),
  ResourceType.COHORT_REVIEW.toString(),
  ResourceType.CONCEPT_SET.toString(),
  ResourceType.DATASET.toString(),
];

interface Props extends WithSpinnerOverlayProps {
  workspace: WorkspaceData;
}

export const DataComponent = withCurrentWorkspace()((props: Props) => {
  useEffect(() => props.hideSpinner(), []);

  const [navigate] = useNavigation();
  const [checks, setChecks] = useState({
    step1: false,
    step2: false,
    step3: false,
    step4: false,
  });
  const [activeTab, setActiveTab] = useState(Tabs.SHOWALL);
  const [resourceList, setResourceList] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const allChecked = Object.values(checks).every(Boolean);
  const profile = profileStore.get().profile;
  const migrationTestingGroup = profile?.migrationTestingGroup ?? false;
  const { cdrVersionIdsForMigration, enableVwbMigration } =
    serverConfigStore.get().config;
  const { workspace } = props;

  if (!workspace) {
    return;
  }

  const loadResources = async () => {
    try {
      setIsLoading(true);
      setResourceList(
        await workspacesApi().getWorkspaceResourcesV2(
          workspace.namespace,
          workspace.terraName,
          resourceTypesToFetch
        )
      );
    } catch (error) {
      console.log(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadResources();
  }, [workspace.namespace, workspace.terraName]);

  const writePermission =
    workspace.accessLevel === WorkspaceAccessLevel.OWNER ||
    workspace.accessLevel === WorkspaceAccessLevel.WRITER;

  const filteredList = resourceList.filter((resource) => {
    switch (activeTab) {
      case Tabs.SHOWALL:
        return true;
      case Tabs.COHORTS:
        return resource.cohort;
      case Tabs.COHORTREVIEWS:
        return resource.cohortReview;
      case Tabs.CONCEPTSETS:
        return resource.conceptSet;
      case Tabs.DATASETS:
        return resource.dataSet;
    }
  });

  return (
    <React.Fragment>
      <div style={{ paddingLeft: '2.25rem' }}>
        <div style={styles.cardButtonArea}>
          {enableVwbMigration &&
            migrationTestingGroup &&
            cdrVersionIdsForMigration.includes(+workspace.cdrVersionId) && (
              <TooltipTrigger
                content={
                  !writePermission &&
                  'Write permission required to perform migration'
                }
                side='top'
              >
                <CardButton
                  style={{
                    ...styles.resourceTypeButton,
                    backgroundColor: '#E9EDF5', // light bluish background
                    border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
                    boxShadow: 'none',
                    padding: '16px',
                    cursor: 'default',
                  }}
                  disabled={!writePermission}
                >
                  {/* Title */}
                  <div
                    style={{
                      fontSize: '20px',
                      fontWeight: 600,
                      color: colors.primary,
                      marginBottom: '4px',
                    }}
                  >
                    Migration
                  </div>

                  {/* Description */}
                  <div
                    style={{
                      fontSize: '12.5px',
                      lineHeight: '20px',
                      color: colors.dark,
                    }}
                  >
                    The All of Us Researcher Workbench is moving to a new
                    platform. Please migrate your workspaces. Any workspace not
                    migrated will be archived.{' '}
                    <StyledExternalLink
                      href={VWB_USER_SUPPORT_MIGRATION_URL}
                      style={{
                        color: colors.accent,
                        textDecoration: 'underline',
                      }}
                      target='_blank'
                    >
                      Learn more
                    </StyledExternalLink>
                  </div>

                  {/* Section Title */}
                  <div
                    style={{
                      fontSize: '12.5px',
                      fontWeight: 600,
                      color: colors.primary,
                    }}
                  >
                    Prepare for Migration:
                  </div>

                  {/* List */}
                  <div
                    style={{
                      fontSize: '11.5px',
                      lineHeight: '18px',
                      color: colors.primary,
                    }}
                  >
                    <div>
                      <CheckBox
                        checked={checks.step1}
                        onChange={(checked) =>
                          setChecks({ ...checks, step1: checked })
                        }
                        style={{ marginRight: '4px' }}
                      />
                      Review your workspaces to decide which ones to migrate
                    </div>
                    <div>
                      <CheckBox
                        checked={checks.step2}
                        onChange={(checked) =>
                          setChecks({ ...checks, step2: checked })
                        }
                      />
                      Delete any inactive workspaces or stored files no longer
                      applicable to your work
                    </div>
                    <div>
                      <CheckBox
                        checked={checks.step3}
                        onChange={(checked) =>
                          setChecks({ ...checks, step3: checked })
                        }
                      />
                      Migrate files from your persistent disk (if applicable) to
                      the workspace bucket{' '}
                      <StyledExternalLink
                        href={VWB_USER_SUPPORT_MIGRATION_URL}
                        style={{
                          color: colors.accent,
                          textDecoration: 'underline',
                        }}
                        target='_blank'
                      >
                        Learn more
                      </StyledExternalLink>
                    </div>
                    <div>
                      <CheckBox
                        checked={checks.step4}
                        onChange={(checked) =>
                          setChecks({ ...checks, step4: checked })
                        }
                      />
                      Set up billing in the new Researcher Workbench 2.0 if
                      initial credits have been used{' '}
                      <StyledExternalLink
                        href={VWB_USER_SUPPORT_BILLING_URL}
                        style={{
                          color: colors.accent,
                          textDecoration: 'underline',
                        }}
                        target='_blank'
                      >
                        Learn more
                      </StyledExternalLink>
                    </div>
                  </div>

                  {/* CTA */}
                  <div style={{ marginTop: 'auto' }}>
                    <div
                      style={{
                        marginTop: '4px',
                        backgroundColor: allChecked
                          ? colors.primary
                          : '#C4C4C4',
                        color: colors.white,
                        display: 'inline-flex',
                        alignItems: 'center',
                        padding: '4px 16px',
                        borderRadius: '4px',
                        fontSize: '13px',
                        fontWeight: 500,
                        cursor: allChecked ? 'pointer' : 'not-allowed',
                        height: '30px',
                        opacity: allChecked ? 1 : 0.6,
                      }}
                      onClick={() => {
                        if (!allChecked) {
                          return;
                        }
                        navigate([
                          'workspaces',
                          workspace.namespace,
                          workspace.terraName,
                          'migration',
                        ]);
                      }}
                    >
                      Get started
                      <ClrIcon
                        shape='arrow right'
                        size={12}
                        style={{ marginLeft: '6px' }}
                      />
                    </div>
                  </div>
                </CardButton>
              </TooltipTrigger>
            )}
          <TooltipTrigger
            content={
              !writePermission && 'Write permission required to create cohorts'
            }
            side='top'
          >
            <CardButton
              style={styles.resourceTypeButton}
              disabled={!writePermission}
              onClick={() => {
                navigate([
                  'workspaces',
                  workspace.namespace,
                  workspace.terraName,
                  'data',
                  'cohorts',
                  'build',
                ]);
              }}
            >
              <div style={styles.cardHeader}>
                <h2 style={styles.cardHeaderText(!writePermission)}>Cohorts</h2>
                <ClrIcon
                  shape='plus-circle'
                  class='is-solid'
                  size={18}
                  style={{ marginTop: 5 }}
                />
              </div>
              <div style={styles.cardText}>{descriptions.cohorts}</div>
              {/* Because the container can stretch based on window size, but the height
              can't we set a max width to cap the height based on aspect ratio*/}
              <div
                style={{
                  width: '100%',
                  maxWidth: '425px',
                  paddingTop: '1.5rem',
                }}
              >
                <img data-test-id={'cohort-diagram'} src={cohortImg} />
              </div>
            </CardButton>
          </TooltipTrigger>
          <TooltipTrigger
            content={
              !writePermission && 'Write permission required to create datasets'
            }
            side='top'
          >
            <CardButton
              style={{
                ...styles.resourceTypeButton,
                ...styles.resourceTypeButtonLast,
              }}
              disabled={!writePermission}
              onClick={() => {
                AnalyticsTracker.DatasetBuilder.OpenCreatePage();
                navigate([
                  'workspaces',
                  workspace.namespace,
                  workspace.terraName,
                  'data',
                  'data-sets',
                ]);
              }}
            >
              <div style={styles.cardHeader}>
                <h2 style={styles.cardHeaderText(!writePermission)}>
                  Datasets
                </h2>
                <ClrIcon
                  shape='plus-circle'
                  class='is-solid'
                  size={18}
                  style={{ marginTop: 5 }}
                />
              </div>
              <div style={styles.cardText}>{descriptions.datasets}</div>
              {/* Because the container can stretch based on window size, but the height
               can't we set a max width to cap the height based on aspect ratio*/}
              <div
                style={{
                  width: '100%',
                  maxWidth: '425px',
                  paddingTop: '2.25rem',
                }}
              >
                <img data-test-id={'dataset-diagram'} src={dataSetImg} />
              </div>
            </CardButton>
          </TooltipTrigger>
        </div>
      </div>
      <FadeBox style={{ marginTop: '1.5rem' }}>
        <div style={styles.tabContainer}>
          <h2
            style={{
              margin: 0,
              color: colors.primary,
              fontSize: '16px',
              fontWeight: 600,
            }}
          >
            Show:
          </h2>
          <TabButton
            active={activeTab === Tabs.SHOWALL}
            onClick={() => setActiveTab(Tabs.SHOWALL)}
          >
            Show All
          </TabButton>
          <TabButton
            active={activeTab === Tabs.COHORTS}
            onClick={() => setActiveTab(Tabs.COHORTS)}
            data-test-id='view-only-cohorts'
          >
            Cohorts
          </TabButton>
          <TabButton
            active={activeTab === Tabs.COHORTREVIEWS}
            onClick={() => setActiveTab(Tabs.COHORTREVIEWS)}
            data-test-id='view-only-cohort-reviews'
          >
            Cohort Reviews
          </TabButton>
          <TabButton
            active={activeTab === Tabs.CONCEPTSETS}
            onClick={() => setActiveTab(Tabs.CONCEPTSETS)}
            data-test-id='view-only-concept-sets'
          >
            Concept Sets
          </TabButton>
          <TabButton
            active={activeTab === Tabs.DATASETS}
            onClick={() => setActiveTab(Tabs.DATASETS)}
            data-test-id='view-only-data-sets'
          >
            Datasets
          </TabButton>
        </div>
        <div
          style={{
            borderBottom: `1px solid ${colors.dark}`,
            marginLeft: '-1.5rem',
            marginRight: '-1.5rem',
            opacity: 0.24,
          }}
        ></div>
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            position: 'relative',
            minHeight: 247,
            padding: '0 0.75rem 1.5rem',
            paddingTop: '2.25rem',
          }}
        >
          {
            <ResourceList
              workspaces={[workspace]}
              workspaceResources={filteredList}
              onUpdate={() => loadResources()}
            />
          }
          {isLoading && <SpinnerOverlay />}
        </div>
      </FadeBox>
    </React.Fragment>
  );
});
