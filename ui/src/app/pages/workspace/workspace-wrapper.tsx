import {Component as AComponent, Component} from '@angular/core';
import {faExclamationTriangle} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import * as fp from 'lodash/fp';
import {useEffect, useState} from 'react';
import * as React from 'react';
import {Redirect} from 'react-router';
import {useParams} from 'react-router-dom';
import {AppRoutingComponent} from '../../app-routing';
import {AppRoute, withRouteData} from '../../components/app-router';
import {FlexRow} from '../../components/flex';
import {HelpSidebar} from '../../components/help-sidebar';
import {TextColumn} from '../../components/text-column';
import {SignInService} from '../../services/sign-in.service';
import {workspacesApi} from '../../services/swagger-fetch-clients';
import colors from '../../styles/colors';
import {ReactWrapperBase, withRouteConfigData} from '../../utils';
import {AnalyticsTracker} from '../../utils/analytics';
import {BreadcrumbType, currentWorkspaceStore} from '../../utils/navigation';
import {routeDataStore, withStore} from '../../utils/stores';
import {WorkspaceRoutes} from '../../workspace-app-routing';
import {WorkspaceAbout} from './workspace-about';
import {WorkspaceNavBarReact} from './workspace-nav-bar';

const NavBar = fp.flow(
  withRouteConfigData(),
  withStore(routeDataStore, 'routeData')
)(({routeConfigData}) => {
  console.log("ASDJASOIDJSAOJDASOIDJAOSIDJOIASJDOAISJDOIAJDOISAJDOIASJDOIAJDOIASDJOIASJDOAISD");
  console.log(routeConfigData);

  return <React.Fragment>
    {!routeConfigData.minimizeChrome && <WorkspaceNavBarReact/>}
  </React.Fragment>;
});

export const WorkspaceWrapper = fp.flow()(({routeConfigData}) => {
  const params = useParams();
  const {ns, wsid} = useParams();
  const [workspace, setWorkspace] = useState(currentWorkspaceStore.getValue());

  console.log(routeConfigData);

  useEffect(() => {
    if (!workspace || workspace.namespace !== ns || workspace.id !== wsid) {
      workspacesApi().getWorkspace(ns, wsid)
        .then((wsResponse) => {
          console.log("setting next workspace");
          currentWorkspaceStore.next({
            ...wsResponse.workspace,
            accessLevel: wsResponse.accessLevel
          });
        });
    }
  }, [ns, wsid]);

  useEffect(() => {
    const sub = currentWorkspaceStore.subscribe(storeWorkspace => {
      console.log("next workspace received");
      setWorkspace(storeWorkspace);
    });

//    return sub.unsubscribe;
  }, []);


  console.log("rendering wrapper");
  console.log(workspace);

// should nav bar actually load before workspace is ready?
  return <React.Fragment>
    <NavBar/>
    {!!workspace &&
    <React.Fragment>
        <HelpSidebar pageKey={'about'}/>
        <div style={{marginRight: '45px', height: 'auto'}}>
            <WorkspaceRoutes/>
        </div>
    </React.Fragment>}
  </React.Fragment>
});

// @AComponent({
//   template: '<div #root style="display: inline;"></div>'
// })
// export class ReactWorkspaceWrapperComponent extends ReactWrapperBase {
//   constructor(private signInService: SignInService) {
//     super(AppRoutingComponent, ['onSignIn', 'signIn']);
//     this.onSignIn = this.onSignIn.bind(this);
//     this.signIn = this.signIn.bind(this);
//   }
//
//   onSignIn(): void {
//     this.signInService.isSignedIn$.subscribe((signedIn) => {
//       if (signedIn) {
//         return <Redirect to='/'/>;
//       }
//     });
//   }
//
//   signIn(): void {
//     AnalyticsTracker.Registration.SignIn();
//     this.signInService.signIn();
//   }
// }
