package org.pmiops.workbench.utils.codegenhelpers;

/**
 * Interface defining a wrapper class for a safer constructor for a Swagger-generated
 * (or otherwise dubiously initialized) class. In particular, it doeasn't look like Swagger
 * Codegen has a way to specify the default initial value for a collection.
 *
 * This class is especially useful for setting null lists or required objects
 * to new ArrayList instances, nullable integers to zero, etc, as appropriate.
 * @param <T> Generated class to be helped
 */
public interface GeneratedClassHelper<T> {

  // Return a new T instance with custom initialization rules applied. This
  // implmenentation should nearly always be
  // @code { return initialize(new Foo()); // T is Foo }
  // but we can't quite define such a default implementation due to type erasure.
  T create();

  // Take in an instance and apply rules as desired. This should be written so
  // that it only changes values that are null (or some other useless default)
  // to begin with.
  T sanitize(T defaultConstructedInstance);
}
