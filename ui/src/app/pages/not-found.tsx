import {WithSpinnerOverlayProps} from "app/components/with-spinner-overlay";
import {useEffect} from "react";
import {PublicLayout} from "app/components/public-layout";
import {BoldHeader} from "app/components/headers";
import * as React from "react";
import colors from "../styles/colors";
import {RouteLink} from "app/components/app-router";

export const SignedOutNotFound = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  return<PublicLayout>
      <NotFound {...spinnerProps} />
    </PublicLayout>;
}

export const NotFound = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  return <React.Fragment>
    <BoldHeader>Page Not Found</BoldHeader>
    <section style={{color: colors.primary, fontSize: '18px', marginTop: '.5rem'}}>
      Please try reloading, or navigating to the <RouteLink path="/">home page</RouteLink>.
    </section>
  </React.Fragment>;
}
