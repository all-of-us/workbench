import * as React from 'react';
import { createPortal } from 'react-dom';
import onClickOutside from 'react-onclickoutside';
import * as fp from 'lodash/fp';

import { switchCase } from '@terra-ui-packages/core-utils';
import colors, { colorWithWhiteness } from 'app/styles/colors';

const styles = {
  tooltip: {
    background: colors.dark,
    color: colors.white,
    padding: '0.75rem',
    // Keep z-index in sync with modals.tsx.
    position: 'fixed',
    top: 0,
    left: 0,
    pointerEvents: 'none',
    zIndex: 105,
    maxWidth: 400,
    borderRadius: 4,
  },
  notch: {
    fill: colors.dark,
    position: 'absolute',
    width: 16,
    height: 8,
    marginLeft: -8,
    marginRight: -8,
    marginTop: -8,
    transformOrigin: 'bottom',
  },
  popup: {
    position: 'fixed',
    top: 0,
    left: 0,
    zIndex: 105,
    backgroundColor: colors.white,
    border: '1px solid',
    borderColor: colorWithWhiteness(colors.dark, 0.6),
    borderRadius: 4,
    boxShadow: '0 3px 2px 0 rgba(0,0,0,0.12)',
  },
};

// Enable stubbing out dimensions for testing purposes. Enzyme does not have
// associated DOM, and therefore cannot do bounding box computations.
let popupDimensionsOverride = null;
export const stubPopupDimensions = (
  stub = {
    element: { width: 0, height: 0 },
    target: { top: 0, bottom: 0, left: 0, right: 0 },
    viewport: { width: 0, height: 0 },
  }
) => (popupDimensionsOverride = stub);

const computeNewDimensions = (el, target) =>
  popupDimensionsOverride || {
    element: fp.pick(['width', 'height'], el.current.getBoundingClientRect()),
    target: fp.pick(
      ['top', 'bottom', 'left', 'right'],
      document.getElementById(target).getBoundingClientRect()
    ),
    viewport: { width: window.innerWidth, height: window.innerHeight },
  };

interface WithDynamicPositionProps {
  target: string;
  handleClickOutside?: () => void;
  outsideClickIgnoreClass?: string;
  onClick?: () => void;
}

export const withDynamicPosition = () => (WrappedComponent) => {
  const Wrapper = class WithDynamicPosition extends React.Component<
    WithDynamicPositionProps,
    any
  > {
    static displayName = 'withDynamicPosition()';

    element: any;
    animation: number;

    constructor(props: any) {
      super(props);
      this.state = {
        dimensions: {
          element: { width: 0, height: 0 },
          target: { top: 0, bottom: 0, left: 0, right: 0 },
          viewport: { width: 0, height: 0 },
        },
      };
      this.element = React.createRef();
    }

    componentDidMount() {
      this.reposition();
    }

    componentWillUnmount() {
      cancelAnimationFrame(this.animation);
    }

    reposition() {
      const { target } = this.props;
      const { dimensions } = this.state;
      this.animation = requestAnimationFrame(() => this.reposition());
      const newDimensions = computeNewDimensions(this.element, target);
      if (!fp.isEqual(newDimensions, dimensions)) {
        this.setState({ dimensions: newDimensions });
      }
    }

    render() {
      const { dimensions } = this.state;
      return (
        <WrappedComponent
          dimensions={dimensions}
          elementRef={this.element}
          {...this.props}
        />
      );
    }
  };
  return Wrapper;
};

export const computePopupPosition = ({
  side,
  viewport,
  target,
  element,
  gap,
}) => {
  const getPosition = (s) => {
    const left = fp.flow(
      fp.clamp(0, viewport.width - element.width),
      fp.clamp(target.left - element.width + 16, target.right - 16)
    )((target.left + target.right) / 2 - element.width / 2);
    const top = fp.flow(
      fp.clamp(0, viewport.height - element.height),
      fp.clamp(target.top - element.height + 16, target.bottom - 16)
    )((target.top + target.bottom) / 2 - element.height / 2);
    return switchCase(
      s,
      ['top', () => ({ top: target.top - element.height - gap, left })],
      ['bottom', () => ({ top: target.bottom + gap, left })],
      ['left', () => ({ left: target.left - element.width - gap, top })],
      ['right', () => ({ left: target.right + gap, top })],
      // Using half a gap here so the popup lines up with the snowman menu.
      [
        'bottom-left',
        () => ({
          top: target.bottom + gap,
          left: target.left - element.width + 0.5 * gap,
        }),
      ],
      [
        'bottom-right',
        () => ({ top: target.bottom + gap, left: target.right - 0.5 * gap }),
      ],
      [
        'top-left',
        () => ({
          top: target.top - element.height - gap,
          left: target.left - element.width + 0.5 * gap,
        }),
      ],
      [
        'top-right',
        () => ({
          top: target.top - element.height - gap,
          left: target.right - 0.5 * gap,
        }),
      ]
    );
  };
  const position = getPosition(side);
  const overflowsTop = position.top < 0;
  const overflowsBottom = position.top + element.height >= viewport.height;
  const overflowsLeft = position.left < 0;
  const overflowsRight = position.left + element.width >= viewport.width;

  const newBottom = overflowsBottom ? 'top' : 'bottom';
  const newLeft = overflowsLeft ? 'right' : 'left';
  const newTop = overflowsTop ? 'bottom' : 'top';
  const newRight = overflowsRight ? 'left' : 'right';
  const maybeFlip = (d) => {
    return switchCase(
      d,
      ['top', () => newTop],
      ['bottom', () => newBottom],
      ['left', () => newLeft],
      ['right', () => newRight],
      ['bottom-left', () => [newBottom, newLeft].join('-')],
      ['bottom-right', () => [newBottom, newRight].join('-')],
      ['top-left', () => [newTop, newLeft].join('-')],
      ['top-right', () => [newTop, newRight].join('-')]
    );
  };
  const finalSide = maybeFlip(side);
  const finalPosition = getPosition(finalSide);
  return { side: finalSide, position: finalPosition };
};

export const PopupPortal = ({ children }) => {
  return createPortal(
    React.Children.only(children),
    document.getElementById('popup-root')
  );
};

export const Tooltip = withDynamicPosition()(
  class TooltipComponent extends React.Component<any> {
    static readonly defaultProps = {
      side: 'bottom',
    };

    render() {
      const {
        children,
        side,
        elementRef,
        dimensions: { target, element, viewport },
      } = this.props;
      const { side: finalSide, position } = computePopupPosition({
        side,
        target,
        element,
        viewport,
        gap: 10,
      });
      const getNotchPosition = () => {
        const left = fp.clamp(
          12,
          element.width - 12,
          (target.left + target.right) / 2 - position.left
        );
        const top = fp.clamp(
          12,
          element.height - 12,
          (target.top + target.bottom) / 2 - position.top
        );
        return switchCase<string, { [key: string]: number | string }>(
          finalSide,
          ['top', () => ({ bottom: 0, left, transform: 'rotate(180deg)' })],
          ['bottom', () => ({ top: 0, left })],
          ['left', () => ({ right: 0, top, transform: 'rotate(90deg)' })],
          ['right', () => ({ left: 0, top, transform: 'rotate(270deg)' })]
        );
      };
      return (
        <PopupPortal>
          <div
            ref={elementRef}
            style={
              {
                transform: `translate(${position.left}px, ${position.top}px)`,
                ...styles.tooltip,
              } as React.CSSProperties
            }
          >
            {children}
            <svg
              viewBox='0 0 2 1'
              style={
                {
                  ...getNotchPosition(),
                  ...styles.notch,
                } as React.CSSProperties
              }
            >
              <path d='M0,1l1,-1l1,1Z' />
            </svg>
          </div>
        </PopupPortal>
      );
    }
  }
);

export class TooltipTrigger extends React.Component<any, any> {
  id: string;

  constructor(props) {
    super(props);
    this.state = { open: false };
    this.id = `tooltip-trigger-${fp.uniqueId('')}`;
  }

  render() {
    const { children, content, disabled, ...props } = this.props;
    const { open } = this.state;
    if (!content) {
      return children;
    }
    const child = React.Children.only(children) as React.ReactElement;
    return (
      <React.Fragment>
        {React.cloneElement(child, {
          id: this.id,
          onMouseEnter: (...args) => {
            if (child.props.onMouseEnter) {
              child.props.onMouseEnter(...args);
            }
            this.setState({ open: true });
          },
          onMouseLeave: (...args) => {
            if (child.props.onMouseLeave) {
              child.props.onMouseLeave(...args);
            }
            this.setState({ open: false });
          },
        })}
        {open && !disabled && (
          <Tooltip target={this.id} {...props}>
            {content}
          </Tooltip>
        )}
      </React.Fragment>
    );
  }
}

export const Popup = fp.flow(
  onClickOutside,
  withDynamicPosition()
)(
  class PopupComponent extends React.Component<any> {
    static displayName = 'Popup';

    static readonly defaultProps = {
      side: 'right',
    };

    render() {
      const {
        children,
        side,
        elementRef,
        dimensions: { target, element, viewport },
        onClick,
      } = this.props;

      const { position } = computePopupPosition({
        side,
        target,
        element,
        viewport,
        gap: 10,
      });
      return (
        <PopupPortal>
          <div
            onClick={onClick}
            ref={elementRef}
            style={
              {
                transform: `translate(${position.left}px, ${position.top}px)`,
                ...styles.popup,
              } as React.CSSProperties
            }
          >
            {children}
          </div>
        </PopupPortal>
      );
    }
  }
);

interface PopupTriggerProps {
  children: any;
  closeOnClick?: boolean;
  content: any;
  onOpen?: () => void;
  onClose?: () => void;
  side?: string;
}

// to test PopupTrigger using Enzyme simulate a click on the child element
export class PopupTrigger extends React.Component<PopupTriggerProps, any> {
  static readonly defaultProps = {
    closeOnClick: false,
    onOpen: () => {},
    onClose: () => {},
  };

  id: string;

  constructor(props: any) {
    super(props);
    this.state = { open: false };
    this.id = `popup-trigger-${fp.uniqueId('')}`;
  }

  close() {
    this.setState({ open: false });
    this.props.onClose();
  }

  render() {
    const { children, content, onOpen, onClose, closeOnClick, ...props } =
      this.props;
    const { open } = this.state;
    const child = React.Children.only(children);
    return (
      <React.Fragment>
        {React.cloneElement(child, {
          id: this.id,
          className: `${child.props.className || ''} ${this.id}`,
          onClick: (...args: any[]) => {
            if (child.props.onClick) {
              child.props.onClick(...args);
            }
            if (!open) {
              onOpen();
            }
            if (open) {
              onClose();
            }
            this.setState({ open: !open });
          },
        })}
        {open && (
          <Popup
            target={this.id}
            handleClickOutside={() => this.close()}
            outsideClickIgnoreClass={this.id}
            onClick={closeOnClick ? () => this.close() : () => {}}
            {...props}
          >
            {content}
          </Popup>
        )}
      </React.Fragment>
    );
  }
}
