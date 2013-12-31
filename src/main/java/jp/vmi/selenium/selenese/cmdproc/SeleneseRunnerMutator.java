package jp.vmi.selenium.selenese.cmdproc;

import org.openqa.selenium.internal.seleniumemulation.CompoundMutator;
import org.openqa.selenium.internal.seleniumemulation.ScriptMutator;
import org.openqa.selenium.internal.seleniumemulation.VariableDeclaration;

import jp.vmi.selenium.selenese.BaseURLHolder;
import jp.vmi.selenium.selenese.cmdproc.VariableDeclarationWithDynamicValue.DynamicValue;

/**
 * Substitute for CompoundMutator without static base URL.
 */
public class SeleneseRunnerMutator extends CompoundMutator implements ScriptMutator {

    private static final String BASE_URL = "selenium.browserbot.baseUrl";

    private final BaseURLHolder holder;

    /**
     * Constructor.
     *
     * @param holder base URL holder.
     */
    public SeleneseRunnerMutator(BaseURLHolder holder) {
        super("");
        this.holder = holder;
    }

    @Override
    public void addMutator(ScriptMutator mutator) {
        if (mutator instanceof VariableDeclaration) {
            StringBuilder mutated = new StringBuilder();
            mutator.mutate(BASE_URL, mutated);
            if (mutated.length() > 0) {
                mutator = new VariableDeclarationWithDynamicValue(BASE_URL, new DynamicValue() {
                    @Override
                    public String getValue() {
                        return holder.getBaseURL();
                    }
                });
            }
        }
        super.addMutator(mutator);
    }
}
