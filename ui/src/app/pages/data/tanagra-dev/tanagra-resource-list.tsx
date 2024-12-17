import * as React from 'react';
import { CSSProperties, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  CdrVersionTiersResponse,
  ResourceType,
  Workspace,
  WorkspaceResource,
} from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { RenameModal } from 'app/components/rename-modal';
import {
  ResourceAction,
  ResourceActionsMenu,
} from 'app/components/resources/resource-actions-menu';
import {
  withConfirmDeleteModal,
  WithConfirmDeleteModalProps,
} from 'app/components/with-confirm-delete-modal';
import {
  WithErrorModalProps,
  withErrorModalWrapper,
} from 'app/components/with-error-modal-wrapper';
import {
  withSpinnerOverlay,
  WithSpinnerOverlayProps,
} from 'app/components/with-spinner-overlay';
import { TanagraWorkspaceResource } from 'app/pages/data/tanagra-dev/data-component-tanagra';
import {
  getCreatedBy,
  getDisplayName,
  getType,
  getTypeString,
  StyledResourceType,
} from 'app/pages/data/tanagra-dev/tanagra-resources';
import { dataTabPath } from 'app/routing/utils';
import {
  cohortsApi,
  featureSetsApi,
  reviewsApi,
} from 'app/services/tanagra-swagger-fetch-clients';
import { reactStyles, withCdrVersions } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { ROWS_PER_PAGE_RESOURCE_TABLE } from 'app/utils/constants';
import { displayDateWithoutHours } from 'app/utils/dates';
import { canDelete, canWrite, getDescription } from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  column: {
    textAlign: 'left',
  },
  typeColumn: {
    textAlign: 'left',
    width: '140px',
  },
  modifiedDateColumn: {
    textAlign: 'left',
    width: '13.5rem',
  },
  menu: {
    width: '30px',
  },
  navigation: {
    fontFamily: 'Montserrat',
    fontSize: '14px',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    display: 'block',
    overflow: 'hidden',
  },
});

interface NavProps {
  workspace: Workspace;
  resource: WorkspaceResource;
  style?: CSSProperties;
}

const WorkspaceNavigation = (props: NavProps) => {
  const {
    workspace: { namespace, displayName, terraName },
    style,
  } = props;
  const url = dataTabPath(namespace, terraName);

  return (
    <Clickable>
      <Link to={url} style={style} data-test-id='workspace-navigation'>
        {displayName}
      </Link>
    </Clickable>
  );
};

interface TableData {
  menu: JSX.Element;
  resourceType: string;
  resourceName: string;
  lastModifiedForSorting: number;
  formattedLastModified: string;
  createdBy: string;
  cdrVersionName: string;
  resource: WorkspaceResource;
  workspace: Workspace;
}

interface Props
  extends WithConfirmDeleteModalProps,
    WithErrorModalProps,
    WithSpinnerOverlayProps {
  existingNameList: string[];
  workspaceResources: TanagraWorkspaceResource[];
  onUpdate: Function;
  workspaces: WorkspaceData[];
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  recentResourceSource?: boolean;
}

export const TanagraResourceList = fp.flow(
  withCdrVersions(),
  withConfirmDeleteModal(),
  withErrorModalWrapper(),
  withSpinnerOverlay()
)((props: Props) => {
  const [resourceToRename, setResourceToRename] =
    useState<TanagraWorkspaceResource>();
  const [tableData, setTableData] = useState<TableData[]>();

  const getCdrVersionName = (r: WorkspaceResource) => {
    const { cdrVersionTiersResponse } = props;

    const cdrVersion = findCdrVersion(r.cdrVersionId, cdrVersionTiersResponse);
    return cdrVersion?.name;
  };

  const deleteResource = async (resource: TanagraWorkspaceResource) => {
    try {
      if (resource.cohortTanagra) {
        await cohortsApi().deleteCohort({
          studyId: resource.workspaceNamespace,
          cohortId: resource.cohortTanagra.id,
        });
      } else if (resource.featureSetTanagra) {
        await featureSetsApi().deleteFeatureSet({
          studyId: resource.workspaceNamespace,
          featureSetId: resource.featureSetTanagra.id,
        });
      } else if (resource.reviewTanagra) {
        await reviewsApi().deleteReview({
          studyId: resource.workspaceNamespace,
          cohortId: resource.reviewTanagra.cohort.id,
          reviewId: resource.reviewTanagra.id,
        });
      }
      props.onUpdate();
    } catch (error) {
      console.error(error);
    }
  };

  const renameResource = async (displayName: string, description: string) => {
    const {
      cohortTanagra,
      featureSetTanagra,
      reviewTanagra,
      workspaceNamespace,
    } = resourceToRename;
    try {
      if (cohortTanagra) {
        await cohortsApi().updateCohort({
          studyId: workspaceNamespace,
          cohortId: cohortTanagra.id,
          cohortUpdateInfo: {
            displayName,
            description,
            criteriaGroupSections: cohortTanagra.criteriaGroupSections,
          },
        });
      } else if (featureSetTanagra) {
        await featureSetsApi().updateFeatureSet({
          studyId: workspaceNamespace,
          featureSetId: featureSetTanagra.id,
          featureSetUpdateInfo: {
            displayName,
            description,
            criteria: featureSetTanagra.criteria,
          },
        });
      } else if (reviewTanagra) {
        await reviewsApi().updateReview({
          studyId: workspaceNamespace,
          cohortId: reviewTanagra.cohort.id,
          reviewId: reviewTanagra.id,
          reviewUpdateInfo: {
            displayName,
            description,
          },
        });
      }
      setResourceToRename(undefined);
      props.onUpdate();
    } catch (error) {
      console.error(error);
    }
  };

  const duplicateResource = async (resource: TanagraWorkspaceResource) => {
    props.showSpinner();
    switch (getType(resource)) {
      case ResourceType.COHORT:
        await cohortsApi()
          .cloneCohort({
            studyId: resource.workspaceNamespace,
            cohortId: resource.cohortTanagra.id,
            cohortCloneInfo: {
              displayName: `Duplicate of ${resource.cohortTanagra.displayName}`,
              description: resource.cohortTanagra.description,
            },
          })
          .then(() => props.onUpdate())
          .catch((error) => {
            console.error(error);
            props.showErrorModal(
              'Duplicating Cohort Error',
              'Cohort could not be duplicated. Please try again.'
            );
          })
          .finally(() => props.hideSpinner());
        break;
      case ResourceType.CONCEPT_SET:
        await featureSetsApi()
          .cloneFeatureSet({
            studyId: resource.workspaceNamespace,
            featureSetId: resource.featureSetTanagra.id,
            featureSetCloneInfo: {
              displayName: `Duplicate of ${resource.featureSetTanagra.displayName}`,
              description: resource.featureSetTanagra.description,
            },
          })
          .then(() => props.onUpdate())
          .catch((error) => {
            console.error(error);
            props.showErrorModal(
              'Duplicating Feature Set Error',
              'Feature set could not be duplicated. Please try again.'
            );
          })
          .finally(() => props.hideSpinner());
        break;
      default:
        props.hideSpinner();
    }
  };

  const actions = (resource): ResourceAction[] => {
    return [
      {
        icon: 'note',
        displayName: 'Rename',
        onClick: () => setResourceToRename(resource),
        disabled: !canWrite(resource),
      },
      {
        icon: 'copy',
        displayName: 'Duplicate',
        onClick: () => duplicateResource(resource),
        disabled: !canWrite(resource),
      },
      {
        icon: 'trash',
        displayName: 'Delete',
        onClick: () => {
          props.showConfirmDeleteModal(
            getDisplayName(resource),
            getType(resource),
            () => deleteResource(resource)
          );
        },
        disabled: !canDelete(resource),
      },
    ];
  };

  useEffect(() => {
    const { workspaces, workspaceResources } = props;
    if (workspaceResources) {
      setTableData(
        fp.flatMap((r) => {
          const workspace = workspaces.find(
            (w) => w.namespace === r.workspaceNamespace
          );

          // Don't return resources where we no longer have access to the workspace.
          // For example: the owner has unshared the workspace, but a recent-resource entry remains.
          return workspace
            ? [
                {
                  resource: r,
                  workspace,
                  menu: (
                    <ResourceActionsMenu
                      actions={actions(r)}
                      title={`${getTypeString(r)} Action Menu `}
                    />
                  ),
                  resourceType: getTypeString(r),
                  resourceName: getDisplayName(r),
                  lastModifiedForSorting: r.lastModifiedEpochMillis,
                  formattedLastModified: displayDateWithoutHours(
                    r.lastModifiedEpochMillis
                  ),
                  cdrVersionName: getCdrVersionName(r),
                  lastModifiedBy: r.lastModifiedBy,
                  createdBy: getCreatedBy(r),
                },
              ]
            : [];
        }, workspaceResources)
      );
    }
  }, [props.workspaceResources]);

  const displayWorkspace = (rowData) => {
    const { workspace, resource } = rowData;
    return (
      <WorkspaceNavigation
        {...{ workspace, resource }}
        style={styles.navigation}
      />
    );
  };

  const displayResourceType = (rowData) => {
    const { resource } = rowData;
    return <StyledResourceType resource={resource} />;
  };

  const displayResourceName = (rowData) => {
    const {
      resource: {
        cohortTanagra,
        featureSetTanagra,
        reviewTanagra,
        workspaceTerraName,
        workspaceNamespace,
      },
    } = rowData;
    let displayName = '';
    let url = '';
    const urlPrefix = `${dataTabPath(
      workspaceNamespace,
      workspaceTerraName
    )}/tanagra`;
    if (cohortTanagra) {
      displayName = cohortTanagra.displayName;
      url = `${urlPrefix}/cohorts/${cohortTanagra.id}/${
        cohortTanagra.criteriaGroupSections?.[0]?.id ?? 'first'
      }/${
        cohortTanagra.criteriaGroupSections?.[0]?.criteriaGroups?.[0]?.id ??
        'none'
      }`;
    } else if (featureSetTanagra) {
      displayName = featureSetTanagra.displayName;
      url = `${urlPrefix}/featureSets/${featureSetTanagra.id}`;
    } else if (reviewTanagra) {
      displayName = reviewTanagra.displayName;
      url = `${urlPrefix}/reviews/${reviewTanagra.cohort.id}/${reviewTanagra.id}`;
    }
    return (
      <Clickable>
        <Link to={url} style={styles.navigation}>
          <TooltipTrigger content={displayName}>
            <span>{displayName}</span>
          </TooltipTrigger>
        </Link>
      </Clickable>
    );
  };

  const existingNameList = (resource: TanagraWorkspaceResource) =>
    (tableData ?? [])
      .filter((td) => td.resourceType === getTypeString(resource))
      .map(({ resourceName }) => resourceName);

  return (
    <React.Fragment>
      <div data-test-id='resources-table' style={{ flex: 1 }}>
        {tableData?.length > 0 && (
          <DataTable
            filterDisplay='row'
            data-test-id='resource-list'
            value={tableData}
            sortMode='multiple'
            paginator
            breakpoint='0px'
            rows={ROWS_PER_PAGE_RESOURCE_TABLE}
          >
            <Column field='menu' style={styles.menu} />
            <Column
              field='resourceType'
              body={displayResourceType}
              header='Item type'
              sortable
              style={styles.typeColumn}
            />
            <Column
              field='resourceName'
              header='Name'
              body={displayResourceName}
              style={styles.column}
              sortable
              filter
              filterPlaceholder={'Search Name'}
            />
            {props.recentResourceSource && (
              <Column
                field='workspaceName'
                header='Workspace name'
                sortable
                body={displayWorkspace}
                style={styles.column}
              />
            )}
            <Column
              field='formattedLastModified'
              header='Last Modified Date'
              style={styles.modifiedDateColumn}
              sortField='lastModifiedForSorting'
              sortable
            />
            {props.recentResourceSource && (
              <Column
                field='cdrVersionName'
                header='Dataset'
                style={styles.column}
              />
            )}
            <Column
              field='createdBy'
              header='Created By'
              style={styles.column}
            />
          </DataTable>
        )}
      </div>
      {!!resourceToRename && (
        <RenameModal
          onRename={(name, description) => renameResource(name, description)}
          resourceType={getType(resourceToRename)}
          onCancel={() => setResourceToRename(undefined)}
          oldDescription={getDescription(resourceToRename)}
          oldName={getDisplayName(resourceToRename)}
          existingNames={existingNameList(resourceToRename)}
        />
      )}
    </React.Fragment>
  );
});
