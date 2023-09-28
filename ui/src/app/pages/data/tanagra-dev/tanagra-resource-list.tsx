import * as React from 'react';
import { CSSProperties, useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  CdrVersionTiersResponse,
  Workspace,
  WorkspaceResource,
} from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import {
  Action,
  ResourceActionsMenu,
} from 'app/components/resource-actions-menu';
import { canDelete } from 'app/components/resource-card';
import {
  withConfirmDeleteModal,
  WithConfirmDeleteModalProps,
} from 'app/components/with-confirm-delete-modal';
import { TanagraWorkspaceResource } from 'app/pages/data/tanagra-dev/data-component-tanagra';
import {
  cohortsApi,
  conceptSetsApi,
  reviewsApi,
} from 'app/services/tanagra-swagger-fetch-clients';
import { reactStyles, withCdrVersions } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { ROWS_PER_PAGE_RESOURCE_TABLE } from 'app/utils/constants';
import { displayDate, displayDateWithoutHours } from 'app/utils/dates';
import {
  getDisplayName,
  getType,
  getTypeString,
  isNotebook,
} from 'app/utils/resources';
import { analysisTabName } from 'app/utils/user-apps-utils';
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
    workspace: { name, namespace, id },
    resource,
    style,
  } = props;
  const tab = isNotebook(resource) ? analysisTabName : 'data';
  const url = `/workspaces/${namespace}/${id}/${tab}`;

  return (
    <Clickable>
      <RouterLink to={url} style={style} data-test-id='workspace-navigation'>
        {name}
      </RouterLink>
    </Clickable>
  );
};

interface TableData {
  menu: JSX.Element;
  resourceType: string;
  resourceName: string;
  formattedLastModified: string;
  lastModifiedDateAsString: string;
  lastModifiedBy: string;
  cdrVersionName: string;
  resource: WorkspaceResource;
  workspace: Workspace;
}

interface Props extends WithConfirmDeleteModalProps {
  existingNameList: string[];
  workspaceResources: TanagraWorkspaceResource[];
  onUpdate: Function;
  workspaces: WorkspaceData[];
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  recentResourceSource?: boolean;
}

export const TanagraResourceList = fp.flow(
  withCdrVersions(),
  withConfirmDeleteModal()
)((props: Props) => {
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
      } else if (resource.conceptSetTanagra) {
        await conceptSetsApi().deleteConceptSet({
          studyId: resource.workspaceNamespace,
          conceptSetId: resource.conceptSetTanagra.id,
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

  const actions = (resource): Action[] => {
    return [
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
                  menu: <ResourceActionsMenu actions={actions(r)} />,
                  resourceType: getTypeString(r),
                  resourceName: getDisplayName(r),
                  formattedLastModified: displayDateWithoutHours(
                    r.lastModifiedEpochMillis
                  ),
                  lastModifiedDateAsString: displayDate(
                    r.lastModifiedEpochMillis
                  ),
                  cdrVersionName: getCdrVersionName(r),
                  lastModifiedBy: r.lastModifiedBy,
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
    if (resource.cohortV2) {
      return 'Cohort';
    } else if (resource.conceptSetV2) {
      return 'Concept Set';
    } else if (resource.reviewV2) {
      return 'Cohort Review';
    }
  };

  const displayResourceName = (rowData) => {
    const {
      resource: {
        cohortV2,
        conceptSetV2,
        reviewV2,
        workspaceFirecloudName,
        workspaceNamespace,
      },
    } = rowData;
    let displayName = '';
    let url = '';
    const urlPrefix = `/workspaces/${workspaceNamespace}/${workspaceFirecloudName}/data/tanagra`;
    if (cohortV2) {
      displayName = cohortV2.displayName;
      url = `${urlPrefix}/cohorts/${cohortV2.id}/${
        cohortV2.criteriaGroupSections?.[0]?.id ?? 'first'
      }`;
    } else if (conceptSetV2) {
      const domain = JSON.parse(conceptSetV2.criteria.uiConfig)?.title ?? '';
      const selection =
        JSON.parse(conceptSetV2.criteria.selectionData)?.selected?.[0]?.name ??
        '';
      displayName = `${domain}: ${selection}`;
      url = `${urlPrefix}/conceptSets/edit/${conceptSetV2.id}`;
    } else if (reviewV2) {
      displayName = reviewV2.displayName;
      url = `${urlPrefix}/reviews/${reviewV2.cohort.id}/${reviewV2.id}`;
    }
    return (
      <Clickable>
        <RouterLink to={url} style={styles.navigation}>
          <TooltipTrigger content={displayName}>
            <span>{displayName}</span>
          </TooltipTrigger>
        </RouterLink>
      </Clickable>
    );
  };

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
              sortField={'lastModifiedDateAsString'}
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
              field='lastModifiedBy'
              header='Last Modified By'
              style={styles.column}
            />
          </DataTable>
        )}
      </div>
    </React.Fragment>
  );
});
