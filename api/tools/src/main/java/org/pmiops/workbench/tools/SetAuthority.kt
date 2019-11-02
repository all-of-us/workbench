package org.pmiops.workbench.tools

import java.util.Arrays
import java.util.HashSet
import java.util.logging.Level
import java.util.logging.Logger
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.model.Authority
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * See api/project.rb set-authority. Adds or removes authorities (permissions) from users in the db.
 */
@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan("org.pmiops.workbench.db.model")
class SetAuthority {

    private fun commaDelimitedStringToSet(str: String): Set<String> {
        return HashSet(Arrays.asList(*str.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
    }

    private fun commaDelimitedStringToAuthoritySet(str: String): Set<Authority> {
        val auths = HashSet()
        for (value in commaDelimitedStringToSet(str)) {
            val cleanedValue = value.trim { it <= ' ' }.toUpperCase()
            if (cleanedValue.isEmpty()) {
                continue
            }
            auths.add(Authority.valueOf(cleanedValue))
        }
        return auths
    }

    @Bean
    fun run(userDao: UserDao): CommandLineRunner {
        return { args ->
            // User-friendly command-line parsing is done in devstart.rb, so we do only simple positional
            // argument parsing here.
            require(args.size == 4) { "Expected 4 args (email_list, authorities, remove, dry_run). Got " + Arrays.asList<String>(*args) }
            val emails = commaDelimitedStringToSet(args[0])
            val authorities = commaDelimitedStringToAuthoritySet(args[1])
            val remove = java.lang.Boolean.valueOf(args[2])
            val dryRun = java.lang.Boolean.valueOf(args[3])
            var numUsers = 0
            var numErrors = 0
            var numChanged = 0

            for (email in emails) {
                numUsers++
                var user: User? = userDao.findUserByEmail(email)
                if (user == null) {
                    log.log(Level.SEVERE, "No user for {0}.", email)
                    numErrors++
                    continue
                }
                // JOIN authorities, not usually fetched.
                user = userDao.findUserWithAuthorities(user.userId)

                val granted = user!!.authoritiesEnum
                val updated = HashSet(granted!!)
                if (remove) {
                    updated.removeAll(authorities)
                } else {
                    updated.addAll(authorities)
                }
                if (updated != granted) {
                    if (!dryRun) {
                        user.authoritiesEnum = updated
                        userDao.save(user)
                    }
                    numChanged++
                    log.log(Level.INFO, "{0} {1} => {2}.", arrayOf(email, granted, updated))
                } else {
                    log.log(Level.INFO, "{0} unchanged.", email)
                }
            }

            log.log(
                    Level.INFO,
                    "{0}. {1} of {2} users changed, {3} errors.",
                    arrayOf(if (dryRun) "Dry run done" else "Done", numChanged, numUsers, numErrors))
        }
    }

    companion object {

        private val log = Logger.getLogger(SetAuthority::class.java.name)

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(SetAuthority::class.java).web(false).run(*args)
        }
    }
}
