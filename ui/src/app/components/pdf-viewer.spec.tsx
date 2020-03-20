import {shallow} from 'enzyme';
import * as React from 'react';
import {Document, Page} from 'react-pdf';

import {PdfViewer, Props} from 'app/components/pdf-viewer';

let props = {
  windowSize: {width: 1700, height: 0},
  pdfPath: '/assets/documents/fake-document-path.pdf'
};

it('should load PDF pages', async() => {
  const wrapper = shallow(<PdfViewer {...props} />).shallow();

  // Initially we should have a document and no pages.
  expect(wrapper.find(Document).length).toEqual(1);
  expect(wrapper.find(Page).length).toEqual(0);

  // Simulate the PDF document loading and calling the 'onLoadSuccess' prop to indicate we have
  // 10 pages in the PDF.
  const pdfDocument = wrapper.find(Document);
  const onSuccess = pdfDocument.prop('onLoadSuccess') as (data: object) => {};
  onSuccess({numPages: 10});

  // We should now be rendering a <Page> component for each of the pages.
  expect(wrapper.find(Page).length).toEqual(10);
});