import Container from 'app/container';
import {LinkText} from 'app/text-labels';
import Button from 'app/element/button';
import {Page} from 'puppeteer';
import {waitForAttributeEquality, waitWhileLoading} from 'utils/waits-utils';
import PrimereactInputNumber from 'app/element/primereact-input-number';
import SelectMenu from 'app/component/select-menu';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';

const defaultXpath = '//*[@id="runtime-panel"]';
const statusIconXpath = '//*[@data-test-id="runtime-status-icon"]';

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
    const button = await Button.findByName(this.page, {name: LinkText.Create}, this);
    return await button.click();
  }

  async clickCustomizeButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Customize}, this);
    return await button.click();
  }

  async clickNextButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Next}, this);
    return await button.click();
  }

  async clickApplyAndRecreateButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Update}, this);
    return await button.click();
  }

  async clickDeleteEnvironmentButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.DeleteEnvironment}, this);
    return await button.click();
  }

  async clickDeleteButton(): Promise<void> {
    const button = await Button.findByName(this.page, {name: LinkText.Delete}, this);
    return await button.click();
  }

  async pickCpus(cpus: number): Promise<void> {
    const cpusDropdown = await SelectMenu.findByName(this.page, {id: 'runtime-cpu'});
    return await cpusDropdown.select(cpus.toString());
  }

  async getCpus(): Promise<string> {
    const cpusDropdown = await SelectMenu.findByName(this.page, {id: 'runtime-cpu'});
    return await cpusDropdown.getSelectedValue();
  }

  async pickRamGbs(ramGbs: number): Promise<void> {
    const ramDropdown = await SelectMenu.findByName(this.page, {id: 'runtime-ram'});
    return await ramDropdown.select(ramGbs.toString());
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
    return await computeTypeDropdown.select(computeType);
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
    return await workerCpusDropdown.select(workerCpus.toString());
  }

  async getWorkerCpus(): Promise<string> {
    const workerCpusDropdown = await SelectMenu.findByName(this.page, {id: 'worker-cpu'});
    return await workerCpusDropdown.getSelectedValue();
  }

  async pickWorkerRamGbs(workerRamGbs: number): Promise<void> {
    const workerRamDropdown = await SelectMenu.findByName(this.page, {id: 'worker-ram'});
    return await workerRamDropdown.select(workerRamGbs.toString());
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
    const runtimePresetMenu = await SelectMenu.findByName(this.page, {id: 'runtime-presets-menu'});
    return await runtimePresetMenu.select(runtimePreset);
  }

  buildStatusIconSrc = (startStopIconState: StartStopIconState) => {
    return `/assets/icons/compute-${startStopIconState}.svg`;
  }

  async waitForStartStopIconState(startStopIconState: StartStopIconState): Promise<boolean> {
    return await waitForAttributeEquality(
        this.page,
        {xpath: statusIconXpath},
        'src',
        this.buildStatusIconSrc(startStopIconState),
        600000
    )
  }

  async clickStatusIcon(): Promise<void> {
    const icon = await this.page.waitForXPath(statusIconXpath, {visible: true});

    // Oddly, though the element is visible it is sometimes unclickable at this point.
    // This *may* be because the click target is an image, which may or may not be loaded yet.
    // Sleeping here is a hacky workaround found after attempting several approaches.
    await this.page.waitForTimeout(1000);

    await icon.focus();
    await icon.click();
  }

  async waitForLoad(): Promise<this> {
    try {
      await this.waitUntilVisible();
      await waitWhileLoading(this.page);
    } catch (err) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      throw err;
    }
    return this;
  }
}
