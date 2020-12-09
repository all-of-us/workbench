import Container from 'app/container';
import {LinkText} from 'app/text-labels';
import Button from 'app/element/button';
import {Page} from 'puppeteer';
import {waitForAttributeEquality} from '../../utils/waits-utils';
import PrimereactInputNumber from '../element/primereact-input-number';
import SelectMenu from "./select-menu";
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

export enum RuntimePreset {
  GeneralAnalysis = 'General Analysis',
  HailGenomicsAnalysis = 'Hail Genomics Analysis'
}

export default class RuntimePanel extends Container {
  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  async clickCreateButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Create});
    await button.click();
    return await page.waitForTimeout(2000);
  }

  async clickCustomizeButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Customize});
    await button.click();
    return await page.waitForTimeout(2000);
  }

  async clickNextButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Next});
    return await button.click();
  }

  async clickApplyAndRecreateButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Update});
    await button.click();
    return await page.waitForTimeout(2000);
  }

  async clickDeleteEnvironmentButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.DeleteEnvironment});
    return await button.click();
  }

  async clickDeleteButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Delete});
    await button.click();
    return await page.waitForTimeout(2000);
  }

  async pickCpus(cpus: number): Promise<void> {
    const cpusDropdown = await SelectMenu.findByName(this.page, {id: 'runtime-cpu'});
    return await cpusDropdown.clickMenuItem(cpus.toString());
  }

  async getCpus(): Promise<string> {
    const cpusDropdown = await SelectMenu.findByName(this.page, {id: 'runtime-cpu'});
    return await cpusDropdown.getSelectedValue();
  }

  async pickRamGbs(ramGbs: number): Promise<void> {
    const ramDropdown = await SelectMenu.findByName(this.page, {id: 'runtime-ram'});
    return await ramDropdown.clickMenuItem(ramGbs.toString());
  }

  async getRamGbs(): Promise<string> {
    const ramDropdown = await SelectMenu.findByName(this.page, {id: 'runtime-ram'});
    return await ramDropdown.getSelectedValue();
  }

  async pickDiskGbs(diskGbs: number): Promise<void> {
    const diskInput = new PrimereactInputNumber(this.page, '//*[@id="runtime-disk"]');
    return await diskInput.setValue(diskGbs);
  }

  async getDiskGbs(): Promise<number> {
    const diskInput = new PrimereactInputNumber(this.page, '//*[@id="runtime-disk"]');
    return await diskInput.getInputValue();
  }

  async pickComputeType(computeType: ComputeType): Promise<void> {
    const computeTypeDropdown = await SelectMenu.findByName(this.page, {id: 'runtime-compute'});
    return await computeTypeDropdown.clickMenuItem(computeType);
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

  async getDataprocNumPreemptibleWorkers(): Promise<number> {
    const dataprocNumPreemptibleWorkers = new PrimereactInputNumber(this.page, '//*[@id="num-preemptible"]');
    return await dataprocNumPreemptibleWorkers.getInputValue();
  }

  async pickWorkerCpus(workerCpus: number): Promise<void> {
    const workerCpusDropdown = await SelectMenu.findByName(this.page, {id: 'worker-cpu'});
    return await workerCpusDropdown.clickMenuItem(workerCpus.toString());
  }

  async getWorkerCpus(): Promise<string> {
    const workerCpusDropdown = await SelectMenu.findByName(this.page, {id: 'worker-cpu'});
    return await workerCpusDropdown.getSelectedValue();
  }

  async pickWorkerRamGbs(workerRamGbs: number): Promise<void> {
    const workerRamDropdown = await SelectMenu.findByName(this.page, {id: 'worker-ram'});
    return await workerRamDropdown.clickMenuItem(workerRamGbs.toString());
  }

  async getWorkerRamGbs(): Promise<string> {
    const workerRamDropdown = await SelectMenu.findByName(this.page, {id: 'worker-ram'});
    return await workerRamDropdown.getSelectedValue();
  }

  async pickWorkerDisk(workerDiskGbs: number): Promise<void> {
    const workerDiskInput = new PrimereactInputNumber(this.page, '//*[@id="worker-disk"]');
    return await workerDiskInput.setValue(workerDiskGbs);
  }

  async getWorkerDisk(): Promise<number> {
    const workerDiskInput = new PrimereactInputNumber(this.page, '//*[@id="worker-disk"]');
    return await workerDiskInput.getInputValue();
  }

  async pickRuntimePreset(runtimePreset: RuntimePreset): Promise<void> {
    const runtimePresetMenu = await SelectMenu.findByName(this.page, {id: 'runtime-preset-menu'});
    return await runtimePresetMenu.clickMenuItem(runtimePreset);
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
