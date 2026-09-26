package com.grupo.formslp.omnipages.chatbot.repository;

import com.grupo.formslp.omnipages.chatbot.domain.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, UUID> {
}
