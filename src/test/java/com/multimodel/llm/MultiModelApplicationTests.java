package com.multimodel.llm;

import com.multimodel.llm.config.ChatClientFactory;
import com.multimodel.llm.controller.MultiModelChatController;
import com.multimodel.llm.controller.RagController;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that evaluate the quality of chat responses using LLM-based
 * evaluators ({@link RelevancyEvaluator} and {@link FactCheckingEvaluator}), covering
 * plain chat, fact-checking, and RAG-grounded scenarios.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "logging.level.org.springframework.ai=DEBUG"})
class MultiModelApplicationTests {

    /**
     * Mocked out since these tests don't exercise vector-store-backed retrieval.
     */
    @MockitoBean
    private VectorStore vectorStore;

    /**
     * Mocked out since these tests don't exercise web-search-grounded chat.
     */
    @MockitoBean
    private ChatClient webSearchChatClient;

    /**
     * Mocked out since these tests don't exercise the RAG controller's endpoints.
     */
    @MockitoBean
    private RagController ragController;

    /**
     * Factory used to build the Bespoke-Minicheck-backed client that powers the evaluators.
     */
    @Autowired
    private ChatClientFactory chatClientFactory;

    /**
     * Controller under test, invoked directly to obtain chat responses to evaluate.
     */
    @Autowired
    private MultiModelChatController multiModelChatController;

    /**
     * Evaluates whether a response is relevant to the question that produced it.
     */
    private RelevancyEvaluator relevancyEvaluator;

    /**
     * Evaluates whether a response is factually consistent with its supporting context.
     */
    private FactCheckingEvaluator factCheckingEvaluator;

    // Minimum acceptable relevancy score
    @Value("${test.relevancy.min-score:0.7}")
    private float minRelevancyScore;

    /**
     * HR policy document used as ground-truth context in the RAG evaluation scenario.
     */
    @Value("classpath:/promptTemplates/hrPolicyTemplate.st")
    Resource hrPolicyTemplate;

    /**
     * Builds the evaluators from a Bespoke-Minicheck-backed chat client before each test.
     */
    @BeforeEach
    void setup() {
        ChatClient.Builder chatClientBuilder = chatClientFactory.createBespokeMinicheck();
        this.relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClientBuilder).build();
    }

    /**
     * Verifies that the chat controller's response to a basic geography question is
     * both non-blank and scored as relevant by {@link #relevancyEvaluator}, above
     * {@link #minRelevancyScore}.
     */
    @Test
    @DisplayName("Should return relevant response for basic geography question")
//    @Timeout(value = 100)
    void evaluateChatControllerResponseRelevancy() {
        // Given
        String question = "What is the capital of India ?";

        // When
        String aiResponse = multiModelChatController.chat(question);
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, aiResponse);
        EvaluationResponse response = relevancyEvaluator.evaluate(evaluationRequest);

        Assertions.assertAll(
                () -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(response.isPass())
                        .withFailMessage("""
                                ========================================
                                The answer was not considered relevant.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, question, aiResponse)
                        .isTrue(),
                () -> assertThat(response.getScore())
                        .withFailMessage("""
                                ========================================
                                The score %.2f is lower than the minimum required %.2f.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, response.getScore(), minRelevancyScore, question, aiResponse)
                        .isGreaterThan(minRelevancyScore));
    }

    /**
     * Verifies that the chat controller's response to a well-known factual question is
     * both non-blank and judged factually correct by {@link #factCheckingEvaluator}.
     */
    @Test
    @DisplayName("Should return factually correct response for gravity-related question")
    void evaluateFactAccuracyForGravityQuestion() {
        // Given
        String question = "Who discovered the law of universal gravitation?";

        // When
        String aiResponse = multiModelChatController.chat(question);
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, aiResponse);
        EvaluationResponse response = factCheckingEvaluator.evaluate(evaluationRequest);

        Assertions.assertAll(
                () -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(response.isPass())
                        .withFailMessage("""
                                ========================================
                                The answer was not considered factually correct.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, question, aiResponse)
                        .isTrue());
    }

    /**
     * Verifies that the chat controller's answer to an HR policy question is factually
     * consistent with the HR policy document content, using {@link #hrPolicyTemplate} as
     * the ground-truth context supplied to {@link #factCheckingEvaluator}.
     *
     * @throws IOException if {@link #hrPolicyTemplate} cannot be read
     */
    @Test
    @DisplayName("Should correctly evaluate factual response based on HR policy context (RAG scenario)")
    public void evaluateHrPolicyAnswerWithRagContext() throws IOException {
        // Given
        String question = "How many paid leaves do employees get annually?";

        // When
        String aiResponse = multiModelChatController.promptStuff(question);
        String retrievedContext = hrPolicyTemplate.getContentAsString(StandardCharsets.UTF_8);
        EvaluationRequest evaluationRequest = new EvaluationRequest(
                question,
                List.of(new Document(retrievedContext)),
                aiResponse);

        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);

        // Then
        Assertions.assertAll(
                () -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(evaluationResponse.isPass())
                        .withFailMessage("""
                                ========================================
                                The response was not considered factually accurate.
                                Question: %s
                                Response: %s
                                Context: %s
                                ========================================
                                """, question, aiResponse, retrievedContext)
                        .isTrue());
    }

}
