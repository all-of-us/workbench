import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { Divider } from 'app/components/divider';
import { flexStyle } from 'app/components/flex';
import { FormSection } from 'app/components/forms';
import { AouTitle } from 'app/components/text-wrappers';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

// Contains style definitions shared across multiple account-creation form steps.
export const commonStyles = reactStyles({
  asideContainer: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.85),
    borderRadius: 8,
    width: '27rem',
    padding: '0.75rem',
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
    justifyContent: 'space-evenly',
    listStylePosition: 'outside',
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
    width: '39rem',
    color: colors.primary,
    fontWeight: 600,
    fontSize: 18,
  },
  sectionInput: {
    width: '18rem',
    height: '2.25rem',
  },
});

/**
 * This content-only component is shown in the side of a couple account-creation sub-components.
 **/
export const WhyWillSomeInformationBePublic = () => {
  return (
    <React.Fragment>
      <div style={commonStyles.asideHeader}>
        Why will some information be public?
      </div>
      <div style={commonStyles.asideText}>
        The <AouTitle /> seeks to be transparent with participants about who can
        access their data and for what purpose. Therefore, we will display your
        name, institution, role, research background/interests, and a link to
        your professional profile (if available) in the
        <a
          target='_blank'
          href='https://www.researchallofus.org/research-projects-directory/'
        >
          &nbsp;Research Projects Directory
        </a>{' '}
        on our public website.
      </div>
      <div style={commonStyles.asideText}>
        This disclosure will also help us comply with the 21st Century Cures
        Act. Some of these categories may not be visible on our website
        currently, but will be added in the future.
      </div>
    </React.Fragment>
  );
};

/**
 * Creates a FormSection with custom base styling for account-creation pages.
 * @param props
 * @constructor
 */
export const Section = (props) => {
  return (
    <FormSection style={{ ...flexStyle.column, ...props.style }}>
      <div>
        <label
          style={{
            ...commonStyles.sectionHeader,
            ...props.sectionHeaderStyles,
          }}
        >
          {props.header}
        </label>
        {props.subHeader && (
          <label
            style={{
              color: colors.primary,
              fontSize: '12px',
              marginLeft: '.375rem',
              ...props.subHeaderStyle,
            }}
          >
            {props.subHeader}
          </label>
        )}
      </div>
      <Divider style={{ marginTop: '.375rem' }} />
      {props.children}
    </FormSection>
  );
};

export const OptionalSection = (props) => {
  return (
    <Section
      subHeader='(Optional)'
      subHeaderStyle={{ fontStyle: 'italic' }}
      {...props}
    >
      {props.children}
    </Section>
  );
};

/**
 * Creates a form section with a dropdown input.
 *
 * @param props
 * @constructor
 */
export const DropDownSection = (props) => {
  return (
    <Section
      header={props.header}
      subHeader={props.subHeader}
      subHeaderStyle={props.subHeaderStyle}
    >
      <Dropdown
        placeholder='Select'
        options={props.options}
        style={{ width: '50%' }}
        value={props.value}
        onChange={(e) => props.onChange(e.value)}
      />
    </Section>
  );
};

export const OptionalDropDownSection = (props) => {
  return (
    <DropDownSection
      subHeader='(Optional)'
      subHeaderStyle={{ fontStyle: 'italic' }}
      {...props}
    >
      {props.children}
    </DropDownSection>
  );
};
