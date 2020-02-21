package org.pmiops.workbench.actionaudit

import org.pmiops.workbench.db.model.DbUser

data class Agent constructor(
    val agentType: AgentType,
    val idMaybe: Long? = null,
    val emailMaybe: String? = null
) {

    companion object {
        @JvmStatic
        fun asUser(u: DbUser): Agent {
            return Agent(
                    agentType = AgentType.USER,
                    idMaybe = u.userId,
                    emailMaybe = u.username)
        }

        @JvmStatic
        fun asAdmin(u: DbUser): Agent {
            return Agent(
                    agentType = AgentType.ADMINISTRATOR,
                    idMaybe = u.userId,
                    emailMaybe = u.username)
        }

        @JvmStatic
        fun asSystem(): Agent {
            return Agent(agentType = AgentType.SYSTEM)
        }
    }
}