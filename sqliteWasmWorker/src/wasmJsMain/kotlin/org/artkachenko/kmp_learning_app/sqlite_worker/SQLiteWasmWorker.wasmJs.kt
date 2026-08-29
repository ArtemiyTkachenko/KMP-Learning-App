package org.artkachenko.kmp_learning_app.sqlite_worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

public actual fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver =
    WebWorkerSQLiteDriver(createWorker())

@OptIn(ExperimentalWasmJsInterop::class)
private fun createWorker(): Worker =
    js("""new Worker(new URL("sqlite-wasm-worker/worker.js", import.meta.url))""")
