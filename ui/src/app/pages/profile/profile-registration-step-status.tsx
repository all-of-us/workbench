import * as React from 'react';

import { Button } from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import * as fp from 'lodash/fp';


const styles = reactStyles({
  container: {
    backgroundColor: colors.white,
    color: colors.primary,
    height: '225px',
    width: '220px',
    borderRadius: 8,
    marginBottom: '10px',
    padding: 14,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`
  },
  title: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600,
    marginBottom: 6
  },
  button: {
    height: '38px'
  },
  successButton: {
    backgroundColor: colors.success,
    cursor: 'default'
  }
});

interface Props {
  containerStylesOverride?: React.CSSProperties;
  title: string | React.ReactNode;
  wasBypassed: boolean;
  isComplete: boolean;
  incompleteButtonText: string;
  completedButtonText: string;
  completeStep: Function;
  childrenStyle?: React.CSSProperties;
  content?: JSX.Element | JSX.Element[]; // | React.ReactChildren;
}

const maybeRenderContent = fp.curry(({childrenStyle, content, isComplete}, components) => {
  return isComplete ? [...components, (<div style={childrenStyle}>{ content }</div>)] : components
})

const maybeRenderCompleteButton = fp.curry(({isComplete, wasBypassed, completedButtonText}, components) => {
  return isComplete ? 
  [ ...components, (<Button disabled={true} data-test-id='completed-button'
    style={{...styles.button, backgroundColor: colors.success,
      width: 'max-content', cursor: 'default'}}>
    <ClrIcon shape='check' style={{marginRight: '0.3rem'}}/>{wasBypassed ? 'Bypassed' : completedButtonText}
  </Button>)] : components
})

const maybeRenderIncompleteButton = fp.curry(({isComplete, completeStep, incompleteButtonText}, components) => {
  return !isComplete 
    ? [...components, (<Button type='purplePrimary' style={ styles.button } onClick={ completeStep }>{ incompleteButtonText }</Button>)]
    : components
})

const renderChildren = fp.curry((children, components) => [children, ...components] )

const ProfileRegistrationStepStatus: React.FunctionComponent<Props> =
  (props) => {
    const {
      title,
      wasBypassed,
      isComplete,
      incompleteButtonText,
      completedButtonText,
      completeStep,
      childrenStyle,
      children,
      content
    } = props;

    const contentToRender = fp.flow(
      maybeRenderContent({childrenStyle, content, isComplete}), 
      renderChildren(children),
      maybeRenderCompleteButton({isComplete, wasBypassed, completedButtonText}), 
      maybeRenderIncompleteButton({isComplete, completeStep, incompleteButtonText})
    )([])

    return (
      <FlexColumn style={{...styles.container, ...props.containerStylesOverride}}>
        <div style={styles.title}>
          { title }
        </div>
        <FlexColumn style={{
          justifyContent: contentToRender.length > 1 ? 'space-between' : 'flex-end', 
          flex: '1 1 auto',
          alignItems: 'baseline'
          }}>
          {React.createElement(React.Fragment, {}, ...contentToRender)}
        </FlexColumn>
      </FlexColumn>
    );
  };

export {
  ProfileRegistrationStepStatus,
  Props as ProfileRegistrationStepStatusProps
};