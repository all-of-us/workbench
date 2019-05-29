import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {
  ReactWrapperBase,
  withCurrentCohort,
  withCurrentConceptSet,
  withCurrentWorkspace,
  withRouteConfigData,
  withUrlParams
} from 'app/utils';
import {BreadcrumbType, navigateByUrl} from 'app/utils/navigation';

const styles = {
  firstLink: {
    color: '#2691D0',
    textDecoration: 'none'
  },
  lastLink: {
    color: '#262262',
    fontWeight: 600,
    fontSize: '1rem',
    textDecoration: 'none'
  }
};

// Generates a trail of breadcrumbs based on currently loaded data.
export const getTrail = (type: BreadcrumbType, data): {label: string, url: string}[] => {
  const {workspace, cohort, conceptSet, urlParams: {ns, wsid, cid, csid, pid, nbName}} = data;
  const prefix = `/workspaces/${ns}/${wsid}`;
  switch (type) {
    case BreadcrumbType.Workspaces:
      return [
        {label: 'Workspaces', url: '/workspaces'}
      ];
    case BreadcrumbType.Workspace:
      return [
        ...getTrail(BreadcrumbType.Workspaces, data),
        {label: workspace ? workspace.name : '...', url: prefix}
      ];
    case BreadcrumbType.WorkspaceEdit:
      return [
        ...getTrail(BreadcrumbType.Workspace, data),
        {label: 'Edit Workspace', url: `${prefix}/edit`}
      ];
    case BreadcrumbType.WorkspaceDuplicate:
      return [
        ...getTrail(BreadcrumbType.Workspace, data),
        {label: 'Duplicate Workspace', url: `${prefix}/duplicate`}
      ];
    case BreadcrumbType.Notebook:
      return [
        ...getTrail(BreadcrumbType.Workspace, data),
        {label: 'Notebooks', url: `${prefix}/notebooks`},
        {
          label: nbName && decodeURIComponent(nbName).replace(/\.ipynb$/, ''),
          url: `${prefix}/notebooks/${nbName}`
        }
      ];
    case BreadcrumbType.ConceptSet:
      return [
        ...getTrail(BreadcrumbType.Workspace, data),
        {label: 'Concept Sets', url: `${prefix}/concepts/sets`},
        {label: conceptSet ? conceptSet.name : '...', url: `${prefix}/concepts/sets/${csid}`}
      ];
    case BreadcrumbType.Cohort:
      return [
        ...getTrail(BreadcrumbType.Workspace, data),
        {label: 'Cohorts', url: `${prefix}/cohorts`},
        {label: cohort ? cohort.name : '...', url: `${prefix}/cohorts/${cid}/review/participants`}
      ];
    case BreadcrumbType.Participant:
      return [
        ...getTrail(BreadcrumbType.Cohort, data),
        {label: `Participant ${pid}`, url: `${prefix}/cohorts/${cid}/review/participants/${pid}`}
      ];
    case BreadcrumbType.CohortAdd:
      return [
        ...getTrail(BreadcrumbType.Workspace, data),
        {label: 'Build Cohort Criteria', url: `${prefix}/cohorts/build`}
      ];
    case BreadcrumbType.Dataset:
      return [
        ...getTrail(BreadcrumbType.Workspace, data),
        {label: 'Dataset', url: `${prefix}/data/datasets`}
      ];
    default: return [];
  }
};

const BreadcrumbLink = ({href, ...props}) => {
  return <a
    href={href}
    onClick={e => {
      e.preventDefault();
      navigateByUrl(href);
    }}
    {...props}
  />;
};

const Breadcrumb = fp.flow(
  withCurrentWorkspace(),
  withCurrentCohort(),
  withCurrentConceptSet(),
  withUrlParams(),
  withRouteConfigData()
)(({workspace, cohort, conceptSet, urlParams, routeConfigData}) => {
  const trail = getTrail(
    routeConfigData.breadcrumb,
    {urlParams, workspace, conceptSet, cohort}
  );
  const first = fp.dropRight(1, trail);
  const last = fp.last(trail);
  return <div style={{marginLeft: '3.25rem', display: 'inline-block'}}>
    {first.map(({label, url}, i) => {
      return <React.Fragment key={i}>
        <BreadcrumbLink href={url} style={styles.firstLink}>
          {label}
        </BreadcrumbLink>
        <span style={{color: '#c3c3c3'}}> &gt; </span>
      </React.Fragment>;
    })}
    {last && <div>
      <BreadcrumbLink href={last.url} style={styles.lastLink}>
        {last.label}
      </BreadcrumbLink>
    </div>}
  </div>;
});

@Component({
  selector: 'app-breadcrumb',
  template: '<div #root></div>'
})
export class BreadcrumbComponent extends ReactWrapperBase {
  constructor() {
    super(Breadcrumb, []);
  }
}
