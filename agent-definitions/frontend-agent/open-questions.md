# Femke: Open Questions

This file records implementation questions raised during coding that remain unresolved. These questions block code production until answered. Format per entry:
```

[2026-07-06] [Session 1] QUESTION: How does Femke communicate to Gerard that the API contract is ready?
Raised by: User
Status: Resolved
Resolution: Femke signals Archibald via `docs/api-ready-signal.md`. Archibald then delegates Gerard. Gerard signals Archibald via `docs/gerard-ready-signal.md`. Archibald activates Naut only after receiving Gerard's signal. This three-phase signal chain ensures strict sequential workflow compliance.
[YYYY-MM-DD] [Session N] QUESTION: <question>
Raised by: Femke or User
Status: Open | Resolved
Resolution: <if resolved>
```
