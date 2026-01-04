# Testing conventions

## Unit tests

- Use AAA pattern: Arrange / Act / Assert (with blank lines between sections).
- Test names must follow: `methodName_condition_result`.
- Avoid suffixes like case1/case2; encode the scenario in `condition`.
- Assertions should be written with AssertJ `assertThat`.

## Mocks (Mockito)

- Test classes requiring mocking are annotated with `@ExtendWith(MockitoExtension.class)`.
- Dependencies should be mocked with `@Mock` and injected with `@InjectMocks`.
- `when(...)` should use argument matchers like `any()`/`anyString()` by default; only match real
  values when they matter for the specific test.
- `verify(...)` should assert the actual values passed to dependencies.
- When verifying a call that should happen exactly once, prefer `verify(mock).method(...)` over
  `verify(mock, times(1)).method(...)`.

## Example

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

## Integration tests

- Integration test class names must end with `IT`.