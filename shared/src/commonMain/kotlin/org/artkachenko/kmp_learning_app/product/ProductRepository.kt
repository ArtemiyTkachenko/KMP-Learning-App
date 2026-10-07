package org.artkachenko.kmp_learning_app.product

/**
 * The public repository the product and its question bank are maintained in — the one place the app
 * learns where to send a learner who has something to tell the maintainers.
 *
 * Both routes to it build on this: a per-Question report opens the `question-report.yml` issue form,
 * and Settings' "Send feedback" opens the issue chooser, which offers that form and a blank issue.
 * A fork that keeps the app but not the repository changes it here.
 */
internal const val ProductRepositoryUrl = "https://github.com/ArtemiyTkachenko/KMP-Learning-App"
