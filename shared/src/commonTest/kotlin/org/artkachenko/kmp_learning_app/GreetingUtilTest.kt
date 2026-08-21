package org.artkachenko.kmp_learning_app

import kotlin.test.Test
import kotlin.test.assertEquals

internal class GreetingUtilTest {

    @Test
    fun sayHelloFormatsGreeting() {
        assertEquals("Hello, shared tests!", sayHello("shared tests"))
    }
}
