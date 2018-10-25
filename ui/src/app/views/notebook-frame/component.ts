import {Component, Input, OnInit} from '@angular/core';
import {DomSanitizer} from '@angular/platform-browser';
import {ActivatedRoute} from '@angular/router';

import {WorkspaceData} from 'app/resolvers/workspace';
import {Workspace} from 'generated';

@Component({
  selector: 'app-notebook-frame',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})

export class NotebookFrameComponent implements OnInit {
  jupyterUrl: string;
  workspace: Workspace;


  constructor (
    private route: ActivatedRoute,
    private sanitizer: DomSanitizer
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
  }

  ngOnInit(): void {
    if (this.route.snapshot.routeConfig.path.match('create')) {
      this.jupyterUrl = `/workspaces/${this.workspace.namespace}/${this.workspace.id}/` +
        `notebooks/create/?notebook-name=` +
        encodeURIComponent(this.route.snapshot.queryParams['notebook-name']) +
        `&kernel-type=${this.route.snapshot.queryParams['kernelType']}`;
    } else {
      this.jupyterUrl = '/workspaces/' + this.workspace.namespace + '/' +
        this.workspace.id + '/notebooks/' + this.route.snapshot.params['nbName'];
    }
  }

  get iFrameSrc() {
    return this.sanitizer.bypassSecurityTrustResourceUrl(this.jupyterUrl);
  }
}
