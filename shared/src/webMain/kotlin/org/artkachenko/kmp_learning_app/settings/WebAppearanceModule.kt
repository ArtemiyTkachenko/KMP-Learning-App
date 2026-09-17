package org.artkachenko.kmp_learning_app.settings

import org.koin.dsl.module

/**
 * The browser's own small key-value store, `localStorage`, for both configured web targets.
 *
 * `webMain` serves Kotlin/JS and Kotlin/Wasm together, so the browser call is made through a
 * top-level `js` function in the shape Kotlin/Wasm requires — a single expression, parameters
 * referenced by name — which Kotlin/JS accepts unchanged. This is the same technique
 * `ui/time/UtcOffset.web.kt` already uses, and it avoids adding a browser-API dependency for two
 * calls.
 *
 * Both calls guard themselves in JavaScript rather than in Kotlin. `localStorage` throws when a
 * browser has site data blocked, and catching it on the JavaScript side keeps one behaviour for
 * both targets instead of depending on how each maps a thrown `DOMException`. An absent or
 * unreadable value reads as the empty string, which becomes "no preference stored".
 */
private class WebAppPreferenceStorage : AppPreferenceStorage {

    override fun read(key: String): String? = readLocalStorage(key).takeIf { it.isNotEmpty() }

    override fun write(key: String, value: String?) {
        if (value == null) removeLocalStorage(key) else writeLocalStorage(key, value)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun readLocalStorage(key: String): String =
    js("(function(){ try { return window.localStorage.getItem(key) || '' } catch (e) { return '' } })()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun writeLocalStorage(key: String, value: String): Boolean =
    js("(function(){ try { window.localStorage.setItem(key, value); return true } catch (e) { return false } })()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun removeLocalStorage(key: String): Boolean =
    js("(function(){ try { window.localStorage.removeItem(key); return true } catch (e) { return false } })()")

internal val webAppearanceModule = module {
    single<AppPreferenceStorage> { WebAppPreferenceStorage() }
}
