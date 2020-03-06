import {AouTitle} from 'app/components/text-wrappers';
import * as React from 'react';
import {commonStyles} from 'app/pages/login/account-creation/common-styles';

const styles = commonStyles;

export const WhyWillSomeInformationBePublic: React.FunctionComponent = () => {
  return <React.Fragment>
    <div style={styles.asideHeader}>Why will some information be public?</div>
    <div style={styles.asideText}>The <AouTitle/> The All of Us Research Program seeks to be transparent
      with participants about who can access their data and for what purpose. Therefore, we will display
      your name, institution, role, research background/interests, and a link to your professional
      profile (if available) in the the <a href='https://researchallofus.org/'>Research Projects
        Directory</a> on our public website.
    </div>
  </React.Fragment>;
};
