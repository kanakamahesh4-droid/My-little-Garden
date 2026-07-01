package com.example.ui

val uiTranslations = mapOf(
    "English" to mapOf(
        "My Garden" to "My Garden",
        "Identify" to "Identify",
        "Diagnose" to "Diagnose",
        "Care Guide" to "Care Guide",
        "GOOD MORNING," to "GOOD MORNING,",
        "Maddy's Garden" to "Maddy's Garden",
        "Settings" to "Settings",
        "App Language" to "App Language",
        "Close" to "Close"
    ),
    "Hindi" to mapOf(
        "My Garden" to "मेरा बगीचा",
        "Identify" to "पहचानें",
        "Diagnose" to "निदान",
        "Care Guide" to "देखभाल गाइड",
        "GOOD MORNING," to "सुप्रभात,",
        "Maddy's Garden" to "मैडी का बगीचा",
        "Settings" to "सेटिंग्स",
        "App Language" to "ऐप की भाषा",
        "Close" to "बंद करें"
    ),
    "Telugu" to mapOf(
        "My Garden" to "నా తోట",
        "Identify" to "గుర్తించండి",
        "Diagnose" to "వ్యాధి నిర్ధారణ",
        "Care Guide" to "సంరక్షణ గైడ్",
        "GOOD MORNING," to "శుభోదయం,",
        "Maddy's Garden" to "మ్యాడీ తోట",
        "Settings" to "సెట్టింగ్‌లు",
        "App Language" to "యాప్ భాష",
        "Close" to "మూసివేయి"
    ),
    "Tamil" to mapOf(
        "My Garden" to "என் தோட்டம்",
        "Identify" to "அடையாளம் காணுங்கள்",
        "Diagnose" to "கண்டறிதல்",
        "Care Guide" to "பராமரிப்பு வழிகாட்டி",
        "GOOD MORNING," to "காலை வணக்கம்,",
        "Maddy's Garden" to "மேடியின் தோட்டம்",
        "Settings" to "அமைப்புகள்",
        "App Language" to "பயன்பாட்டு மொழி",
        "Close" to "மூடு"
    ),
    "Kannada" to mapOf(
        "My Garden" to "ನನ್ನ ತೋಟ",
        "Identify" to "ಗುರುತಿಸಿ",
        "Diagnose" to "ರೋಗನಿರ್ಣಯ",
        "Care Guide" to "ಆರೈಕೆ ಮಾರ್ಗದರ್ಶಿ",
        "GOOD MORNING," to "ಶುಭೋದಯ,",
        "Maddy's Garden" to "ಮ್ಯಾಡಿಯ ತೋಟ",
        "Settings" to "ಸೆಟ್ಟಿಂಗ್‌ಗಳು",
        "App Language" to "ಅಪ್ಲಿಕೇಶನ್ ಭಾಷೆ",
        "Close" to "ಮುಚ್ಚಿ"
    )
)

fun String.localized(language: String): String {
    return uiTranslations[language]?.get(this) ?: this
}
