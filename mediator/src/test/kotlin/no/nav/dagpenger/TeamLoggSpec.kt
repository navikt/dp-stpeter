package no.nav.dagpenger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class TeamLoggSpec :
    StringSpec({
        lateinit var appender: ListAppender<ILoggingEvent>
        lateinit var logger: Logger

        beforeTest {
            // "tjenestekall" er forelder til alle "tjenestekall.*"-loggere, og events
            // fra barne-loggere propagerer hit med mindre additivity er skrudd av.
            logger = LoggerFactory.getLogger("tjenestekall") as Logger
            logger.level = Level.TRACE
            appender = ListAppender<ILoggingEvent>().apply { start() }
            logger.addAppender(appender)
        }

        afterTest {
            logger.detachAppender(appender)
            appender.stop()
            MDC.clear()
        }

        "skal bruke 'tjenestekall' som standard loggernavn" {
            TeamLogg().info { "en melding" }

            appender.list shouldHaveSize 1
            appender.list.single().loggerName shouldBe "tjenestekall"
        }

        "skal bruke 'tjenestekall.<klassenavn>' når KClass er oppgitt" {
            TeamLogg(TeamLoggSpec::class).info { "en melding" }

            appender.list shouldHaveSize 1
            appender.list.single().loggerName shouldBe "tjenestekall.TeamLoggSpec"
        }

        "info/warn/error skal logge på riktig nivå med riktig melding" {
            val teamLogg = TeamLogg()

            teamLogg.info { "info-melding" }
            teamLogg.warn { "warn-melding" }
            teamLogg.error { "error-melding" }

            appender.list shouldHaveSize 3
            appender.list[0].level shouldBe Level.INFO
            appender.list[0].formattedMessage shouldBe "info-melding"
            appender.list[1].level shouldBe Level.WARN
            appender.list[1].formattedMessage shouldBe "warn-melding"
            appender.list[2].level shouldBe Level.ERROR
            appender.list[2].formattedMessage shouldBe "error-melding"
        }

        "skal legge kontekst-par i MDC for loggkallet" {
            TeamLogg().info(
                "behandlingId" to "123456",
                "team" to "dagpenger",
            ) { "melding med kontekst" }

            val event = appender.list.single()
            event.mdcPropertyMap shouldContain ("behandlingId" to "123456")
            event.mdcPropertyMap shouldContain ("team" to "dagpenger")
        }

        "skal fjerne MDC-felt fra loggkallet etter at det er ferdig" {
            TeamLogg().info("behandlingId" to "123456") { "melding" }

            MDC.get("behandlingId") shouldBe null
        }

        "withContext skal gi alle kall i blokken tilgang på felles kontekst" {
            TeamLogg().withContext("behandlingId" to "123456") {
                info("team" to "dagpenger") { "melding en" }
                info("team" to "annet-team") { "melding to" }
            }

            appender.list shouldHaveSize 2
            appender.list[0].mdcPropertyMap shouldContain ("behandlingId" to "123456")
            appender.list[0].mdcPropertyMap shouldContain ("team" to "dagpenger")
            appender.list[1].mdcPropertyMap shouldContain ("behandlingId" to "123456")
            appender.list[1].mdcPropertyMap shouldContain ("team" to "annet-team")
        }

        "withContext skal gjenopprette tidligere MDC-verdi etter blokken" {
            MDC.put("behandlingId", "opprinnelig-verdi")

            TeamLogg().withContext("behandlingId" to "ny-verdi") {
                MDC.get("behandlingId") shouldBe "ny-verdi"
            }

            MDC.get("behandlingId") shouldBe "opprinnelig-verdi"
        }

        "withContextAsync skal fungere i en suspend-kontekst og returnere blokkens verdi" {
            val resultat =
                TeamLogg().withContextAsync("behandlingId" to "123456") {
                    MDC.get("behandlingId") shouldBe "123456"
                    "resultat"
                }

            resultat shouldBe "resultat"
        }
    })
