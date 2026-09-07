CREATE SCHEMA IF NOT EXISTS landing;

CREATE TABLE landing.forms (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    form_key VARCHAR(100) UNIQUE NOT NULL, -- identificador do formulário
    render_mode VARCHAR(50) NOT NULL DEFAULT 'CLASSIC', -- define o comportamento do front, CLASSIC interface estática, CHATBOT interface conversacional (tipo bate bola)
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    chatbot_greeting TEXT,
    chatbot_farewell TEXT,
    success_action VARCHAR(50), -- guarda a ação (redirect, show message, download)
    success_config JSONB, -- guarda os metadados flexíveis da ação (se for redirect vai guardar a URL)
    consent_required BOOLEAN NOT NULL DEFAULT FALSE, -- se estiver como true o visitante precisa marcar o checkbox para aceitar os termos e enviar os dados
    consent TEXT, -- termos e condições, SE necessário
    allowed_domains JSONB, -- lista de URLs autorizadas a carregar o form.js
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    updated_by UUID
);

CREATE TABLE landing.form_fields (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    form_id UUID NOT NULL REFERENCES landing.forms(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    label VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    position INT NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    placeholder VARCHAR(255), 
    help_text VARCHAR(255),
    validation JSONB, -- todas as regras do formulário que o cliente vai passar, o front lê o JSON para montar a validação no navegador (tamanho mínimo, regex, máscara de CPF)
    options JSONB, -- usado somente se o type do campon for select, radio ou checkbox, guarda a lista de alternativas
    crm_mapping VARCHAR(100), -- integração com os microserviços vizinhos, guarda a chave de destino para sabermos em qual schema o dado deve ir
    chatbot_prompt TEXT, -- exclusivo para o render_mode CHATBOT, texto que o bot envia
    chatbot_order INT, -- exclusivo para o render_mode CHATBOT
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    updated_by UUID,
    UNIQUE (form_id, name)
);

CREATE INDEX idx_forms_tenant ON landing.forms(tenant_id);
CREATE INDEX idx_forms_key ON landing.forms(form_key);
CREATE INDEX idx_fields_form ON landing.form_fields(form_id);