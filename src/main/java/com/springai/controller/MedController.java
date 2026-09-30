package com.springai.controller;

import com.springai.assistant.MedAssistant;
import com.springai.entity.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "med", description = "Medical assistant")
@RestController
@RequestMapping("/med")
public class MedController {

    @Autowired
    private MedAssistant medAssistant;

    @Operation(summary = "Chat with a medical assistant")
    @PostMapping("/chat")
    public String chat(@RequestBody ChatForm chatForm) {
        return medAssistant.chat(Long.valueOf(chatForm.getMemoryId()), chatForm.getMessage());
    }
}
