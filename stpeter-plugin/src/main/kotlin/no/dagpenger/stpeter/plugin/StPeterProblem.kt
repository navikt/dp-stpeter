package no.dagpenger.stpeter.plugin

data class StPeterProblem(
    val title: String,
    val status: Int,
    val type: String,
    val detail: String,
    val instance: String,
    val navIdent: String? = null,
    val traceId: String? = null,
    val kanOverstyres: Boolean? = null,
)
