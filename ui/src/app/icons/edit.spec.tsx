import '@testing-library/jest-dom';

import * as React from 'react';

import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react';

import { EditComponentProps, EditComponentReact } from './edit';

// Note that the description is slightly different from the component for easier test filtering.
describe('EditIconComponent', () => {
  let props: EditComponentProps;

  const component = () => {
    return render(<EditComponentReact {...props} />);
  };

  const getRoot = () => screen.getByTitle('Edit').closest('svg');

  beforeEach(() => {
    props = {
      disabled: false,
      enableHoverEffect: true,
      style: { backgroundColor: 'antiquewhite' },
    };
  });

  it('should render', () => {
    component();
    expect(getRoot()).toBeInTheDocument();
  });

  it('should change style if disabled', () => {
    component();
    const style = getRoot().style;
    props.disabled = true;
    cleanup();
    component();
    const disabledStyle = getRoot().style;
    expect(style).not.toEqual(disabledStyle);
  });

  it('should change style on mouse over', async () => {
    component();
    const element = getRoot();
    const initialFillColor = element.style.fill;
    fireEvent.mouseOver(element);
    const finalFillColor = element.style.fill;
    await waitFor(() => expect(initialFillColor).not.toEqual(finalFillColor));
  });

  it('should change style on mouse out', async () => {
    component();
    const element = getRoot();

    fireEvent.mouseOver(element);
    const initialFillColor = element.style.fill;
    fireEvent.mouseLeave(element);
    const finalFillColor = element.style.fill;
    await waitFor(() => expect(initialFillColor).not.toEqual(finalFillColor));
  });
});
