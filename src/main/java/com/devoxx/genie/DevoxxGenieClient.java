package com.devoxx.genie;

import com.devoxx.genie.chatmodel.anthropic.AnthropicChatModelFactory;
import com.devoxx.genie.chatmodel.deepinfra.DeepInfraChatModelFactory;
import com.devoxx.genie.chatmodel.gpt4all.GPT4AllChatModelFactory;
import com.devoxx.genie.chatmodel.groq.GroqChatModelFactory;
import com.devoxx.genie.chatmodel.lmstudio.LMStudioChatModelFactory;
import com.devoxx.genie.chatmodel.mistral.MistralChatModelFactory;
import com.devoxx.genie.chatmodel.ollama.OllamaChatModelFactory;
import com.devoxx.genie.chatmodel.openai.OpenAIChatModelFactory;
import com.devoxx.genie.model.ChatModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.service.OllamaService;
import com.devoxx.genie.ui.SettingsState;
import com.intellij.ide.util.PropertiesComponent;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.ui.Settings.MODEL_PROVIDER;

public class DevoxxGenieClient {

    public static final String YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN =
            "You are a software developer with expert knowledge in ";
    public static final String PROGRAMMING_LANGUAGE =
            " programming language.";
    private static final Logger log = LoggerFactory.getLogger(DevoxxGenieClient.class);
    private ModelProvider modelProvider = getModelProvider(ModelProvider.Ollama.name());

    private String modelName;

    private DevoxxGenieClient() {
    }

    public static DevoxxGenieClient getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Create GPT4All model.
     *
     * @param chatModel the chat model
     * @return the chat language model
     */
    private static ChatLanguageModel createGPT4AllModel(ChatModel chatModel) {
        chatModel.baseUrl = SettingsState.getInstance().getGpt4allModelUrl();
        return new GPT4AllChatModelFactory().createChatModel(chatModel);
    }

    /**
     * Create LMStudio model.
     *
     * @param chatModel the chat model
     * @return the chat language model
     */
    private static ChatLanguageModel createLmStudioModel(ChatModel chatModel) {
        chatModel.baseUrl = SettingsState.getInstance().getLmstudioModelUrl();
        return new LMStudioChatModelFactory().createChatModel(chatModel);
    }

    protected ModelProvider getModelProvider(String defaultValue) {
        String value = PropertiesComponent.getInstance().getValue(MODEL_PROVIDER, defaultValue);
        return ModelProvider.valueOf(value);
    }

    public void setModelProvider(ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Get the chat language model for selected model provider.
     *
     * @return the chat language model
     */
    private ChatLanguageModel getChatLanguageModel() {
        log.debug("Get chat language model: {}", modelProvider);
        ChatModel chatModel = initChatModelSettings();
        SettingsState settings = SettingsState.getInstance();
        return switch (modelProvider) {
            case Ollama -> createOllamaModel(chatModel);
            case LMStudio -> createLmStudioModel(chatModel);
            case GPT4All -> createGPT4AllModel(chatModel);
            case OpenAI -> new OpenAIChatModelFactory(settings.getOpenAIKey(), modelName).createChatModel(chatModel);
            case Mistral -> new MistralChatModelFactory(settings.getMistralKey(), modelName).createChatModel(chatModel);
            case Anthropic ->
                    new AnthropicChatModelFactory(settings.getAnthropicKey(), modelName).createChatModel(chatModel);
            case Groq -> new GroqChatModelFactory(settings.getGroqKey(), modelName).createChatModel(chatModel);
            case DeepInfra ->
                    new DeepInfraChatModelFactory(settings.getDeepInfraKey(), modelName).createChatModel(chatModel);
        };
    }

    /**
     * Create Ollama model.
     *
     * @param chatModel the chat model
     * @return the chat language model
     */
    private ChatLanguageModel createOllamaModel(ChatModel chatModel) {
        setLanguageModelName(chatModel);
        chatModel.baseUrl = SettingsState.getInstance().getOllamaModelUrl();
        return new OllamaChatModelFactory().createChatModel(chatModel);
    }

    /**
     * Initialize chat model settings by default or by user settings.
     *
     * @return the chat model
     */
    private @NotNull ChatModel initChatModelSettings() {
        ChatModel chatModel = new ChatModel();
        chatModel.temperature = SettingsState.getInstance().getTemperature();
        chatModel.maxRetries = SettingsState.getInstance().getMaxRetries();
        chatModel.topP = SettingsState.getInstance().getTopP();
        chatModel.timeout = SettingsState.getInstance().getTimeout();
        return chatModel;
    }

    /**
     * Set the (default) language model name when none is selected.
     *
     * @param chatModel the chat model
     */
    private void setLanguageModelName(ChatModel chatModel) {
        if (modelName == null) {
            try {
                OllamaModelEntryDTO[] models = new OllamaService().getModels();
                chatModel.modelName = models[0].getName();
            } catch (IOException e) {
                log.error("Failed to get Ollama models", e);
            }
        } else {
            chatModel.modelName = modelName;
        }
    }

    /**
     * Execute the user prompt
     *
     * @param userPrompt   the user prompt
     * @param selectedText the selected text
     * @return the prompt
     */
    public String executeGeniePrompt(String userPrompt,
                                     String language,
                                     String selectedText) {
        ChatLanguageModel chatLanguageModel = getChatLanguageModel();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(
                YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN + language + PROGRAMMING_LANGUAGE +
                        "Always return the response in Markdown." +
                        "\n\nSelected code: " + selectedText));
        messages.add(new UserMessage(userPrompt));
        Response<AiMessage> generate = chatLanguageModel.generate(messages);
        return generate.content().text();
    }

    public String executeGenieAutocompletionPrompt(String sourcecode, String language) {
        ChatLanguageModel chatLanguageModel = getChatLanguageModel();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(
                "You are an IDE Autocompletion Agent. The user provides you the current sourcecode up to the current cursor position. Your task is to complete the current line, statement or method. Only respond with the necessary code to finish the current line, method or class. Your response shouldn't include the sourcecode provided by the user. Respond in Markdown format."
                        + "\n"));
        messages.add(new UserMessage("The Sourcecode to complete: \n" + sourcecode));
        Response<AiMessage> generate = chatLanguageModel.generate(messages);
        return generate.content().text();
    }

    /**
     * EXPERIMENTAL : Execute continue prompt
     *
     * @param selectedText the selected text
     * @return the prompt
     */
    public String executeGenieContinuePrompt(String selectedText) {
        ChatLanguageModel chatLanguageModel = getChatLanguageModel();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new dev.langchain4j.data.message.SystemMessage(
                YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN + "JAVA" + PROGRAMMING_LANGUAGE +
                        "\n\nSelected code: " + selectedText));
        messages.add(new UserMessage("Only return the code which finalises the code block."));
        Response<AiMessage> generate = chatLanguageModel.generate(messages);
        return generate.content().text();
    }

    public static final class InstanceHolder {
        private static final DevoxxGenieClient instance = new DevoxxGenieClient();
    }
}
