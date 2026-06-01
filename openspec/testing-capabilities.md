## Testing Capabilities

**Strict TDD Mode**: enabled
**Detected**: 2026-05-30

### Test Runner

- **Backend**: JUnit 5 (via `spring-boot-starter-webmvc-test`) — `./mvnw test`
  - Framework: JUnit 5 + Mockito + AssertJ + MockMvc
  - Files: 6 test classes (unit + integration)
  - Profile: `test` (H2 in-memory database)
- **Frontend**: ❌ No test runner detected
  - `package.json` has no test script
  - No test dependencies (vitest, jest, etc.)
  - No test files under `src/`

### Test Layers

| Layer       | Available | Tool                                      |
| ----------- | --------- | ----------------------------------------- |
| Unit        | ✅        | JUnit 5 + Mockito (AuthServiceTest, etc.) |
| Integration | ✅        | Spring Boot Test + MockMvc (controller integration tests) |
| E2E         | ❌        | —                                         |

### Coverage

- Available: ❌
- Command: —

### Quality Tools

| Tool         | Available | Command                 |
| ------------ | --------- | ----------------------- |
| Linter       | ✅ (frontend only) | `pnpm lint` (ESLint 10) |
| Type checker | ❌        | —                       |
| Formatter    | ❌        | —                       |

### Notes

- Backend tests use H2 in-memory with `application-test.properties`
- No coverage tool configured (no JaCoCo, no Vitest coverage)
- Frontend has no test infrastructure at all
- Strict TDD is enabled because backend tests exist, but frontend changes will lack test safety nets
