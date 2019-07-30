import {mount} from 'enzyme';
import * as React from 'react';
import {SearchInput} from './search-input';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

// The delay between editing the search input and the dropdown appearing,
// padded to ensure DOM rendering can complete.
const DROPDOWN_DELAY_MS = 400;

test('component should render', () => {
  const input = mount(<SearchInput/>);
  expect(input.exists()).toBeTruthy();
});

test('component has sane defaults', () => {
  const input = <SearchInput/>;
  const p = input.props;
  expect(p.enabled).toBeTruthy();
  expect(p.placeholder).toBe('');
  expect(p.value).toBe('');
  expect(p.tooltip).toBe('');
  expect(p.onChange).toBeTruthy();
  expect(p.onSearch).toBeTruthy();
});

test('no dropdown is displayed on user input by default', async() => {
  const input = mount(<SearchInput/>);
  input.find('[data-test-id="search-input"]')
    .first().simulate('change', {target: {value: 'foo'}});
  await waitOneTickAndUpdate(input);
  expect(input.find('[data-test-id="search-input-drop-down"]').exists()).toBeFalsy();
});

test('dropdown is displayed when results are available', async() => {
  function onSearch(keyword: string) {
    return new Promise<Array<string>>((accept, reject) => {
      accept(['bar']);
    });
  }
  const input = mount(<SearchInput onSearch={onSearch} />);
  input.find('[data-test-id="search-input"]')
    .first().simulate('change', {target: {value: 'foo'}});
  await new Promise((accept, reject) => {
    input.update();
    setTimeout(() => {
      input.update();
      accept();
    }, DROPDOWN_DELAY_MS);
  }).then(() => {
    expect(input.find('[data-test-id="search-input-drop-down"]').exists()).toBeTruthy();
  });
});

test('selecting a result from the dropdown closes the dropdown', async() => {
  function onSearch(keyword: string) {
    return new Promise<Array<string>>((accept, reject) => {
      accept(['bar']);
    });
  }
  const input = mount(<SearchInput onSearch={onSearch} />);
  input.find('[data-test-id="search-input"]')
    .first().simulate('change', {target: {value: 'foo'}});
  await new Promise((accept, reject) => {
    input.update();
    setTimeout(() => {
      input.update();
      accept();
    }, DROPDOWN_DELAY_MS);
  }).then(async() => {
    const match = input.find('[data-test-id="search-input-drop-down-element-0"]');
    expect(match.exists()).toBeTruthy();
    match.simulate('mousedown');
    input.find('input').simulate('blur');
    await waitOneTickAndUpdate(match);
    await waitOneTickAndUpdate(input);
    expect(input.find('[data-test-id="search-input-drop-down"]').exists()).toBeFalsy();
  });
});

test('onChange handler is called when the contents changes', async() => {
  let changed = false;
  const input = mount(<SearchInput onChange={() => { changed = true; }}/>);
  input.find('[data-test-id="search-input"]')
    .first().simulate('change', {target: {value: 'foo'}});
  await waitOneTickAndUpdate(input);
  expect(changed).toBeTruthy();
});
