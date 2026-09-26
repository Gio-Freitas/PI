package com.grupo.formslp.omnipages.chatbot.service;

import com.grupo.formslp.omnipages.chatbot.domain.ChatbotMessage;
import com.grupo.formslp.omnipages.chatbot.domain.ChatbotMessageRole;
import com.grupo.formslp.omnipages.chatbot.domain.ChatbotSession;
import com.grupo.formslp.omnipages.chatbot.domain.ChatbotSessionStatus;
import com.grupo.formslp.omnipages.chatbot.dto.ChatAnswerResponse;
import com.grupo.formslp.omnipages.chatbot.dto.ChatMessageResponse;
import com.grupo.formslp.omnipages.chatbot.dto.ChatStartResponse;
import com.grupo.formslp.omnipages.chatbot.engine.ChatbotEngine;
import com.grupo.formslp.omnipages.chatbot.exception.ChatbotBusinessException;
import com.grupo.formslp.omnipages.chatbot.exception.ChatbotNotFoundException;
import com.grupo.formslp.omnipages.chatbot.repository.ChatbotMessageRepository;
import com.grupo.formslp.omnipages.chatbot.repository.ChatbotSessionRepository;
import com.grupo.formslp.omnipages.form.entity.Form;
import com.grupo.formslp.omnipages.form.entity.FormField;
import com.grupo.formslp.omnipages.form.repository.FormFieldRepository;
import com.grupo.formslp.omnipages.form.repository.FormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private static final String RENDER_MODE_CHATBOT = "CHATBOT";
    // TODO: confirmar com o módulo de formulários qual status representa um form publicado
    private static final String FORM_STATUS_PUBLISHED = "PUBLISHED";

    private static final String DEFAULT_GREETING = "Olá! Vou te fazer algumas perguntas rápidas.";
    private static final String DEFAULT_FAREWELL = "Obrigado pelas respostas! Entraremos em contato.";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PHONE_DIGITS = 10;

    private final FormRepository formRepository;
    private final FormFieldRepository formFieldRepository;
    private final ChatbotSessionRepository sessionRepository;
    private final ChatbotMessageRepository messageRepository;
    private final ChatbotEngine engine;

    @Transactional
    public ChatStartResponse start(String formKey) {
        Form form = formRepository.findByFormKeyAndDeletedAtIsNull(formKey)
                .orElseThrow(() -> new ChatbotNotFoundException("Formulário não encontrado."));

        if (!RENDER_MODE_CHATBOT.equals(form.getRenderMode())) {
            throw new ChatbotBusinessException("Este formulário não está configurado para o modo chatbot.");
        }
        if (!FORM_STATUS_PUBLISHED.equals(form.getStatus())) {
            throw new ChatbotBusinessException("Este formulário não está ativo.");
        }

        List<FormField> fields = findFields(form, form.getTenantId());
        if (fields.isEmpty()) {
            throw new ChatbotBusinessException("Este formulário não possui campos.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        ChatbotSession session = new ChatbotSession();
        session.setTenantId(form.getTenantId());
        session.setForm(form);
        session.setSessionId(UUID.randomUUID());
        session.setStartedAt(now);
        session.setLastActivityAt(now);

        List<ChatbotMessage> messages = new ArrayList<>();
        messages.add(botMessage(session, textOrDefault(form.getChatbotGreeting(), DEFAULT_GREETING), null));

        FormField first = engine.nextField(session, fields);
        session.setCurrentField(first.getName());
        messages.add(botMessage(session, question(first), first.getName()));

        sessionRepository.save(session);
        messageRepository.saveAll(messages);

        return new ChatStartResponse(session.getSessionId(), toResponse(messages));
    }

    @Transactional
    public ChatAnswerResponse answer(UUID sessionId, String answer) {
        ChatbotSession session = sessionRepository.findBySessionIdAndDeletedAtIsNull(sessionId)
                .orElseThrow(() -> new ChatbotNotFoundException("Sessão não encontrada."));

        if (session.getStatus() != ChatbotSessionStatus.ACTIVE) {
            throw new ChatbotBusinessException("Esta conversa já foi encerrada.");
        }

        // rota pública: o tenant vem da própria sessão, não do contexto da requisição
        Form form = session.getForm();
        List<FormField> fields = findFields(form, session.getTenantId());

        String value = answer.trim();
        String fieldName = session.getCurrentField();
        List<ChatbotMessage> messages = new ArrayList<>();
        messages.add(message(session, ChatbotMessageRole.USER, value, fieldName));

        // novo mapa para o Hibernate detectar a alteração na coluna JSONB
        Map<String, Object> answers = new HashMap<>(session.getAnswers());
        answers.put(fieldName, value);
        session.setAnswers(answers);
        session.setLastActivityAt(OffsetDateTime.now());

        logPartialLead(session);

        FormField next = engine.nextField(session, fields);
        if (next != null) {
            session.setCurrentField(next.getName());
            messages.add(botMessage(session, question(next), next.getName()));
        } else {
            session.setCurrentField(null);
            session.setStatus(ChatbotSessionStatus.COMPLETED);
            session.setCompletedAt(OffsetDateTime.now());
            messages.add(botMessage(session, textOrDefault(form.getChatbotFarewell(), DEFAULT_FAREWELL), null));
            submitLead(session);
        }

        sessionRepository.save(session);
        messageRepository.saveAll(messages);

        // o cliente já tem a mensagem que o usuário digitou; devolve só o que o bot respondeu
        List<ChatbotMessage> botReplies = messages.stream()
                .filter(m -> m.getRole() == ChatbotMessageRole.BOT)
                .toList();
        return new ChatAnswerResponse(session.getStatus(), toResponse(botReplies));
    }

    private List<FormField> findFields(Form form, UUID tenantId) {
        return formFieldRepository.findAllByFormIdAndTenantIdAndDeletedAtIsNullOrderByPositionAsc(form.getId(), tenantId);
    }

    // TODO: entregar ao SubmissionService.submitPublic quando o módulo de envios existir,
    // para reaproveitar validação, deduplicação e mapeamento para CRM
    private void submitLead(ChatbotSession session) {
        log.info("Conversa {} concluída para o formulário {}; lead pronto para envio: {}",
                session.getSessionId(), session.getForm().getId(), session.getAnswers().keySet());
    }

    // lead parcial: já existe um contato entre as respostas, mesmo que a pessoa abandone a conversa
    private void logPartialLead(ChatbotSession session) {
        Map<String, Object> answers = session.getAnswers();
        if (answers.size() < 2) {
            return;
        }
        boolean hasContact = answers.values().stream()
                .map(String::valueOf)
                .anyMatch(v -> EMAIL.matcher(v).matches() || v.replaceAll("\\D", "").length() >= MIN_PHONE_DIGITS);
        if (hasContact) {
            log.info("Lead parcial na conversa {}: {} respostas, com contato", session.getSessionId(), answers.size());
        }
    }

    // se o campo não tiver pergunta própria, ela é montada a partir do rótulo: "Qual o seu [rótulo]?"
    private String question(FormField field) {
        if (field.getChatbotPrompt() != null && !field.getChatbotPrompt().isBlank()) {
            return field.getChatbotPrompt();
        }
        return "Qual o seu " + lowerFirst(field.getLabel()) + "?";
    }

    // "Nome" -> "nome", mas siglas como "CPF" continuam iguais
    private String lowerFirst(String label) {
        if (label.length() > 1 && Character.isUpperCase(label.charAt(1))) {
            return label;
        }
        return Character.toLowerCase(label.charAt(0)) + label.substring(1);
    }

    private String textOrDefault(String text, String fallback) {
        return text != null && !text.isBlank() ? text : fallback;
    }

    private ChatbotMessage botMessage(ChatbotSession session, String content, String fieldName) {
        return message(session, ChatbotMessageRole.BOT, content, fieldName);
    }

    private ChatbotMessage message(ChatbotSession session, ChatbotMessageRole role, String content, String fieldName) {
        ChatbotMessage message = new ChatbotMessage();
        message.setTenantId(session.getTenantId());
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setFieldName(fieldName);
        return message;
    }

    private List<ChatMessageResponse> toResponse(List<ChatbotMessage> messages) {
        return messages.stream().map(ChatMessageResponse::from).toList();
    }
}
