package jp.vmi.selenium.selenese.command;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import jp.vmi.selenium.selenese.Runner;
import jp.vmi.selenium.selenese.TestCase;
import jp.vmi.selenium.selenese.inject.Binder;
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
        WebDriverManager wdm = WebDriverManager.getInstance();
        wdm.setWebDriverFactory(WebDriverManager.HTMLUNIT);
        wdm.setDriverOptions(new DriverOptions());
        Runner runner = new Runner();
        runner.setDriver(wdm.get());
        runner.setBaseURL(wsr.getBaseURL());
        CommandFactory cf = runner.getCommandFactory();
        TestCase testCase = Binder.newTestCase("dummy", "dummy", wsr.getBaseURL());
        testCase.addCommand(cf, "open", "/index.html");
        testCase.addCommand(cf, "click", "link=linktext");
        Result result = runner.execute(testCase);
        assertThat(result.getMessage(), is("Failure: Element link=linktext not found"));
    }
}
