import {Component, Input, OnDestroy} from '@angular/core';

import {SignInService} from 'app/services/sign-in.service';
import {isBlank} from 'app/utils/index';
import {Kernels} from 'app/utils/notebook-kernels';

import {environment} from 'environments/environment';

import {FileDetail, Workspace} from 'generated';


@Component({
  selector: 'app-new-notebook-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/inputs.css'],
  templateUrl: './component.html',
})
export class NewNotebookModalComponent implements OnDestroy {
  public creatingNotebook = false;
  public newName = '';
  @Input() workspace: Workspace;
  @Input() existingNotebooks: FileDetail[];
  Kernels = Kernels;
  kernelType: number = Kernels.Python3;
  nameConflict = false;
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];

  loading = false;

  constructor(
    private signInService: SignInService,
  ) {}

  ngOnDestroy(): void {
    this.notebookAuthListeners.forEach(f => window.removeEventListener('message', f));
  }

  open(): void {
    this.creatingNotebook = true;
    this.loading = false;
  }

  close(): void {
    this.creatingNotebook = false;
  }

  submitNewNotebook(): void {
    const existingNotebook =
      this.existingNotebooks.find((currentNotebook) =>
        currentNotebook.name === this.newName + '.ipynb');
    if (existingNotebook) {
      this.nameConflict = true;
      return;
    }
    const nbUrl = `/workspaces/${this.workspace.namespace}/${this.workspace.id}/` +
        `notebooks/create/?notebook-name=` + encodeURIComponent(this.newName) +
        `&kernel-type=${this.kernelType}`;

    const notebook = window.open(nbUrl, '_blank');

    // TODO(RW-474): Remove the authHandler integration. This is messy,
    // non-standard, and currently will break in the following situation:
    // - User opens a new notebook tab.
    // - While that tab is loading, user immediately navigates away from this
    //   page.
    // This is not easily fixed without leaking listeners outside the lifespan
    // of the workspace component.
    const authHandler = (e: MessageEvent) => {
      if (e.source !== notebook) {
        return;
      }
      if (e.origin !== environment.leoApiUrl) {
        return;
      }
      if (e.data.type !== 'bootstrap-auth.request') {
        return;
      }
      notebook.postMessage({
        'type': 'bootstrap-auth.response',
        'body': {
          'googleClientId': this.signInService.clientId
        }
      }, environment.leoApiUrl);
    };
    window.addEventListener('message', authHandler);
    this.notebookAuthListeners.push(authHandler);
  }

  get newNotebookDisabled(): boolean {
    return this.loading || this.newNameEmpty;
  }

  get newNameEmpty(): boolean {
    return isBlank(this.newName);
  }
}
