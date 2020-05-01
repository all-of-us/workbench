# MapStruct Best Practices
## What is MapStruct?
[MapStruct](https://mapstruct.org/documentation/stable/reference/html/) is a Java code generator specializing in writing
conversion or mapping functions between an object instance and an instance of
a corresponding object in a class designed for another architectural tier, protocol, or technology.
It works mainly by matching types and names via reflection, or using programmer-provided
hints where that strategy isn't effective.
### Examples
Consider an `EmployeeModel` class:
```java
class EmployeeModel
```
## Is this a gimmick? Why do I need it?
## Strategies
### Handling missing target properties
### Avoid `ignore = true` unless absolutely necessary
### Setting defaults
## Gotchas
