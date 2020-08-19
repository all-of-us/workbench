# Java Unit Tests
All Backend services and utility classes (and any controllers with nontrivial logic) should have a
unit test class. The test class for the `MyService` Bean is named `MyServiceTest`.
## JUnit Test Framework
[JUnit 4](https://junit.org/) is the unit test driver for our backend unit tests.
It has native integration with IntelliJ, although tests can be run from a terminal as well.

## Spring Considerations
### Application Context
Any tests using [Spring Boot](https://docs.spring.io/spring-boot/docs/2.1.6.RELEASE/reference/html/boot-features-testing.html) should use the `@RunWith(SpringRunner.class)`
annotation. This sets up the application context for dependency injection.
### Test Configuration
In order to introduce beans for services or other dependencies, they must first be 
provided to a [nested class](https://docs.spring.io/spring-boot/docs/2.1.6.RELEASE/reference/html/boot-features-testing.html#boot-features-testing-spring-boot-applications-detecting-config)
annotated with `@TestConfiguration` and conventionally named `Configuration`. 

The test configuration introduces new items into the application's primary 
configuration as one of
* a named import of a concrete class in an `@Import` annnotation
* a named mock of an injectable interface in a `@MockBean` annontation
* a public method on the class returning an instance of the bean annotated with `@Bean`.

#### Imported Configuration Classes
When a class instance is included in the `@Import` list, the DI system searches
for a matching class by class name (i.e. not an interface). This clause
generally contains the primary class under test (assuming it's a Bean), and possibly
a few other dependencies whose code should be tested in the current test class. If too
many classes are listed in this section, it may be a sign that the class(es) being
tested are not well factored for testability.

Class insetances may be instantiated via an `@Autowired` annotation, generally on an interface that
the class implements on either a direct test class member, or as a transitive dependency. For
example, if my test class is for `WidgetService`, I'll likely have `WidgetServiceImpl` in the
`@Import` list, and declare a class member like
```java
@Autowired private WidgetService widgetService;
```
The runtime class of the `widgetService` member will of type `WidgetServiceImpl` in this case. 
#### Mocked Classes
Anonymous, mocked, injected classes may be listed in the `@MockBean` list, directly
after the `@Import` list. Dependencies will pull in mocked instances of these types,
and in general this is the preferred style only if no test code needs to refer to these instances.

#### Explicit Bean Methods

## Mocking
See [Mockito Best Practices](mockito-best-practices.md).

## Assertions with Truth

## Workbench Config
