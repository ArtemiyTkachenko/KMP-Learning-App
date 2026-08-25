package org.artkachenko.kmp_learning_app.curriculum.content

internal val dependencyInjectionQuestions = listOf(
    question(
        id = "constructor_injection_001",
        topicId = "dependency_injection",
        subtopicId = "constructor_injection",
        text = "Why is constructor injection often preferred for required dependencies?",
        correctAnswerIds = listOf("constructor_injection_001_a"),
        explanation = "Constructor injection makes dependencies explicit, allows immutable properties, and lets tests construct the class directly with fakes.",
        sources = listOf(hiltSource, daggerSource),
        answers = listOf(
            answer("constructor_injection_001_a", "It makes required dependencies explicit at construction time."),
            answer("constructor_injection_001_b", "It hides dependencies behind global mutable state."),
            answer("constructor_injection_001_c", "It requires Android reflection for every object."),
            answer("constructor_injection_001_d", "It prevents unit testing without instrumentation."),
        ),
    ),
    question(
        id = "composition_root_001",
        topicId = "dependency_injection",
        subtopicId = "composition_root",
        text = "What is the role of a composition root in manual dependency injection?",
        correctAnswerIds = listOf("composition_root_001_c"),
        explanation = "The composition root is where object graphs are assembled. Keeping wiring there avoids hidden lookups spread through feature code.",
        sources = listOf(androidArchitectureSource),
        answers = listOf(
            answer("composition_root_001_a", "It stores UI state for every composable."),
            answer("composition_root_001_b", "It validates JSON schemas at runtime."),
            answer("composition_root_001_c", "It centralizes construction and wiring of application dependencies."),
            answer("composition_root_001_d", "It replaces stable IDs in content models."),
        ),
    ),
    question(
        id = "di_scopes_001",
        topicId = "dependency_injection",
        subtopicId = "di_scopes",
        text = "Which statements about DI scopes are correct? Select all that apply.",
        correctAnswerIds = listOf("di_scopes_001_b", "di_scopes_001_d"),
        explanation = "A scope defines how long a provided instance is shared. Mis-scoping can cause unnecessary recreation or leaks when a long-lived object holds a short-lived dependency.",
        sources = listOf(hiltSource, daggerSource),
        answers = listOf(
            answer("di_scopes_001_a", "A scope always means the object lives forever."),
            answer("di_scopes_001_b", "A scope controls the lifetime and sharing boundary of provided instances."),
            answer("di_scopes_001_c", "A shorter-lived object can safely be retained by any longer-lived singleton without risk."),
            answer("di_scopes_001_d", "Incorrect scoping can create leaks or unintended shared state."),
        ),
    ),
    question(
        id = "service_locator_vs_di_001",
        topicId = "dependency_injection",
        subtopicId = "service_locator_vs_di",
        text = "What is a common downside of a service locator compared with explicit dependency injection?",
        correctAnswerIds = listOf("service_locator_vs_di_001_b"),
        explanation = "A service locator hides dependency lookup inside implementation code, making dependencies less visible and often making tests and lifecycle boundaries harder to reason about.",
        sources = listOf(androidArchitectureSource, koinSource),
        answers = listOf(
            answer("service_locator_vs_di_001_a", "It makes all dependencies visible in constructors."),
            answer("service_locator_vs_di_001_b", "It can hide dependencies and lifecycle assumptions behind runtime lookup."),
            answer("service_locator_vs_di_001_c", "It prevents any object from being shared."),
            answer("service_locator_vs_di_001_d", "It is required by Kotlin Multiplatform."),
        ),
    ),
)

