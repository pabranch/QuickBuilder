[![Build Status](https://travis-ci.org/hcoles/QuickBuilder.svg?branch=master)](https://travis-ci.org/hcoles/QuickBuilder)

# QuickBuilder

Builders without boiler plate. 

## Rationale

The builder pattern helps keep tests readable and maintainable but requires tedious boilerplate. QuickBuilder lets you spend more time on your real code, and less writing boilerplate, by generating fully featured builders on the fly. 

You supply an interface, QuickBuilder supplies an implementation.

QuickBuilder can create builders for pretty much any class - it cleanly handles immutable types and other classes not following the bean convention without resorting to dirty tricks like setting fields via reflection. Only the public interface of your class is used.

QuickBuilder is **not** a code generator. Builders are generated at runtime, there's no autogenerated code for you to maintain.

## Download

It's on maven central

http://search.maven.org/#search|ga|1|a%3A%22quickbuilder%22

```xml
<dependency>
    <groupId>org.pitest.quickbuilder</groupId>
    <artifactId>quickbuilder</artifactId>
    <version>0.0.2</version>
</dependency>
```

## 60 second quickstart

For any bean e.g.

```java
public class Person {
    private String name;
    private int age;
    private Person partner;

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setPartner(Person partner) {
        this.partner = partner;
    }

    // etc
}
```

Create a builder by declaring an interface

```java
interface PersonBuilder extends Builder<Person> {
  PersonBuilder withName(String name);
  PersonBuilder withAge(int age);
  PersonBuilder withPartner(PersonBuilder partner);
}

```

Get an instance by calling QB.builder, then use it like any other builder

```java
    Person p = QB.builder(PersonBuilder.class).withName("Bob").withAge(42).build();
```

For classes without a public no-args constructor you can supply a "seed" function that creates the instance.

```java
 class Seed implements Generator<PersonBuilder,Person> {
    @Override
    public Person generate(PersonBuilder builder) {
      return Person.create();
    }
 }

 // pass an instance of the seed function to QuickBuilder
 PersonBuilder pb = QB.builder(PersonBuilder.class, new Seed());
```

For classes that don't provide setter methods (e.g immutable classes) you can tell QuickBuilder that you will take responsibility for a property by declaring an underscore method in your builder.

```java
interface PersonBuilder extends Builder<Person> {
  PersonBuilder withName(String name);
  PersonBuilder withAge(int age);
  PersonBuilder withPartner(PersonBuilder partner);

  // underscore method telling QuickBuilder not to handle this property
  String _Name();
}
```

The underscore method can then be used in your seed function to route the value to the right place.

```java
 class Seed implements Generator<Person, PersonBuilder> {
    @Override
    public Person generate(PersonBuilder builder) {
      return Person.create(builder._Name());
    }
 }
```

You can of course choose to supply a seed function even when QuickBuilder does not require it and thereby move some some errors to being compile time errors rather than runtime.

When a seed function is used to set all properties the only types of runtime error that can occur are if :-

* The name of an underscore method does not match with a property declared on the builder
* The type of an underscore method does not match the type of the corresponding property

Note that these are mistakes within the builder interface - these probelms cannot be introduced by making changes to the built class.

## 2 minute overview of features

In addition to being much easier to declare, the builders that QuickBuilder creates are better behaved and more richly featured than the ones you might have been bulding by hand.

### Immutable by default

By default QuickBuilder creates immutable builders - their internal state is never updated, instead they return updated shallow copies of themselves.

Builders may therefore be reused without unexpected side effects

```java

interface PersonBuilder extends Builder<Person> {
  // ... etc
}

PersonBuilder youngPerson = QB.builder(PersonBuilder.class).withAge(18);

Person rita = youngPerson.withName("Rita").withPartner(bob).build();
Person sue = youngPerson.withName("Sue").build();

// Rita and Sue do not share Bob

```

If you prefer working with mutable objects extend the `org.pitest.quickbuilder.MutableBuilder` interface instead of `org.pitest.quickbuilder.Builder`. QuickBuilder will then generate a builder that updates its own state, supplying a `but` method that must be explicitly called when you wish to re-use a builder.

```java
interface PersonBuilder extends MutableBuilder<Person> {
  ... etc
}

PersonBuilder youngPerson = QB.builder(PersonBuilder.class).withAge(18);

// call but to create a copy
Person rita = youngPerson.but().withName("Rita").withPartner(bob).build();
Person sue = youngPerson.but().withName("Sue").build();

// Rita and Sue do not share Bob

```

### Respects default values

If you don't supply a value for a property QuickBuilder will not try to overwrite it.

Given the bean

```java
public class Person {

    private int age;

    public Person() {
      age = 18; // set a default
    }

    // etc
}
```

Common hand rolled builder implementations would overwrite the default age with primitive int default of 0 when no value was supplied, but calling

```java
   Person p = QB.builder(PersonBuilder).withName("Tom").build();
```

Will return a person called Tom with the default age of 18.

### Sequences

Normally a builder returns objects built from the same values each time build is called. Occasionally you may want to create a series of objects that differ from each other on one or more property. Allthough you could create these by calling `with` methods generating objects from a sequence may result in more readable code.

A sequence is a mutable builder that updates it's state each time the build method is called. QuickBuilder currently provides one simple sequence implementation that takes it's values from a list that you supply.

```java
import static org.pitest.quickbuilder.sequence.ElementSequence.from;
import static java.util.Arrays.asList;

PersonBuilder person = QB.builder(PersonBuilder.class)
                            .withName(from(asList("Paul", "Barry", "Sarah")))
                            .withAge(from(asList(20,29,42)))

person.build(); // a person named Paul with age 20
person.build(); // a person named Barry with age 29
person.build(); // a person named Sarah with age 42
```

You may need place the built objects into a Collection. Instead of doing it by hand you can make your builder extend `org.pitest.quickbuilder.SequenceBuilder`, in which case QuickBuilder will supply you with a method `List<T> build(int numberToBuild)`


```java

interface PersonBuilder extends SequenceBuilder<Person> {
  // ... etc
}


PersonBuilder person = QB.builder(PersonBuilder.class)
                            .withName(from(asList("Paul", "Barry", "Sarah")))
                            .withAge(from(asList(20,29,42)))

List<Person> people = person.build(3); // creates list containing Paul, Barry and Sarah
```





## Features

* Automatically creates builders for beans - you just supply an interface
* Creates builders for non-beans (e.g. immutable classes) by defining interface and small seeder function
* Builder methods may take normal types or other builders as parameters
* Supports any lowercase prefix for property methods eg "withName", "andName", "usingName"
* Supports generation of thread safe immutable builders
* Supports generation of traditional mutable builders 

## Design principles

1. Well behaved - only public interface of built type used
2. Non invasive - no changes or annotations needed on built type
3. Fail as early as possible - error on type creation or compile if possible

## TODO

* Separate interface for underscore methods
* Collect list entries individually
* Auto generation for classes with single factory method
* Auto generation for classes with constructor without repeated types

## Alternatives

Other approaches you might want to consider

* https://code.google.com/p/make-it-easy/ 
* http://code.google.com/a/eclipselabs.org/p/bob-the-builder/

