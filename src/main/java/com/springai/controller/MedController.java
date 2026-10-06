package com.springai.controller;

import com.springai.assistant.MedAssistant;
import com.springai.entity.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Tag(name = "med", description = "Medical assistant")
@RestController
@RequestMapping("/med")
public class MedController {

    @Autowired
    private MedAssistant medAssistant;



    @Operation(summary = "Chat with a medical assistant")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatForm chatForm) {
        SseEmitter emitter = new SseEmitter(300000L);
        medAssistant.chat(Long.valueOf(chatForm.getMemoryId()), chatForm.getMessage())
                .subscribe(
                        data -> {
                            try {
                                emitter.send(data);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
        return emitter;
    }
}
