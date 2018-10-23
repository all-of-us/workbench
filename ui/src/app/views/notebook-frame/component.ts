import {Component, Input, OnInit} from '@angular/core';
import {DomSanitizer} from '@angular/platform-browser';
import {ActivatedRoute} from '@angular/router';

import {WorkspaceData} from 'app/resolvers/workspace';
import {SignInService} from 'app/services/sign-in.service';
import {environment} from 'environments/environment';
import {Workspace} from 'generated';

@Component({
  selector: 'app-notebook-frame',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})

export class NotebookFrameComponent implements OnInit {
  jupyterUrl: string;
  workspace: Workspace;
  wsNamespace: string;
  wsName: string;
  nbName: string;
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];


  constructor (
    private route: ActivatedRoute,
    private signInService: SignInService,
    private sanitizer: DomSanitizer
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
  }

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsName = this.route.snapshot.params['wsid'];
    this.nbName = this.route.snapshot.params['nbName'];
    this.jupyterUrl = '/workspaces/' + this.wsNamespace + '/' + this.wsName + '/notebooks/'
      + encodeURIComponent(this.nbName);
    const authHandler = (e: MessageEvent) => {
      // if (e.source !== notebook) {
      //   return;
      // }
      if (e.origin !== environment.leoApiUrl) {
        return;
      }
      if (e.data.type !== 'bootstrap-auth.request') {
        return;
      }
      window.postMessage({
        'type': 'bootstrap-auth.response',
        'body': {
          'googleClientId': this.signInService.clientId
        }
      }, environment.leoApiUrl);
    };
    window.addEventListener('message', authHandler);
    this.notebookAuthListeners.push(authHandler);
  }
}
