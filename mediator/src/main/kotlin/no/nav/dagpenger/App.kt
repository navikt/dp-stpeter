package no.nav.dagpenger

import no.nav.dagpenger.konfigurasjon.Configuration

fun main() {
    ApplicationBuilder(Configuration.config).start()
}
