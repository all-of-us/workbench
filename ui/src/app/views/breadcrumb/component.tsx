import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {ReactWrapperBase, withCurrentCohort, withCurrentConceptSet, withCurrentWorkspace, withRouteConfigData, withUrlParams} from 'app/utils/index';
import {navigateByUrl} from 'app/utils/navigation';

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

export const getTrail = (type, data) => {
  const {workspace, cohort, conceptSet, urlParams: {ns, wsid, cid, csid, pid, nbName}} = data;
  const prefix = `/workspaces/${ns}/${wsid}`;
  switch (type) {
    case 'workspaces':
      return [
        {label: 'Workspaces', url: '/workspaces'}
      ];
    case 'workspace':
      return [
        ...getTrail('workspaces', data),
        {label: workspace ? workspace.name : '...', url: prefix}
      ];
    case 'workspaceEdit':
      return [
        ...getTrail('workspace', data),
        {label: 'Edit Workspace', url: `${prefix}/edit`}
      ];
    case 'workspaceClone':
      return [
        ...getTrail('workspace', data),
        {label: 'Duplicate Workspace', url: `${prefix}/clone`}
      ];
    case 'notebook':
      return [
        ...getTrail('workspace', data),
        {label: 'Notebooks', url: `${prefix}/notebooks`},
        {
          label: nbName && decodeURIComponent(nbName).replace(/\.ipynb$/, ''),
          url: `${prefix}/notebooks/${nbName}`
        }
      ];
    case 'conceptSet':
      return [
        ...getTrail('workspace', data),
        {label: 'Concept Sets', url: `${prefix}/concepts/sets`},
        {label: conceptSet ? conceptSet.name : '...', url: `${prefix}/concepts/sets/${csid}`}
      ];
    case 'cohort':
      return [
        ...getTrail('workspace', data),
        {label: 'Cohorts', url: `${prefix}/cohorts`},
        {label: cohort ? cohort.name : '...', url: `${prefix}/cohorts/${cid}/review/participants`}
      ];
    case 'participant':
      return [
        ...getTrail('cohort', data),
        {label: `Participant ${pid}`, url: `${prefix}/cohorts/${cid}/review/participants/${pid}`}
      ];
    case 'cohortAdd':
      return [
        ...getTrail('workspace', data),
        {label: 'Build Cohort Criteria', url: `${prefix}/cohorts/build`}
      ];
    case 'dataset':
      return [
        ...getTrail('workspace', data),
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
  const last = fp.last(trail) as any;
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
