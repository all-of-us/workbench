import pathToRegexp from 'path-to-regexp';
import * as qs from 'qs';

import {history} from 'app/services/history';
import {BreadcrumbType} from 'app/utils/navigation';
import {AdminReviewWorkspace} from 'app/views/admin-review-workspace/component';
import {AdminUser} from 'app/views/admin-user/component';
import {CohortList} from 'app/views/cohort-list/component';
import {ConceptHomepage} from 'app/views/concept-homepage/component';
import {ConceptSetDetails} from 'app/views/concept-set-details/component';
import {ConceptSetList} from 'app/views/concept-set-list/component';
import {DataPage} from 'app/views/data-page/component';
import {DataSet} from 'app/views/dataset/component';
import {Homepage} from 'app/views/homepage/component';
import {NotebookList} from 'app/views/notebook-list/component';
import {NotebookRedirect} from 'app/views/notebook-redirect/component';
import {ProfilePage} from 'app/views/profile-page/component';
import {Settings} from 'app/views/settings/component';
import {SignIn} from 'app/views/sign-in/component';
import {StigmatizationPage} from 'app/views/stigmatization-page/component';
import {Unregistered} from 'app/views/unregistered/component';
import {WorkspaceEdit, WorkspaceEditMode} from 'app/views/workspace-edit/component';
import {WorkspaceList} from 'app/views/workspace-list/component';
import {WorkspaceWrapper} from 'app/views/workspace-wrapper/component';
import {Workspace} from 'app/views/workspace/component';

const routes = [
  {
    name: 'login',
    path: '/login',
    render: () => <SignIn />,
    title: 'Sign In',
    public: true
  },
  {
    name: 'homepage',
    path: '/',
    render: () => <Homepage />,
    title: 'Homepage',
  },
  {
    name: 'unregistered',
    path: '/unregistered',
    render: () => <Unregistered />,
    title: 'Awaiting ID Verification'
  },
  {
    name: 'stigmatization',
    path: '/definitions/stigmatization',
    render: () => <StigmatizationPage />,
    title: 'Stigmatization Definition'
  },
  {
    name: 'nihCallback',
    path: '/nih-callback',
    render: () => <Homepage />,
    title: 'Homepage',
  },
  {
    name: 'workspaces',
    path: '/workspaces',
    render: () => <WorkspaceList />,
    title: 'View Workspaces',
    breadcrumb: BreadcrumbType.Workspaces
  },
  {
    name: 'workspace',
    path: '/workspaces/:ns/:wsid',
    render: () => <WorkspaceWrapper><Workspace /></WorkspaceWrapper>,
    title: 'View Workspace Details',
    breadcrumb: BreadcrumbType.Workspace
  },
  {
    name: 'workspaceEdit',
    path: '/workspaces/:ns/:wsid/edit',
    render: () => <WorkspaceWrapper>
      <WorkspaceEdit mode={WorkspaceEditMode.Edit}/>
    </WorkspaceWrapper>,
    title: 'Edit Workspace',
    breadcrumb: BreadcrumbType.WorkspaceEdit
  },
  {
    name: 'workspaceClone',
    path: '/workspaces/:ns/:wsid/clone',
    render: () => <WorkspaceWrapper>
      <WorkspaceEdit mode={WorkspaceEditMode.Clone}/>
    </WorkspaceWrapper>,
    title: 'Clone Workspace',
    breadcrumb: BreadcrumbType.WorkspaceClone
  },
  {
    name: 'notebooks',
    path: '/workspaces/:ns/:wsid/notebooks',
    render: () => <WorkspaceWrapper><NotebookList /></WorkspaceWrapper>,
    title: 'View Notebooks',
    breadcrumb: BreadcrumbType.Workspace
  },
  {
    name: 'notebook',
    path: '/workspaces/:ns/:wsid/notebooks/:nbName',
    render: () => <WorkspaceWrapper minimizeChrome><NotebookRedirect /></WorkspaceWrapper>,
    title: 'Notebook',
    breadcrumb: BreadcrumbType.Notebook,
  }
  {
    name: 'cohorts',
    path: '/workspaces/:ns/:wsid/notebooks/cohorts',
    render: () => <WorkspaceWrapper><CohortList /></WorkspaceWrapper>,
    title: 'View Cohorts',
    breadcrumb: BreadcrumbType.Workspace
  },
  {
    name: 'cohortAdd',
    path: '/workspaces/:ns/:wsid/notebooks/cohorts/build',
    render: () => <WorkspaceWrapper><CohortSearch /></WorkspaceWrapper>,
    title: 'Build Cohort Criteria',
    breadcrumb: BreadcrumbType.CohortAdd
  },
  {
    name: 'cohortReview',
    path: '/workspaces/:ns/:wsid/notebooks/cohorts/:cid/review',
    render: () => <WorkspaceWrapper><PageLayout /></WorkspaceWrapper>,
    title: 'Review Cohort Participants',
    breadcrumb: BreadcrumbType.Cohort
  },
  {
    name: 'cohortParticipants',
    path: '/workspaces/:ns/:wsid/notebooks/cohorts/:cid/review/participants',
    render: () => <WorkspaceWrapper><PageLayout><TablePage /></PageLayout></WorkspaceWrapper>,
    title: 'Review Cohort Participants',
    breadcrumb: BreadcrumbType.Cohort
  },
  {
    name: 'cohortParticipant',
    path: '/workspaces/:ns/:wsid/notebooks/cohorts/:cid/review/participants/:pid',
    render: () => <WorkspaceWrapper><PageLayout><DetailPage /></PageLayout></WorkspaceWrapper>,
    title: 'Review Cohort Participants',
    breadcrumb: BreadcrumbType.Participant
  },
  {
    name: 'concepts',
    path: '/workspaces/:ns/:wsid/concepts',
    render: () => <WorkspaceWrapper><ConceptHomepage /></WorkspaceWrapper>,
    title: 'Search Concepts',
    breadcrumb: BreadcrumbType.Workspace
  },
  {
    name: 'data',
    path: '/workspaces/:ns/:wsid/data',
    render: () => <WorkspaceWrapper><DataPage /></WorkspaceWrapper>,
    title: 'Data Page',
    breadcrumb: BreadcrumbType.Workspace
  },
  {
    name: 'datasets',
    path: '/workspaces/:ns/:wsid/data/datasets',
    render: () => <WorkspaceWrapper><DataSet /></WorkspaceWrapper>,
    title: 'Dataset Page',
    breadcrumb: BreadcrumbType.Dataset
  },
  {
    name: 'conceptSets',
    path: '/workspaces/:ns/:wsid/concepts/sets',
    render: () => <WorkspaceWrapper><ConceptSetList /></WorkspaceWrapper>,
    title: 'View Concept Sets',
    breadcrumb: BreadcrumbType.Workspace
  },
  {
    name: 'conceptSet',
    path: '/workspaces/:ns/:wsid/concepts/sets/:csid',
    render: () => <WorkspaceWrapper><ConceptSetDetails /></WorkspaceWrapper>,
    title: 'Concept Set',
    breadcrumb: BreadcrumbType.ConceptSet
  },
  {
    name: 'workspaceReview',
    path: '/admin/review-workspace',
    render: () => <AdminReviewWorkspace />,
    title: 'Review Workspaces'
  },
  {
    name: 'users',
    path: '/admin/user',
    render: () => <AdminUser />,
    title: 'User Admin Table'
  },
  {
    name: 'profile',
    path: '/profile',
    render: () => <ProfilePage />,
    title: 'Profile'
  },
  {
    name: 'settings',
    path: '/settings',
    render: () => <Settings />,
    title: 'Settings'
  },
  {
    name: 'workspaceCreate',
    path: '/workspaces/build',
    render: () => <WorkspaceEdit mode={WorkspaceEditMode.Create} />,
    title: 'Create Workspace'
  },
];

const handlers = routes.map(({path, ...rest}) => {
  const keys = []; // mutated by pathToRegexp
  const regex = pathToRegexp(path, keys);
  return {
    regex, ...rest,
    keys: keys.map(k => k.name),
    makePath: pathToRegexp.compile(path)
  };
});

const handlersByName = fp.keyBy('name', handlers);

const parseRoute = ({pathname, search}) => {
  const handler = handlers.find(({regex}) => regex.test(pathname));
  return handler && {
    ...handler,
    params: fp.zipObject(handler.keys, fp.tail(handler.regex.exec(pathname))),
    query: qs.parse(search, {ignoreQueryPrefix: true, plainObjects: true}),
  };
};

/**
 * HOC that injects a `route` prop, whose value is the matched route handler, extended
 * with `params` (path params) and `query` (query params). If no route matches, the value
 * will be undefined.
 */
export const withRoute = () => WrappedComponent => {
  class Wrapper extends React.Component<any, {location: any}> {
    static displayName = 'withRoute()';
    unlisten: Function;
    constructor(props) {
      super(props);
      this.state = {location: history.location};
    }

    componentDidMount() {
      this.unlisten = history.listen(location => {
        this.setState({location});
      });
    }

    componentWillUnmount() {
      this.unlisten();
    }

    render() {
      const {location} = this.state;
      return <WrappedComponent {...this.props} route={parseRoute(location)} />;
    }
  }
  return Wrapper;
};

/**
 * Return a URL path string, given a route name and its params.
 */
export const getPath = (name: string, params: any = {}): string => {
  return handlersByName[name].makePath(params);
};
