import {
  AppType,
  Disk,
  DisksApi,
  DiskStatus,
  DiskType,
  EmptyResponse,
  ListDisksResponse,
} from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export const stubDisk = () => ({
  size: 1000,
  diskType: DiskType.Standard,
  isGceRuntime: true,
  name: 'stub-disk',
  blockSize: 1,
});

export const mockJupyterDisk = (): Disk => ({
  size: 1000,
  diskType: DiskType.Standard,
  isGceRuntime: true,
  name: 'mock-disk1',
  blockSize: 1,
  status: DiskStatus.Ready,
  appType: null,
  creator: '"evrii@fake-research-aou.org"',
  createdDate: '2023-05-22T18:55:10.108838Z',
});

export const mockCromwellDisk = (): Disk => ({
  size: 1000,
  diskType: DiskType.Standard,
  isGceRuntime: false,
  name: 'mock-disk2',
  blockSize: 1,
  status: DiskStatus.Ready,
  appType: AppType.CROMWELL,
  creator: '"evrii@fake-research-aou.org"',
  createdDate: '2023-05-22T18:55:10.108838Z',
});

export const mockRStudioDisk = (): Disk => ({
  size: 1000,
  diskType: DiskType.Standard,
  isGceRuntime: false,
  name: 'mock-disk3',
  blockSize: 1,
  status: DiskStatus.Ready,
  appType: AppType.RSTUDIO,
  creator: '"evrii@fake-research-aou.org"',
  createdDate: '2023-05-22T18:55:10.108838Z',
});

export class DisksApiStub extends DisksApi {
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

  listOwnedDisksInWorkspace(
    _workspaceNamespace: string,
    _options?: any
  ): Promise<ListDisksResponse> {
    return new Promise<ListDisksResponse>((resolve, reject) => {
      if (!this.disk) {
        reject(Error('disk not found'));
      }
      resolve([this.disk]);
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
