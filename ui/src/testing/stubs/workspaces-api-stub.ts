import {
  WorkspacesApi
} from 'generated/fetch';

export class WorkspacesApiStub extends WorkspacesApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  getNoteBookList() {
    return Promise.resolve([]);
  }
}
