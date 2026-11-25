# How the Mini Test Framework Works

Annotations mark methods so the runner knows what to do with them. `@MyTest` marks a test, `@MyBeforeEach` marks setup, and `@MyAfterEach` marks cleanup. All three use `RetentionPolicy.RUNTIME`, which makes them available while the program runs. `ElementType.METHOD` means they can only be placed on methods.

The `run(Class<?> testClass)` method receives the test class. It creates one object using `getDeclaredConstructor().newInstance()`. The class needs an accessible constructor with no arguments. The runner uses `getDeclaredMethods()` and `isAnnotationPresent()` to find the annotated methods and put them in ArrayLists.

Before running tests, the runner sorts them by priority with Java's built-in stable sort. A smaller priority runs first, and tests with equal priorities keep their discovery order. The default priority is zero. The `enabled` setting is true by default. When it is false, the runner prints SKIP and does not run that test or its setup and cleanup. These two settings provide the bonus features.

For each enabled test, the runner calls the setup methods, then the test method. It uses Reflection's `invoke()` method for these calls. The annotated methods must have no arguments. Setup resets the object's state before each test.

If setup or a test throws an exception, the runner records the failure. Setup failures are labeled `BeforeEach`, and the test body does not run after a setup failure. Reflection wraps exceptions from invoked methods in `InvocationTargetException`, so the runner reads the original cause to show its message. Cleanup runs inside a `finally` block, even when setup or a test fails. A cleanup exception also counts as a failure; when another failure already occurred, the cleanup failure is appended so it does not hide the original one. The runner then continues with the next test.

After cleanup, the runner prints PASS or FAIL and updates one counter. At the end, it prints the total executed tests, passed tests, and failed tests. Skipped tests have a separate count. The sample has no authentication service: `loginTest` verifies that setup starts from a logged-out state, then demonstrates changing the local state. Logout deliberately fails, and registration still runs and passes.
