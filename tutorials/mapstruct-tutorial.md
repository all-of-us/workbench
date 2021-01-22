#  MapStruct Tutorial
Here's a walkthrough of a typical MapStruct Mapper creation
exercise, including frequent erros and how to address them.

* This tutorial existed prior to the tutorial format being established *

## Source and Target Classes
Consider an `EmployeeModel` class. It's a Plain Old Java Object with a default constructor
and mutators for all its member variables:
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

Additionally, we have a (nearly) corresponding entity in the database. ORM bindings are omitted for
this efxample.
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

We wish to create a mapper from the entity class to the API model. To begin, we declare an interface
for the mapper and give it the `@Mapper` annotation. A naive first cut at a mapping function might
look like this:
```java
import org.mapstruct.Mapper;

@Mapper
public interface EmployeeMapper {

  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);
}
```

MapStruct will inspect this interface definition and attempt to generate code to satisfy the
methods declared in it. Using `./gradlew compileJava`, I see that this compiles, but gives warnings
```
examples/EmployeeMapper.java:8: warning: Unmapped target properties: "name, department, salary".
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);
                ^
```

So the good news is I've successfully generated a mapper. The bad news is that it was only able to
generate mapping code for one of four target properties.
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
Notice as well that MapStruct's default policty for `null` refernces is to assume any input
parameter or property could be `null` and to happily return `null` for the output. (It may
actually help to annotate the mapper method with `@Nullable` as a reminder.)

To fix the name mapping we need to add a method to build a full name from first & last names. Let's pretend our
system didn't already have a good one, and add it directly in the mapper itself for brevity. To do
this, we need to create a `default` method on the interface, which is a relatively new facility in
Java to make life easier in this kind of situation.

### Default mappers
Our default method is itself another mapper, though you want to be careful when the source type of
such a mapper is a common type like String or Timestamp. In one instance as there can be more than one path to
conversion. For example, the `java.sql.Timestamp` class has a valid `toString()` override, but
MapStruct, with tasked with getting a String from such a `Timestamp`, generated a mapper that
converted to and from a third type, resulting in a slight difference in the `String` format.
```java
  workspaceResource.setModifiedTime(
    xmlGregorianCalendarToString(
      dateToXmlGregorianCalendar(dbCohort.getLastModifiedTime()), null));
```
The upshot was that the automatic mapper formtted a timestamp like `2020-03-30T18:31:50.000Z` and
the preferred behavior, using the SQL Timestamp class is `2020-03-30 18:31:50.0`.

It's possible that MapStruct's format is actually more standard, but it didn't match the one we use, so we had to
define another mapper:
```java
  public String timestampToString(Timestamp timestamp) {
    // We are using this method because mapstruct defaults to gregorian conversion. The difference
    // is:
    // Gregorian: "2020-03-30T18:31:50.000Z"
    // toString: "2020-03-30 18:31:50.0"
    if (timestamp != null) {
      return timestamp.toString();
    }
    return null;
  }
```


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

The easiest solution here is to use the `qualifiedByName` prorperty on the `@Mapping` attribute
to give the name of the preferred mapping method (which must be annotated with `@Named`). See
[this article](https://mapstruct.org/documentation/1.1/api/org/mapstruct/Named.html) for an example
(as well as a couple of spots in our code.)

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

  default double toAnnualSalary(double hourlyRate) {
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
        employeeModel.setSalary( toAnnualSalary( employeeDbEntity.getHourlyRate() ) );
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
