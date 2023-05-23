import { Disk, DiskType } from 'generated/fetch';

const y: Disk = {
  size: 1000,
  diskType: DiskType.Standard,
  isGceRuntime: true,
  name: 'stub-disk',
  blockSize: 1,
};

export const disksAdminApi = () => ({
  listDisksInWorkspace: () => Promise.resolve([y]),
});
