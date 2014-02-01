package jp.vmi.selenium.selenese.command;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import jp.vmi.selenium.selenese.Runner;
import jp.vmi.selenium.selenese.TestCase;
import jp.vmi.selenium.selenese.cmdproc.WDCommand;
import jp.vmi.selenium.selenese.result.Result;
import jp.vmi.selenium.testutils.TestBase;
import jp.vmi.selenium.webdriver.DriverOptions;
import jp.vmi.selenium.webdriver.WebDriverManager;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Test for {@link BuiltInCommand}.
 */
public class BuiltInCommandTest extends TestBase {

    /**
    * Test of user friendly error message.
    *
    * @throws IOException exception.
    */
    @Test
    @Ignore("test fail on buildhive....")
    public void userFriendlyErrorMessage() throws IOException {
        File selenesefile = File.createTempFile("selenese", ".html");

        TestCase testcase = new TestCase();
        WebDriverManager wdm = WebDriverManager.getInstance();
        wdm.setWebDriverFactory(WebDriverManager.HTMLUNIT);
        wdm.setDriverOptions(new DriverOptions());
        Runner runner = new Runner();
        runner.setDriver(wdm.get());
        testcase.initialize(selenesefile.getPath(), "test", runner, wsr.getBaseURL());

        WDCommand wdcpClick = runner.getProc().getCommand("click");
        Command click = new BuiltInCommand(1, "click", new String[] { "link=linktext" }, wdcpClick, false);
        Command open = new Open(1, "open", new String[] { "/index.html" }, "open", false);

        assertTrue(open.doCommand(testcase, runner).isSuccess());
        Result result = click.doCommand(testcase, runner);

        assertThat(result.getMessage(), is("Failure: Element link=linktext not found"));
    }
}
