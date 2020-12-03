import Container from 'app/container';
import {LinkText} from 'app/text-labels';
import Button from 'app/element/button';
import {ElementHandle, Page} from 'puppeteer';
import Dropdown from "app/element/dropdown";
import InputNumber from "app/element/input-number";

const defaultXpath = '//*[@data-test-id="runtime-panel"]';

export default class RuntimePanel extends Container {
  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  async getStartStopIcon(): Promise<ElementHandle> {
    return this.page.waitForXPath('//*[@data-test-id="runtime-status-icon"]');
  }

  async clickCreateButton(): Promise<void> {
    await Button.findByName(this.page, {name: LinkText.Create}).then(b => b.click());
  }

  async clickCustomizeButton(): Promise<void> {
    await Button.findByName(this.page, {name: LinkText.Customize}).then(b => b.click());
  }

  async pickCpus(cpus: number): Promise<void> {
    const cpusDropdown = new Dropdown(this.page, '//*[@id="runtime-cpu"]');
    await cpusDropdown.selectOption(cpus.toString());
  }

  async pickRamGbs(ramGbs: number): Promise<void> {
    const ramDropdown = new Dropdown(this.page, '//*[@id="runtime-ram"]');
    await ramDropdown.selectOption(ramGbs.toString());
  }

  async pickDiskGbs(diskGbs: number): Promise<void> {
    const diskInput = new InputNumber(this.page, '//*[@id="runtime-disk"]/input');
    await diskInput.type(diskGbs.toString());
  }
}
