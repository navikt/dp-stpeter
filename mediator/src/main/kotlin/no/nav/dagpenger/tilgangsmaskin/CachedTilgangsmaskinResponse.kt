package no.nav.dagpenger.tilgangsmaskin

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "typeInfo",
    defaultImpl = Void::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CachedTilgangsmaskinResponse.TilgangGodkjent::class, name = "GODKJENT"),
    JsonSubTypes.Type(value = CachedTilgangsmaskinResponse.TilgangAvvist::class, name = "AVVIST"),
    JsonSubTypes.Type(value = CachedTilgangsmaskinResponse.NavIdentIkkeFunnet::class, name = "IKKE_FUNNET"),
)
internal sealed interface CachedTilgangsmaskinResponse {
    data class TilgangGodkjent(
        val harTilgang: Boolean,
    ) : CachedTilgangsmaskinResponse

    data class TilgangAvvist(
        val type: URI,
        val title: String,
        val status: Int,
        val navIdent: String,
        val begrunnelse: String,
        val traceId: String,
        val kanOverstyres: Boolean,
    ) : CachedTilgangsmaskinResponse

    data class NavIdentIkkeFunnet(
        val detail: String,
        val instance: String,
        val status: Int,
        val title: String,
        val navident: String,
    ) : CachedTilgangsmaskinResponse
}

internal fun TilgangsmaskinResponse.toCached(): CachedTilgangsmaskinResponse =
    when (this) {
        is TilgangsmaskinResponse.TilgangGodkjent -> CachedTilgangsmaskinResponse.TilgangGodkjent(harTilgang = harTilgang)
        is TilgangsmaskinResponse.TilgangAvvist ->
            CachedTilgangsmaskinResponse.TilgangAvvist(
                type = type,
                title = title,
                status = status,
                navIdent = navIdent,
                begrunnelse = begrunnelse,
                traceId = traceId,
                kanOverstyres = kanOverstyres,
            )

        is TilgangsmaskinResponse.NavIdentIkkeFunnet ->
            CachedTilgangsmaskinResponse.NavIdentIkkeFunnet(
                detail = detail,
                instance = instance,
                status = status,
                title = title,
                navident = navident,
            )
    }

internal fun CachedTilgangsmaskinResponse.toDomain(): TilgangsmaskinResponse =
    when (this) {
        is CachedTilgangsmaskinResponse.TilgangGodkjent ->
            TilgangsmaskinResponse.TilgangGodkjent(
                harTilgang = harTilgang,
            )

        is CachedTilgangsmaskinResponse.TilgangAvvist ->
            TilgangsmaskinResponse.TilgangAvvist(
                type = type,
                title = title,
                status = status,
                navIdent = navIdent,
                begrunnelse = begrunnelse,
                traceId = traceId,
                kanOverstyres = kanOverstyres,
            )

        is CachedTilgangsmaskinResponse.NavIdentIkkeFunnet ->
            TilgangsmaskinResponse.NavIdentIkkeFunnet(
                detail = detail,
                instance = instance,
                status = status,
                title = title,
                navident = navident,
            )
    }
