package org.pmiops.workbench.testconfig.fixtures;

public interface ReportingTestFixture<ENTITY_T, PROJECTION_T, DTO_T> {

  /**
   * Creates a Mockito mock instance of the projection class,
   * which has no constructor of its own and can't be created
   * in the usual way.
   */
  PROJECTION_T mockProjection();

  /**
   * Construct a Hibernate entity object (e.g. DbUser) for this
   * test fixture.
   * @return
   */
  ENTITY_T createEntity();

  /**
   * Construct a data transfer object and set all its fields
   * to known, distinct constant values.
   * @return
   */
  DTO_T createDto();

  /**
   * Assert that all fields in this DTO match the
   * expected (constant) values.
   */
  void assertDTOFieldsMatchConstants(DTO_T dto);

  /**
   * Assert that all fields in this DTO match the
   * expected (constant) values.
   */
  void assertProjectionFieldsMatchConstants(PROJECTION_T projection);
}
