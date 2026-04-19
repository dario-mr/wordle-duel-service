# Auth Flow Sequence

High-level view of the current auth flow:

- Google identity is used during login reconciliation.
- The local app user id is the canonical identity inside the system.
- Access tokens use the local app user id as `sub`.

```mermaid
sequenceDiagram
    actor Browser
    participant Google as Google OIDC
    participant Service as Wordle Duel Service
    participant UserRepo as Local User Repository
    Browser ->> Google: Login with Google
    Google -->> Service: External identity
    Service ->> UserRepo: Find or create local user
    UserRepo -->> Service: Local app user
    Service -->> Browser: Refresh cookie + access token
    Browser ->> Service: Send access token
    Service ->> Service: Use JWT sub = local app user id
```

## Identity Model

- External identity: Google `sub`
- Login reconciliation data: Google `sub`, email
- Internal canonical identity: `app_user.id`
- JWT subject (`sub`): `app_user.id`
- Email claim: profile data only
