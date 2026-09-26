package com.grupo.formslp.omnipages.chatbot.engine;

import com.grupo.formslp.omnipages.chatbot.domain.ChatbotSession;
import com.grupo.formslp.omnipages.form.entity.FormField;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Motor determinístico: ordena os campos por chatbot_order (ou pela posição, quando a ordem não foi definida)
 * e devolve o primeiro que ainda não tem resposta. Decide pelas respostas já registradas, e não por um contador,
 * então campos que já vierem preenchidos são pulados naturalmente.
 */
@Component
public class SequentialChatbotEngine implements ChatbotEngine {

    @Override
    public FormField nextField(ChatbotSession session, List<FormField> fields) {
        Map<String, Object> answers = session.getAnswers();
        List<FormField> sorted = fields.stream()
                .sorted(Comparator.comparingInt(f -> f.getChatbotOrder() != null
                        ? f.getChatbotOrder() : f.getPosition()))
                .toList();
        for (FormField field : sorted) {
            if (!answers.containsKey(field.getName())) {
                return field;
            }
        }
        return null; // todos respondidos
    }
}
