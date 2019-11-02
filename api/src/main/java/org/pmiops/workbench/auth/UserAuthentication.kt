package org.pmiops.workbench.auth

import com.google.api.services.oauth2.model.Userinfoplus
import com.google.common.collect.ImmutableList
import org.pmiops.workbench.db.model.User
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class UserAuthentication(
        val user: User, private val userInfo: Userinfoplus, private val bearerToken: String, val userType: UserType) : Authentication {

    enum class UserType {
        // A researcher or their pet service account
        RESEARCHER,
        // A GCP service account (not affiliated with a researcher)
        SERVICE_ACCOUNT
    }

    override fun getName(): String {
        return userInfo.email
    }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return ImmutableList.of()
    }

    override fun getCredentials(): String {
        return bearerToken
    }

    override fun getDetails(): Any? {
        return null
    }

    override fun getPrincipal(): Userinfoplus {
        return userInfo
    }

    override fun isAuthenticated(): Boolean {
        return true
    }

    @Throws(IllegalArgumentException::class)
    override fun setAuthenticated(isAuthenticated: Boolean) {
    }
}
