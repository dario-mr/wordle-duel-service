# Testing conventions

## Unit tests

- Use AAA pattern: Arrange / Act / Assert (with blank lines between sections).
- Test names must follow: `methodName_condition_result`.
- Avoid suffixes like case1/case2; encode the scenario in `condition`.
- Assertions should be written with AssertJ `assertThat`.
- Avoid using `ReflectionTestUtils`, always prefer getters and setters.
- Catch exception with `catchThrowable`, example:
  `var thrown = catchThrowable(() -> converter.convertTo(response));`

## Mocks

- Test classes requiring mocking are annotated with `@ExtendWith(MockitoExtension.class)`.
- Dependencies should be mocked with `@Mock` and injected with `@InjectMocks`.
- `when(...)` should use argument matchers like `any()`/`anyString()` by default; only match real
  values when they matter for the specific test.
- `verify(...)` should assert the actual values passed to dependencies.
- When verifying a call that should happen exactly once, prefer `verify(mock).method(...)` over
  `verify(mock, times(1)).method(...)`.
- When only some of the dependencies should be mocked, injected them manually on `@BeforeEach`.
- Do not mock mappers, if they do not contain any dependency.

## Example 1

```java

@ExtendWith(MockitoExtension.class)
class GreetingServiceTest {

  @Mock
  private Formatter formatter;

  @InjectMocks
  private GreetingService service;

  @Test
  void greet_validName_returnsFormattedMessage() {
    // Arrange
    var name = "Ada";
    when(formatter.format(anyString(), anyString())).thenReturn("Hello Ada");

    // Act
    var result = service.greet(name);

    // Assert
    assertThat(result).isEqualTo("Hello Ada");
    verify(formatter).format("Hello %s", name);
  }
}
```

## Example 2 - partial mock injections

```java

@ExtendWith(MockitoExtension.class)
class GreetingServiceTest {

  @Mock
  private Formatter formatter;

  private final DateService dateService;

  private GreetingService service;

  @BeforeEach
  void setUp() {
    service = new GreetingService(formatter, dateService);
  }

  // test cases
}
```

## Integration tests

- Integration test class names must end with `IT`.

## Repository tests

- Repository integration tests should be annotated with `@DataJpaTest`.
- Avoid `jpaRepository.deleteAll()` in `@BeforeEach` when default `@DataJpaTest` rollback already
  provides isolation.
- Only add explicit cleanup when rollback is disabled or data can be committed outside the test
  transaction.
