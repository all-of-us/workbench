import * as React from 'react';

import {TextInput} from 'app/components/inputs';
import {AouTitle} from 'app/components/text-wrappers';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';

// Contains style definitions shared across multiple account-creation form steps.
export const commonStyles = reactStyles({
  asideContainer: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.85),
    borderRadius: 8,
    width: '18rem',
    padding: '0.5rem'
  },
  asideHeader: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: 16,
  },
  asideList: {
    display: 'flex',
    height: '100%',
    flexDirection: 'column',
    justifyContent: 'space-evenly'
  },
  asideText: {
    fontSize: 14,
    fontWeight: 400,
    color: colors.primary,
  },
  text: {
    fontSize: 14,
    color: colors.primary,
    lineHeight: '22px',
  },
  boldText: {
    fontSize: 14,
    color: colors.primary,
    lineHeight: '22px',
    fontWeight: 600,
  },
  sectionInput: {
    width: '14rem',
    height: '1.5rem'
  },
});

/**
 * This content-only component is shown in the side of a couple account-creation sub-components.
 **/
export const WhyWillSomeInformationBePublic: React.FunctionComponent = () => {
  return <React.Fragment>
    <div style={commonStyles.asideHeader}>Why will some information be public?</div>
    <div style={commonStyles.asideText}>The <AouTitle/> The All of Us Research Program seeks to be transparent
      with participants about who can access their data and for what purpose. Therefore, we will display
      your name, institution, role, research background/interests, and a link to your professional
      profile (if available) in the the <a href='https://researchallofus.org/'>Research Projects
        Directory</a> on our public website.
    </div>
  </React.Fragment>;
};

/**
 * Creates a text input component with a label shown above it.
 * @param props
 * @constructor
 */
export function TextInputWithLabel(props) {
  return <div style={{width: '12rem', ...props.containerStyle}}>
    {props.labelContent}
    {props.labelText && <label style={{...commonStyles.text, fontWeight: 600}}>{props.labelText}</label>}
    <div style={{marginTop: '0.1rem'}}>
      <TextInput data-test-id={props.inputId}
                 id={props.inputId}
                 name={props.inputName}
                 placeholder={props.placeholder}
                 value={props.value}
                 disabled={props.disabled}
                 onChange={props.onChange}
                 onBlur={props.onBlur}
                 invalid={props.invalid ? props.invalid.toString() : undefined}
                 style={{...commonStyles.sectionInput, ...props.inputStyle}}/>
      {props.children}
    </div>
  </div>;
}
