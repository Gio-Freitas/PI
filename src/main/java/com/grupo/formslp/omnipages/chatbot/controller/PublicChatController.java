package com.grupo.formslp.omnipages.chatbot.controller;

import com.grupo.formslp.omnipages.chatbot.dto.ApiResponse;
import com.grupo.formslp.omnipages.chatbot.dto.ChatAnswerRequest;
import com.grupo.formslp.omnipages.chatbot.dto.ChatAnswerResponse;
import com.grupo.formslp.omnipages.chatbot.dto.ChatStartResponse;
import com.grupo.formslp.omnipages.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Área pública (acesso anon, sem token): o cliente só guarda o sessionId e exibe o que o servidor devolve
@RestController
@RequestMapping("/api/landing/public/chat")
@RequiredArgsConstructor
public class PublicChatController {

    private final ChatbotService chatbotService;

    @PostMapping("/{formKey}/iniciar")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatStartResponse> start(@PathVariable String formKey) {
        return ApiResponse.ok(chatbotService.start(formKey), "Conversa iniciada.");
    }

    @PostMapping("/{sessionId}/responder")
    public ApiResponse<ChatAnswerResponse> answer(@PathVariable UUID sessionId,
                                                  @Valid @RequestBody ChatAnswerRequest request) {
        return ApiResponse.ok(chatbotService.answer(sessionId, request.answer()), "Resposta registrada.");
    }
}
