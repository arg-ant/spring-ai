package com.multimodel.llm.controller;

import com.openai.models.audio.AudioResponseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
@RequestMapping("/api")
public class AudioController {

    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;

    public AudioController(TranscriptionModel transcriptionModel,
                           TextToSpeechModel textToSpeechModel) {
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
    }

    @GetMapping("/transcribe")
    String transcribe(@Value("classpath:SpringAI.mp3") Resource audioFile) {

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioFile);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);
        return response.getResult().getOutput();

    }

    // returns a transcription using OpenAI-specific options (e.g. VTT subtitles)
    @GetMapping("/transcribe-options")
    String transcribeWithOptions(@Value("classpath:SpringAI.mp3") Resource audioFile) {

        // Build OpenAI-specific transcription options
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                // Optional context hint to help guide the model's output (e.g. domain vocabulary)
                .prompt("Talking about Spring AI")
                // Force the transcription language to English (ISO-639-1 code)
                .language("en")
                // Lower temperature = more deterministic, less "creative" transcription output
                .temperature(0.5f)
                // Request output as WebVTT (subtitle format with timestamps), not plain text
                .responseFormat(AudioResponseFormat.VTT)
                .build();

        // Wrap both the audio resource and the OpenAI-specific options into the prompt
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        // Call the model with the prompt + options to get the formatted (VTT) response
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);

        return response.getResult().getOutput();
    }

    @GetMapping("/speech")
    String speech(@RequestParam("message") String message) throws IOException {
        // @RequestParam binds the "message" query param (e.g. /api/speech?message=Hello) to the text to synthesize
        Path path = Paths.get("output.mp3");

        deleteIfPresent(path);

        // Call the text-to-speech model with the input text; returns raw audio bytes (MP3-encoded by default)
        byte[] audioBytes = textToSpeechModel.call(message);
        // Write the audio bytes to disk, creating or overwriting "output.mp3"
        Files.write(path, audioBytes);
        // Return a confirmation message including the absolute path of the saved file
        return "MP3 saved successfully to " + path.toAbsolutePath();
    }

    @GetMapping("/speech-options")
    String speechWithOptions(@RequestParam("message") String message) throws IOException {
        Path path = Paths.get("speech-options.mp3");

        deleteIfPresent(path);

        TextToSpeechResponse speechResponse = textToSpeechModel.call(new TextToSpeechPrompt(message,
                OpenAiAudioSpeechOptions.builder().voice(OpenAiAudioSpeechOptions.Voice.NOVA.getValue())
                        .speed(2.0)
                        .responseFormat(OpenAiAudioSpeechOptions.AudioResponseFormat.MP3.getValue()).build()));

        Files.write(path, speechResponse.getResult().getOutput());
        return "MP3 saved successfully to " + path.toAbsolutePath();
    }

    private void deleteIfPresent(Path path) throws IOException {
        if (Files.deleteIfExists(path)) {
            logger.info("Deleted existing file at {}", path.toAbsolutePath());
        }
    }

}
