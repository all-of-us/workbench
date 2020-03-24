import * as React from 'react';

import {Divider} from 'app/components/divider';
import {flexStyle} from 'app/components/flex';
import {FormSection} from 'app/components/forms';
import {TextInput} from 'app/components/inputs';
import {AouTitle} from 'app/components/text-wrappers';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {Dropdown} from 'primereact/dropdown';

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
  sectionHeader: {
    width: '26rem',
    color: colors.primary,
    fontWeight: 600,
    fontSize: 18,
  },
  sectionInput: {
    width: '12rem',
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
  return <div style={{...props.containerStyle}}>
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

/**
 * Creates a FormSection with custom base styling for account-creation pages.
 * @param props
 * @constructor
 */
export const Section = (props) => {
  return <FormSection
    style={{...flexStyle.column, ...props.style}}>
    <div>
      <label style={{...commonStyles.sectionHeader, ...props.sectionHeaderStyles}}>
        {props.header}
      </label>
      {props.subHeader &&
      <label style={{color: colors.primary, fontSize: '12px', marginLeft: '.25rem'}}> {props.subHeader} </label>
      }
    </div>
    <Divider style={{marginTop: '.25rem'}}/>
    {props.children}
  </FormSection>;
};

/**
 * Creates a form section with a dropdown input.
 *
 * @param props
 * @constructor
 */
export const DropDownSection = (props) => {
  return <Section header={props.header}>
    <Dropdown placeholder='Select'
              options={props.options}
              style={{width: '50%'}}
              value={props.value}
              onChange={(e) => props.onChange(e.value)}/>
  </Section>;
};
