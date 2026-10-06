package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.form.entity.Form;
import com.grupo.formslp.omnipages.form.enums.FormRenderMode;
import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.entity.LandingPageBlock;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingPageHtmlRendererTest {

    private final LandingPageHtmlRenderer renderer = new LandingPageHtmlRenderer("/chatbot/index.html");

    @Test
    void userTextIsAlwaysEscaped() {
        LandingPage page = page(block(BlockType.HERO, Map.of(
                "titulo", "<script>alert('xss')</script>",
                "subtitulo", "Tom & Jerry \"aspas\"")));

        String html = renderer.render(page, seo("<b>Título</b>"));

        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
        assertTrue(html.contains("Tom &amp; Jerry &quot;aspas&quot;"));
        assertTrue(html.contains("<title>&lt;b&gt;Título&lt;/b&gt;</title>"));
    }

    @Test
    void onlySafeLinksAndImagesAreRendered() {
        LandingPage page = page(
                block(BlockType.CTA, Map.of("botaoTexto", "Mau", "botaoLink", "javascript:alert(1)")),
                block(BlockType.CTA, Map.of("botaoTexto", "Espaço", "botaoLink", "https://a.com\" onclick=\"x")),
                block(BlockType.IMAGE, Map.of("url", "data:image/svg+xml;base64,PHN2Zz4=")),
                block(BlockType.CTA, Map.of("botaoTexto", "Bom", "botaoLink", "https://loja.com/oferta?a=1&b=2")),
                block(BlockType.FOOTER, Map.of("links", List.of(
                        Map.of("texto", "Email", "url", "mailto:contato@loja.com"),
                        Map.of("texto", "Vbscript", "url", "VBSCRIPT:msgbox")))));

        String html = renderer.render(page, seo("Loja"));

        assertFalse(html.toLowerCase().contains("javascript:"));
        assertFalse(html.toLowerCase().contains("vbscript:"));
        assertFalse(html.contains("data:image"));
        assertFalse(html.contains("onclick"));
        assertTrue(html.contains("href=\"https://loja.com/oferta?a=1&amp;b=2\""));
        assertTrue(html.contains("href=\"mailto:contato@loja.com\""));
    }

    @Test
    void themeFallsBackToDefaultsWhenValuesAreNotSafe() {
        LandingPage page = page(block(BlockType.TEXT, Map.of("texto", "Oi")));
        page.setTheme(new HashMap<>(Map.of("corPrimaria", "red;}body{display:none", "corFundo", "#112233",
                "fonte", "Inter';}*{x:y")));

        String html = renderer.render(page, seo("Loja"));

        assertTrue(html.contains("--lp-primaria:#0B6B4E"));
        assertTrue(html.contains("--lp-fundo:#112233"));
        assertFalse(html.contains("body{display:none"));
        assertFalse(html.contains("x:y"));
        assertTrue(html.contains("--lp-fonte:'Inter',"));
    }

    @Test
    void pageHasASingleH1FromTheFirstHeroWithTitle() {
        LandingPage page = page(
                block(BlockType.HERO, Map.of("subtitulo", "Sem título")),
                block(BlockType.HERO, Map.of("titulo", "Primeiro")),
                block(BlockType.HERO, Map.of("titulo", "Segundo")));

        String html = renderer.render(page, seo("Loja"));

        assertEquals(1, html.split("<h1>", -1).length - 1);
        assertTrue(html.contains("<h1>Primeiro</h1>"));
        assertTrue(html.contains("<h2>Segundo</h2>"));
    }

    @Test
    void formBlockBecomesAPlaceholderForFormJs() {
        Form form = new Form();
        form.setFormKey("contato-2026");
        LandingPageBlock block = block(BlockType.FORM, Map.of("titulo", "Fale com a gente"));
        block.setForm(form);

        String html = renderer.render(page(block), seo("Loja"));

        assertTrue(html.contains("<div class=\"lp-formulario-embed\" data-landing-form=\"contato-2026\"></div>"));
    }

    @Test
    void listsKeepOnlyWellFormedItemsAndEmptyBlocksDisappear() {
        LandingPage page = page(
                block(BlockType.BENEFITS, Map.of("itens", List.of("Frete grátis", Map.of("titulo", "Garantia",
                        "descricao", "12 meses"), 42))),
                block(BlockType.FAQ, Map.of("itens", List.of(Map.of("pergunta", "Entrega?", "resposta", "Sim"),
                        Map.of("resposta", "sem pergunta")))),
                block(BlockType.TESTIMONIALS, Map.of()),
                block(BlockType.IMAGE, Map.of("alt", "sem url")));

        String html = renderer.render(page, seo("Loja"));

        assertTrue(html.contains("<li><strong>Frete grátis</strong></li>"));
        assertTrue(html.contains("<li><strong>Garantia</strong><p>12 meses</p></li>"));
        assertFalse(html.contains("42"));
        assertEquals(1, html.split("<details>", -1).length - 1);
        assertFalse(html.contains("lp-depoimentos"));
        assertFalse(html.contains("lp-imagem"));
    }

    @Test
    void textKeepsParagraphsAndLineBreaks() {
        String html = renderer.render(page(block(BlockType.TEXT, Map.of("texto", "Linha 1\nLinha 2\n\nParágrafo 2"))),
                seo("Loja"));

        assertTrue(html.contains("<p>Linha 1<br>Linha 2</p><p>Parágrafo 2</p>"));
    }

    @Test
    void seoControlsTheHead() {
        Map<String, Object> seo = new LinkedHashMap<>(seo("Black Friday"));
        seo.put("descricao", "Até 40%");
        seo.put("imagemCompartilhamento", "https://cdn.loja.com/bf.png");
        seo.put("indexar", false);

        String html = renderer.render(page(block(BlockType.TEXT, Map.of("texto", "Oi"))), seo);

        assertTrue(html.contains("<meta name=\"description\" content=\"Até 40%\">"));
        assertTrue(html.contains("<meta name=\"robots\" content=\"noindex\">"));
        assertTrue(html.contains("<meta property=\"og:image\" content=\"https://cdn.loja.com/bf.png\">"));
        assertTrue(html.startsWith("<!DOCTYPE html>"));
    }

    @Test
    void chatbotFormBecomesASingleFloatingLauncher() {
        LandingPageBlock first = block(BlockType.FORM, Map.of("titulo", "Atendimento", "botaoTexto", "Tire <suas> dúvidas",
                "abrirEm", "modal"));
        first.setForm(form("chat de vendas", FormRenderMode.CHATBOT));
        LandingPageBlock second = block(BlockType.FORM, Map.of("titulo", "Outro chat"));
        second.setForm(form("outro-chat", FormRenderMode.CHATBOT));

        String html = renderer.render(page(first, second), seo("Loja"));

        assertEquals(1, html.split("id=\"lp-chat\"", -1).length - 1);
        assertTrue(html.contains("data-src=\"/chatbot/index.html?form=chat+de+vendas&amp;modo=embutido\""));
        assertTrue(html.contains("Tire &lt;suas&gt; dúvidas</button>"));
        assertTrue(html.contains("id=\"lp-chat-painel\" role=\"dialog\" aria-modal=\"true\" aria-label=\"Chat\" hidden"));
        assertFalse(html.contains("data-landing-form"));
        // o chatbot não ocupa espaço na página: abre em modo foco
        assertFalse(html.contains("Atendimento"));
        // sem nenhum botão "#chat" na página, o botão flutuante aparece para o chat não ficar inacessível
        assertTrue(html.contains("data-flutuante=\"true\""));
        assertEquals(1, html.split("<script>", -1).length - 1);
    }

    @Test
    void chatLinkOpensTheChatAndHidesTheFloatingButton() {
        LandingPageBlock chat = block(BlockType.FORM, Map.of("abrirEm", "modal"));
        chat.setForm(form("chat", FormRenderMode.CHATBOT));
        LandingPageBlock hero = block(BlockType.HERO, Map.of("titulo", "Oferta", "botaoTexto", "Quero meu cupom",
                "botaoLink", " #chat "));

        String html = renderer.render(page(hero, chat), seo("Loja"));

        assertTrue(html.contains("href=\"#chat\">Quero meu cupom"));
        assertTrue(html.contains("data-flutuante=\"false\""));
        assertTrue(html.contains("id=\"lp-chat-botao\" hidden"));
    }

    @Test
    void companyCanAskForTheFloatingButtonEvenWithChatLinks() {
        LandingPageBlock chat = block(BlockType.FORM, Map.of("botaoFlutuante", true, "abrirEm", "modal"));
        chat.setForm(form("chat", FormRenderMode.CHATBOT));
        LandingPageBlock cta = block(BlockType.CTA, Map.of("titulo", "Bora", "botaoTexto", "Começar", "botaoLink", "#chat"));

        String html = renderer.render(page(cta, chat), seo("Loja"));

        assertTrue(html.contains("data-flutuante=\"true\""));
        assertFalse(html.contains("id=\"lp-chat-botao\" hidden"));
    }

    @Test
    void chatScriptOnlyRedirectsToHttpAddresses() {
        LandingPageBlock chat = block(BlockType.FORM, Map.of("abrirEm", "modal"));
        chat.setForm(form("chat", FormRenderMode.CHATBOT));

        String html = renderer.render(page(chat), seo("Loja"));

        assertTrue(html.contains("e.data.acao === 'redirecionar' && /^https?:\\/\\//i.test(String(e.data.url))"));
    }

    @Test
    void heroSealHighlightAndSectionOverlineAreEscaped() {
        LandingPage page = page(
                block(BlockType.HERO, Map.of("selo", "Só <hoje>", "titulo", "Até 40%", "tituloDestaque", "em <tudo>")),
                block(BlockType.BENEFITS, Map.of("sobretitulo", "Vantagens & mais", "titulo", "Por quê",
                        "itens", List.of("Frete"))));

        String html = renderer.render(page, seo("Loja"));

        assertTrue(html.contains("Só &lt;hoje&gt;</span>"));
        assertTrue(html.contains("<h1>Até 40% <em>em &lt;tudo&gt;</em></h1>"));
        assertTrue(html.contains("<span class=\"lp-sobretitulo\">Vantagens &amp; mais</span><h2>Por quê</h2>"));
    }

    @Test
    void callToActionIsADarkBandAndLightSectionsAlternate() {
        LandingPage page = page(
                block(BlockType.HERO, Map.of("titulo", "Oi")),
                block(BlockType.TEXT, Map.of("texto", "Um")),
                block(BlockType.TEXT, Map.of("texto", "Dois")),
                block(BlockType.CTA, Map.of("titulo", "Bora")),
                block(BlockType.TEXT, Map.of("texto", "Tres")));

        String html = renderer.render(page, seo("Loja"));

        assertTrue(html.contains("<section class=\"lp-bloco lp-texto\"><div class=\"lp-conteudo\"><p>Um</p>"));
        assertTrue(html.contains("<section class=\"lp-bloco lp-texto lp-alt\"><div class=\"lp-conteudo\"><p>Dois</p>"));
        assertTrue(html.contains("<section class=\"lp-bloco lp-chamada lp-escuro\">"));
        // a faixa escura não entra na alternância: a terceira seção clara volta ao fundo normal
        assertTrue(html.contains("<section class=\"lp-bloco lp-texto\"><div class=\"lp-conteudo\"><p>Tres</p>"));
    }

    @Test
    void companyColorDrivesTheHighlight() {
        LandingPage page = page(block(BlockType.TEXT, Map.of("texto", "Oi")));
        page.setTheme(new HashMap<>(Map.of("corPrimaria", "#7C3AED")));

        String html = renderer.render(page, seo("Loja"));

        assertTrue(html.contains("--lp-primaria:#7C3AED;--lp-destaque:color-mix(in srgb,#7C3AED 55%,#fff)"));
    }

    @Test
    void pageWithoutChatbotHasNoJavaScript() {
        Form form = new Form();
        form.setFormKey("contato");
        LandingPageBlock block = block(BlockType.FORM, Map.of());
        block.setForm(form);

        String html = renderer.render(page(block, block(BlockType.HERO, Map.of("titulo", "Oi"))), seo("Loja"));

        assertFalse(html.contains("<script"));
        assertFalse(html.contains("lp-chat-botao\""));
    }

    @Test
    void chatScriptOnlyTrustsMessagesFromTheChatIframe() {
        LandingPageBlock block = block(BlockType.FORM, Map.of("abrirEm", "modal"));
        block.setForm(form("chat", FormRenderMode.CHATBOT));

        String html = renderer.render(page(block), seo("Loja"));

        assertTrue(html.contains("e.source !== frame.contentWindow"));
        assertTrue(html.contains("e.data.origem !== 'omnipages-chatbot'"));
        // modal de verdade: a página atrás fica inerte enquanto o chat está aberto
        assertTrue(html.contains("pagina.inert = true;"));
    }

    @Test
    void chatbotLauncherHasADefaultLabel() {
        LandingPageBlock block = block(BlockType.FORM, Map.of());
        block.setForm(form("chat", FormRenderMode.CHATBOT));

        assertTrue(renderer.render(page(block), seo("Loja")).contains("Deixe seu contato</a>"));
    }

    @Test
    void byDefaultChatLinksGoToTheChatPageWithoutJavaScript() {
        LandingPageBlock chat = block(BlockType.FORM, Map.of());
        chat.setForm(form("cupom bf", FormRenderMode.CHATBOT));
        LandingPage page = page(
                block(BlockType.HERO, Map.of("titulo", "Oferta", "botaoTexto", "Quero meu cupom", "botaoLink", "#chat")),
                block(BlockType.CTA, Map.of("titulo", "Bora", "botaoTexto", "Começar", "botaoLink", "#chat")),
                chat);

        String html = renderer.render(page, seo("Loja"));

        String chatPage = "href=\"/chatbot/index.html?form=cupom+bf&amp;modo=pagina\"";
        assertEquals(2, html.split(Pattern.quote(chatPage), -1).length - 1);
        assertFalse(html.contains("href=\"#chat\""));
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("class=\"lp-chat__botao\""));
    }

    @Test
    void chatPageModeShowsAFloatingLinkWhenNoButtonOpensTheChat() {
        LandingPageBlock chat = block(BlockType.FORM, Map.of("botaoTexto", "Quero meu cupom"));
        chat.setForm(form("cupom", FormRenderMode.CHATBOT));

        String html = renderer.render(page(block(BlockType.HERO, Map.of("titulo", "Oferta")), chat), seo("Loja"));

        assertTrue(html.contains("<a class=\"lp-chat__botao\" href=\"/chatbot/index.html?form=cupom&amp;modo=pagina\">"));
        assertTrue(html.contains("Quero meu cupom</a>"));
        assertFalse(html.contains("<script"));
    }

    @Test
    void chatLinkWithoutAChatbotStaysAsAnAnchor() {
        String html = renderer.render(page(block(BlockType.HERO, Map.of("titulo", "Oferta", "botaoTexto", "Ver",
                "botaoLink", "#chat"))), seo("Loja"));

        assertTrue(html.contains("href=\"#chat\""));
    }

    @Test
    void designSystemDefaultsApplyWhenTheCompanyDefinesNoTheme() {
        String html = renderer.render(page(block(BlockType.TEXT, Map.of("texto", "Oi"))), seo("Loja"));

        assertTrue(html.contains("--lp-primaria:#0B6B4E"));
        assertTrue(html.contains("--lp-fonte:'Inter'"));
        assertTrue(html.contains("fonts.googleapis.com/css2?family=Inter"));
    }

    @Test
    void customFontDoesNotLoadInter() {
        LandingPage page = page(block(BlockType.TEXT, Map.of("texto", "Oi")));
        page.setTheme(new HashMap<>(Map.of("fonte", "Georgia")));

        String html = renderer.render(page, seo("Loja"));

        assertTrue(html.contains("--lp-fonte:'Georgia'"));
        assertFalse(html.contains("fonts.googleapis.com"));
    }

    private static Form form(String key, FormRenderMode mode) {
        Form form = new Form();
        form.setFormKey(key);
        form.setRenderMode(mode);
        return form;
    }

    private static Map<String, Object> seo(String title) {
        Map<String, Object> seo = new LinkedHashMap<>();
        seo.put("titulo", title);
        seo.put("indexar", true);
        return seo;
    }

    private static LandingPage page(LandingPageBlock... blocks) {
        LandingPage page = new LandingPage();
        page.setName("Loja");
        page.getBlocks().addAll(List.of(blocks));
        return page;
    }

    private static LandingPageBlock block(BlockType type, Map<String, Object> content) {
        LandingPageBlock block = new LandingPageBlock();
        block.setType(type);
        block.setContent(new LinkedHashMap<>(content));
        return block;
    }
}
