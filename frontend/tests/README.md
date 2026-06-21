# Frontend tests

All frontend tests live outside production source code.

```text
tests/
├── run-tests.js
└── unit/
    └── *.test.js
```

Run the suite with:

```bash
npm test
```

The current suite uses Node's built-in `node:test` module and focuses on deterministic business
logic. Browser/component integration tests can be added later under `tests/integration/` when a DOM
test environment is introduced.
