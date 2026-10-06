package no.nav.dagpenger.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class OppslagsloggSpec :
    StringSpec({

        lateinit var appender: ListAppender<ILoggingEvent>
        lateinit var logger: Logger

        beforeTest {
            // "tjenestekall" er forelder til alle "tjenestekall.*"-loggere, og events
            // fra barne-loggere propagerer hit med mindre additivity er skrudd av.
            logger = LoggerFactory.getLogger("auditLogger") as Logger
            logger.level = Level.TRACE
            appender = ListAppender<ILoggingEvent>().apply { start() }
            logger.addAppender(appender)
        }

        afterTest {
            logger.detachAppender(appender)
            appender.stop()
            MDC.clear()
        }

        "skal bruke 'auditLogger' som standard loggernavn" {
            Oppslagslogg().les(
                appName = "testApp",
                navIdent = "navIdent",
                borgerIdent = "borgerIdent",
                callId = "callId",
            )

            appender.list.size shouldBe 1
            appender.list.single().loggerName shouldBe "auditLogger"
        }

        "skal logge med riktig nivå og melding" {
            val oppslagslogg = Oppslagslogg()

            oppslagslogg.les(
                appName = "testApp",
                navIdent = "navIdent",
                borgerIdent = "borgerIdent",
                callId = "callId",
            )

            appender.list.size shouldBe 1
            appender.list[0].level shouldBe Level.INFO
            appender.list[0].formattedMessage shouldStartWith
                "CEF:0|DAGPENGER|AuditLogger|1.0|audit:access|testApp|INFO|flexString1=Permit sproc=callId msg=NAV-ansatt har gjort oppslag på bruker duid=borgerIdent flexString1Label=Decision"
        }
    })
