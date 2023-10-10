import * as React from 'react';
import { useState } from 'react';
import ReCAPTCHA from 'react-google-recaptcha';

import { environment } from 'environments/environment';
import { serverConfigStore } from 'app/utils/stores';

interface Props {
  captchaRef: any;
  captureCaptchaResponse: Function;
}

export const ReCaptcha = (props: Props) => {
  const [captcha, setCaptcha] = useState<boolean>(false);
  const { enableCaptcha } = serverConfigStore.get().config;

  const onClick = (token: string) => {
    setCaptcha(!captcha);
    window.dispatchEvent(new Event('captcha-solved'));
    props.captureCaptchaResponse(token);
  };

  return (
    enableCaptcha && (
      <ReCAPTCHA
        sitekey={environment.captchaSiteKey}
        ref={props.captchaRef}
        onChange={(token: string) => {
          onClick(token);
        }}
      />
    )
  );
};
