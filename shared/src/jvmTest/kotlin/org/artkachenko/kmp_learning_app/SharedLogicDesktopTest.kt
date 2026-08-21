package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SharedLogicDesktopTest {

    @Test
    fun sharedGreetingRunsOnDesktopJvm() {
        assertEquals("Hello, desktop JVM!", sayHello("desktop JVM"))
    }
}
