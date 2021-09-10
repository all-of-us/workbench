import {dropNotebookFileSuffix} from 'app/pages/analysis/util';
import {InvalidBillingBanner} from 'app/pages/workspace/invalid-billing-banner';
import colors from 'app/styles/colors';
import {
  withCurrentCohort,
  withCurrentCohortReview,
  withCurrentConceptSet,
  withCurrentWorkspace
} from 'app/utils';
import {
  BreadcrumbType
} from 'app/utils/navigation';
import {MatchParams, RouteDataStore, routeDataStore, withStore} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {BillingStatus, Cohort, CohortReview, ConceptSet} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Link as RouterLink, matchPath} from 'react-router-dom';

const styles = {
  firstLink: {
    color: colors.accent,
    textDecoration: 'none'
  },
  lastLink: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: '1rem',
    textDecoration: 'none'
  }
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
  const {ns, wsid, cid, csid, pid, nbName} = params;
  const prefix = `/workspaces/${ns}/${wsid}`;
  switch (type) {
    case BreadcrumbType.Workspaces:
      return [
        new BreadcrumbData('Workspaces', '/workspaces')
      ];
    case BreadcrumbType.Workspace:
      return [
        ...getTrail(BreadcrumbType.Workspaces, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData(workspace ? workspace.name : '...', `${prefix}/data`)
      ];
    case BreadcrumbType.WorkspaceEdit:
      return [
        ...getTrail(BreadcrumbType.Workspace, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData('Edit Workspace', `${prefix}/edit`)
      ];
    case BreadcrumbType.WorkspaceDuplicate:
      return [
        ...getTrail(BreadcrumbType.Workspace, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData('Duplicate Workspace', `${prefix}/duplicate`)
      ];
    case BreadcrumbType.Notebook:
      return [
        ...getTrail(BreadcrumbType.Workspace, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData('Notebooks', `${prefix}/notebooks`),
        new BreadcrumbData(
          nbName && dropNotebookFileSuffix(decodeURIComponent(nbName)),
          `${prefix}/notebooks/${nbName}`
        )
      ];
    case BreadcrumbType.ConceptSet:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData(
          conceptSet
            ? conceptSet.name
            : '...', `${prefix}/data/concepts/sets/${csid}`
        )
      ];
    case BreadcrumbType.Cohort:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData(
          cohort ? cohort.name : '...',
          `${prefix}/data/cohorts/${cid}/review/participants`
        )
      ];
    case BreadcrumbType.CohortReview:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData(
          cohortReview ? cohortReview.cohortName : '...',
          `${prefix}/data/cohorts/${cid}/review/participants`
        )
      ];
    case BreadcrumbType.Participant:
      return [
        ...getTrail(BreadcrumbType.CohortReview, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData(
          `Participant ${pid}`,
          `${prefix}/data/cohorts/${cid}/review/participants/${pid}`
        )
      ];
    case BreadcrumbType.CohortAdd:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData('Build Cohort Criteria', `${prefix}/data/cohorts/build`)
      ];
    case BreadcrumbType.SearchConcepts:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData('Search Concepts', `${prefix}/data/concepts`)
      ];
    case BreadcrumbType.Dataset:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData('Dataset', `${prefix}/data/datasets`)
      ];
    case BreadcrumbType.Data:
      return [
        ...getTrail(BreadcrumbType.Workspaces, workspace, cohort, cohortReview, conceptSet, params),
        new BreadcrumbData(workspace ? workspace.name : '...', `${prefix}/data`)
      ];
    default: return [];
  }
};

const BreadcrumbLink = ({href, ...props}) => {
  return <RouterLink to={href} {...props}/>;
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
  withStore(routeDataStore, 'routeData'),
)(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        showInvalidBillingBanner: false
      };
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (!prevProps.workspace && this.props.workspace &&
        this.props.workspace.billingStatus === BillingStatus.INACTIVE) {
        this.setState({showInvalidBillingBanner: true});
      } else if (prevProps.workspace && !this.props.workspace) {
        this.setState({showInvalidBillingBanner: false});
      } else if (prevProps.workspace && this.props.workspace && prevProps.workspace !== this.props.workspace) {
        // Workspace was reloaded
        if (prevProps.workspace.billingStatus !== this.props.workspace.billingStatus) {
          this.setState({showInvalidBillingBanner: this.props.workspace.billingStatus === BillingStatus.INACTIVE});
        }
      }
    }

    trail(): Array<BreadcrumbData> {
      const workspaceMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:wsid'
      });
      const {ns = '', wsid = ''} = workspaceMatch ? workspaceMatch.params : {};

      const cohortMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:wsid/data/cohorts/:cid'
      });
      const {cid = ''} = cohortMatch ? cohortMatch.params : {};

      const conceptSetMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:wsid/data/concepts/sets/:csid'
      });
      const {csid = ''} = conceptSetMatch ? conceptSetMatch.params : {};

      const participantMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:wsid/data/cohorts/:cid/review/participants/:pid'
      });
      const {pid = ''} = participantMatch ? participantMatch.params : {};

      const notebookMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:wsid/notebooks/:nbName'
      });
      const notebookPreviewMatch = matchPath<MatchParams>(location.pathname, {
        path: '/workspaces/:ns/:wsid/notebooks/preview/:nbName'
      });
      const nbName = notebookMatch
        ? notebookMatch.params.nbName
        : notebookPreviewMatch
          ? notebookPreviewMatch.params.nbName
          : undefined;

      return getTrail(
        this.props.routeData.breadcrumb,
        this.props.workspace,
        this.props.cohort,
        this.props.cohortReview,
        this.props.conceptSet,
        {ns: ns, wsid: wsid, cid: cid, csid: csid, pid: pid, nbName: nbName}
      );
    }

    first(): Array<BreadcrumbData> {
      return fp.dropRight(1, this.trail());
    }

    last(): BreadcrumbData {
      return fp.last(this.trail());
    }

    render() {
      return <React.Fragment>
        {this.state.showInvalidBillingBanner &&
        <InvalidBillingBanner onClose={() => this.setState({showInvalidBillingBanner: false})}/>}

        <div style={{
          marginLeft: '3.25rem',
          display: 'inline-block',
        }}>
          {this.first().map(({label, url}, i) => {
            return <React.Fragment key={i}>
              <BreadcrumbLink href={url} style={styles.firstLink}>
                {label}
              </BreadcrumbLink>
              <span style={{
                color: colors.primary
              }}> &gt; </span>
            </React.Fragment>;
          })}
          {this.last() && <div>
            <BreadcrumbLink href={this.last().url} style={styles.lastLink}>
              {this.last().label}
            </BreadcrumbLink>
          </div>}
        </div>
      </React.Fragment>;
    }
  }
);
