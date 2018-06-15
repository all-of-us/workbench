package org.pmiops.workbench.cdr.dao;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * A DAO used to get results using Specification without issuing a count query.
 */
public class NoCountFindAllDao<T, I extends Serializable> extends SimpleJpaRepository<T, I> {

  public NoCountFindAllDao(Class<T> domainClass, EntityManager em) {
    super(domainClass, em);
  }

  @Override
  protected <S extends T> Page<S> readPage(TypedQuery<S> query, final Class<S> domainClass, Pageable pageable,
      final Specification<S> spec) {
    query.setFirstResult(pageable.getOffset());
    query.setMaxResults(pageable.getPageSize());
    List<S> content = query.getResultList();
    return new PageImpl<S>(content, pageable, content.size());
  }
}
