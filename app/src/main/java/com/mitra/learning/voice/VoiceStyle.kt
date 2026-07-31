package com.mitra.learning.voice

enum class VoiceStyle(
    val label: String,
    val description: String,
    val pitch: Float,
    val rate: Float,
) {
    WARM("Warm Mitra", "નરમ અને મિત્રતાભર્યો અવાજ", 1.03f, 0.92f),
    ENERGETIC_HERO("Energetic Hero", "ઉત્સાહી anime-hero જેવી energy; કોઈ ચોક્કસ પાત્રની નકલ નહીં", 1.18f, 1.08f),
    PLAYFUL_HERO("Playful Hero", "રમૂજી cartoon-hero જેવી energy; કોઈ ચોક્કસ પાત્રની નકલ નહીં", 0.96f, 1.02f),
    CALM_STORYTELLER("Storyteller", "વાર્તા માટે ધીમો અને શાંત અવાજ", 0.98f, 0.84f),
}
