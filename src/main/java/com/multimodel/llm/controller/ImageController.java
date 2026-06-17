package com.multimodel.llm.controller;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.multimodel.llm.config.Constants.*;

@RestController
@RequestMapping("/api")
public class ImageController {

    private final ImageModel imageModel;

    public ImageController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    @GetMapping("/image")
    String generateImage(@RequestParam("message") String message) {
        var imageResponse = imageModel.call(new ImagePrompt(message));
        return imageResponse.getResults().getFirst().getOutput().getB64Json();

    }

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
