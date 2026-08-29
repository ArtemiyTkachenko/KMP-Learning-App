package org.artkachenko.kmp_learning_app.sqlite_worker

import androidx.sqlite.driver.web.WebWorkerSQLiteDriver

public expect fun createSQLiteWasmWorker(): WebWorkerSQLiteDriver
