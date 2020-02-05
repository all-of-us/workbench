import {FlexColumn} from "app/components/flex";
import * as React from "react";
import colors from "app/styles/colors";

export const AouP = (props) => {
  return <FlexColumn style={{
    color: colors.primary,
    fontSize: '14px',
    lineHeight: '22px',
    marginBottom: '0.5rem'
  }}>
    {props.children}
  </FlexColumn>
}