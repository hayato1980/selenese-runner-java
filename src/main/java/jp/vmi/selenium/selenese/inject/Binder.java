package jp.vmi.selenium.selenese.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.ErrorTestCase;
import jp.vmi.selenium.selenese.ErrorTestSuite;
import jp.vmi.selenium.selenese.InvalidSeleneseException;
import jp.vmi.selenium.selenese.Runner;
import jp.vmi.selenium.selenese.TestCase;
import jp.vmi.selenium.selenese.TestSuite;

/**
 * Apply aspect.
 */
public class Binder {
    private static Injector injector;

    static {
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bindInterceptor(
                        Matchers.any(),
                        Matchers.annotatedWith(DoCommand.class),
                        new CommandLogInterceptor(), /* 1st */
                        new HighlightInterceptor(), /* 2nd */
                        new ScreenshotInterceptor() /* 3rd */
                    );
                    bindInterceptor(
                        Matchers.any(),
                        Matchers.annotatedWith(ExecuteTestCase.class),
                        new ExecuteTestCaseInterceptor()
                    );
                    bindInterceptor(
                        Matchers.any(),
                        Matchers.annotatedWith(ExecuteTestSuite.class),
                        new ExecuteTestSuiteInterceptor()
                    );
                }
            }
            );
    }

    /**
     * Constructs TestCase applied aspect.
     *
     * @param filename selenese script file.
     * @param name test-case name.
     * @param runner Runner instance.
     * @param baseURL effective base URL.
     * @return TestCase instance.
     */
    @Deprecated
    public static TestCase newTestCase(String filename, String name, Runner runner, String baseURL) {
        return newTestCase(filename, name, baseURL, runner);
    }

    /**
     * Constructs TestCase applied aspect.
     *
     * @param filename selenese script file.
     * @param name test-case name.
     * @param baseURL effective base URL.
     * @param context Selenese Runner context.
     * @return TestCase instance.
     */
    public static TestCase newTestCase(String filename, String name, String baseURL, Context context) {
        TestCase testCase = injector.getInstance(TestCase.class);
        return testCase.initialize(filename, name, baseURL, context);
    }

    /**
     * Constructs TestSuite applied aspect.
     *
     * @param filename Selenese script file.
     * @param name test-case name.
     * @param runner Runner instance.
     * @return TestSuite instance.
     */
    public static TestSuite newTestSuite(String filename, String name, Runner runner) {
        TestSuite testSuite = injector.getInstance(TestSuite.class);
        return testSuite.initialize(filename, name, runner);
    }

    /**
     * Constructs ErrorTestCase applied aspect.
     *
     * @param filename Selenese script file.
     * @param e InvalidSeleneseException instance.
     * @return ErrorTestCase instance.
     */
    public static ErrorTestCase newErrorTestCase(String filename, InvalidSeleneseException e) {
        ErrorTestCase errorTestCase = injector.getInstance(ErrorTestCase.class);
        return errorTestCase.initialize(filename, e);
    }

    /**
     * Constructs ErrorTestSuite applied aspect.
     *
     * @param filename Selenese script file.
     * @param e InvalidSeleneseException instance.
     * @return ErrorSuiteCase instance.
     */
    public static ErrorTestSuite newErrorTestSuite(String filename, InvalidSeleneseException e) {
        ErrorTestSuite errorTestSuite = injector.getInstance(ErrorTestSuite.class);
        return errorTestSuite.initialize(filename, e);
    }
}
