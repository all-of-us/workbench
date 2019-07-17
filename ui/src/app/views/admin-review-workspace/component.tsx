import {Component} from '@angular/core';
import * as React from 'react';

import {SpinnerOverlay} from 'app/components/spinners';
import {ReactWrapperBase, withUserProfile} from 'app/utils';
import {Profile} from 'generated/fetch';


/**
 * Review Workspaces. Users with the REVIEW_RESEARCH_PURPOSE permission use this
 * to view other users' workspaces for which a review has been requested, and approve/reject them.
 */
// TODO(RW-85) Design this UI. Current implementation is a rough sketch.
// export class AdminReviewWorkspaceComponent implements OnInit {
//   workspaces: Workspace[] = [];
//   contentLoaded = false;
//   fetchingWorkspacesError = false;
//   reviewedWorkspace: Workspace;
//   reviewError = false;
//
//   bugReportOpen: boolean;
//   bugReportDescription = '';
//   constructor(
//     private workspacesService: WorkspacesService
//   ) {
//     this.closeBugReport = this.closeBugReport.bind(this);
//   }
//
//   ngOnInit(): void {
//     this.workspacesService.getWorkspacesForReview()
//       .subscribe(
//         workspacesResp => {
//           this.workspaces = workspacesResp.items;
//           this.contentLoaded = true;
//         }, () => {
//           this.fetchingWorkspacesError = true;
//         });
//   }
//
//   approve(workspace: Workspace, approved: boolean): void {
//     const request = <ResearchPurposeReviewRequest> {approved};
//     this.workspacesService.reviewWorkspace(
//       workspace.namespace, workspace.id, request)
//       .subscribe(
//         resp => {
//           const i = this.workspaces.indexOf(workspace, 0);
//           if (i >= 0) {
//             this.workspaces.splice(i, 1);
//           }
//         }, () => {
//           const i = this.workspaces.indexOf(workspace, 0);
//           if (i >= 0) {
//             this.reviewedWorkspace = this.workspaces[i];
//           }
//           this.reviewError = true;
//         });
//   }
//
//   submitFetchingWorkspacesBugReport(): void {
//     this.bugReportDescription =
//       'Could not fetch workspaces for approval';
//     this.bugReportOpen = true;
//   }
//
//   submitReviewWorkspaceBugReport(): void {
//     this.reviewError = false;
//     this.bugReportDescription =
//       'Could not review workspace: \'' + this.reviewedWorkspace.namespace + '/' +
//       this.reviewedWorkspace.name + '\'';
//     this.bugReportOpen = true;
//   }
//
//   closeBugReport(): void {
//     this.bugReportOpen = false;
//   }
// }

export const AdminReviewWorkspace = withUserProfile()(class extends React.Component<
  {profileState: {profile: Profile, reload: Function, updateCache: Function}},
  {contentLoaded: boolean}> {

  constructor(props) {
    super(props);

    this.state = {
      contentLoaded: false
    };
  }

  render() {
    const {contentLoaded} = this.state;
    return <div style={{position: 'relative'}}>
      <h2>Review Workspaces</h2>
      {contentLoaded ?
        <div>content</div> :
        <div>
          Loading workspaces for review...
          <SpinnerOverlay overrideStylesOverlay={{alignItems: 'flex-start', marginTop: '2rem'}}/>
        </div>
      }
    </div>;
  }
});


@Component({
  template: '<div #root></div>'
})
export class AdminReviewWorkspaceComponent extends ReactWrapperBase {
  constructor() {
    super(AdminReviewWorkspace, []);
  }
}
