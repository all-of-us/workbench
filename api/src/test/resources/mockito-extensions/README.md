The extension file in this directory enables Mockito functionality to mock final classes,
as described in https://www.baeldung.com/mockito-final and
https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2#unmockable .

This enables seamless mocking of final classes and methods. The one potential downside is that
this has a known tendency to slow down test runs. If we notice unit tests begin to take too long,
we could try disabling this feature. The downside is that we'll have to remove unit tests which
rely on mocking final classes (which will hopefully be few and far between).