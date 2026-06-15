package com.multimodel.llm.controller;

import com.openai.models.audio.AudioResponseFormat;
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

    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;

    public AudioController(TranscriptionModel transcriptionModel,
                           TextToSpeechModel textToSpeechModel) {
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
    }

    /**
     * Transcribes audio content from a provided audio file resource.
     *
     * @param audioFile the audio file resource to be transcribed, typically resolved from the classpath
     * @return the transcribed text content from the given audio file
     */

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

    // converts text to speech and saves the resulting audio as an MP3 file
    @GetMapping("/speech")
    String speech(@RequestParam("message") String message) throws IOException {
        // @RequestParam binds the "message" query param (e.g. /api/speech?message=Hello) to the text to synthesize

        // Call the text-to-speech model with the input text; returns raw audio bytes (MP3-encoded by default)
        byte[] audioBytes = textToSpeechModel.call(message);

        // Define the target file path (relative to the application's working directory)
        Path path = Paths.get("output.mp3");

        // Write the audio bytes to disk, creating or overwriting "output.mp3"
        Files.write(path, audioBytes);

        // Return a confirmation message including the absolute path of the saved file
        return "MP3 saved successfully to " + path.toAbsolutePath();
    }

    @GetMapping("/speech-options")
    String speechWithOptions(@RequestParam("message") String message) throws IOException {
        TextToSpeechResponse speechResponse = textToSpeechModel.call(new TextToSpeechPrompt(message,
                OpenAiAudioSpeechOptions.builder().voice(OpenAiAudioSpeechOptions.Voice.NOVA.getValue())
                        .speed(2.0)
                        .responseFormat(OpenAiAudioSpeechOptions.AudioResponseFormat.MP3.getValue()).build()));
        Path path = Paths.get("speech-options.mp3");
        Files.write(path, speechResponse.getResult().getOutput());
        return "MP3 saved successfully to " + path.toAbsolutePath();
    }

}
