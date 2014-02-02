package jp.vmi.selenium.selenese;

import org.openqa.selenium.internal.WrapsDriver;

import jp.vmi.selenium.selenese.cmdproc.Eval;
import jp.vmi.selenium.selenese.cmdproc.SeleneseRunnerCommandProcessor;
import jp.vmi.selenium.selenese.locator.WebDriverElementFinder;

/**
 * Selenese Runner Context.
 */
public interface Context extends WrapsDriver {

    /**
     * Get current base URL.
     *
     * @return base URL.
     */
    String getBaseURL();

    /**
     * Get variables map.
     *
     * @return varsMap.
     */
    VarsMap getVarsMap();

    /**
     * Get initial window handle.
     *
     * @return window handle.
     */
    String getInitialWindowHandle();

    /**
     * Get elemnt finder.
     *
     * @return element finder.
     */
    WebDriverElementFinder getElementFinder();

    /**
     * Get evaluater.
     *
     * @return eval object.
     */
    Eval getEval();

    /**
     * Get SeleneseRunnerCommandProcessor instance.
     * 
     * @return SeleneseRunnerCommandProcessor instance.
     */
    SeleneseRunnerCommandProcessor getProc();
}
