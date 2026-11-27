
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.model.chat.ChatModel;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.scenario.common.StructuredResult;
import my.edu.taylors.ap.scenario.textbooks.TextbooksModule;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

/**
 * Group 45 scenario module.
 * Delegates Scenario 5 functionality to TextbooksModule.
 */
public class MyScenarioModule implements ScenarioModule {

    private final TextbooksModule textbooksModule;

    public MyScenarioModule(
            GroupProfile profile,
            Path dataDir,
            Path runtimeDir,
            ChatModel model,
            TraceLogger trace,
            Clock clock
    ) throws DataAccessException {

        this.textbooksModule = new TextbooksModule(
                profile, dataDir, runtimeDir, model, trace, clock
        );
    }

    @Override
    public Scenario scenario() {
        return textbooksModule.scenario();
    }

    @Override
    public List<Object> tools() {
        return textbooksModule.tools();
    }

    @Override
    public List<InputGuardrail> inputGuardrails() {
        return textbooksModule.inputGuardrails();
    }

    @Override
    public List<OutputGuardrail> outputGuardrails() {
        return textbooksModule.outputGuardrails();
    }

    @Override
    public String structuredActionLabel() {
        return textbooksModule.structuredActionLabel();
    }

    @Override
    public String defaultUserId() {
        return textbooksModule.defaultUserId();
    }

    @Override
    public List<Path> dataFiles() {
        return textbooksModule.dataFiles();
    }

    @Override
    public StructuredResult runStructured(
            String userId,
            String userText
    ) throws RuleViolationException {
        return textbooksModule.runStructured(userId, userText);
    }
}
