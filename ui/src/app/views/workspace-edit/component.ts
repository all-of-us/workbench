import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {WorkspaceComponent} from 'app/views/workspace/component';

import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceEditComponent implements OnInit {
  workspace: Workspace;
  workspaceId: string;
  adding = false;
  buttonClicked = false;


  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
  ) {}

  ngOnInit(): void {
    this.workspace = {
      name: '',
      description: '',
      dataAccessLevel: Workspace.DataAccessLevelEnum.Registered,
      /**
       * TODO: use the free billing project created for the user once registration work is done
       */
      namespace: 'all-of-us-broad',
      researchPurpose: {
        diseaseFocusedResearch: false,
        methodsDevelopment: false,
        controlSet: false,
        aggregateAnalysis: false,
        ancestry: false,
        commercialPurpose: false,
        population: false
      }};
  }

  addWorkspace(): void {
    if (!this.buttonClicked) {
      this.buttonClicked = true;
      this.workspacesService
          .createWorkspace(
              this.workspace)
          .retry(2)
          .subscribe(cohorts => this.router.navigate(['../..'], {relativeTo : this.route}));
    }
  }
}
