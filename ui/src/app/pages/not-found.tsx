import {RouteLink} from 'app/components/app-router';
import {BoldHeader} from 'app/components/headers';
import {PublicLayout} from 'app/components/public-layout';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';
import {useEffect} from 'react';
import * as React from 'react';

export const NotFound = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  return<PublicLayout>
    <BoldHeader>Page Not Found</BoldHeader>
    <section style={{color: colors.primary, fontSize: '18px', marginTop: '.5rem'}}>
      Please try navigating to the <RouteLink path='/'>home page</RouteLink>.
    </section>
    </PublicLayout>;
};
