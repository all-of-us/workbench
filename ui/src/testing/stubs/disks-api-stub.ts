import {
  AppType,
  Disk,
  DisksApi,
  DiskStatus,
  DiskType,
  EmptyResponse,
  ListDisksResponse,
} from 'generated/fetch';

export const stubDisk = (): Disk => ({
  size: 1000,
  diskType: DiskType.STANDARD,
  gceRuntime: true,
  name: 'stub-disk',
  blockSize: 1,
  zone: 'us-central1-a',
});

export const mockJupyterDisk = (): Disk => ({
  size: 1000,
  diskType: DiskType.STANDARD,
  gceRuntime: true,
  name: 'mock-disk1',
  blockSize: 1,
  status: DiskStatus.READY,
  appType: null,
  creator: '"evrii@fake-research-aou.org"',
  createdDate: '2023-05-22T18:55:10.108838Z',
  zone: 'us-central1-a',
});

export const mockCromwellDisk = (): Disk => ({
  size: 1000,
  diskType: DiskType.STANDARD,
  gceRuntime: false,
  name: 'mock-disk2',
  blockSize: 1,
  status: DiskStatus.READY,
  appType: AppType.CROMWELL,
  creator: '"evrii@fake-research-aou.org"',
  createdDate: '2023-05-22T18:55:10.108838Z',
  zone: 'us-central1-a',
});

export const mockRStudioDisk = (): Disk => ({
  size: 1000,
  diskType: DiskType.STANDARD,
  gceRuntime: false,
  name: 'mock-disk3',
  blockSize: 1,
  status: DiskStatus.READY,
  appType: AppType.RSTUDIO,
  creator: '"evrii@fake-research-aou.org"',
  createdDate: '2023-05-22T18:55:10.108838Z',
  zone: 'us-central1-a',
});

export class DisksApiStub extends DisksApi {
  constructor(public disk?: Disk) {
    super(undefined);
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
