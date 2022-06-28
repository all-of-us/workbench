# MapStruct Best Practices
## What is MapStruct?
[MapStruct](https://mapstruct.org/documentation/stable/reference/html/) is a Java code generator specializing in writing
conversion or mapping functions between an object instance and an instance of
a corresponding object in a class designed for another architectural tier, protocol, or technology.
It works mainly by matching types and names via reflection, or using programmer-provided
hints where that strategy isn't effective.

If you haven't worked with MapStruct, please see the [tutorial](./tutorials/mapstruct-tutorial.md).
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
a new target field that doesn't have a source, you'll get a compile-time warning reminding you. (It's
important to stay on top of those). 

## Tips & Strategies

### Stub it so it builds, and go one property at a time
For big target classes, put in some temporary `ignore` statements or other hacks just to get it
to the point that it generates the implementation class and actually compiles without errors. Then
proceed one property at a time until all the warnings are gone.
 
### Proofread the generated code
First, you should *always* look at the generated code and step through it in the debugger. This is
because, while the codegen should be deterministic, most of us don't go around with its order of
operator precedence in our head. Particularly if you have many input parameters with multiple fields
with common names, you may find that `foo.id` is getting used instead of `bar.id` because one's a 
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
Additionally, these methods may perform validation or fixup on values (for example to pick a default
based on some business logic MapStruct wouldn't know how to guess).

### Tune the classes' public APIs
If you find yourself writing arcane helper methods, ask yourself if adding a simple setter or
getter on one of your classes would make the job easier and the generated code cleaner. Since our
model classes are generated, we frequently have to have external helper methods defined in either
the mapper class or a utility class.

## Handling missing target properties
### Using `ignore = true` for target properties
#### Caveats
There are times when properties on the target type can't be determined by the source type(s). By
defaultThe, MapStruct emits a warning in this sitation. The quickest way to
stifle these is to use the `ignore` parameter, which tells MapStruct to elide the setter code for
that target property.
```java
@Mapping(target = "billingAccountName", ignore = true)
```

While there are some places this is desirable behavior (see below), this should be carefully
considered, for a couple of reasons.

First, we plan to use mappers to replace existing chunks of code in a clean way. We don't want to
have to document that mapping function X leaves the created object in state Y with respect to fields
P, Q, and R. Many of our objects are relatively complex, and it's not obvious when looking at a call
to `fooMapper.toModel()` that the output model is incomplete in some way(s).

Second, if a target property is difficult to satisfy, this is a signal that either our class design
could be better, or we've left out a prerequisite.

One strategy for avoiding unmatched target property is to add parameters to the method specifying
them. Then simply specify defaults for those at the call site as appropriate.

If the type of the target property has a reasonable default (such as zero), and `null` isn't a valid
state for it, then using `NullValuePropertyMappingStrategy.SET_TO_DEFAULT` is an option.

#### Use Cases for `ignore = true`
The following situations are some places where ignoring the target property in a mapping annotation
may be appropriate:
* the property will be mapped in an `@AfterMapping`-annotated method, and included in the generated
code
* the property is not intended to be written by any outside callers (for example, an automatically
incremented database column property such as a primary key or `@Transient` entity property)
* read-only fields on an API model class, assuming the target of the mapper method represents
a model input to a CREATE method. For mapping to the response's version of a created object, these
fields should be populated (and these are not ignored). So if an `OctopusModel` passed into a 
`createOctopus` API only has one leg as `required` and seven legs `readOnly`, we may need separate
methods `OctoppusModel toCreateOctopusRequestModel(Tentalcle t);` and a
`OctoppusModel toHydratedOctopusRequest(Tentalcle t1, Tentacle T2, ..., Tentacle T8);`
* The target type's default constructor initializes the property to non-trivial value. (For trivial
defaults, the `NullValuePropertyMappingStrategy.DEFAULT` annotation may be used).
## Handling Default Values

### Handling database defaults
If the target class is a Hibernate model, and one or more fields have defaults
defined in the database schema, we do not want to overwrite that field with `null`
or any default value listed in the mapper's annotations. To get around this, we need
to set the `nullValuePropertyMappingStrategy` to `NullValuePropertyMappingStrategy.IGNORE`.
This prevents the MapStruct implementation from writing to the target property if the source is null.
```java
  @Mapping(
      source = "workspace.billingStatus",
      target = "billingStatus",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
```
Note that in this case I'm adding an annotation for a field that MapStruct was previously happy
to map implicitly to override its default behavior.

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
strategies. The first is to use dot indexing in the `@Mapping` annotation, for example
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
`swagger-codegen`, often the default constructor is lacking. For example, it does not initialize
array members to empty arrays but leaves them as null. We can fix this with an appropriate
after mapping method that initializes those fields if they're not already.

## Unexpected behavior
Although the rules used in the codegen are reliable, they may not be intuitive when you're dealing
with large classes with many similar fields of the same types. In particular for this project,
be very careful with various combinations of `Workspace`, `DbWorkspace`, and `FirecloudWorkspace`. 
- *Timestamp vs SqlTimestamp*. SqlTimestamp is slightly special, and many of the generated time
conversions are not _quite_ what we
want. For example, we spun our own Sql Timestamps to String mapper
- *Semantic Coupling* If the designer of a class somehow tacitly assumes that property A will be set
before property B is set, and there are side effect(s) to setting these properties (for example
with an observer or ORM magic), then MapStruct will likely violate that expected order. The solution
is to carefully document and test this kind of class, or to eliminate this kind of coupling altogether.
