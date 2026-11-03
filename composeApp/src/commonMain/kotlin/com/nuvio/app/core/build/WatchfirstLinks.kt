package com.nuvio.app.core.build

// Placeholder domain (.invalid never resolves): set HOST to the real WATCHFIRST domain before shipping.
object WatchfirstLinks {
    const val HOST = "watchfirst.invalid"
    const val SITE = "https://$HOST"
    const val OFFICIAL_BACKEND = "https://api.$HOST"
    const val TERMS_URL = "$SITE/terms"
    const val PRIVACY_POLICY_URL = "$SITE/privacy-policy"
    const val HELP_URL = "$SITE/faq"
    const val DEVICE_LINK_URL = "$SITE/link"
    const val UPDATES_API_URL = "https://api.$HOST/releases"
}
