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

export class NotebookIFrameComponent implements OnInit {
  jupyterUrl: string;
  workspace: Workspace;
  notebookName: string;


  constructor (
    public route: ActivatedRoute,
    private sanitizer: DomSanitizer
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
  }

  ngOnInit(): void {
    this.notebookName = this.route.snapshot.params['nbName'];
    if (this.notebookName) {
      this.jupyterUrl = '/workspaces/' + this.workspace.namespace + '/' +
        this.workspace.id + '/notebooks/' + this.notebookName;
    } else {
      this.jupyterUrl = `/workspaces/${this.workspace.namespace}/${this.workspace.id}/` +
        `notebooks/create/?notebook-name=` +
        encodeURIComponent(this.route.snapshot.queryParams['notebook-name']) +
        `&kernel-type=${this.route.snapshot.queryParams['kernelType']}`;
    }
  }

  get iFrameSrc() {
    return this.sanitizer.bypassSecurityTrustResourceUrl(this.jupyterUrl);
  }
}
