package org.pmiops.workbench.google

import com.google.api.services.directory.model.User

/** Encapsulate Googe APIs for handling GSuite user accounts.  */
interface DirectoryService {
    fun isUsernameTaken(username: String): Boolean

    fun getUser(email: String): User

    fun createUser(givenName: String, familyName: String, username: String, contactEmail: String): User

    fun resetUserPassword(userName: String): User

    fun deleteUser(username: String)
}
