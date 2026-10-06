package no.nav.dagpenger.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.common.audit_log.cef.AuthorizationDecision
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent

class Oppslagslogg {
    private val auditlogger = KotlinLogging.logger("auditLogger")

    private companion object {
        private const val SYSTEM_NAVN = "DAGPENGER"
    }

    fun les(
        appName: String,
        navIdent: String,
        borgerIdent: String,
        callId: String?,
    ) {
        val cefMessage =
            CefMessage
                .builder()
                .event(CefMessageEvent.ACCESS)
                .applicationName(SYSTEM_NAVN)
                .name(appName)
                .authorizationDecision(AuthorizationDecision.PERMIT)
                .sourceUserId(navIdent)
                .destinationUserId(borgerIdent)
                .timeEnded(System.currentTimeMillis())
                .callId(callId)
                .extension("msg", "NAV-ansatt har gjort oppslag på bruker")
                .build()

        auditlogger.info { cefMessage.toString() }
    }
}
