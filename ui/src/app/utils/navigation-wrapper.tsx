import * as React from 'react';
import {useNavigation} from 'app/utils/navigation';

export const withNavigation = WrappedComponent => ({...props}) => {
  const [navigate, navigateByUrl] = useNavigation();

  return <WrappedComponent navigate={navigate}
                           navigateByUrl={navigateByUrl}
                           {...props}/>;
};
