public class SampleTests {
    private boolean loggedIn;

    @MyBeforeEach
    public void setUp() {
        loggedIn = false;
        System.out.println("BeforeEach: reset login state");
    }

    @MyTest
    public void loginTest() {
        loggedIn = true;
        if (!loggedIn) {
            throw new AssertionError("Login should succeed");
        }
    }

    @MyTest(priority = 1)
    public void logoutTest() {
        // Deliberate failure to demonstrate failure handling.
        if (!loggedIn) {
            throw new AssertionError("Expected true but was false");
        }
    }

    @MyTest(priority = 2)
    public void registrationTest() {
        if (loggedIn) {
            throw new AssertionError("Registration should start logged out");
        }
    }

    @MyTest(enabled = false, priority = 3)
    public void disabledTest() {
        throw new AssertionError("A disabled test must never execute");
    }

    @MyAfterEach
    public void tearDown() {
        loggedIn = false;
        System.out.println("AfterEach: clean up login state");
    }
}
