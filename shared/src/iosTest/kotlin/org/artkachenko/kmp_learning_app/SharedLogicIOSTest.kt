package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SharedLogicIOSTest {

    @Test
    fun sharedGreetingRunsOnIos() {
        assertEquals("Hello, iOS!", sayHello("iOS"))
    }
}
