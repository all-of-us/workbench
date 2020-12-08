import Container from 'app/container';
import {LinkText} from 'app/text-labels';
import Button from 'app/element/button';
import {ElementHandle, Page} from 'puppeteer';
import Dropdown from 'app/element/dropdown';
import {waitForAttributeEquality} from '../../utils/waits-utils';
import PrimereactInputNumber from '../element/primereact-input-number';

const defaultXpath = '//*[@data-test-id="runtime-panel"]';
const startStopIconXpath = '//*[@data-test-id="runtime-status-icon"]';

export enum StartStopIconState {
  Error = 'error',
  None = 'none',
  Running = 'running',
  Starting = 'starting',
  Stopped = 'stopped',
  Stopping = 'stopping'
}

export enum ComputeType {
  Standard = 'Standard VM',
  Dataproc = 'Dataproc Cluster'
}

export default class RuntimePanel extends Container {
  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  async getStartStopIcon(): Promise<ElementHandle> {
    return this.page.waitForXPath(startStopIconXpath);
  }

  async clickCreateButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Create});
    return await button.click();
  }

  async clickCustomizeButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Customize});
    return await button.click();
  }

  async clickNextButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Next});
    return await button.click();
  }

  async clickApplyAndRecreateButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Update});
    return await button.click();
  }

  async clickDeleteEnvironmentButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.DeleteEnvironment});
    return await button.click();
  }

  async clickDeleteButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Delete});
    return await button.click();
  }

  async pickCpus(cpus: number): Promise<void> {
    const cpusDropdown = new Dropdown(this.page, '//*[@id="runtime-cpu"]');
    return await cpusDropdown.selectOption(cpus.toString());
  }

  async pickRamGbs(ramGbs: number): Promise<void> {
    const ramDropdown = new Dropdown(this.page, '//*[@id="runtime-ram"]');
    return await ramDropdown.selectOption(ramGbs.toString());
  }

  async pickDiskGbs(diskGbs: number): Promise<void> {
    const diskInput = new PrimereactInputNumber(this.page, '//*[@id="runtime-disk"]');
    return await diskInput.setValue(diskGbs);
  }

  async pickComputeType(computeType: ComputeType): Promise<void> {
    await this.page.waitForXPath('//*[@id="runtime-compute"]');
    const computeTypeDropdown = new Dropdown(this.page, '//*[@id="runtime-compute"]');
    return await computeTypeDropdown.selectOption(computeType);
  }

  async pickDataprocNumWorkers(numWorkers: number): Promise<void> {
    const dataprocNumWorkers = new PrimereactInputNumber(this.page, '//*[@id="num-workers"]');
    return await dataprocNumWorkers.setValue(numWorkers);
  }

  async getDataprocNumWorkers(): Promise<number> {
    const dataprocNumWorkers = new PrimereactInputNumber(this.page, '//*[@id="num-workers"]');
    return await dataprocNumWorkers.getInputValue();
  }

  async pickDataprocNumPreemptibleWorkers(numPreemptibleWorkers: number): Promise<void> {
    const dataprocNumPreemptibleWorkers = new PrimereactInputNumber(this.page, '//*[@id="num-preemptible"]');
    return await dataprocNumPreemptibleWorkers.setValue(numPreemptibleWorkers);
  }

  async pickWorkerCpus(workerCpus: number): Promise<void> {
    const workerCpusDropdown = new Dropdown(this.page, '//*[@id="worker-cpu"]');
    return await workerCpusDropdown.selectOption(workerCpus.toString());
  }

  async pickWorkerRamGbs(workerRamGbs: number): Promise<void> {
    await this.page.waitForXPath('//*[@id="worker-ram"]');
    const workerRamDropdown = new Dropdown(this.page, '//*[@id="worker-ram"]');
    return await workerRamDropdown.selectOption(workerRamGbs.toString());
  }

  async pickWorkerDisk(workerDiskGbs: number): Promise<void> {
    const workerDiskInput = new PrimereactInputNumber(this.page, '//*[@id="worker-disk"]');
    return await workerDiskInput.setValue(workerDiskGbs);
  }

  buildStatusIconSrc = (startStopIconState: StartStopIconState) => {
    return `/assets/icons/compute-${startStopIconState}.svg`;
  }

  waitForStartStopIconState = async (startStopIconState: StartStopIconState): Promise<boolean> => {
    return await waitForAttributeEquality(
        this.page,
        {xpath: startStopIconXpath},
        'src',
        this.buildStatusIconSrc(startStopIconState),
        300000
    )
  }
}
