import * as React from 'react';
import { Link, matchPath } from 'react-router-dom';
import * as fp from 'lodash/fp';

import {
  BillingStatus,
  Cohort,
  CohortReview,
  ConceptSet,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { dropJupyterNotebookFileSuffix } from 'app/pages/analysis/util';
import { InvalidBillingBanner } from 'app/pages/workspace/invalid-billing-banner';
import {
  analysisTabName,
  analysisTabPath,
  appDisplayPath,
  dataTabPath,
  workspacePath,
} from 'app/routing/utils';
import colors from 'app/styles/colors';
import {
  withCurrentCohort,
  withCurrentCohortReview,
  withCurrentConceptSet,
  withCurrentWorkspace,
} from 'app/utils';
import {
  MatchParams,
  RouteDataStore,
  routeDataStore,
  withStore,
} from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { BreadcrumbType } from './breadcrumb-type';

const styles = {
  firstLink: {
    color: colors.accent,
    textDecoration: 'none',
  },
  lastLink: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: '1.5rem',
    textDecoration: 'none',
  },
};

class BreadcrumbData {
  label: string;
  url: string;

  constructor(label: string, url: string) {
    this.label = label;
    this.url = url;
  }
}

// Generates a trail of breadcrumbs based on currently loaded data.
export const getTrail = (
  type: BreadcrumbType,
  workspace: WorkspaceData,
  cohort: Cohort,
  cohortReview: CohortReview,
  conceptSet: ConceptSet,
  params: MatchParams
): Array<BreadcrumbData> => {
  const { ns, terraName, cid, crid, csid, pid, nbName, appType } = params;
  switch (type) {
    case BreadcrumbType.UserApp:
      return [
        ...getTrail(
          BreadcrumbType.Workspace,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          fp.upperFirst(analysisTabName),
          analysisTabPath(ns, terraName)
        ),
        new BreadcrumbData(appType, appDisplayPath(ns, terraName, appType)),
      ];
    case BreadcrumbType.Workspaces:
      return [new BreadcrumbData('Workspaces', '/workspaces')];
    case BreadcrumbType.Workspace:
      return [
        ...getTrail(
          BreadcrumbType.Workspaces,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          workspace ? workspace.name : '...',
          dataTabPath(ns, terraName)
        ),
      ];
    case BreadcrumbType.WorkspaceEdit:
      return [
        ...getTrail(
          BreadcrumbType.Workspace,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          'Edit Workspace',
          `${workspacePath(ns, terraName)}/edit`
        ),
      ];
    case BreadcrumbType.WorkspaceDuplicate:
      return [
        ...getTrail(
          BreadcrumbType.Workspace,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          'Duplicate Workspace',
          `${workspacePath(ns, terraName)}/duplicate`
        ),
      ];
    case BreadcrumbType.Analysis:
      return [
        ...getTrail(
          BreadcrumbType.Workspace,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          fp.upperFirst(analysisTabName),
          analysisTabPath(ns, terraName)
        ),
        new BreadcrumbData(
          nbName && dropJupyterNotebookFileSuffix(decodeURIComponent(nbName)),
          `${analysisTabPath(ns, terraName)}/${nbName}`
        ),
      ];
    case BreadcrumbType.AnalysisPreview:
      return [
        ...getTrail(
          BreadcrumbType.Workspace,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          fp.upperFirst(analysisTabName),
          analysisTabPath(ns, terraName)
        ),
        new BreadcrumbData(
          nbName && dropJupyterNotebookFileSuffix(decodeURIComponent(nbName)),
          `${analysisTabPath(ns, terraName)}/preview/${nbName}`
        ),
      ];
    case BreadcrumbType.ConceptSet:
      return [
        ...getTrail(
          BreadcrumbType.Data,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          conceptSet ? conceptSet.name : '...',
          `${dataTabPath(ns, terraName)}/concepts/sets/${csid}`
        ),
      ];
    case BreadcrumbType.Cohort:
      return [
        ...getTrail(
          BreadcrumbType.Data,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          cohort ? cohort.name : '...',
          `${dataTabPath(ns, terraName)}/cohorts/${cid}`
        ),
      ];
    case BreadcrumbType.CohortReview:
      return [
        ...getTrail(
          BreadcrumbType.Data,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          cohortReview ? cohortReview.cohortName : '...',
          `${dataTabPath(ns, terraName)}/cohorts/${cid}/reviews/${crid}`
        ),
      ];
    case BreadcrumbType.Participant:
      return [
        ...getTrail(
          BreadcrumbType.CohortReview,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          `Participant ${pid}`,
          `${dataTabPath(
            ns,
            terraName
          )}/cohorts/${cid}/reviews/${crid}/participants/${pid}`
        ),
      ];
    case BreadcrumbType.CohortAdd:
      return [
        ...getTrail(
          BreadcrumbType.Data,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          'Build Cohort Criteria',
          `${dataTabPath(ns, terraName)}/cohorts/build`
        ),
      ];
    case BreadcrumbType.SearchConcepts:
      return [
        ...getTrail(
          BreadcrumbType.Data,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          'Search Concepts',
          `${dataTabPath(ns, terraName)}/concepts`
        ),
      ];
    case BreadcrumbType.Dataset:
      return [
        ...getTrail(
          BreadcrumbType.Data,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData('Dataset', `${dataTabPath(ns, terraName)}/datasets`),
      ];
    case BreadcrumbType.Data:
      return [
        ...getTrail(
          BreadcrumbType.Workspaces,
          workspace,
          cohort,
          cohortReview,
          conceptSet,
          params
        ),
        new BreadcrumbData(
          workspace ? workspace.name : '...',
          `${dataTabPath(ns, terraName)}`
        ),
      ];
    default:
      return [];
  }
};

const BreadcrumbLink = ({ href, ...props }) => {
  return <Link to={href} {...props} />;
};

interface Props {
  workspace: WorkspaceData;
  cohort: Cohort;
  cohortReview: CohortReview;
  conceptSet: ConceptSet;
  routeData: RouteDataStore;
}

interface State {
  showInvalidBillingBanner: boolean;
}

export const Breadcrumb = fp.flow(
  withCurrentWorkspace(),
  withCurrentCohort(),
  withCurrentCohortReview(),
  withCurrentConceptSet(),
  withStore(routeDataStore, 'routeData')
)(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        showInvalidBillingBanner: false,
      };
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (
        !prevProps.workspace &&
        this.props.workspace &&
        this.props.workspace.billingStatus === BillingStatus.INACTIVE
      ) {
        this.setState({ showInvalidBillingBanner: true });
      } else if (prevProps.workspace && !this.props.workspace) {
        this.setState({ showInvalidBillingBanner: false });
      } else if (
        prevProps.workspace &&
        this.props.workspace &&
        prevProps.workspace !== this.props.workspace
      ) {
        // Workspace was reloaded
        if (
          prevProps.workspace.billingStatus !==
          this.props.workspace.billingStatus
        ) {
          this.setState({
            showInvalidBillingBanner:
              this.props.workspace.billingStatus === BillingStatus.INACTIVE,
          });
        }
      }
    }

    trail(): Array<BreadcrumbData> {
      const workspaceMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:terraName',
      });
      const { ns = '', terraName = '' } = workspaceMatch
        ? workspaceMatch.params
        : {};

      const cohortMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:terraName/data/cohorts/:cid',
      });
      const { cid = '' } = cohortMatch ? cohortMatch.params : {};

      const conceptSetMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:terraName/data/concepts/sets/:csid',
      });
      const { csid = '' } = conceptSetMatch ? conceptSetMatch.params : {};

      const participantMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:terraName/data/cohorts/:cid/review/participants/:pid',
      });
      const { pid = '' } = participantMatch ? participantMatch.params : {};

      // WARNING
      // because this pattern *also* matches previews and user apps, it must be checked AFTER those in the cond()
      const analysisMatch = matchPath<MatchParams>(location.pathname, {
        path: `/workspaces/:ns/:terraName/${analysisTabName}/:nbName`,
      });

      const analysisPreviewMatch = matchPath<MatchParams>(location.pathname, {
        path: `/workspaces/:ns/:terraName/${analysisTabName}/preview/:nbName`,
      });

      const userAppMatch = matchPath<MatchParams>(location.pathname, {
        path: `/workspaces/:ns/:terraName/${analysisTabName}/userApp/:appType`,
      });

      const {
        nbName = '',
        appType = '',
        breadcrumbType,
      } = cond<MatchParams & { breadcrumbType: BreadcrumbType }>(
        [
          !!analysisPreviewMatch,
          () => ({
            ...analysisPreviewMatch.params,
            breadcrumbType: BreadcrumbType.AnalysisPreview,
          }),
        ],
        [
          !!userAppMatch,
          () => ({
            ...userAppMatch.params,
            breadcrumbType: BreadcrumbType.UserApp,
          }),
        ],
        [
          // this check must go after analysisPreviewMatch and userAppMatch
          !!analysisMatch,
          () => ({
            ...analysisMatch.params,
            breadcrumbType: BreadcrumbType.Analysis,
          }),
        ],
        () => ({ breadcrumbType: this.props.routeData.breadcrumb })
      );

      return getTrail(
        breadcrumbType,
        this.props.workspace,
        this.props.cohort,
        this.props.cohortReview,
        this.props.conceptSet,
        { ns, terraName, cid, csid, pid, nbName, appType }
      );
    }

    first(): Array<BreadcrumbData> {
      return fp.dropRight(1, this.trail());
    }

    last(): BreadcrumbData {
      return fp.last(this.trail());
    }

    render() {
      return (
        <React.Fragment>
          {this.state.showInvalidBillingBanner && (
            <InvalidBillingBanner
              onClose={() => this.setState({ showInvalidBillingBanner: false })}
            />
          )}

          <div
            style={{
              marginLeft: '4.875rem',
              display: 'inline-block',
            }}
          >
            {this.first().map(({ label, url }, i) => {
              return (
                <React.Fragment key={i}>
                  <BreadcrumbLink href={url} style={styles.firstLink}>
                    {label}
                  </BreadcrumbLink>
                  <span
                    style={{
                      color: colors.primary,
                    }}
                  >
                    {' '}
                    &gt;{' '}
                  </span>
                </React.Fragment>
              );
            })}
            {this.last() && (
              <div>
                <BreadcrumbLink href={this.last().url} style={styles.lastLink}>
                  {this.last().label}
                </BreadcrumbLink>
              </div>
            )}
          </div>
        </React.Fragment>
      );
    }
  }
);
