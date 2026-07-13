package com.multimodel.llm.controller;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.multimodel.llm.config.Constants.*;

/**
 * REST controller exposing image generation endpoints backed by Spring AI's {@link ImageModel}.
 */
@RestController
@RequestMapping("/api")
public class ImageController {

    private final ImageModel imageModel;

    /**
     * Creates a new controller backed by the given image model.
     *
     * @param imageModel the model used to generate images
     */
    public ImageController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    /**
     * Generates an image from the given text prompt using default options.
     *
     * @param message the text prompt describing the desired image, bound from the {@code message} query parameter
     * @return the generated image as a base64-encoded JSON string
     */
    @GetMapping("/image")
    String generateImage(@RequestParam("message") String message) {
        var imageResponse = imageModel.call(new ImagePrompt(message));
        return imageResponse.getResults().getFirst().getOutput().getB64Json();

    }

    /**
     * Generates an image from the given text prompt using OpenAI-specific options (image
     * count, quality, height, and width, as configured in {@link com.multimodel.llm.config.Constants}).
     *
     * @param message the text prompt describing the desired image, bound from the {@code message} query parameter
     * @return the generated image as a base64-encoded JSON string
     */
    @GetMapping("/image-options")
    String generateImageWithOptions(@RequestParam("message") String message) {
        var imageResponse = imageModel.call(new ImagePrompt(message,
                OpenAiImageOptions.builder()
                        .N(IMAGES_NR)
                        .quality(IMAGE_QUALITY)
                        .height(IMAGE_HEIGHT)
                        .width(IMAGE_WIDTH)
                        .build()));
        return imageResponse.getResults().getFirst().getOutput().getB64Json();
    }

}
