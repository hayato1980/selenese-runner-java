package jp.vmi.selenium.selenese.command;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.selenium.SeleniumException;

import jp.vmi.selenium.selenese.Context;
import jp.vmi.selenium.selenese.cmdproc.CustomCommandProcessor;
import jp.vmi.selenium.selenese.cmdproc.SeleneseRunnerCommandProcessor;
import jp.vmi.selenium.selenese.cmdproc.WDCommand;

/**
 * Factory of selenese command.
 */
@SuppressWarnings("deprecation")
public class CommandFactory {

    private static final Map<String, Constructor<? extends Command>> constructorMap = new HashMap<String, Constructor<? extends Command>>();

    private static void addConstructor(Class<? extends Command> cmdClass) {
        try {
            String name = StringUtils.uncapitalize(cmdClass.getSimpleName());
            Constructor<? extends Command> constructor;
            constructor = cmdClass.getDeclaredConstructor(int.class, String.class, String[].class, String.class, boolean.class);
            constructorMap.put(name, constructor);
        } catch (Exception e) {
            throw new SeleniumException(e);
        }
    }

    static {
        // commands overriding the command of WebDriverCommandProcessor.
        addConstructor(Open.class);
        addConstructor(Highlight.class);

        // commands unsupported by WebDriverCommandProcessor
        addConstructor(Echo.class);
        addConstructor(CaptureEntirePageScreenshot.class);
        addConstructor(Pause.class);
        addConstructor(SetSpeed.class);

        // commands of selenium-ide-flowcontrol
        // https://github.com/davehunt/selenium-ide-flowcontrol
        addConstructor(While.class);
        addConstructor(EndWhile.class);
        addConstructor(AddCollection.class);
        addConstructor(AddToCollection.class);
        addConstructor(StoreFor.class);
        addConstructor(EndFor.class);
        addConstructor(Label.class);
        addConstructor(Gotolabel.class);
        addConstructor(GotoIf.class);

        // commands for comment
        addConstructor(Comment.class);
    }

    private static final String AND_WAIT = "AndWait";

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
        "(?:(assert|verify|waitFor)(Not)?|store)(?:(.+?)(?:(Not)?(Present))?)?",
        Pattern.CASE_INSENSITIVE);

    private static final int ASSERTION = 1;
    private static final int IS_INVERSE = 2;
    private static final int TARGET = 3;
    private static final int IS_PRESENT_INVERSE = 4;
    private static final int PRESENT = 5;

    private final List<UserDefinedCommandFactory> userDefinedCommandFactories = new ArrayList<UserDefinedCommandFactory>();

    private Context context = null;

    /**
     * Constructor.
     */
    @Deprecated
    public CommandFactory() {
    }

    /**
     * Constructor.
     *
     * @param context Selenese Runner context.
     */
    public CommandFactory(Context context) {
        this.context = context;
    }

    /**
     * Register user defined command factoryName.
     *
     * @param factory user defined command factory.
     */
    public void registerUserDefinedCommandFactory(UserDefinedCommandFactory factory) {
        userDefinedCommandFactories.add(factory);
    }

    /**
     * Set CustomCommandProcessor instance.
     *
     * @param proc CustomCommandProcessor instance.
     */
    @Deprecated
    public void setProc(CustomCommandProcessor proc) {
        this.context = proc.getProc().getContext();
    }

    /**
     * Set SeleneseRunnerCommandProcessor instance.
     *
     * @param proc SeleneseRunnerCommandProcessor instance.
     */
    @Deprecated
    public void setProc(SeleneseRunnerCommandProcessor proc) {
        this.context = proc.getContext();
    }

    /**
     * Constructs selenese command.
     *
     * @param index index in selenese script file.
     * @param cmdWithArgs cmmand and arguments.
     * @return Command instance.
     */
    public Command newCommand(int index, List<String> cmdWithArgs) {
        String name = cmdWithArgs.remove(0);
        return newCommand(index, name, cmdWithArgs.toArray(new String[cmdWithArgs.size()]));
    }

    /**
     * Constructs selenese command.
     *
     * @param index index in selenese script file.
     * @param name command name.
     * @param args command arguments.
     * @return Command instance.
     */
    public Command newCommand(int index, String name, String... args) {
        // user defined command.
        for (UserDefinedCommandFactory factory : userDefinedCommandFactories) {
            Command command = factory.newCommand(index, name, args);
            if (command != null)
                return command;
        }

        boolean andWait = name.endsWith(AND_WAIT);
        String realName = andWait ? name.substring(0, name.length() - AND_WAIT.length()) : name;

        // command supported by subclass of Command without BuiltInCommand
        Constructor<? extends Command> constructor = constructorMap.get(realName);
        if (constructor != null) {
            try {
                return constructor.newInstance(index, name, args, realName, andWait);
            } catch (Exception e) {
                throw new SeleniumException(e);
            }
        }

        // command supported by WebDriverCommandProcessor
        SeleneseRunnerCommandProcessor proc = context.getProc();
        WDCommand command = proc.getCommand(realName);
        if (command != null)
            return new BuiltInCommand(index, name, args, command, andWait);

        // FIXME #32 workaround alert command handling.
        if (realName.matches("(?i)(?:assert|verify|waitFor)(?:Alert|Confirmation|Prompt)(?:(?:Not)?Present)?")) {
            StringBuilder echo = new StringBuilder(name);
            for (String arg : args)
                echo.append(" ").append(arg);
            return new Echo(index, name, new String[] { echo.toString() }, "echo", false);
        }

        // See: http://selenium.googlecode.com/svn/trunk/ide/main/src/content/selenium-core/reference.html
        // Assertion or Accessor
        Matcher matcher = COMMAND_PATTERN.matcher(name);
        if (!matcher.matches())
            throw new SeleniumException("No such command: " + name);
        String assertion = matcher.group(ASSERTION);
        String target = matcher.group(TARGET);
        if (target == null)
            target = "Expression";
        if (matcher.group(PRESENT) != null)
            target += "Present";
        boolean isBoolean = false;
        String getter = "get" + target;
        WDCommand getterCommand = proc.getCommand(getter);
        if (getterCommand == null) {
            getter = "is" + target;
            getterCommand = proc.getCommand(getter);
            if (getterCommand == null)
                throw new SeleniumException("No such command: " + name);
            isBoolean = true;
        }
        if (assertion != null) {
            boolean isInverse = matcher.group(IS_INVERSE) != null || matcher.group(IS_PRESENT_INVERSE) != null;
            return new Assertion(index, name, args, assertion, getterCommand, isBoolean, isInverse);
        } else { // Accessor
            return new Store(index, name, args, getterCommand);
        }
    }

    /**
     * Add names of command implemented by selenese-runner.
     *
     * @param commandNames collection of command names.
     */
    public static void addCommandNames(Collection<String> commandNames) {
        for (String name : constructorMap.keySet())
            commandNames.add(name);
        commandNames.add("store"); // rewrite storeExpression
    }
}
