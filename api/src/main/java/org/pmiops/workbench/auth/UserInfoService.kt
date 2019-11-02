package org.pmiops.workbench.auth

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Userinfoplus
import org.pmiops.workbench.google.GoogleRetryHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserInfoService @Autowired
internal constructor(
        private val httpTransport: HttpTransport, private val jsonFactory: JsonFactory, private val retryHandler: GoogleRetryHandler) {

    fun getUserInfo(token: String): Userinfoplus {
        val credential = GoogleCredential().setAccessToken(token)
        val oauth2 = Oauth2.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
        return retryHandler.run { context -> oauth2.userinfo().get().execute() }
    }

    companion object {

        private val APPLICATION_NAME = "AllOfUs Workbench"
    }
}
