import * as React from 'react';
import { shallow } from 'enzyme';

import {
  HtmlViewer,
  MS_WORD_PARAGRAPH_CLASS,
} from 'app/components/html-viewer';
import { SpinnerOverlay } from 'app/components/spinners';
import { readFileSync } from 'fs';

it('should load html pages', async () => {
  let reachedLastPage = false;

  const props = {
    filePath: '/assets/documents/fake-html-page.html',
    onLastPage: () => (reachedLastPage = true),
  };

  const wrapper = shallow(<HtmlViewer {...props} />).shallow();
  const iframe = wrapper.find('iframe');

  expect(wrapper.find(SpinnerOverlay).length).toBe(1);
  expect(iframe.length).toBe(1);

  iframe.simulate('load');
  expect(wrapper.find(SpinnerOverlay).length).toBe(0);

  wrapper.setState({ hasReadEntireDoc: true });
  expect(reachedLastPage).toBe(true);
});

// for example, .MsoNormal -> class=MsoNormal
const querySelectorToProp = (qs: string): string => qs.replace('.', 'class=');

test.each([
  ['DUCC', 'public/data-user-code-of-conduct-v4.html'],
  ['TOS', 'public/aou-tos.html'],
])(
  'should fail if the %s is missing, renamed, or has a format incompatible with MS_WORD_PARAGRAPH_CLASS',
  (documentName, documentPath) => {
    const fileContents = readFileSync(documentPath);
    expect(
      fileContents.includes(querySelectorToProp(MS_WORD_PARAGRAPH_CLASS))
    ).toBeTruthy();
  }
);
