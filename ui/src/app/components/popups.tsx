import _ from 'lodash/fp';
import * as React from 'react';
import {createPortal} from 'react-dom';
import onClickOutside from 'react-onclickoutside';
import {switchCase} from '../utils/index';

const styles = {
  tooltip: {
    background: 'black', color: 'white',
    padding: '0.5rem',
    position: 'fixed', top: 0, left: 0, pointerEvents: 'none',
    maxWidth: 400, borderRadius: 4
  },
  notch: {
    fill: 'black',
    position: 'absolute',
    width: 16, height: 8,
    marginLeft: -8, marginRight: -8, marginTop: -8,
    transformOrigin: 'bottom'
  },
  popup: {
    position: 'fixed', top: 0, left: 0,
    backgroundColor: 'white',
    border: `1px solid #b7b7b7`, borderRadius: 4,
    boxShadow: '0 3px 2px 0 rgba(0,0,0,0.12)'
  }
};

export const withDynamicPosition = () => WrappedComponent => {
  const Wrapper = class extends React.Component {
    constructor(props) {
      super(props)
      this.state = {
        dimensions: {
          element: { width: 0, height: 0 },
          target: { top: 0, bottom: 0, left: 0, right: 0 },
          viewport: { width: 0, height: 0 }
        }
      }
      this.element = React.createRef()
    }

    static displayName = `withDynamicPosition()`

    componentDidMount() {
      this.reposition()
    }

    componentWillUnmount() {
      cancelAnimationFrame(this.animation)
    }

    reposition() {
      const { target } = this.props
      const { dimensions } = this.state
      this.animation = requestAnimationFrame(() => this.reposition())
      const newDimensions = {
        element: _.pick(['width', 'height'], this.element.current.getBoundingClientRect()),
        target: _.pick(['top', 'bottom', 'left', 'right'], document.getElementById(target).getBoundingClientRect()),
        viewport: { width: window.innerWidth, height: window.innerHeight }
      }
      if (!_.isEqual(newDimensions, dimensions)) {
        this.setState({ dimensions: newDimensions })
      }
    }

    render() {
      const { dimensions } = this.state
      return <WrappedComponent
        dimensions={dimensions}
        elementRef={this.element}
        {...this.props}
      />
    }
  }
  return Wrapper
}

export const computePopupPosition = ({ side, viewport, target, element, gap }) => {
  const getPosition = s => {
    const left = _.flow(
      _.clamp(0, viewport.width - element.width),
      _.clamp(target.left - element.width + 16, target.right - 16)
    )(((target.left + target.right) / 2) - (element.width / 2))
    const top = _.flow(
      _.clamp(0, viewport.height - element.height),
      _.clamp(target.top - element.height + 16, target.bottom - 16)
    )(((target.top + target.bottom) / 2) - (element.height / 2))
    return switchCase(s,
      ['top', () => ({ top: target.top - element.height - gap, left })],
      ['bottom', () => ({ top: target.bottom + gap, left })],
      ['left', () => ({ left: target.left - element.width - gap, top })],
      ['right', () => ({ left: target.right + gap, top })]
    )
  }
  const position = getPosition(side)
  const maybeFlip = d => {
    return switchCase(d,
      ['top', () => position.top < 0 ? 'bottom' : 'top'],
      ['bottom', () => position.top + element.height >= viewport.height ? 'top' : 'bottom'],
      ['left', () => position.left < 0 ? 'right' : 'left'],
      ['right', () => position.left + element.width >= viewport.width ? 'left' : 'right']
    )
  }
  const finalSide = maybeFlip(side)
  const finalPosition = getPosition(finalSide)
  return { side: finalSide, position: finalPosition }
}

export const PopupPortal = ({ children }) => {
  return createPortal(React.Children.only(children), document.getElementById('popup-root'))
}

export const Tooltip = withDynamicPosition()(class Tooltip extends React.Component {
  static defaultProps = {
    side: 'bottom'
  }

  render() {
    const { children, side, elementRef, dimensions: { target, element, viewport } } = this.props
    const { side: finalSide, position } = computePopupPosition({ side, target, element, viewport, gap: 10 })
    const getNotchPosition = () => {
      const left = _.clamp(12, element.width - 12,
        (target.left + target.right) / 2 - position.left
      )
      const top = _.clamp(12, element.height - 12,
        (target.top + target.bottom) / 2 - position.top
      )
      return switchCase(finalSide,
        ['top', () => ({ bottom: 0, left, transform: 'rotate(180deg)' })],
        ['bottom', () => ({ top: 0, left })],
        ['left', () => ({ right: 0, top, transform: 'rotate(90deg)' })],
        ['right', () => ({ left: 0, top, transform: 'rotate(270deg)' })]
      )
    }
    return <PopupPortal>
      <div
        ref={elementRef}
        style={{
          transform: `translate(${position.left}px, ${position.top}px)`,
          ...styles.tooltip
        }}
      >
        {children}
        <svg viewBox='0 0 2 1' style={{...getNotchPosition(), ...styles.notch}}>
          <path d='M0,1l1,-1l1,1Z' />
        </svg>
      </div>
    </PopupPortal>
  }
})

export class TooltipTrigger extends React.Component {
  constructor(props) {
    super(props)
    this.state = { open: false }
    this.id = `tooltip-trigger-${_.uniqueId()}`
  }

  render() {
    const { children, content, ...props } = this.props
    const { open } = this.state
    if (!content) {
      return children
    }
    const child = React.Children.only(children)
    return <React.Fragment>
      {React.cloneElement(child, {
        id: this.id,
        onMouseEnter: (...args) => {
          child.props.onMouseEnter && child.props.onMouseEnter(...args)
          this.setState({ open: true })
        },
        onMouseLeave: (...args) => {
          child.props.onMouseLeave && child.props.onMouseLeave(...args)
          this.setState({ open: false })
        }
      })}
      {open && <Tooltip target={this.id} {...props}>{content}</Tooltip>}
    </React.Fragment>
  }
}

export const Popup = _.flow(
  onClickOutside,
  withDynamicPosition()
)(class Popup extends React.Component {
  static defaultProps = {
    side: 'right'
  }

  render() {
    const { children, side, elementRef, dimensions: { target, element, viewport }, onClick } = this.props
    const { position } = computePopupPosition({ side, target, element, viewport, gap: 10 })
    return <PopupPortal>
      <div
        onClick={onClick}
        ref={elementRef}
        style={{ transform: `translate(${position.left}px, ${position.top}px)`, ...styles.popup }}
      >{children}</div>
    </PopupPortal>
  }
})

export class PopupTrigger extends React.Component {
  static defaultProps = {
    closeOnClick: false
  }

  constructor(props) {
    super(props)
    this.state = { open: false }
    this.id = `popup-trigger-${_.uniqueId()}`
  }

  close() {
    this.setState({ open: false })
  }

  render() {
    const { children, content, closeOnClick, ...props } = this.props
    const { open } = this.state
    const child = React.Children.only(children)
    return <React.Fragment>
      {React.cloneElement(child, {
        id: this.id,
        className: `${child.props.className || ''} ${this.id}`,
        onClick: (...args) => {
          child.props.onClick && child.props.onClick(...args)
          this.setState({ open: !open })
        }
      })}
      {open && <Popup
        target={this.id}
        handleClickOutside={() => this.setState({ open: false })}
        outsideClickIgnoreClass={this.id}
        onClick={closeOnClick ? () => this.close() : undefined}
        {...props}
      >{content}</Popup>}
    </React.Fragment>
  }
}
