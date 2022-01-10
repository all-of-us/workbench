import { Frame } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import { waitForFn } from 'utils/waits-utils';

export async function waitForPreviewCellsRendered(previewFrame: Frame): Promise<void> {
  await waitForFn(
    async () => {
      return (await previewFrame.$$('.jp-CodeCell')).length === (await previewFrame.$$('.jp-CodeMirrorEditor')).length;
    },
    1000,
    30000
  );
}

export async function getFormattedPreviewCode(previewFrame: Frame): Promise<string[]> {
  const css = '.jp-CodeMirrorEditor';
  await previewFrame.waitForSelector(css, { visible: true });
  const elements = await previewFrame.$$(css);
  return Promise.all(elements.map(async (content) => await getPropValue<string>(content, 'textContent')));
}
