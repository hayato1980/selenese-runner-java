package jp.vmi.selenium.selenese;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.selenium.SeleniumException;

import jp.vmi.html.result.HtmlResult;
import jp.vmi.html.result.HtmlResultHolder;
import jp.vmi.junit.result.JUnitResult;
import jp.vmi.selenium.selenese.cmdproc.Eval;
import jp.vmi.selenium.selenese.cmdproc.HighlightStyle;
import jp.vmi.selenium.selenese.cmdproc.HighlightStyleBackup;
import jp.vmi.selenium.selenese.cmdproc.SeleneseRunnerCommandProcessor;
import jp.vmi.selenium.selenese.command.CommandFactory;
import jp.vmi.selenium.selenese.inject.Binder;
import jp.vmi.selenium.selenese.locator.WebDriverElementFinder;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.utils.LogRecorder;

import static jp.vmi.selenium.selenese.result.Unexecuted.*;
import static org.openqa.selenium.remote.CapabilityType.*;

/**
 * Provide Java API to run Selenese script.
 */
public class Runner implements Context, HtmlResultHolder {

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    private static final FastDateFormat FILE_DATE_TIME = FastDateFormat.getInstance("yyyyMMdd_HHmmssSSS");

    private WebDriver driver;
    private String initialWindowHandle;
    private String screenshotDir = null;
    private String screenshotAllDir = null;
    private String screenshotOnFailDir = null;
    private String baseURL = "";
    private boolean ignoreScreenshotCommand = false;
    private boolean isHighlight = false;
    private int timeout = 30 * 1000; /* ms */
    private long initialSpeed = 0; /* ms */
    private long speed = 0; /* ms */

    private final Eval eval;
    private final SeleneseRunnerCommandProcessor proc;
    private final WebDriverElementFinder elementFinder;
    private final CommandFactory commandFactory;
    private VarsMap varsMap = new VarsMap();
    private final List<HighlightStyleBackup> styleBackups;

    private int countForDefault = 0;

    private final JUnitResult jUnitResult = new JUnitResult();
    private final HtmlResult htmlResult = new HtmlResult();

    /**
     * Constructor.
     */
    public Runner() {
        this.eval = new Eval(this);
        this.elementFinder = new WebDriverElementFinder();
        this.proc = new SeleneseRunnerCommandProcessor(this);
        this.commandFactory = new CommandFactory(this);
        this.varsMap = new VarsMap();
        this.styleBackups = new ArrayList<HighlightStyleBackup>();
    }

    /**
     * Set PrintStream for logging.
     *
     * @param out PrintStream for logging.
     */
    public static void setPrintStream(PrintStream out) {
        LogRecorder.setPrintStream(out);
    }

    private TakesScreenshot getTakesScreenshot() {
        if (driver instanceof TakesScreenshot) {
            return (TakesScreenshot) driver;
        } else if (driver instanceof RemoteWebDriver && ((HasCapabilities) driver).getCapabilities().is(TAKES_SCREENSHOT)) {
            return (TakesScreenshot) new Augmenter().augment(driver);
        } else {
            return null;
        }
    }

    private void takeScreenshot(TakesScreenshot tss, File file, TestCase testcase) {
        // cf. http://prospire-developers.blogspot.jp/2013/12/selenium-webdriver-tips.html (Japanese)
        driver.switchTo().defaultContent();
        File tmp = tss.getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.moveFile(tmp, file);
        } catch (IOException e) {
            throw new RuntimeException("failed to rename captured screenshot image: " + file, e);
        }
        log.info("- captured screenshot: {}", file);
        testcase.getLogRecorder().info("[[ATTACHMENT|" + file.getAbsolutePath() + "]]");
    }

    /**
     * Take screenshot to filename. (override directory if --screenshot-dir option specified)
     * <p>
     * <p>Internal use only.</p>
     * </p>
     * @param filename filename.
     * @param testcase test-case instance.
     * @exception UnsupportedOperationException WebDriver does not supoort capturing screenshot.
     */
    public void takeScreenshot(String filename, TestCase testcase) throws UnsupportedOperationException {
        TakesScreenshot tss = getTakesScreenshot();
        if (tss == null)
            throw new UnsupportedOperationException("webdriver does not support capturing screenshot.");
        if (File.separatorChar != '\\' && filename.contains("\\"))
            filename = filename.replace('\\', File.separatorChar);
        File file = new File(filename).getAbsoluteFile();
        if (screenshotDir != null)
            file = new File(screenshotDir, file.getName()).getAbsoluteFile();
        takeScreenshot(tss, file, testcase);
    }

    /**
     * Take screenshot at all commands if --screenshot-all option specified.
     * <p>
     * <b>Internal use only.</b>
     * </p>
     * @param prefix prefix name.
     * @param index command index.
     * @param testcase test-case instance.
     */
    public void takeScreenshotAll(String prefix, int index, TestCase testcase) {
        if (screenshotAllDir == null)
            return;
        TakesScreenshot tss = getTakesScreenshot();
        if (tss == null)
            return;
        String filename = String.format("%s_%s_%d.png", prefix, FILE_DATE_TIME.format(Calendar.getInstance()), index);
        takeScreenshot(tss, new File(screenshotAllDir, filename), testcase);
    }

    /**
     * Take screenshot on fail commands if --screenshot-on-fail option specified.
     * <p>
     * <b>Internal use only.</b>
     * </p>
     * @param prefix prefix name.
     * @param index command index.
     * @param testcase test-case instance.
     */
    public void takeScreenshotOnFail(String prefix, int index, TestCase testcase) {
        if (screenshotOnFailDir == null)
            return;
        TakesScreenshot tss = getTakesScreenshot();
        if (tss == null)
            return;
        String filename = String.format("%s_%s_%d_fail.png", prefix, FILE_DATE_TIME.format(Calendar.getInstance()), index);
        takeScreenshot(tss, new File(screenshotOnFailDir, filename), testcase);
    }

    /**
     * Get WebDriver.
     * <p>
     * <b>Internal use only.</b>
     * </p>
     * @return WebDriver.
     */
    @Deprecated
    public WebDriver getDriver() {
        return getWrappedDriver();
    }

    @Override
    public WebDriver getWrappedDriver() {
        return driver;
    }

    @Override
    public String getInitialWindowHandle() {
        return initialWindowHandle;
    }

    /**
     * Set WebDriver.
     *
     * @param driver WebDriver.
     */
    public void setDriver(WebDriver driver) {
        this.driver = driver;
        this.initialWindowHandle = driver.getWindowHandle();
    }

    /**
     * Set directory for storing screenshots.
     *
     * @param screenshotDir directory.
     * @exception IllegalArgumentException throws if screenshotDir is not directory.
     */
    public void setScreenshotDir(String screenshotDir) throws IllegalArgumentException {
        if (screenshotDir != null && !new File(screenshotDir).isDirectory())
            throw new IllegalArgumentException(screenshotDir + " is not directory.");
        this.screenshotDir = screenshotDir;
    }

    /**
     * Set directory for storing screenshots at all commands.
     *
     * @param screenshotAllDir directory.
     * @exception IllegalArgumentException throws if screenshotAllDir is not directory.
     */
    public void setScreenshotAllDir(String screenshotAllDir) throws IllegalArgumentException {
        if (screenshotAllDir != null && !new File(screenshotAllDir).isDirectory())
            throw new IllegalArgumentException(screenshotAllDir + " is not directory.");
        this.screenshotAllDir = screenshotAllDir;
    }

    /**
     * Set directory for storing screenshot on fail.
     *
     * @param screenshotOnFailDir directory.
     */
    public void setScreenshotOnFailDir(String screenshotOnFailDir) {
        if (screenshotOnFailDir != null && !new File(screenshotOnFailDir).isDirectory())
            throw new IllegalArgumentException(screenshotOnFailDir + " is not directory.");
        this.screenshotOnFailDir = screenshotOnFailDir;
    }

    /**
     * Get URL for overriding selenium.base in Selenese script.
     * <p>
     * <b>Internal use only.</b>
     * </p>
     * @return URL.
     */
    @Override
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Set URL for overriding selenium.base in Selenese script.
     *
     * @param baseURL URL.
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Set ignore screenshot command flag.
     *
     * @param ignoreScreenshotCommand set true if you want to ignore "captureEntirePageScreenshot"
     */
    public void setIgnoreScreenshotCommand(boolean ignoreScreenshotCommand) {
        this.ignoreScreenshotCommand = ignoreScreenshotCommand;
    }

    /**
     * Get ignore screenshot command flag.
     *
     * @return flag to ignore "captureEntirePageScreenshot"
     */
    public boolean isIgnoreScreenshotCommand() {
        return ignoreScreenshotCommand;
    }

    /**
     * Get locator highlighting.
     *
     * @return true if use locator highlighting.
     */
    public boolean isHighlight() {
        return isHighlight;
    }

    /**
     * Set locator highlighting.
     *
     * @param isHighlight true if use locator highlighting.
     */
    public void setHighlight(boolean isHighlight) {
        this.isHighlight = isHighlight;
    }

    /**
     * Get <b>effective</b> base URL for running Selenese script.
     * <p>
     * <b>Internal use only.</b>
     * </p>
     * @param baseURL URL specified in file.
     * @return effective URL.
     */
    public String getEffectiveBaseURL(String baseURL) {
        if (StringUtils.isBlank(this.baseURL))
            return baseURL;
        else
            return this.baseURL;
    }

    /**
     * Get timeout for waiting. (ms)
     *
     * @return timeout for waiting.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Set timeout for waiting. (ms)
     *
     * @param timeout for waiting.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Get initial speed at starting test-suite. (ms)
     * 
     * @return initial speed.
     */
    public long getInitialSpeed() {
        return initialSpeed;
    }

    /**
     * Set initial speed at starting test-suite. (ms)
     * 
     * @param initialSpeed initial speed.
     */
    public void setInitialSpeed(long initialSpeed) {
        this.initialSpeed = initialSpeed;
    }

    /**
     * Get speed for setSpeed command.
     *
     * @return speed.
     */
    public long getSpeed() {
        return speed;
    }

    /**
     * Set speed for setSpeed command.
     *
     * @param speed speed.
     */
    public void setSpeed(long speed) {
        this.speed = speed;
    }

    /**
     * Reset speed as initial speed.
     */
    public void resetSpeed() {
        speed = initialSpeed;
    }

    /**
     * Wait according to speed setting. 
     */
    public void waitSpeed() {
        if (speed > 0) {
            try {
                Thread.sleep(speed);
            } catch (InterruptedException e) {
                // ignore it.
            }
        }
    }

    /**
     * Get SeleneseRunnerCommandProcessor instance.
     * 
     * @return SeleneseRunnerCommandProcessor instance.
     */
    @Override
    public SeleneseRunnerCommandProcessor getProc() {
        return proc;
    }

    /**
     * Get CommandFactory instance.
     * 
     * @return CommandFactory instance.
     */
    public CommandFactory getCommandFactory() {
        return commandFactory;
    }

    /**
     * Get variables map used for this session.
     *
     * @return the evaluated variables (state) for the current context.
     */
    @Override
    public VarsMap getVarsMap() {
        return this.varsMap;
    }

    /**
     * Set variables map used for this session.
     *
     * @param varsMap the evaluated variables (state) for the current context.
     */
    public void setVarsMap(VarsMap varsMap) {
        this.varsMap = varsMap;
    }

    @Override
    public Eval getEval() {
        return eval;
    }

    @Override
    public WebDriverElementFinder getElementFinder() {
        return elementFinder;
    }

    /**
     * Execute test-suite / test-case.
     *
     * @param selenese test-suite or test-case.
     * @return result.
     */
    public Result execute(Selenese selenese) {
        try {
            return selenese.execute(null, this);
        } catch (InvalidSeleneseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get boolean value of expr.
     * 
     * @param expr expression.
     * @return cast from result of expr to Javascript Boolean.
     */
    public boolean isTrue(String expr) {
        return (Boolean) eval.eval(driver, varsMap.replaceVars(expr), "Boolean");
    }

    /**
     * Run Selenese script files.
     *
     * @param filenames Selenese script filenames.
     * @return result.
     */
    public Result run(String... filenames) {
        Result totalResult = UNEXECUTED;
        TestSuite defaultTestSuite = null;
        List<TestSuite> testSuiteList = new ArrayList<TestSuite>();
        for (String filename : filenames) {
            Selenese selenese = Parser.parse(filename, this);
            if (selenese.isError()) {
                log.error(selenese.toString());
                totalResult = ((ErrorSource) selenese).getResult();
                continue;
            }
            switch (selenese.getType()) {
            case TEST_SUITE:
                testSuiteList.add((TestSuite) selenese);
                break;
            case TEST_CASE:
                if (defaultTestSuite == null) {
                    defaultTestSuite = Binder.newTestSuite(null, String.format("default-%02d", countForDefault++));
                    testSuiteList.add(defaultTestSuite);
                }
                defaultTestSuite.addSelenese(selenese);
            }
        }
        if (totalResult != UNEXECUTED)
            return totalResult;
        for (TestSuite testSuite : testSuiteList) {
            Result result;
            try {
                result = execute(testSuite);
            } catch (RuntimeException e) {
                log.error(e.getMessage());
                throw e;
            }
            totalResult = totalResult.update(result);
        }
        return totalResult;
    }

    /**
     * Run Selenese script from input stream.
     *
     * @param filename selenese script file. (not open. used for label or generating output filename)
     * @param is input stream of script file. (test-case or test-suite)
     * @return result.
     */
    public Result run(String filename, InputStream is) {
        TestSuite testSuite;
        Selenese selenese = Parser.parse(filename, is, this);
        switch (selenese.getType()) {
        case TEST_CASE:
            testSuite = Binder.newTestSuite(null, String.format("default-%02d", countForDefault++));
            testSuite.addSelenese(selenese);
            break;
        case TEST_SUITE:
            testSuite = (TestSuite) selenese;
            break;
        default:
            // don't reach here.
            throw new RuntimeException("Unknown Selenese object: " + selenese);
        }
        return testSuite.execute(null, this);
    }

    /**
     * Initialize JUnitResult.
     *
     * @param dir JUnit result directory.
     */
    public void setJUnitResultDir(String dir) {
        jUnitResult.setDir(dir);
    }

    /**
     * Get JUnit result instance.
     *
     * @return JUnit result instance.
     */
    public JUnitResult getJUnitResult() {
        return jUnitResult;
    }

    /**
     * Initialize HTMLResult.
     * 
     * @param dir HTML result directory.
     */
    public void setHtmlResultDir(String dir) {
        htmlResult.setDir(dir);
    }

    /**
     * Get HTML result instance.
     *
     * @return HTML result instance.
     */
    @Override
    public HtmlResult getHtmlResult() {
        return htmlResult;
    }

    /**
     * Finish test.
     * 
     * generate index.html for HTML result.
     */
    public void finish() {
        htmlResult.generateIndex();
    }

    /**
     * Highlight and backup specified locator.
     *
     * @param locator locator.
     * @param highlightStyle highlight style.
     */
    public void highlight(String locator, HighlightStyle highlightStyle) {
        WebElement element;
        try {
            element = elementFinder.findElement(driver, locator);
        } catch (SeleniumException e) {
            // element specified by locator is not found.
            return;
        }
        Map<String, String> prevStyles = highlightStyle.doHighlight(driver, element);
        HighlightStyleBackup backup = new HighlightStyleBackup(prevStyles, element);
        styleBackups.add(backup);
    }

    /**
     * Unhighlight backed up styles.
     */
    public void unhighlight() {
        if (styleBackups.isEmpty())
            return;
        for (HighlightStyleBackup backup : styleBackups)
            backup.restore(driver);
        styleBackups.clear();
    }

}
