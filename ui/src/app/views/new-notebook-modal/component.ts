import {Component, Input} from '@angular/core';
import {Router} from '@angular/router';

import {isBlank} from 'app/utils/index';
import {Kernels} from 'app/utils/notebook-kernels';


import {FileDetail, UserMetricsService, Workspace} from 'generated';

@Component({
  selector: 'app-new-notebook-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/inputs.css',
    '../../styles/errors.css'],
  templateUrl: './component.html',
})
export class NewNotebookModalComponent {
  public creatingNotebook = false;
  public newName = '';
  @Input() workspace: Workspace;
  @Input() existingNotebooks: FileDetail[];
  Kernels = Kernels;
  kernelType: number = Kernels.Python3;
  nameConflict = false;

  loading = false;

  constructor(
    private userMetricsService: UserMetricsService,
    private route: Router
  ) {}

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
    this.userMetricsService.updateRecentResource(this.workspace.namespace, this.workspace.id,
      {notebookName: this.newName}).subscribe();
    this.route.navigate(['workspaces', this.workspace.namespace, this.workspace.id,
        'notebooks', 'create', 'frame'], {
      queryParams: {
        'notebook-name': encodeURIComponent(this.newName),
        'kernelType': this.kernelType
      },
      relativeTo: null
    });
    this.close();
  }

  get newNotebookDisabled(): boolean {
    return this.loading || this.newNameEmpty;
  }

  get newNameEmpty(): boolean {
    return isBlank(this.newName);
  }
}
