package jp.vmi.selenium.selenese.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utililities for Selenium. 
 */
public class SeleniumUtils {

    /**
     * Get Selenium version.
     *
     * @return version string. (X.Y.Z format)
     */
    public static String getVersion() {
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = SeleniumUtils.class.getResourceAsStream("/META-INF/maven/org.seleniumhq.selenium/selenium-java/pom.properties");
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return props.getProperty("version", "<unknown>");
    }

    /**
     * String-match pattern.
     * 
     * @param pattern pattern. prefix is "glob:", "regexp:", "regexpi:", or "exact:".
     * @param input input string.
     * @return true if matched pattern.
     */
    public static boolean patternMatches(String pattern, CharSequence input) {
        String[] p = pattern.split(":", 2);
        if (p.length == 2) {
            String type = p[0].toLowerCase();
            if ("regexp".equals(type))
                return regexpMatches(p[1], input, 0);
            else if ("regexpi".equals(type))
                return regexpMatches(p[1], input, Pattern.CASE_INSENSITIVE);
            else if ("exact".equals(type))
                return StringUtils.equals(input, p[1]);
            else if ("glob".equals(type))
                pattern = p[1];
        }
        return globMatches(pattern, input);

    }

    private static boolean regexpMatches(String pattern, CharSequence input, int flags) {
        Pattern p = Pattern.compile(pattern, flags);
        Matcher m = p.matcher(input);
        return m.find();
    }

    private static boolean globMatches(String pattern, CharSequence input) {
        // see http://stackoverflow.com/a/3619098
        Pattern p = Pattern.compile("\\Q" + pattern.replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q"), Pattern.DOTALL);
        Matcher m = p.matcher(input);
        return m.matches();
    }
}
