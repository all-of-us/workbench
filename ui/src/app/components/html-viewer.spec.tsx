import {HtmlViewer} from 'app/components/html-viewer';
import {SpinnerOverlay} from 'app/components/spinners';
import {shallow} from 'enzyme';
import * as React from 'react';


it('should load html pages', async () => {
  let reachedLastPage = false;

  const props = {
    filePath: '/assets/documents/fake-html-page.html',
    onLastPage: () => reachedLastPage = true
  };

  const wrapper = shallow(<HtmlViewer {...props} />).shallow();
  const iframe = wrapper.find('iframe');

  expect(wrapper.find(SpinnerOverlay).length).toBe(1);
  expect(iframe.length).toBe(1);

  iframe.simulate('load');
  expect(wrapper.find(SpinnerOverlay).length).toBe(0);

  wrapper.setState({hasReadEntireDoc: true});
  expect(reachedLastPage).toBe(true);
});
