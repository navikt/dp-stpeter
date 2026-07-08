package no.nav.dagpenger

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.blackbird.BlackbirdModule
import tools.jackson.module.kotlin.KotlinModule

val objectMapper: ObjectMapper =
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(BlackbirdModule())
        .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
        .build()

fun JsonMapper.Builder.applyDefault(): JsonMapper.Builder =
    this
        .accessorNaming(
            DefaultAccessorNamingStrategy
                .Provider()
                .withFirstCharAcceptance(true, true),
        ).disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
