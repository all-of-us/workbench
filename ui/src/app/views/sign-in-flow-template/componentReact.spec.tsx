import {Button} from 'app/components/buttons';

import {mount} from 'enzyme';
import * as React from 'react';

import RegistrationPageTemplateReact, {pageImages, PageTemplateProps} from './component';

describe('SignPageTemplateReact', () => {
  let props: PageTemplateProps;

  const signIn = jest.fn();

  const component = () => {
    return mount(<RegistrationPageTemplateReact {...props}/>);
  };

  beforeEach(() => {
    props = {
      signIn: signIn,
      windowSize: {width: 1400, height: 0}
    } as PageTemplateProps;
  });

  it('should display login background image and directive by default', () => {
    const wrapper = component();
    console.log(wrapper.html());
    const templateImage = wrapper.find('div').find({id: 'template'});
    const backgroundImage = templateImage.prop('style').backgroundImage;
    expect(backgroundImage).toBe('url(\'' + pageImages.login.backgroundImgSrc + '\')');
    expect(wrapper.find('div').find({dataTestId: 'login'})).toBeTruthy();
  });

  it('should display small background image when window width is between 900 and 1300', () => {
    props.windowSize.width = 999;
    const wrapper = component();
    const templateImage = wrapper.find('div').find({id: 'template'});
    const backgroundImage = templateImage.prop('style').backgroundImage;

    expect(backgroundImage)
        .toBe('url(\'' + pageImages.login.smallerBackgroundImgSrc + '\')');
    expect(wrapper.find('div').find({dataTestId: 'invitation'})).toBeTruthy();
  });

  it('should display invitation key component on clicking Create account on login page ', () => {
    const wrapper = component();
    const createAccountButton = wrapper.find(Button).find({type: 'secondary'});
    createAccountButton.simulate('click');
    wrapper.update();
    const templateImage = wrapper.find('div').find({id: 'template'});
    const backgroundImage = templateImage.prop('style').backgroundImage;

    expect(backgroundImage)
        .toBe('url(\'' + pageImages.invitationKey.backgroundImgSrc + '\')');
    expect(wrapper.find('div').find({dataTestId: 'invitation'})).toBeTruthy();
  });

  it('should display invitation key with small image when width is between 900 and 1300 ', () => {
    props.windowSize.width = 999;
    const wrapper = component();
    const createAccountButton = wrapper.find(Button).find({type: 'secondary'});
    createAccountButton.simulate('click');
    wrapper.update();
    const templateImage = wrapper.find('div').find({id: 'template'});
    expect(templateImage.prop('style').backgroundImage)
        .toBe('url(\'' + pageImages.invitationKey.smallerBackgroundImgSrc + '\')');
  });
});
