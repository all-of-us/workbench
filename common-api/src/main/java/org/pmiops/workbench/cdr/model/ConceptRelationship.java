package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EmbeddedId;
import javax.persistence.Table;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;

import org.pmiops.workbench.cdr.model.ConceptRelationshipId;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "concept_relationship")
public class ConceptRelationship {

    private ConceptRelationshipId conceptRelationshipId;

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name="conceptId1",
                    column=@Column(name="concept_id_1")),
            @AttributeOverride(name="conceptId2",
                    column=@Column(name="concept_id_2")),
            @AttributeOverride(name="relationshipId",
                    column=@Column(name="relationship_id"))
    })
    public ConceptRelationshipId getConceptRelationshipId() {
        return conceptRelationshipId;
    }

    public void setConceptRelationshipId(ConceptRelationshipId conceptRelationshipId) {
        this.conceptRelationshipId = conceptRelationshipId;
    }

    public ConceptRelationship conceptRelationshipId(ConceptRelationshipId conceptRelationshipId) {
        this.conceptRelationshipId = conceptRelationshipId;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("conceptRelationshipId", conceptRelationshipId)
                .toString();
    }
}
