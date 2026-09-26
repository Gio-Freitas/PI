CREATE TABLE landing.chatbot_sessions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    form_id UUID NOT NULL REFERENCES landing.forms(id),
    session_id UUID UNIQUE NOT NULL, -- identificador público entregue ao cliente, funciona como credencial da conversa (o id interno nunca é exposto)
    current_field VARCHAR(100), -- nome do campo que está sendo perguntado no momento
    answers JSONB NOT NULL DEFAULT '{}'::jsonb, -- mapa nome do campo -> valor respondido
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED ou ABANDONED
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    updated_by UUID
);

CREATE TABLE landing.chatbot_messages (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    session_id UUID NOT NULL REFERENCES landing.chatbot_sessions(id) ON DELETE CASCADE, -- aponta para o id interno da sessão
    role VARCHAR(10) NOT NULL, -- BOT ou USER
    content TEXT NOT NULL,
    field_name VARCHAR(100), -- campo do formulário a que a mensagem se refere, se houver
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_chatbot_sessions_tenant ON landing.chatbot_sessions(tenant_id);
CREATE INDEX idx_chatbot_sessions_form ON landing.chatbot_sessions(form_id);
CREATE INDEX idx_chatbot_messages_session ON landing.chatbot_messages(session_id);
