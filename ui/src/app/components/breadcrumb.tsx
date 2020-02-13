import * as fp from 'lodash/fp';
import * as React from 'react';

import {dropNotebookFileSuffix} from 'app/pages/analysis/util';
import {InvalidBillingBanner} from 'app/pages/workspace/invalid-billing-banner';
import colors from 'app/styles/colors';
import {
  withCurrentCohort,
  withCurrentConceptSet,
  withCurrentWorkspace,
  withRouteConfigData,
  withUrlParams
} from 'app/utils';
import {BreadcrumbType, navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {BillingStatus, Cohort, ConceptSet} from 'generated/fetch';

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
  conceptSet: ConceptSet,
  urlParams: any
): Array<BreadcrumbData> => {
  const {ns, wsid, cid, csid, pid, nbName} = urlParams;
  const prefix = `/workspaces/${ns}/${wsid}`;
  switch (type) {
    case BreadcrumbType.Workspaces:
      return [
        new BreadcrumbData('Workspaces', '/workspaces')
      ];
    case BreadcrumbType.Workspace:
      return [
        ...getTrail(BreadcrumbType.Workspaces, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData(workspace ? workspace.name : '...', `${prefix}/data`)
      ];
    case BreadcrumbType.WorkspaceEdit:
      return [
        ...getTrail(BreadcrumbType.Workspace, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData('Edit Workspace', `${prefix}/edit`)
      ];
    case BreadcrumbType.WorkspaceDuplicate:
      return [
        ...getTrail(BreadcrumbType.Workspace, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData('Duplicate Workspace', `${prefix}/duplicate`)
      ];
    case BreadcrumbType.Notebook:
      return [
        ...getTrail(BreadcrumbType.Workspace, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData('Notebooks', `${prefix}/notebooks`),
        new BreadcrumbData(
          nbName && dropNotebookFileSuffix(decodeURIComponent(nbName)),
          `${prefix}/notebooks/${nbName}`
        )
      ];
    case BreadcrumbType.ConceptSet:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData(
          conceptSet
            ? conceptSet.name
            : '...', `${prefix}/data/concepts/sets/${csid}`
        )
      ];
    case BreadcrumbType.Cohort:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData(
          cohort ? cohort.name : '...',
          `${prefix}/data/cohorts/${cid}/review/participants`
        )
      ];
    case BreadcrumbType.Participant:
      return [
        ...getTrail(BreadcrumbType.Cohort, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData(
          `Participant ${pid}`,
          `${prefix}/data/cohorts/${cid}/review/participants/${pid}`
        )
      ];
    case BreadcrumbType.CohortAdd:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData('Build Cohort Criteria', `${prefix}/data/cohorts/build`)
      ];
    case BreadcrumbType.SearchConcepts:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData('Search Concepts', `${prefix}/data/concepts`)
      ];
    case BreadcrumbType.Dataset:
      return [
        ...getTrail(BreadcrumbType.Data, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData('Dataset', `${prefix}/data/datasets`)
      ];
    case BreadcrumbType.Data:
      return [
        ...getTrail(BreadcrumbType.Workspaces, workspace, cohort, conceptSet, urlParams),
        new BreadcrumbData(workspace ? workspace.name : '...', `${prefix}/data`)
      ];
    default: return [];
  }
};

const BreadcrumbLink = ({href, ...props}) => {
  return <a
    href={href}
    onClick={e => {
      navigateAndPreventDefaultIfNoKeysPressed(e, href);
    }}
    {...props}
  />;
};

interface Props {
  workspace: WorkspaceData;
  cohort: Cohort;
  conceptSet: ConceptSet;
  urlParams: any;
  routeConfigData: any;
}

interface State {
  showInvalidBillingBanner: boolean;
}

export const Breadcrumb = fp.flow(
  withCurrentWorkspace(),
  withCurrentCohort(),
  withCurrentConceptSet(),
  withUrlParams(),
  withRouteConfigData()
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
      return getTrail(
        this.props.routeConfigData.breadcrumb,
        this.props.workspace,
        this.props.cohort,
        this.props.conceptSet,
        this.props.urlParams
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
