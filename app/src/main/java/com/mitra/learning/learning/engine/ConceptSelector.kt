package com.mitra.learning.learning.engine

import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity

object ConceptSelector {
    private const val prerequisiteThreshold = 0.70f
    private const val masteredThreshold = 0.85f
    private const val rotationBand = 0.12f

    fun select(
        concepts: List<ConceptEntity>,
        mastery: List<MasteryEntity>,
        prerequisites: List<ConceptPrerequisiteEntity>,
    ): ConceptEntity? {
        if (concepts.isEmpty()) return null

        val masteryById = mastery.associateBy { it.conceptId }
        val prereqByConcept = prerequisites.groupBy { it.conceptId }
        val eligible = concepts.filter { it.practiceReady }.filter { concept ->
            prereqByConcept[concept.id].orEmpty().all { link ->
                (masteryById[link.prerequisiteConceptId]?.mastery ?: 0f) >= prerequisiteThreshold
            }
        }
        if (eligible.isEmpty()) return null

        val preferred = eligible.filter { !it.builtIn }.ifEmpty { eligible }
        val pool = preferred.filter { (masteryById[it.id]?.mastery ?: 0f) < masteredThreshold }
            .ifEmpty { preferred }

        // Do not drill one concept endlessly. Rotate among skills whose mastery is close to the
        // weakest one, choosing the least recently practised concept first.
        val weakest = pool.minOf { masteryById[it.id]?.mastery ?: 0f }
        val rotationPool = pool.filter { (masteryById[it.id]?.mastery ?: 0f) <= weakest + rotationBand }
        return rotationPool.minWithOrNull(
            compareBy<ConceptEntity> { masteryById[it.id]?.lastPracticedAt ?: Long.MIN_VALUE }
                .thenBy { masteryById[it.id]?.mastery ?: 0f }
                .thenBy { it.sortOrder }
        )
    }
}
