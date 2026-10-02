// Serve browser tests the way webApp's dev server serves the application. The SQLite worker
// opens its database through OPFS, which needs SharedArrayBuffer and therefore a
// cross-origin-isolated page; without these headers no web test can reach the real database.
// Opening that database also starts a worker and compiles SQLite's WebAssembly, which can take
// longer than Mocha's two-second default.
//
// The two timeouts are ordered on purpose. Mocha's fails one test that never settles; Karma's
// no-activity window drops the whole browser. Karma's default window is also 30 seconds, so with
// equal values a hung test — a database request that never settles, for example — disconnected
// the browser first: no report was written, the hung test was not named, and every later test
// in the suite was never run. Keeping Karma's window longer lets Mocha report that one failure.
config.set({
    customHeaders: [
        { match: '.*', name: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
        { match: '.*', name: 'Cross-Origin-Embedder-Policy', value: 'require-corp' },
    ],
    client: {
        mocha: { timeout: 30000 },
    },
    browserNoActivityTimeout: 60000,
});
