# Mockito Best Practices
## What to Mock
## Creating Mock Instances
### `Mockito.mock(MyClass.class)`
The `mock()` method declares a mock object at local scope. This method is
most suitable for creating temporary mocks that are only needed in a single
test case or helper method. In particular, these mocks do not participate
in dependency injection unless explicitly returned via a `@Bean` method.
### `@MockBean({MyService.class, MyDependency.class})`
Declaring a class in the mock bean list on the test configuration class
makes that mocked class available via dependency injection, so that it
can be accessed via `@Autowired` injection. This may take place either
in the test class itself, or (preferably) within the class under test
or one of its dependencies.

This method is preferred when the mocked class's behavior is not relevant
to the test class but it's presence is required to construct something
that is required.

### `@MockBean` Annotation on Test Class Member
Using the `@MockBean` annotation on a member declaration, such as the following will instantiate
a named mock object that is available for dependency injection using the test class's application
context:
```java
@MockBean private BigQueryService mockBigQueryService;
``` 

This method is preferred when there is a need to stub methods on the mock and have it injected. It's
effectively equivalent to having the class in the `@MockBean` configuration list and then declaring
an `@Autowired` member, but is more expressive of intent (as there's only one place to look.) Named
mock instances should be given a `mock` prefix so that they're readily apparent in the debugger.

## Stubbing a method
## Argument Captors
## Verifying Interactions

## Style
### Naming
Named mock instances should be given a `mock` prefix so that they're readily apparent in the
debugger. For example, a value object might be named `mockEmployee` and a mock service named
`mockRegistrationService`.
### Static Imports
The mockito library is an exception to our overall Java style guide due to the fact that
it has many small methods that are more like operators. Static import statements are thus preferred:
```java
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
``` 

## Gotchas

### `doReturn()` and `doAnswer` vs `return().when()` and `answer().when()`

### Null Return Values
Mocks of methods that return an object will return `null` unless
stubbed. Thus, it's a major cause of `NullPointerException`. Note
that in many cases it's necessary to return default-constructed
structures such as empty lists or result objects.

### Prototype vs Singleton Scope
This is true of all objects in an application context, but bears repeating. If a bean has Prototype
scope, it will be re-created with each injection. This means that there can be coupling between test
cases, introducing a subtle order dependence among the cases. It's best to use singletons whenever possible,
or to reset the instance in the `@Before` method.

### Mixing Argument Matchers with Explicit Values
When using matchers such as `any()`, `anyInt()`, etc, the entire argument list provided must use
matchers. In order to require an exact match, the `eq()` matcher is available. Conversely, it's
possible to provide all arguments as literal values. 
