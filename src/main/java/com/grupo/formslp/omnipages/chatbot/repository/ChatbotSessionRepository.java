package com.grupo.formslp.omnipages.chatbot.repository;

import com.grupo.formslp.omnipages.chatbot.domain.ChatbotSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotSessionRepository extends JpaRepository<ChatbotSession, UUID> {
    // Área pública: a sessão é localizada pelo identificador público, nunca pelo id interno
    Optional<ChatbotSession> findBySessionIdAndDeletedAtIsNull(UUID sessionId);
}
