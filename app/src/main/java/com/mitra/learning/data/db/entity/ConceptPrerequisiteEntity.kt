package com.mitra.learning.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "concept_prerequisites",
    primaryKeys = ["conceptId", "prerequisiteConceptId"],
)
data class ConceptPrerequisiteEntity(
    val conceptId: String,
    val prerequisiteConceptId: String,
)
