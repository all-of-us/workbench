import * as React from 'react';

import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header, SmallHeader} from 'app/components/headers';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';

export const styles = reactStyles({
  welcomeMessageIcon: {
    height: '2.25rem', width: '2.75rem'
  },
});

export const WelcomeHeader = () => {
  return <FlexRow style={{marginLeft: '3%'}}>
        <FlexColumn style={{width: '50%'}}>
            <FlexRow>
                <FlexColumn>
                    <Header style={{fontWeight: 500, color: colors.secondary, fontSize: '0.92rem'}}>
                        Welcome to the</Header>
                    <Header style={{textTransform: 'uppercase', marginTop: '0.2rem'}}>
                        Researcher Workbench</Header>
                </FlexColumn>
                <FlexRow style={{alignItems: 'flex-end', marginLeft: '1rem'}}>
                    <img style={styles.welcomeMessageIcon} src='/assets/images/workspace-icon.svg'/>
                    <img style={styles.welcomeMessageIcon} src='/assets/images/cohort-icon.svg'/>
                    <img style={styles.welcomeMessageIcon} src='/assets/images/analysis-icon.svg'/>
                </FlexRow>
            </FlexRow>
            <SmallHeader style={{color: colors.primary, marginTop: '0.25rem'}}>
                The secure platform to analyze <i>All of Us</i> data</SmallHeader>
        </FlexColumn>
        <div/>
    </FlexRow>;
};
