package org.artkachenko.kmp_learning_app.settings

/**
 * Whether the learner has chosen an appearance, and which one.
 *
 * Three states rather than a `Boolean`, because "the app is dark" and "the learner asked for dark"
 * are different facts. [System] is the absence of a choice, which is what a learner who has never
 * opened Settings has, and it is why an existing installation keeps following the operating system
 * exactly as it did before this preference existed.
 *
 * The Settings UI still shows a single switch: it presents the *effective* theme, and moving it
 * records an explicit [Light] or [Dark]. Nothing in the current product writes [System] back, and
 * nothing needs to — it is the initial state, not a third option to pick.
 */
internal enum class ThemePreference {
    /** No override has been stored. The operating system decides. */
    System,

    /** The learner explicitly asked for the light theme. */
    Light,

    /** The learner explicitly asked for the dark theme. */
    Dark,
}

/**
 * The effective theme: the one decision function the whole application resolves through.
 *
 * Deliberately pure and outside composition. The system value is consulted only when no override
 * exists, which is the entire compatibility rule stated once — an explicit choice must keep
 * standing when the operating system later switches.
 */
internal fun ThemePreference.resolveDarkTheme(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        ThemePreference.System -> systemInDarkTheme
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
