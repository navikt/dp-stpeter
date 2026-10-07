package no.nav.dagpenger

import no.nav.dagpenger.api.models.IdentForesporsel

private const val MAX_ORIGIN_ROUTE_LENGTH = 200

private val APPLICATION_NAME_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")
private val ROUTE_SEGMENT_PATTERN = Regex("[A-Za-z][A-Za-z._~-]*|v[0-9]{1,2}|\\{[A-Za-z][A-Za-z0-9_]*}")

fun IdentForesporsel.toContextMap() =
    mapOf(
        "application" to application,
        "oppslagslogg" to oppslagslogg.toString(),
        "originRoute" to originRoute,
    ).toList().toTypedArray()

fun IdentForesporsel.hasValidLogMetadata(): Boolean =
    APPLICATION_NAME_PATTERN.matches(application) &&
        (originRoute == null || originRoute.isRouteTemplate())

private fun String.isRouteTemplate(): Boolean {
    if (length !in 2..MAX_ORIGIN_ROUTE_LENGTH || !startsWith('/')) return false

    return removePrefix("/")
        .split("/")
        .all { segment ->
            ROUTE_SEGMENT_PATTERN.matches(segment) &&
                segment != "." &&
                segment != ".."
        }
}
