package org.pmiops.workbench.captcha;

import org.springframework.stereotype.Service;

public interface CaptchaVerificationService {

  boolean verifyCaptcha(String responseToken) throws ApiException;

}
