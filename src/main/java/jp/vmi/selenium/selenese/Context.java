package jp.vmi.selenium.selenese;

import org.openqa.selenium.internal.WrapsDriver;

/**
 * Selenese Runner Context.
 */
public interface Context extends WrapsDriver {

    /**
     * Get base URL.
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
}
