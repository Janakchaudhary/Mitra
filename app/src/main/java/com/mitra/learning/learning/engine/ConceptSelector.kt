package com.mitra.learning.learning.engine

import com.mitra.learning.data.db.entity.ConceptEntity
import com.mitra.learning.data.db.entity.ConceptPrerequisiteEntity
import com.mitra.learning.data.db.entity.MasteryEntity

object ConceptSelector {
    private const val prerequisiteThreshold = 0.70f
    private const val masteredThreshold = 0.85f

    fun select(
        concepts: List<ConceptEntity>,
        mastery: List<MasteryEntity>,
        prerequisites: List<ConceptPrerequisiteEntity>,
    ): ConceptEntity? {
        if (concepts.isEmpty()) return null

        val masteryById = mastery.associateBy { it.conceptId }
        val prereqByConcept = prerequisites.groupBy { it.conceptId }

        val eligible = concepts.filter { concept ->
            prereqByConcept[concept.id].orEmpty().all { link ->
                (masteryById[link.prerequisiteConceptId]?.mastery ?: 0f) >= prerequisiteThreshold
            }
        }

        val pool = eligible.filter { (masteryById[it.id]?.mastery ?: 0f) < masteredThreshold }
            .ifEmpty { eligible.ifEmpty { concepts } }

        return pool.minWithOrNull(
            compareBy<ConceptEntity> { masteryById[it.id]?.mastery ?: 0f }
                .thenBy { masteryById[it.id]?.lastPracticedAt ?: Long.MIN_VALUE }
                .thenBy { it.sortOrder }
        )
    }
}
