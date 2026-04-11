# Agent instructions

- Prefer `var` for local variables when the type is obvious.
- Prefer `Optional` return values instead of returning `null`.
- Prefer static imports whenever possible.
- Prefer Lombok `@RequiredArgsConstructor` over explicit constructors for dependency injection.
- Any meaningful change (new or edit) must be covered by unit tests, except runtime hints configs
  and other purely plumbing configs.
- In JPA entities, explicitly name all columns (e.g. `@Column(name = "status")`,
  `@JoinColumn(name = "room_id")`).
