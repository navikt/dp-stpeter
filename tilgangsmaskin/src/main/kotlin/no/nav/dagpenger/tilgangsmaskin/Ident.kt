package no.nav.dagpenger.tilgangsmaskin

import no.nav.dagpenger.api.models.IdentForesporsel
import no.nav.dagpenger.tilgangsmaskin.Ident.Companion.tilPersonIdentfikator

data class Ident(
    private val ident: String,
) {
    init {
        if (!ident.matches(Regex("[0-9]{11}"))) {
            throw UnprocessableContentException(
                message = "Personident må ha 11 siffer",
            )
        }
    }

    companion object {
        fun String.tilPersonIdentfikator() = Ident(this)
    }

    fun identifikator() = ident

    fun alleIdentifikatorer() = listOf(ident)

    override fun toString(): String = "Ident(${ident.substring(0, 6)}*****)"
}

fun IdentForesporsel.tilIdent() = this.ident.tilPersonIdentfikator()
