package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.model.DbDomainCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DomainCardDaoTest extends SpringTest {

  @Autowired private DomainCardDao domainCardDao;
  private DbDomainCard domainCardObservation;
  private DbDomainCard domainCardCondition;
  private DbDomainCard domainCardPM;

  @BeforeEach
  public void setUp() {
    domainCardObservation =
        domainCardDao.save(
            new DbDomainCard()
                .id(1L)
                .category("Standard")
                .domain((short) 5)
                .name("Observations")
                .description("descr")
                .conceptCount(0)
                .participantCount(0)
                .standard(false)
                .sortOrder(1));
    domainCardCondition =
        domainCardDao.save(
            new DbDomainCard()
                .id(9L)
                .domain((short) 0)
                .name("Conditions")
                .description("descr")
                .conceptCount(0)
                .participantCount(0)
                .standard(false)
                .sortOrder(2));
    domainCardPM =
        domainCardDao.save(
            new DbDomainCard()
                .id(2L)
                .domain((short) 10)
                .name("Physical Measurements")
                .description("descr")
                .conceptCount(33)
                .participantCount(453542)
                .standard(false)
                .sortOrder(3));
  }

  @Test
  public void findByOrderById() {
    List<DbDomainCard> infos = domainCardDao.findByOrderById();
    assertThat(infos.size()).isEqualTo(3);
    assertThat(domainCardObservation).isEqualTo(infos.get(0));
    assertThat(domainCardPM).isEqualTo(infos.get(1));
    assertThat(domainCardCondition).isEqualTo(infos.get(2));
  }
}
