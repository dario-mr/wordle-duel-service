# Agent instructions

- Prefer `var` for local variables when the type is obvious.
- Prefer `Optional` return values instead of returning `null`.
- Prefer static imports whenever possible.
- Prefer Lombok `@RequiredArgsConstructor` over explicit constructors for dependency injection.
- Any change (new or edit) must be covered by unit tests.
- In JPA entities, explicitly name all columns (e.g. `@Column(name = "status")`,
  `@JoinColumn(name = "room_id")`).
