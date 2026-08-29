package org.artkachenko.kmp_learning_app.sqlite_worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

public actual fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver =
    WebWorkerSQLiteDriver(
        Worker(js("""new URL("sqlite-wasm-worker/worker.js", import.meta.url)""")),
    )
