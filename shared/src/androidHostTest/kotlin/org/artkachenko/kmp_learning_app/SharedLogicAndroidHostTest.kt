package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertEquals

internal class SharedLogicAndroidHostTest {

    @Test
    fun sharedGreetingRunsOnAndroidHost() {
        assertEquals("Hello, Android host!", sayHello("Android host"))
    }
}
