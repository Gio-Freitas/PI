package com.grupo.formslp.omnipages.chatbot.engine;

import com.grupo.formslp.omnipages.chatbot.domain.ChatbotSession;
import com.grupo.formslp.omnipages.form.entity.FormField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SequentialChatbotEngineTest {

    private final SequentialChatbotEngine engine = new SequentialChatbotEngine();

    @Test
    void segueAPosicaoQuandoNaoHaOrdemDoChatbot() {
        List<FormField> fields = List.of(field("email", 2, null), field("nome", 1, null));

        assertEquals("nome", engine.nextField(session(Map.of()), fields).getName());
    }

    @Test
    void chatbotOrderTemPrioridadeSobreAPosicao() {
        List<FormField> fields = List.of(field("email", 1, 2), field("nome", 2, 1));

        assertEquals("nome", engine.nextField(session(Map.of()), fields).getName());
    }

    @Test
    void pulaCamposJaRespondidos() {
        List<FormField> fields = List.of(field("nome", 1, null), field("email", 2, null), field("telefone", 3, null));

        assertEquals("telefone", engine.nextField(session(Map.of("nome", "Ana", "email", "ana@x.com")), fields).getName());
    }

    @Test
    void retornaNullQuandoTudoFoiRespondido() {
        List<FormField> fields = List.of(field("nome", 1, null));

        assertNull(engine.nextField(session(Map.of("nome", "Ana")), fields));
    }

    private static ChatbotSession session(Map<String, Object> answers) {
        ChatbotSession session = new ChatbotSession();
        session.setAnswers(answers);
        return session;
    }

    private static FormField field(String name, int position, Integer chatbotOrder) {
        FormField field = new FormField();
        field.setName(name);
        field.setPosition(position);
        field.setChatbotOrder(chatbotOrder);
        return field;
    }
}
