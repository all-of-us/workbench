import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {WorkspacesService} from 'generated/api/workspaces.service';

export enum ResourceType { Workspace = 1 }

@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class NotFoundComponent implements OnInit {
  notFoundMessage: string;
  wsNamespace: string;
  wsId: string;
  resourceType: ResourceType;

  ResourceType = ResourceType;

  constructor(private route: ActivatedRoute,
      private router: Router,
      private workspacesService: WorkspacesService) {}

  ngOnInit() {
    this.resourceType = this.route.snapshot.params['mode'];

    if (this.resourceType = ResourceType.Workspace) {
      this.wsNamespace = this.route.snapshot.params['ns'];
      this.wsId = this.route.snapshot.params['wsid'];

      this.notFoundMessage = 'Workspace \'' + this.wsNamespace + '/' + this.wsId + '\'';

      this.workspacesService.getWorkspace(this.wsNamespace, this.wsId).subscribe(() => {
        this.router.navigate(['..'], {relativeTo: this.route});
      });
    }
  }

}
