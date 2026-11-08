import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MyTestRunner {
    public void run(Class<?> testClass) {
        Object instance;
        try {
            instance = testClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            System.out.println("Cannot create test class -> " + failureMessage(exception));
            return;
        }

        ArrayList<Method> tests = new ArrayList<>();
        ArrayList<Method> beforeMethods = new ArrayList<>();
        ArrayList<Method> afterMethods = new ArrayList<>();

        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MyTest.class)) {
                tests.add(method);
            }
            if (method.isAnnotationPresent(MyBeforeEach.class)) {
                beforeMethods.add(method);
            }
            if (method.isAnnotationPresent(MyAfterEach.class)) {
                afterMethods.add(method);
            }
        }

        // Put lower priorities first.
        for (int i = 0; i < tests.size() - 1; i++) {
            for (int j = 0; j < tests.size() - 1 - i; j++) {
                int firstPriority = tests.get(j).getAnnotation(MyTest.class).priority();
                int nextPriority = tests.get(j + 1).getAnnotation(MyTest.class).priority();
                if (firstPriority > nextPriority) {
                    Method temp = tests.get(j);
                    tests.set(j, tests.get(j + 1));
                    tests.set(j + 1, temp);
                }
            }
        }

        int passed = 0;
        int failed = 0;
        int skipped = 0;

        for (Method test : tests) {
            if (!test.getAnnotation(MyTest.class).enabled()) {
                System.out.println("SKIP " + test.getName());
                skipped++;
                continue;
            }

            String failure = null;
            try {
                for (Method before : beforeMethods) {
                    before.setAccessible(true);
                    before.invoke(instance);
                }
                test.setAccessible(true);
                test.invoke(instance);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                failure = failureMessage(exception);
            } finally {
                // Cleanup runs even if setup or the test throws an exception.
                for (Method after : afterMethods) {
                    try {
                        after.setAccessible(true);
                        after.invoke(instance);
                    } catch (ReflectiveOperationException | RuntimeException exception) {
                        String cleanupFailure = "AfterEach: " + failureMessage(exception);
                        if (failure == null) {
                            failure = cleanupFailure;
                        } else {
                            failure = failure + "; " + cleanupFailure;
                        }
                    }
                }
            }

            if (failure == null) {
                passed++;
                System.out.println("PASS " + test.getName());
            } else {
                failed++;
                System.out.println("FAIL " + test.getName() + " -> " + failure);
            }
        }

        System.out.println("Total: " + (passed + failed)
                + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println("Skipped: " + skipped);
    }

    private String failureMessage(Exception exception) {
        Throwable cause = exception;
        if (exception instanceof InvocationTargetException) {
            cause = ((InvocationTargetException) exception).getTargetException();
        }
        if (cause.getMessage() == null) {
            return cause.getClass().getSimpleName();
        }
        return cause.getMessage();
    }
}
