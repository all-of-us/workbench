import {mount} from 'enzyme';
import * as React from 'react';

import {NotificationModal} from 'app/components/modals';
import {notificationStore} from 'app/utils/stores';
import {findNodesByExactText, waitOneTickAndUpdate} from 'testing/react-test-helpers';


describe('NotificationModal', () => {

  const component = () => {
    return mount(<div>
      <NotificationModal/>
    </div>);
  };

  beforeEach(async () => {
    notificationStore.set(null);
  });

  it('should appear and disappear based on store changes', async () => {
    const wrapper = component();
    const meta = {title: 'Hello', message: 'World'}
    // Notification modal should not render
    expect(wrapper.find(NotificationModal)).toEqual({});
    notificationStore.set(meta);

    // enzyme needs some encouragement
    await waitOneTickAndUpdate(wrapper);
    expect(notificationStore.get()).toEqual(meta);

    // When the store has data it should render
    await waitOneTickAndUpdate(wrapper);
    expect(findNodesByExactText(wrapper, meta.title).length).toBe(1);
    expect(findNodesByExactText(wrapper, meta.message).length).toBe(1);

    // Click button to dismiss - modal should not render
    wrapper.find('[role="button"]').first().simulate('click');

    // enzyme needs some encouragement
    await waitOneTickAndUpdate(wrapper);
    expect(notificationStore.get()).toEqual(null);

    // Modal should be gone
    await waitOneTickAndUpdate(wrapper);
    expect(findNodesByExactText(wrapper, meta.title).length).toBe(0);
  });
});
