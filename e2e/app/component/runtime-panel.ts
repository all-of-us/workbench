import { Page } from 'puppeteer';
import SelectMenu from 'app/component/select-menu';
import PrimereactInputNumber from 'app/element/primereact-input-number';
import { LinkText, SideBarLink } from 'app/text-labels';
import Button from 'app/element/button';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import BaseHelpSidebar from './base-help-sidebar';
import { logger } from 'libs/logger';

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

export default class RuntimePanel extends BaseHelpSidebar {
  constructor(page: Page, xpath: string = defaultXpath) {
    super(page);
    super.setXpath(`${super.getXpath()}${xpath}`);
  }

  async pickCpus(cpus: number): Promise<void> {
    const cpusDropdown = await SelectMenu.findByName(this.page, { id: 'runtime-cpu' }, this);
    return await cpusDropdown.select(cpus.toString());
  }

  async getCpus(): Promise<string> {
    const cpusDropdown = await SelectMenu.findByName(this.page, { id: 'runtime-cpu' }, this);
    return await cpusDropdown.getSelectedValue();
  }

  async pickRamGbs(ramGbs: number): Promise<void> {
    const ramDropdown = await SelectMenu.findByName(this.page, { id: 'runtime-ram' }, this);
    return await ramDropdown.select(ramGbs.toString());
  }

  async getRamGbs(): Promise<string> {
    const ramDropdown = await SelectMenu.findByName(this.page, { id: 'runtime-ram' }, this);
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
    const computeTypeDropdown = await SelectMenu.findByName(this.page, { id: 'runtime-compute' }, this);
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
    const workerCpusDropdown = await SelectMenu.findByName(this.page, { id: 'worker-cpu' }, this);
    return await workerCpusDropdown.select(workerCpus.toString());
  }

  async getWorkerCpus(): Promise<string> {
    const workerCpusDropdown = await SelectMenu.findByName(this.page, { id: 'worker-cpu' }, this);
    return await workerCpusDropdown.getSelectedValue();
  }

  async pickWorkerRamGbs(workerRamGbs: number): Promise<void> {
    const workerRamDropdown = await SelectMenu.findByName(this.page, { id: 'worker-ram' }, this);
    return await workerRamDropdown.select(workerRamGbs.toString());
  }

  async getWorkerRamGbs(): Promise<string> {
    const workerRamDropdown = await SelectMenu.findByName(this.page, { id: 'worker-ram' }, this);
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
    const runtimePresetMenu = await SelectMenu.findByName(this.page, { id: 'runtime-presets-menu' }, this);
    return await runtimePresetMenu.select(runtimePreset);
  }

  buildStatusIconSrc = (startStopIconState: StartStopIconState): string => {
    return `/assets/icons/compute-${startStopIconState}.svg`;
  };

  /**
   * Wait up to 20 minutes before timing out here. We expect runtime changes to be quite slow.
   *   So we want to give a generous amount of time before failing the test.
   * @param startStopIconState
   * @param timeout
   */
  async waitForStartStopIconState(
    startStopIconState: StartStopIconState,
    timeout: number = 20 * 60 * 1000
  ): Promise<void> {
    const xpath = `${this.getXpath()}${statusIconXpath}[@src="${this.buildStatusIconSrc(startStopIconState)}"]`;
    await this.page.waitForXPath(xpath, { visible: true, timeout });
  }

  async clickPauseRuntimeIcon(): Promise<void> {
    const xpath = `${this.getXpath()}${statusIconXpath}[@src="${this.buildStatusIconSrc(StartStopIconState.Running)}"]`;
    const icon = new Button(this.page, xpath);
    await icon.click();
  }

  async clickResumeRuntimeIcon(): Promise<void> {
    const xpath = `${this.getXpath()}${statusIconXpath}[@src="${this.buildStatusIconSrc(StartStopIconState.Stopped)}"]`;
    const icon = new Button(this.page, xpath);
    await icon.click();
  }

  async open(): Promise<void> {
    const isOpen = await this.isVisible();
    if (isOpen) {
      return;
    }
    await this.clickIcon(SideBarLink.ComputeConfiguration);
    await this.waitUntilVisible();
    // Wait for visible texts
    await this.page.waitForXPath(`${this.getXpath()}//h3`, { visible: true });
    // Wait for visible button
    await this.page.waitForXPath(`${this.getXpath()}//*[@role="button" and @aria-label]`, { visible: true });
    logger.info(`Opened "${await this.getTitle()}" runtime sidebar`);
  }

  /**
   * Create runtime and wait until running.
   */
  async createRuntime(): Promise<void> {
    logger.info('Creating runtime');
    await this.open();
    await this.waitForStartStopIconState(StartStopIconState.None);
    await this.clickButton(LinkText.Create);
    await this.waitUntilClose();
    // Runtime panel automatically close after click Create button.
    // Reopen panel in order to check icon status.
    await this.open();
    // Runtime state transition: Starting -> Running
    await this.waitForStartStopIconState(StartStopIconState.Starting, 10 * 60 * 1000);
    await this.waitForStartStopIconState(StartStopIconState.Running);
    await this.close();
    logger.info('Runtime is running');
  }

  /**
   * Delete runtime.
   */
  async deleteRuntime(): Promise<void> {
    logger.info('Deleting runtime');
    await this.open();
    await this.clickButton(LinkText.DeleteEnvironment);
    await this.clickButton(LinkText.Delete);
    await this.waitUntilClose();
    // Runtime panel automatically close after click Create button.
    // Reopen panel in order to check icon status.
    await this.open();
    await this.waitForStartStopIconState(StartStopIconState.None);
    await this.close();
    logger.info('Runtime is deleted');
  }

  /**
   * Pause runtime.
   */
  async pauseRuntime(): Promise<void> {
    logger.info('Pausing runtime');
    await this.open();
    await this.waitForStartStopIconState(StartStopIconState.Running, 60 * 1000);
    await this.clickPauseRuntimeIcon();
    // Runtime state transition: Stopping -> Stopped
    await this.waitForStartStopIconState(StartStopIconState.Stopping);
    await this.waitForStartStopIconState(StartStopIconState.Stopped);
    await this.close();
    logger.info('Runtime is paused');
  }

  /**
   * Resume runtime.
   */
  async resumeRuntime(): Promise<void> {
    logger.info('Resuming runtime');
    await this.open();
    await this.clickResumeRuntimeIcon();
    // Runtime state transition: Stopped -> Starting -> Running
    await this.waitForStartStopIconState(StartStopIconState.Stopped);
    await this.waitForStartStopIconState(StartStopIconState.Starting);
    await this.waitForStartStopIconState(StartStopIconState.Running);
    await this.close();
    logger.info('Runtime is resumed');
  }

  async applyChanges(): Promise<NotebookPreviewPage> {
    await this.clickButton(LinkText.Next);
    await this.clickButton(LinkText.ApplyRecreate);
    await this.waitUntilClose();

    // Automatically opens the Preview page
    const notebookPreviewPage = new NotebookPreviewPage(this.page);
    await notebookPreviewPage.waitForLoad();

    // Wait for new runtime running. The runtime status transition from Stopping to None to Running
    await this.open();
    await this.waitForStartStopIconState(StartStopIconState.Stopping);
    await this.waitForStartStopIconState(StartStopIconState.None);
    await this.waitForStartStopIconState(StartStopIconState.Starting);
    await this.waitForStartStopIconState(StartStopIconState.Running);

    return notebookPreviewPage;
  }
}
