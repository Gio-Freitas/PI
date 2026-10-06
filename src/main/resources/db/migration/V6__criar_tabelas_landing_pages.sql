CREATE TABLE landing.landing_pages (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL, -- endereço público da página; único em toda a instalação, porque identifica o tenant na área pública
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED ou ARCHIVED
    seo JSONB, -- título, descrição, imagem de compartilhamento e se a página deve ser indexada
    tracking JSONB, -- identificadores de rastreamento da página (pixel, tag manager...)
    theme JSONB, -- cor primária, cor de fundo e fonte
    has_unpublished_changes BOOLEAN NOT NULL DEFAULT TRUE, -- o rascunho mudou depois da última publicação
    published_html TEXT, -- HTML congelado gerado na publicação; é o que o visitante recebe
    published_form_keys JSONB, -- chaves públicas dos formulários embutidos na versão publicada, usadas pelo form.js
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    updated_by UUID
);

CREATE TABLE landing.landing_page_blocks (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    page_id UUID NOT NULL REFERENCES landing.landing_pages(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- HERO, TEXT, IMAGE, BENEFITS, TESTIMONIALS, FORM, CTA, FAQ ou FOOTER
    position INT NOT NULL, -- ordem do bloco na página, começando em 0
    content JSONB NOT NULL DEFAULT '{}'::jsonb, -- propriedades do bloco, livres por tipo (título, texto, URL da imagem...)
    form_id UUID REFERENCES landing.forms(id), -- obrigatório só quando type = FORM
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    updated_by UUID
);

-- slug único só entre as páginas não excluídas, para que uma página apagada libere o endereço
CREATE UNIQUE INDEX uq_landing_pages_slug ON landing.landing_pages(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_landing_pages_tenant ON landing.landing_pages(tenant_id);
CREATE INDEX idx_landing_page_blocks_page ON landing.landing_page_blocks(page_id);
CREATE INDEX idx_landing_page_blocks_form ON landing.landing_page_blocks(form_id); -- saber em quais páginas um formulário aparece sem varrer a tabela
