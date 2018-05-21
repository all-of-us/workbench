import {Injectable} from '@angular/core';

@Injectable()
export class WorkspaceShareService {

  public shareModalOpen: Boolean;

  constructor() {
    this.shareModalOpen = false;
  }
}
