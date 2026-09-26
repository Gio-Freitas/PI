package com.grupo.formslp.omnipages.chatbot.engine;

import com.grupo.formslp.omnipages.chatbot.domain.ChatbotSession;
import com.grupo.formslp.omnipages.form.entity.FormField;

import java.util.List;

/**
 * Decide qual é o próximo campo a ser perguntado (padrão Strategy).
 * O ChatbotService depende só desta interface, então trocar a regra de decisão
 * não exige mudança no controlador, no serviço ou no banco.
 */
public interface ChatbotEngine {
    // Retorna o próximo campo a ser perguntado, ou null se já acabou.
    FormField nextField(ChatbotSession session, List<FormField> fields);
}
