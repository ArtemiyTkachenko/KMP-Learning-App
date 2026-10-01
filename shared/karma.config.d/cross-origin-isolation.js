// Serve browser tests the way webApp's dev server serves the application. The SQLite worker
// opens its database through OPFS, which needs SharedArrayBuffer and therefore a
// cross-origin-isolated page; without these headers no web test can reach the real database.
// Opening that database also starts a worker and compiles SQLite's WebAssembly, which can take
// longer than Mocha's two-second default.
config.set({
    customHeaders: [
        { match: '.*', name: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
        { match: '.*', name: 'Cross-Origin-Embedder-Policy', value: 'require-corp' },
    ],
    client: {
        mocha: { timeout: 30000 },
    },
});
