package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SharedLogicWebTest {

    @Test
    fun sharedGreetingRunsOnWebTargets() {
        assertEquals("Hello, web!", sayHello("web"))
    }
}
