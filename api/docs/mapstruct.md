# MapStruct Best Practices
## What is MapStruct?
[MapStruct](https://mapstruct.org/documentation/stable/reference/html/) is a Java code generator specializing in writing
conversion or mapping functions between an object instance and an instance of
a corresponding object in a class designed for another architectural tier, protocol, or technology.
It works mainly by matching types and names via reflection, or using programmer-provided
hints where that strategy isn't effective.
## Is this a gimmick? Why do I need it?
Everything MapStruct does is simple and deterministic, and every programmer has done it before. So why
bring in another library and codegen step? What's the advantage?

First, by defining a mapper, you're adding some executable documentation to your types, and being
explicit about how they should map.
 
Second, you don't have to rely on developers to perform awkward null
checks for every field, as you get that for free. The generated code is very uniform and thus
quite readable.

Third, and most important, is that once you've defined a mapper, maintenance is very easy. New fields
will simply be mapped for you (assuming they exist on both the source and the target type). If there's
a new target field that doesn't have a sourcce, youll gget a compile-time warning reminding you. (It's
important to stay on top of those). 

### Examples
Consider an `EmployeeModel` class:
```java
public class EmployeeModel {
  private String name;
  private Department department;
  private String address;
  private double salary;

  public EmployeeModel() {
  }

  public EmployeeModel(String name,
      Department department, String address, double salary) {
    this.name = name;
    this.department = department;
    this.address = address;
    this.salary = salary;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  public Department getDepartment() {
    return department;
  }
  public void setDepartment(Department department) {
    this.department = department;
  }

  public double getSalary() {
    return salary;
  }
  public void setSalary(double salary) {
    this.salary = salary;
  }

  public double getWeeklySalary() {
    return getSalary() / 52.0;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
```

Additionally, we have a (nearly) corresponding entity in the database. (Hibernate bindings omitted.)
```java
// ORM details omitted.
public class EmployeeDbEntity {

  private String firstName;
  private String lastName;
  private int departmentCode; // corresponds to enum
  private double hourlyRate;
  private String address;

  public EmployeeDbEntity(String firstName, String lastName, int departmentCode, double hourlyRate,
      String address) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.departmentCode = departmentCode;
    this.hourlyRate = hourlyRate;
    this.address = address;
  }

  public String getFirstName() {
    return firstName;
  }
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public int getDepartmentCode() {
    return departmentCode;
  }
  public void setDepartmentCode(int departmentCode) {
    this.departmentCode = departmentCode;
  }

  public double getHourlyRate() {
    return hourlyRate;
  }
  public void setHourlyRate(double hourlyRate) {
    this.hourlyRate = hourlyRate;
  }

  public String getLastName() {
    return lastName;
  }
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getAddress() {
    return address;
  }
  public void setAddress(String address) {
    this.address = address;
  }
}
```

A naive first cut at a mapping function might look like this:
```java
import org.mapstruct.Mapper;

@Mapper
public interface EmployeeMapper {

  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);
}
```

Using `./gradlew compileJava`, I see that this compiles, but gives warnings
```
examples/EmployeeMapper.java:8: warning: Unmapped target properties: "name, department, salary".
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);
                ^
```

So the good news is I've successfully generated a mapper. The bad news is that it's only one for
four properties.
```java
@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2020-05-02T18:10:18-0400",
    comments = "version: 1.3.1.Final, compiler: javac, environment: Java 1.8.0_221 (Oracle Corporation)"
)
public class EmployeeMapperImpl implements EmployeeMapper {

    @Override
    public EmployeeModel toModel(EmployeeDbEntity employeeDbEntity) {
        if ( employeeDbEntity == null ) {
            return null;
        }

        EmployeeModel employeeModel = new EmployeeModel();

        employeeModel.setAddress( employeeDbEntity.getAddress() );

        return employeeModel;
    }
}
```

To fix the name mapping we need to add a method to build a full name from first & last names. Let's pretend our
system didn't already have a good one, and add it directly in the mapper itself for brevity. To do
this, we need to create a `default` method on the interface, which is a relatively new facility in
Java to make life easier in this kind of situation.

Our default method is itself another mapper, though you want to be careful when the source type of
such a mapper is a common type like String or Timestamp.

```java
@Mapper
public interface EmployeeMapper {

  @Mapping(source = "employeeDbEntity", target = "name")
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);

  default String toName(EmployeeDbEntity employeeDbEntity) {
    return String.format("%s %s", employeeDbEntity.getFirstName(), employeeDbEntity.getLastName());
  }
}
```

On compiling this, we have a shorter error message:
```
EmployeeMapper.java:11: warning: Unmapped target properties: "department, salary".
```

Our generated implementation class now looks like this:
```java
public class EmployeeMapperImpl implements EmployeeMapper {

    @Override
    public EmployeeModel toModel(EmployeeDbEntity employeeDbEntity) {
        if ( employeeDbEntity == null ) {
            return null;
        }

        EmployeeModel employeeModel = new EmployeeModel();

        employeeModel.setName( toName( employeeDbEntity ) ); // <-- our default mapper method
        employeeModel.setAddress( employeeDbEntity.getAddress() );

        return employeeModel;
    }
}
```

If you're following along, notice that MapStruct is being clever here, but not too clever
(by design). Since our `EmployeeDbEntity` class doesn't have an attribute named `name`
(or a `getName()` method), it doesn't just grab any String property from that class and hope for the
best. We had to tell it in a `@Mapping` annotation that it can in fact get a `String` called "name" from this
`employeeDbEntity` if it looks a little harder.

Since our default method takes in the entity and returns a string, this is a good candidate, and
that's what it uses.

What if we hadn't named it `toName()`? I tried that just now and the answer was slightly surprising:
it generated a line like `employeeModel.setName( toNickname( employeeDbEntity ) );`. I, for one,
welcome our new automatically programmed overlords.

We have two more properties to fix: the enum `department`, and the `double salary`. Let's start with
the enum. It's class definition looks like this:
```java
public enum Department {
  SALES(0),
  MARKETING(1),
  IT(2);

  private int departmentCode;

  Department(int departmentCode) {
    this.departmentCode = departmentCode;
  }

  public int getDepartmentCode() {
    return departmentCode;
  }

  public static Department fromDepartmentCode(int code) {
    switch(code) {
      case 0:
        return SALES;
      case 1:
        return MARKETING;
      case 2:
        return IT;
      default:
        return SALES; // they never know their code, so probably them
    }
  }
}
```

I thought I'd be clever and just pull in the enum type via a `uses` directive at the top of the mapper:
```java
@Mapper(uses = Department.class)
```
This did exactly the wrong thing. It generated the class without errors, but then failed to compile:
```java
public class EmployeeMapperImpl implements EmployeeMapper {

    private final Department department = new Department();

    @Override
    public EmployeeModel toModel(EmployeeDbEntity employeeDbEntity) {
        if ( employeeDbEntity == null ) {
            return null;
        }

        EmployeeModel employeeModel = department.clone();

        employeeModel.setName( toNickname( employeeDbEntity ) );
        employeeModel.setAddress( employeeDbEntity.getAddress() );

        return employeeModel;
    }
}
```

New errors:
```
> Task :compileJava 
/Users/jaycarlton/repos/workbench/api/src/main/java/org/pmiops/workbench/utils/mappers/examples/EmployeeMapper.java:10: warning: Unmapped target properties: "department, salary".
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);
                ^
/Users/jaycarlton/repos/workbench/api/build/generated/sources/annotationProcessor/java/main/org/pmiops/workbench/utils/mappers/examples/EmployeeMapperImpl.java:12: error: enum types may not be instantiated
    private final Department department = new Department();
                                          ^
/Users/jaycarlton/repos/workbench/api/build/generated/sources/annotationProcessor/java/main/org/pmiops/workbench/utils/mappers/examples/EmployeeMapperImpl.java:20: error: clone() has protected access in Enum
        EmployeeModel employeeModel = department.clone();
                                                ^
/Users/jaycarlton/repos/workbench/api/build/generated/sources/annotationProcessor/java/main/org/pmiops/workbench/utils/mappers/examples/EmployeeMapperImpl.java:20: error: incompatible types: Object cannot be converted to EmployeeModel
        EmployeeModel employeeModel = department.clone();
                                                      ^
```

Fine, we can do it the less slick but more reliable way:

```java
@Mapper(uses = SampleEnumMapper.class)
public interface EmployeeMapper {

  @Mapping(source = "employeeDbEntity", target = "name")
  @Mapping(source = "employeeDbEntity.departmentCode", target = "department")
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);

  default String toNickname(EmployeeDbEntity employeeDbEntity) {
    return String.format("%s %s", employeeDbEntity.getFirstName(), employeeDbEntity.getLastName());
  }
}
```

where the new `SampleEnumMapper` looks like:
```java
@Mapper
public interface SampleEnumMapper {
  default Department toDepartment(int departmentCode) {
    return Department.fromDepartmentCode(departmentCode);
  }
}
```

I'm sure there's a way to do this without creating another mapper class. Anyway, this gives us 
```java
public class EmployeeMapperImpl implements EmployeeMapper {

    private final SampleEnumMapper sampleEnumMapper = Mappers.getMapper( SampleEnumMapper.class );

    @Override
    public EmployeeModel toModel(EmployeeDbEntity employeeDbEntity) {
        if ( employeeDbEntity == null ) {
            return null;
        }

        EmployeeModel employeeModel = new EmployeeModel();

        employeeModel.setName( toNickname( employeeDbEntity ) );
        employeeModel.setDepartment( sampleEnumMapper.toDepartment( employeeDbEntity.getDepartmentCode() ) );
        employeeModel.setAddress( employeeDbEntity.getAddress() );

        return employeeModel;
    }
}
```
Our `@Mapping` instruction let MapStruct know which source property (an int) to match to the target
property (`Department`). There was only one such method, so it was selected.

To see what happens when there's ambiguity, add another method to `SampleEnumMapper`:
```java
  default Department anotherOne(int anInt) {
    return Department.SALES;
  }
```

This gives us a helpful, if verbose error message and breaks the build. I've stripped the packages
and full paths for clarity:
```
> Task :compileJava 
examples/EmployeeMapper.java:11: error: Ambiguous mapping methods found for mapping property 
"int departmentCode" to Department: 
  Department SampleEnumMapper.toDepartment(int departmentCode),
  Department SampleEnumMapper.anotherOne(int anInt).
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);
```


Finally, for the salary mapping, we need to do real math. The database stores an hourly rate as a
double (never do this), and the API exposes an annual salary. We assume 2000.0 hours in a work year.

We can do this via a simple method `toAnnualSalary()`
```java
@Mapper(uses = SampleEnumMapper.class)
public interface EmployeeMapper {

  double HOURS_IN_YEAR = 2000.0;

  @Mapping(source = "employeeDbEntity", target = "name")
  @Mapping(source = "employeeDbEntity.departmentCode", target = "department")
  @Mapping(source = "employeeDbEntity.hourlyRate", target = "salary")
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);

  default String toNickname(EmployeeDbEntity employeeDbEntity) {
    return String.format("%s %s", employeeDbEntity.getFirstName(), employeeDbEntity.getLastName());
  }

  default double toAnnalSalary(double hourlyRate) {
    return hourlyRate * HOURS_IN_YEAR;
  }
}
```

This compiles with no warnings, and our mapper implementation is now:
```java
public class EmployeeMapperImpl implements EmployeeMapper {

    private final SampleEnumMapper sampleEnumMapper = Mappers.getMapper( SampleEnumMapper.class );

    @Override
    public EmployeeModel toModel(EmployeeDbEntity employeeDbEntity) {
        if ( employeeDbEntity == null ) {
            return null;
        }

        EmployeeModel employeeModel = new EmployeeModel();

        employeeModel.setName( toNickname( employeeDbEntity ) );
        employeeModel.setDepartment( sampleEnumMapper.toDepartment( employeeDbEntity.getDepartmentCode() ) );
        employeeModel.setSalary( toAnnalSalary( employeeDbEntity.getHourlyRate() ) );
        employeeModel.setAddress( employeeDbEntity.getAddress() );

        return employeeModel;
    }
}
```

### Testing
It's pretty easy to add a test class for the mapper. Here's a basic test case for the happy path.
If your input class has any nullable parameters or fields on those parameters, or there are any other
edge cases, you'd want to test those as well.
```java
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.utils.mappers.examples.Department;
import org.pmiops.workbench.utils.mappers.examples.EmployeeDbEntity;
import org.pmiops.workbench.utils.mappers.examples.EmployeeMapper;
import org.pmiops.workbench.utils.mappers.examples.EmployeeMapperImpl;
import org.pmiops.workbench.utils.mappers.examples.EmployeeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class EmployeeMapperTest {

  public static final String ADDRESS = "123 Sesame Street";
  @Autowired private EmployeeMapper employeeMapper;

  @TestConfiguration
  @Import({EmployeeMapperImpl.class})
  static class Configuration {}

  @Test
  public void testToModel() {
    final EmployeeDbEntity employeeDbEntity = new EmployeeDbEntity("John", "Doe", 2, 3.00, ADDRESS);

    final EmployeeModel employeeModel = employeeMapper.toModel(employeeDbEntity);
    assertThat(employeeModel.getDepartment()).isEqualTo(Department.IT);
    assertThat(employeeModel.getSalary()).isWithin(1.0e-6).of(6000.0);
    assertThat(employeeModel.getName()).isEqualTo("John Doe");
    assertThat(employeeModel.getAddress()).isEqualTo(ADDRESS);
  }
}
```
## Tips & Strategies

### Stub it so it builds, and go one property at a time
For big target classes, put in some temporary `ignore` statements or other hacks just to get it
to the point that it generates the implemmentation class and actually compiles without errors. Then
proceed one property at a time until all the warnings are gone.
 
### Proofread the generated code
First, you should *always* look at the generated code and step through it in the debugger. This is
because, while the codegen should be deterministic, most of us don't go around with its order of
operator precedence in our head. Particularly if you have many input parameters with multiple fields
with common names, you may find that `foo.id` is getting used instead of `bar.id` becase one's a 
`long` and the other an `int`. Or something. 

In general, the library gives very reasonable error
messages and is very cranky at compile time. This is A Good Thing.

## Write tests for mappers
It's pretty tedious to write tests for generated code, as the computer is coding faster than you.
I still think it's worth it, because,
- it documents usage of your generated methods
- you catch things the compiler can't (such as when a string format changes)
- it's a good opportunity to update old test code
- It makes it look less magical

### Consider other use cases
It's possible to write a mapper from a type to itself and use that as a copy constructor of sorts.

### Tune the classes' public APIs
If you find yourself writing arcane helper methods, ask yourself if adding a simple setter or
getter on one of your classes would make the job easier and the generated code cleaner. Since our
model classes are generated, we frequently have to have external helper methods defined in either
the mapper class or a utility class.

## Handling missing target properties
### Avoid `ignore = true` unless absolutely necessary
It's annoying to see warnings about missing target properties, and for good reason. The quick way to
stifle these is to use the `ignore` parameter, which tells MapStruct to elide the setter code for
that target property.
```java
@Mapping(target = "billingAccountName", ignore = true)
```

While there are some places this is necessary, it should really be a last resort, for a couple of
reasons.

First, we plan to use mappers to replace existing chunks of code in a clean way. We don't want to
have to docment that mapping function X leaves the created object in state Y with respect to fields
P, Q, and R. Many of our objects are relatively complex, and it's not obvious when looking at a call
to `fooMapper.toModel()` that the output model is incomplete in some way(s).

Second, if a target property is difficuult to satisfy, this is a signal that either our class design
could be better, or we've left out a prereqisite.

One strategy for avoiding unmatched target property is to add parameters to the method specifying
them. Then simply specify defaults for those at the call site as appropriate.

If the type of the target property has a reasonable defalt (such as zero), and `null` isn't a valid
state for it, then using `NullValuePropertyMappingStrategy.SET_TO_DEFAULT` is an option.

## Handling Default Values
### Handling database defalts
If the target class is a Hibernate model, and one or more fields have defaults
defined in the database schema, we do not want to overwrite that field with `null`
or any defauld value listed in the mapper's annotations. To get around this, we need
to set the `nullValuePropertyMappingStrategy` to `NullValuePropertyMappingStrategy.IGNORE`.
This prevents the MapStruct implementation from writing to the target property if the source is null.
```java
  @Mapping(
      source = "workspace.billingStatus",
      target = "billingStatus",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
```
Note that in this case I'm adding an annotation for a field that MapStruct was previously happy
to map implicitly to override its defalt behavior.

### Specifying a non-default default
It's possible to set a default value in the `@Mapping` by
using `defaultValue`. This is rarely a good idea, as
either the constructor of the target class should be
doing this, or the application should specify it. It's mainly
useful for dealing with awkward legacy structure that you can't change.

One place where this seems to be the least bad option is with
the read-only `etag` column in the `workspace` table:
```java
// Use default CDR version if null. There are really no other good options (i.e. a zero version is just wrong).
@Mapping(source = "workspace.etag", target = "version", qualifiedByName = "etagToCdrVersion", defaultValue = "1")
```
## Semi-advanced topics

### Mapping to a subset of target properties
If you need to map from an object with sub-objects to a target type that's flat, there are two
stratgegies. The first is to use dot indexing in the `@Mapping` annotation, for example
`@Mapping(source = "employee.contact.address", target = "address")`. This works well, but the
generated code will look sloppy if you have to do this many times (e.g. for ResearchPurpose).
If you want to have the address member on the source accessed only once in the mapping (e.g. if it
involves some expensive calculation or network call), then it's better to have a separate method
using `@MappingTarget` on the target type. Something like the following may work
```java
default void setAddress(@MappingTarget DbEmployee dbEmployee, EmployeeModel employee) {
  dbEmployee.setAddress(employee.address);
}
```

You could also elect to have a setter for this object on the target class itself, but then you may
lose some of the benefits of MapStruct's compile-time checking.

Finally, note that this hierarchical mismatch may be a design smell.

### Pulling in other mappers
The `uses` directive instructs MapStruct to look for candidates in external classes. We have one
class just for this purpose called `CommonMappers`. Additionally, classes exposing public static
methods, such as `DbStorageEnums` can be used to good effect.

### Using the `@AfterMapping` annotation
Sometimes it's desirable to perform some fixup or post-mapping operations. While this should not be
common, it's a handy tool to have around. In the case of target types that are generated from
`swagger-codegen`, often the default constructor is lacking. For example it does not initialize
array members to empty arrays but leaves them as null. We can fix this with an appropriate
after mapping method that initializes those fields if they're not already.

### Using DAOs
If you specify `componentModel = "spring"` in the top-level `@Mapper` annotation, you can inject
Spring beans and services. If you go this route, be sure to document and test all side effects. I'm
not sure what the convention should be on whether a target Hibernate entity should be saved inside
the mapping method or not.

## False positive matches
Although the rules used in the codegen are reliable, they may not be intuitive when you're dealing
with large classes with many similar fields of the same types. In particular for this project,
be very careful with various combinations of `Workspace`, `DbWorkspace`, and `FirecloudWorkspace`. 
- Timestamp vs SqlTimestamp. SqlTimestamp is slightly special, and many of the generated time conversions are not _quite_ what we
want. For example, we spun our own Sql Timestamps to String mapper
- Application-specific order dependence
