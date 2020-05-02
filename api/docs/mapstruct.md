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
the enum.

## Tips & Strategies
### Proofread the generated code
First, you should *always* look at the generated code and step through it in the debugger. This is
because, while the codegen should be deterministic, most of us don't go around with its order of
operator precedence in our head. Particularly if you have many input parameters with multiple fields
with common names, you may find that `foo.id` is getting used instead of `bar.id` becase one's a 
`long` and the other an `int`. Or something. 

In general, the library gives very reasonable error
messages and is very cranky at compile time. This is A Good Thing.

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

## Pulling in other mappers
The `uses` directive instructs MapStruct to look for candidates in external classes. We have one
class just for this purpose called `CommonMappers`. Additionally, classes exposing public static
methods, such as `DbStorageEnums` can be used to good effect.

## Gotchas
- Default values defined by database
- False positive matches
- Timestamp vs SqlTimestamp
SqlTimestamp is slightly special, and many of the generated time conversions are not _quite_ what we
want. For example, we spun our own Sql Timestamps to String mapper
- Application-specific order dependence
