package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

  List<Criteria> findCriteriaByTypeAndParentIdOrderByIdAsc(@Param("type") String type, @Param("parentId") Long parentId);

  List<Criteria> findCriteriaByType(@Param("type") String type);

  @Query(value = "select * from criteria where id in ( " +
    "select id from " +
    "(select @curRow \\:= case " +
    "when @curType = name " +
    "then @curRow + 1 " +
    "else 1 end as rank, " +
    "id, " +
    "code, " +
    "@curType \\:= name as name, " +
    "concept_id " +
    "from (select * from criteria where type = upper(:type) and " +
    "(upper(code) like upper(concat('%',:value,'%')) or upper(name) like upper(concat('%',:value,'%'))) ) a, " +
    "(select @curRow \\:= 1, @curType \\:= '') r " +
    "order by name, id) as x " +
    "where rank = 1) " +
    "limit 250", nativeQuery = true)
  List<Criteria> findCriteriaByTypeForCodeOrName(@Param("type") String type, @Param("value") String value);

  @Query(value = "select * from ( " +
    "  select * from criteria " +
    "  where id in ( " +
    "    select _id from ( " +
    "      select  @r as _id, " +
    "        (select @r \\:= parent_id " +
    "        from criteria " +
    "        where id = _id) as parent, " +
    "        @l \\:= @l + 1 as lvl from " +
    "    (select  @r \\:= :criteriaId,    @l \\:= 0) vars, " +
    "    criteria h where @r <> 0) outer_id) " +
    "    union " +
    "select * from criteria where parent_id in ( " +
    "    select parent from ( " +
    "      select  @x as _id, " +
    "        (select @x \\:= parent_id " +
    "        from criteria " +
    "        where id = _id) as parent, " +
    "        @y \\:= @y + 1 as lvl from " +
    "          (select  @x \\:= :criteriaId, @y \\:= 0 ) vars, " +
    "          criteria where @x <> 0) outer_parent " +
    "          where parent <> 0) ) final order by id", nativeQuery = true)
  List<Criteria> findCriteriaById(@Param("criteriaId") Long criteriaId);

  @Query(value = "select * from criteria c " +
    "where c.type = :type " +
    "and c.subtype = :subtype " +
    "order by c.id asc", nativeQuery = true)
  List<Criteria> findCriteriaByTypeAndSubtypeOrderByIdAsc(@Param("type") String type, @Param("subtype") String subtype);

  @Query(value = "select * from criteria c " +
    "where c.type = 'DRUG' " +
    "and c.subtype in ('ATC', 'BRAND') " +
    "and c.is_selectable = 1 " +
    "and upper(c.name) like upper(concat('%',:name,'%')) " +
    "order by c.name asc", nativeQuery = true)
  List<Criteria> findDrugBrandOrIngredientByName(@Param("name") String name);

  @Query(value = "select * from criteria c " +
    "inner join ( " +
    "select cr.concept_id_2 from concept_relationship cr " +
    "join concept c1 on (cr.concept_id_2 = c1.concept_id " +
    "and cr.concept_id_1 = :conceptId " +
    "and c1.concept_class_id = 'Ingredient') ) cr1 on c.concept_id = cr1.concept_id_2", nativeQuery = true)
  List<Criteria> findDrugIngredientByConceptId(@Param("conceptId") Long conceptId);

  @Query(value = "select distinct c.domain_id as domainId from criteria c " +
    "where c.parent_id in (" +
    "select id from criteria " +
    "where type = :type " +
    "and code like :code% " +
    "and is_selectable = 1 " +
    "and is_group = 1) " +
    "and c.is_group = 0 and c.is_selectable = 1", nativeQuery = true)
  List<String> findCriteriaByTypeAndCode(@Param("type") String type, @Param("code") String code);

  @Query(value = "select distinct c.domain_id as domainId from criteria c " +
    "where c.parent_id in (" +
    "select id from criteria " +
    "where type = :type " +
    "and subtype = :subtype " +
    "and code like :code% " +
    "and is_selectable = 1 " +
    "and is_group = 1) " +
    "and c.is_group = 0 and c.is_selectable = 1", nativeQuery = true)
  List<String> findCriteriaByTypeAndSubtypeAndCode(@Param("type") String type, @Param("subtype") String subtype, @Param("code") String code);

  @Query(value = "select * from criteria c " +
    "where c.type = :type " +
    "and (match(c.name) against(:value in boolean mode) or match(c.code) against(:value in boolean mode)) " +
    "and c.is_selectable = 1 " +
    "order by c.code asc", nativeQuery = true)
  List<Criteria> findCriteriaByTypeAndNameOrCode(@Param("type") String type, @Param("value") String value);

}
