import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import {
  CdrVersionTiersResponse,
  WorkspaceAccessLevel,
  WorkspaceResource,
} from 'generated/fetch';

import { CardButton, TabButton } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { CreateModal } from 'app/components/create-modal';
import { ClrIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { TanagraResourceList } from 'app/pages/data/tanagra-dev/tanagra-resource-list';
import {
  cohortsV2Api,
  conceptSetsV2Api,
  reviewsV2Api,
} from 'app/services/tanagra-swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { withCdrVersions, withCurrentWorkspace } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { useNavigation } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';
import cohortImg from 'assets/images/cohort-diagram.svg';
import dataSetImg from 'assets/images/dataset-diagram.svg';
import {
  CohortV2,
  ConceptSetV2,
  CreateCohortRequest,
  ReviewV2,
} from 'tanagra-generated';

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

export interface TanagraWorkspaceResource extends WorkspaceResource {
  cohortV2?: CohortV2;
  conceptSetV2?: ConceptSetV2;
  reviewV2?: ReviewV2;
}

const mapTanagraWorkspaceResource = ({
  cohort,
  conceptSet,
  review,
  workspace,
}: {
  cohort?: CohortV2;
  conceptSet?: ConceptSetV2;
  review?: ReviewV2;
  workspace: WorkspaceData;
}): TanagraWorkspaceResource => ({
  workspaceNamespace: workspace.namespace,
  workspaceFirecloudName: workspace.id, // TODO verify this is the correct value to set
  workspaceBillingStatus: workspace.billingStatus,
  cdrVersionId: workspace.cdrVersionId,
  accessTierShortName: workspace.accessTierShortName,
  permission: workspace.accessLevel.toString(),
  cohortV2: cohort,
  conceptSetV2: conceptSet,
  reviewV2: review,
  lastModifiedEpochMillis: workspace.lastModifiedTime,
  adminLocked: workspace.adminLocked,
});

interface Props extends WithSpinnerOverlayProps {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  workspace: WorkspaceData;
}

export const DataComponentTanagra = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace()
)((props: Props) => {
  useEffect(() => props.hideSpinner(), []);
  const [navigate] = useNavigation();
  const [activeTab, setActiveTab] = useState(Tabs.SHOWALL);
  const [isLoading, setIsLoading] = useState(true);
  const [resourceList, setResourceList] = useState([]);
  const [showCohortModal, setShowCohortModal] = useState(false);

  const { cdrVersionTiersResponse, workspace } = props;
  const { bigqueryDataset } = findCdrVersion(
    workspace.cdrVersionId,
    cdrVersionTiersResponse
  );

  if (!workspace) {
    return;
  }

  const loadResources = async () => {
    try {
      setIsLoading(true);
      const [cohorts, conceptSets] = await Promise.all([
        cohortsV2Api().listCohorts({ studyId: workspace.namespace }),
        conceptSetsV2Api().listConceptSets({ studyId: workspace.namespace }),
      ]);
      let reviews = [];
      if (cohorts.length > 0) {
        reviews = await Promise.all(
          cohorts.map((cohort) =>
            reviewsV2Api().listReviews({
              studyId: workspace.namespace,
              cohortId: cohort.id,
            })
          )
        );
        // This is a kind of hacky way to consolidate the array of arrays of reviews.
        // TODO Use Array.prototype.flat() if we upgrade to ES2019
        reviews = [].concat(...reviews);
      }
      setResourceList([
        ...cohorts.map((cohort) =>
          mapTanagraWorkspaceResource({ cohort, workspace })
        ),
        ...conceptSets.map((conceptSet) =>
          mapTanagraWorkspaceResource({ conceptSet, workspace })
        ),
        ...reviews.map((review) =>
          mapTanagraWorkspaceResource({ review, workspace })
        ),
      ]);
    } catch (error) {
      console.log(error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadResources();
  }, [workspace.namespace, workspace.id]);

  const writePermission =
    workspace.accessLevel === WorkspaceAccessLevel.OWNER ||
    workspace.accessLevel === WorkspaceAccessLevel.WRITER;

  const filteredList = resourceList.filter((resource) => {
    switch (activeTab) {
      case Tabs.SHOWALL:
        return true;
      case Tabs.COHORTS:
        return resource.cohortV2;
      case Tabs.COHORTREVIEWS:
        return resource.reviewV2;
      case Tabs.CONCEPTSETS:
        return resource.conceptSetV2;
      case Tabs.DATASETS:
        return false; // Currently no saved datasets in Tanagra
    }
  });

  const getCohortNames = async () => [];

  const createCohort = async (name: string, desc: string) => {
    const createCohortRequest: CreateCohortRequest = {
      studyId: workspace.namespace,
      cohortCreateInfoV2: {
        description: desc,
        displayName: name,
        underlayName: bigqueryDataset,
      },
    };
    const newCohort = await cohortsV2Api().createCohort(createCohortRequest);
    navigate([
      'workspaces',
      workspace.namespace,
      workspace.id,
      'data',
      'tanagra',
      'export',
      'cohorts',
      newCohort.id,
      'first',
    ]);
  };

  return (
    <React.Fragment>
      <div style={{ paddingLeft: '2.25rem' }}>
        <div style={styles.cardButtonArea}>
          <TooltipTrigger
            content={
              !writePermission && 'Write permission required to create cohorts'
            }
            side='top'
          >
            <CardButton
              style={styles.resourceTypeButton}
              disabled={!writePermission}
              onClick={() => setShowCohortModal(true)}
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
                  workspace.id,
                  'data',
                  'tanagra',
                  'export',
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
            <TanagraResourceList
              workspaces={[workspace]}
              workspaceResources={filteredList}
              onUpdate={() => loadResources()}
            />
          }
          {isLoading && <SpinnerOverlay />}
        </div>
      </FadeBox>
      {showCohortModal && (
        <CreateModal
          entityName='Cohort'
          title='Save Cohort as'
          getExistingNames={() => getCohortNames()}
          save={(name, desc) => createCohort(name, desc)}
          close={() => setShowCohortModal(false)}
        />
      )}
    </React.Fragment>
  );
});
