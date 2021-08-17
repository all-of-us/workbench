import {useNavigation} from 'app/utils/navigation';
import * as React from 'react';

export const withNavigation = WrappedComponent => ({...props}) => {
  const [navigate, navigateByUrl] = useNavigation();

  return <WrappedComponent navigate={navigate}
                           navigateByUrl={navigateByUrl}
                           {...props}/>;
};
