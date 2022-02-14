import { Disk, DiskApi, DiskType, EmptyResponse } from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export const stubDisk = () => ({
  size: 1000,
  diskType: DiskType.Standard,
  name: 'stub-disk',
  blockSize: 1,
});

export class DiskApiStub extends DiskApi {
  constructor(public disk?: Disk) {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  deleteDisk(
    _workspaceNamespace: string,
    _diskName: string,
    _options?: any
  ): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>((resolve, reject) => {
      if (!this.disk) {
        reject(Error('disk not found'));
      }
      this.disk = null;
      resolve({});
    });
  }

  getDisk(_workspaceNamespace: string, _options?: any): Promise<Disk> {
    return new Promise<Disk>((resolve, reject) => {
      if (!this.disk) {
        reject(Error('disk not found'));
      }
      resolve(this.disk);
    });
  }

  updateDisk(
    _workspaceNamespace: string,
    _diskName: string,
    diskSize: number,
    _options?: any
  ): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>((resolve, reject) => {
      if (!this.disk) {
        reject(Error('disk not found'));
      }
      if (this.disk.size > diskSize) {
        reject(Error('cannot decrease disk size'));
      }
      this.disk.size = diskSize;
      resolve({});
    });
  }
}
