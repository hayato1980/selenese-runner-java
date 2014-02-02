package jp.vmi.selenium.testutils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.android.AndroidDriver;

import jp.vmi.selenium.selenese.Runner;
import jp.vmi.selenium.selenese.Selenese;
import jp.vmi.selenium.selenese.TestSuite;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.selenese.result.Unexecuted;

@SuppressWarnings({ "javadoc", "deprecation" })
public abstract class TestCaseTestBase extends TestBase {

    @Rule
    public final TemporaryFolder screenshotDir = new TemporaryFolder();

    @Rule
    public final TemporaryFolder screenshotOnFailDir = new TemporaryFolder();

    @Rule
    public final TemporaryFolder xmlResultDir = new TemporaryFolder();

    public WebDriver driver;

    public Runner runner;

    public List<TestSuite> testSuites;

    public Result result;

    public String xmlResult;

    protected abstract void initDriver();

    @Before
    public void initialize() {
        initDriver();

        if (driver instanceof AndroidDriver) {
            //for AndroidEmulator
            wsr.setFqdn("10.0.2.2");
        }

        testSuites = new ArrayList<TestSuite>();
        runner = new Runner() {
            @Override
            public Result execute(Selenese testSuite) {
                if (!(testSuite instanceof TestSuite))
                    throw new RuntimeException("The parameter is not TestSuite instance: " + testSuite);
                testSuites.add((TestSuite) testSuite);
                return super.execute(testSuite);
            }
        };
        runner.setDriver(driver);
        runner.setBaseURL(wsr.getBaseURL());
        runner.setScreenshotDir(screenshotDir.getRoot().getPath());
        runner.setScreenshotOnFailDir(screenshotOnFailDir.getRoot().getPath());
        runner.setJUnitResultDir(xmlResultDir.getRoot().getPath());
    }

    protected void execute(String scriptName) {
        result = Unexecuted.UNEXECUTED;
        xmlResult = null;
        try {
            String scriptFile = TestUtils.getScriptFile(scriptName);
            result = runner.run(scriptFile);
            if (testSuites.isEmpty()) {
                xmlResult = null;
            } else {
                String xmlFile = String.format("TEST-%s.xml", testSuites.get(0).getName());
                xmlResult = FileUtils.readFileToString(new File(xmlResultDir.getRoot(), xmlFile), "UTF-8");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            runner.setJUnitResultDir(null);
        }
    }
}
