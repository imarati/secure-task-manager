# SecureFlow threat model

## Scope

SecureFlow is a JWT-protected REST API for managing private tasks and comments.

## Assets

- User credentials
- Password hashes
- JWT signing key
- JWT access tokens
- Tasks and comments
- Audit events
- PostgreSQL data
- RabbitMQ credentials
- CI/CD secrets

## Actors

- Anonymous client
- Authenticated user
- Administrator
- Attacker
- CI/CD runner
- PostgreSQL
- RabbitMQ

## Trust boundaries

1. Client → REST API
2. REST API → PostgreSQL
3. REST API → RabbitMQ
4. CI/CD → repository, container registry and dependencies

## Threats and mitigations

| Threat | Example attack | Mitigation | Verification |
|---|---|---|---|
| BOLA / IDOR | User B requests User A task by changing ID | Ownership checks in service layer | Integration test returns 403 |
| Broken authentication | Forged or modified JWT | Signature and expiration validation | JWT negative tests return 401 |
| Brute force | Repeated login attempts | Rate limiting and audit events | Sixth failed request returns 429 |
| Sensitive data exposure | Stack trace or password leaks | Safe `ApiError`, log policy | Error-response tests |
| Injection | Malicious input passed to persistence | DTO validation, ORM parameter binding | Validation and security tests |
| Dependency vulnerability | Known CVE in transitive dependency | Dependency-Check, Dependabot | CI report |
| Secret exposure | `.env` / JWT secret committed | `.gitignore`, Gitleaks | Secret scan CI job |
| Misconfigured container | App runs as root, exposed services | Docker hardening, Trivy | Trivy config/image scan |
