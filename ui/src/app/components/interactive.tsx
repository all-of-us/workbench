import * as Utils from 'app/utils';
import * as fp from 'lodash/fp';
import * as React from 'react';

const {useState, forwardRef} = React;
const allowedHoverVariables = ['backgroundColor', 'border', 'color', 'boxShadow', 'opacity', 'textDecoration'];
const pointerTags = ['button', 'area', 'a', 'select'];
const pointerTypes = ['radio', 'checkbox', 'submit', 'button'];

interface InteractiveProps {
  className?: string;
  type?: string;
  role?: string;
  onClick?: () => any;
  onKeyDown?: () => any;
  onMouseDown?: Function;
  onBlur?: Function;
  disabled?: boolean;
  children?: React.ReactElement | React.ReactElement[];
  tabIndex?: number;
  hover?: React.CSSProperties;
  style?: React.CSSProperties;
  tagName: keyof JSX.IntrinsicElements;
}

export const Interactive: React.ForwardRefExoticComponent<InteractiveProps> = forwardRef(({
  className = '',
  tagName: TagName = 'div',
  type,
  role,
  onClick,
  onKeyDown,
  onMouseDown,
  onBlur,
  disabled,
  children,
  tabIndex,
  hover = {},
  style = {},
  ...props}: InteractiveProps, ref) => {
  const [outline, setOutline] = useState(undefined);
  const { cursor } = style;

  const computedCursor = Utils.cond(
    [!!cursor, () => cursor],
    [disabled, () => undefined],
    [!!onClick || pointerTags.includes(TagName) || pointerTypes.includes(type), () => 'pointer']
  );

  const computedTabIndex = Utils.cond(
    [fp.isNumber(tabIndex), () => tabIndex],
    [disabled, () => -1],
    [!!onClick, () => 0],
    () => undefined);

  const computedRole = Utils.cond(
    [!!role, () => role],
    [onClick && !['input', ...pointerTags].includes(TagName), () => 'button'],
    () => undefined);

  const cssVariables = fp.flow(
    fp.toPairs,
    fp.flatMap(([key, value]) => {
      console.assert(allowedHoverVariables.includes(key),
        `${key} needs to be added to the hover-style in style.css for the style to be applied`);
      return [[`--app-hover-${key}`, value], [key, `var(--hover-${key}, ${style[key]})`]];
    }),
    fp.fromPairs
  )(hover);

  return React.createElement(TagName, {
    ref,
    className: `hover-style ${className}`,
    style: {...style, ...cssVariables, fill: `var(--hover-color, ${style.color})`,  cursor: computedCursor, outline},
    role: computedRole,
    tabIndex: computedTabIndex,
    onClick,
    onMouseDown: e => {
      setOutline('none');
      if (onMouseDown) {
        onMouseDown(e);
      }
    },
    onBlur: e => {
      if (!!outline) {
        setOutline(undefined);
      }
      if (onBlur) {
        onBlur(e);
      }
    },
    onKeyDown: onKeyDown || ((event: React.KeyboardEvent) => {
      if (event.key === 'Enter') {
        event.stopPropagation();
        (event.target as HTMLElement).click();
      }
    }),
    ...props
  }, [children]);
});
