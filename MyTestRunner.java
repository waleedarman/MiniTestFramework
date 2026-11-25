import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;

public class MyTestRunner {
    public void run(Class<?> testClass) {
        Object instance;
        try {
            instance = testClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            System.out.println("Cannot create test class -> " + failureMessage(exception));
            return;
        }

        ArrayList<Method> tests = findMethods(testClass, MyTest.class);
        ArrayList<Method> beforeMethods = findMethods(testClass, MyBeforeEach.class);
        ArrayList<Method> afterMethods = findMethods(testClass, MyAfterEach.class);
        sortByPriority(tests);

        int passed = 0;
        int failed = 0;
        int skipped = 0;

        for (Method test : tests) {
            if (!test.getAnnotation(MyTest.class).enabled()) {
                System.out.println("SKIP " + test.getName());
                skipped++;
                continue;
            }

            String failure = runTest(instance, test, beforeMethods, afterMethods);
            if (failure == null) {
                passed++;
                System.out.println("PASS " + test.getName());
            } else {
                failed++;
                System.out.println("FAIL " + test.getName() + " -> " + failure);
            }
        }

        printReport(passed, failed, skipped);
    }

    private ArrayList<Method> findMethods(Class<?> testClass,
                                         Class<? extends Annotation> annotationType) {
        ArrayList<Method> methods = new ArrayList<>();
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationType)) {
                methods.add(method);
            }
        }
        return methods;
    }

    private void sortByPriority(ArrayList<Method> tests) {
        tests.sort(Comparator.comparingInt(
                method -> method.getAnnotation(MyTest.class).priority()));
    }

    private String runTest(Object instance, Method test, ArrayList<Method> beforeMethods,
                           ArrayList<Method> afterMethods) {
        String failure = null;
        try {
            for (Method before : beforeMethods) {
                try {
                    before.setAccessible(true);
                    before.invoke(instance);
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    failure = "BeforeEach: " + failureMessage(exception);
                    break;
                }
            }

            if (failure == null) {
                test.setAccessible(true);
                test.invoke(instance);
            }
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
        return failure;
    }

    private void printReport(int passed, int failed, int skipped) {
        System.out.println("Total: " + (passed + failed)
                + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println("Skipped: " + skipped);
    }

    private String failureMessage(Exception exception) {
        Throwable cause = exception;
        while (cause instanceof InvocationTargetException
                && ((InvocationTargetException) cause).getTargetException() != null) {
            cause = ((InvocationTargetException) cause).getTargetException();
        }
        if (cause.getMessage() == null) {
            return cause.getClass().getSimpleName();
        }
        return cause.getMessage();
    }
}
