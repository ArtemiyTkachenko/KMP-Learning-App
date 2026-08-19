package org.artkachenko.kmp_learning_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform