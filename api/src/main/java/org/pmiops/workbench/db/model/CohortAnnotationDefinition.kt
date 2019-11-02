package org.pmiops.workbench.db.model

import java.util.Objects
import java.util.SortedSet
import java.util.TreeSet
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.Version
import org.apache.commons.lang3.builder.ToStringBuilder
import org.pmiops.workbench.model.AnnotationType

@Entity
@Table(name = "cohort_annotation_definition")
class CohortAnnotationDefinition {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "cohort_annotation_definition_id")
    var cohortAnnotationDefinitionId: Long = 0
    @get:Version
    @get:Column(name = "version")
    var version: Int = 0
    @get:Column(name = "cohort_id")
    var cohortId: Long = 0
    @get:Column(name = "column_name")
    var columnName: String? = null
    @get:Column(name = "annotation_type")
    var annotationType: Short? = null
    @get:OneToMany(fetch = FetchType.EAGER, mappedBy = "cohortAnnotationDefinition", orphanRemoval = true, cascade = [CascadeType.ALL])
    @get:OrderBy("cohortAnnotationEnumValueId ASC")
    var enumValues: SortedSet<CohortAnnotationEnumValue> = TreeSet()

    var annotationTypeEnum: AnnotationType
        @Transient
        get() = StorageEnums.annotationTypeFromStorage(annotationType)
        set(annotationType) {
            annotationType = StorageEnums.annotationTypeToStorage(annotationType)
        }

    fun cohortAnnotationDefinitionId(
            cohortAnnotationDefinitionId: Long): CohortAnnotationDefinition {
        this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId
        return this
    }

    fun version(version: Int): CohortAnnotationDefinition {
        this.version = version
        return this
    }

    fun cohortId(cohortId: Long): CohortAnnotationDefinition {
        this.cohortId = cohortId
        return this
    }

    fun columnName(columnName: String): CohortAnnotationDefinition {
        this.columnName = columnName
        return this
    }

    fun annotationType(annotationType: Short?): CohortAnnotationDefinition {
        this.annotationType = annotationType
        return this
    }

    fun annotationTypeEnum(annotationType: AnnotationType): CohortAnnotationDefinition {
        return this.annotationType(StorageEnums.annotationTypeToStorage(annotationType))
    }

    fun enumValues(enumValues: SortedSet<CohortAnnotationEnumValue>): CohortAnnotationDefinition {
        this.enumValues = enumValues
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as CohortAnnotationDefinition?
        return (version == that!!.version
                && cohortId == that.cohortId
                && columnName == that.columnName
                && annotationType === that.annotationType)
    }

    override fun hashCode(): Int {
        return Objects.hash(version, cohortId, columnName, annotationType)
    }

    override fun toString(): String {
        return ToStringBuilder(this)
                .append("cohortAnnotationDefinitionId", cohortAnnotationDefinitionId)
                .append("version", version)
                .append("cohortId", cohortId)
                .append("columnName", columnName)
                .append("annotationType", annotationType)
                .toString()
    }
}
