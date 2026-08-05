package com.mitra.learning.voice

enum class VoiceStyle(
    val label: String,
    val description: String,
    val pitch: Float,
    val rate: Float,
) {
    CARTOON_ADVENTURE(
        "Cartoon Adventure",
        "ઉત્સાહી, બહાદુર અને રમૂજી મૂળ Mitra અવાજ. કોઈ ચોક્કસ cartoon/anime પાત્રની નકલ નથી.",
        1.12f,
        1.04f,
    ),
    WARM("Warm Mitra", "નરમ અને મિત્રતાભર્યો અવાજ", 1.03f, 0.92f),
    ENERGETIC_HERO(
        "Energetic Hero",
        "ઝડપી અને ઉત્સાહી મૂળ hero-style અવાજ; કોઈ ચોક્કસ પાત્રની નકલ નહીં",
        1.17f,
        1.09f,
    ),
    PLAYFUL_HERO(
        "Playful Hero",
        "રમૂજી cartoon energy સાથેનો મૂળ Mitra અવાજ; કોઈ ચોક્કસ પાત્રની નકલ નહીં",
        0.98f,
        1.03f,
    ),
    CALM_STORYTELLER("Storyteller", "વાર્તા માટે ધીમો અને શાંત અવાજ", 0.98f, 0.84f),
}
