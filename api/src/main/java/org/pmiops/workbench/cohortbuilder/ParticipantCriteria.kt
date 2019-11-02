package org.pmiops.workbench.cohortbuilder

import com.google.common.collect.ImmutableSet
import java.util.Objects
import org.pmiops.workbench.model.SearchRequest

/**
 * A class representing participants to use when querying data from a CDR version.
 *
 *
 * Either a cohort definition (with optional blacklist of participants to exclude), or a list of
 * participant IDs to include.
 *
 *
 * (We could have instead decided to have an interface capable of producing SQL, with different
 * implementations for request + blacklist and whitelist; but this would spread out SQL generation
 * and break the separation between query model and SQL generation code. So instead, this class
 * represents criteria that can be transformed into SQL for any way of selecting participants from
 * BigQuery.)
 */
class ParticipantCriteria {

    val searchRequest: SearchRequest?
    val participantIdsToInclude: Set<Long>?
    val participantIdsToExclude: Set<Long>?

    @JvmOverloads
    constructor(searchRequest: SearchRequest, participantIdsToExclude: Set<Long> = NO_PARTICIPANTS_TO_EXCLUDE) {
        this.searchRequest = searchRequest
        this.participantIdsToExclude = participantIdsToExclude
        this.participantIdsToInclude = null
    }

    constructor(participantIdsToInclude: Set<Long>) {
        this.participantIdsToInclude = participantIdsToInclude
        this.searchRequest = null
        this.participantIdsToExclude = null
    }

    override fun hashCode(): Int {
        return Objects.hash(searchRequest, participantIdsToExclude, participantIdsToExclude)
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is ParticipantCriteria) {
            return false
        }
        val that = obj as ParticipantCriteria?
        return (this.searchRequest == that!!.searchRequest
                && this.participantIdsToExclude == that.participantIdsToExclude
                && this.participantIdsToInclude == that.participantIdsToInclude)
    }

    companion object {

        private val NO_PARTICIPANTS_TO_EXCLUDE = ImmutableSet.of<Long>()
    }
}
