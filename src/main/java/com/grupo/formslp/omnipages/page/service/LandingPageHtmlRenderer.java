package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.form.entity.Form;
import com.grupo.formslp.omnipages.form.enums.FormRenderMode;
import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.entity.LandingPageBlock;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Gera o HTML congelado da publicação a partir do rascunho (blocos, tema e SEO).
//
// Segurança: o conteúdo dos blocos é livre e vem do usuário, então TODO texto passa por HtmlUtils.htmlEscape
// e todo link/imagem passa por safeUrl (só http(s), mailto:, tel: ou âncora #). Tema e SEO já são validados
// na entrada (LandingPageRequest), mas são conferidos de novo aqui: o HTML vai para o visitante.
// O rastreamento (pixel, tag manager) NÃO entra no HTML: injetar script de terceiros fica para quando o
// contrato definir as chaves aceitas.
//
// Chaves lidas em cada tipo de bloco (as que faltarem ou não forem texto são ignoradas). Todo bloco de seção
// aceita também "sobretitulo", a linha pequena em destaque acima do título:
//   TITULO: selo, titulo, tituloDestaque (segunda linha, na cor de destaque), subtitulo, imagemUrl, imagemAlt,
//     botaoTexto, botaoLink
//   TEXTO: titulo, texto (linha em branco separa parágrafos)
//   IMAGEM: url, alt, legenda
//   BENEFICIOS: titulo, itens [{titulo, descricao}] ou [texto]
//   DEPOIMENTOS: titulo, itens [{texto, nome, cargo}]
//   FORMULARIO: titulo, descricao. Formulário tradicional: vira um espaço reservado que o form.js preenche pela
//     chave pública. Formulário em modo CHATBOT: é o chat que os botões com link "#chat" abrem (só o primeiro
//     da página). abrirEm: "pagina" (padrão; os botões levam para o chat em tela cheia, sem JavaScript na
//     landing page) ou "modal" (o chat abre por cima da página). botaoFlutuante (true) e botaoTexto controlam
//     o botão fixo no canto
//   CHAMADA_ACAO: titulo, texto, botaoTexto, botaoLink (faixa escura, para fechar a página com a chamada)
//   PERGUNTAS_FREQUENTES: titulo, itens [{pergunta, resposta}]
//   RODAPE: texto, links [{texto, url}]
//
// Aparência: o tema é da empresa (cor principal, fundo e fonte), e o que ela não definir segue o design system
// da plataforma (ui/plataforma.css no infra-integrador-2026). A abertura, a faixa de chamada e o rodapé usam o
// azul-marinho da marca (brand-950/900) com o brilho da cor principal; botões rounded-lg e cartões rounded-xl.
//
// JavaScript: a página só tem um, o CHAT_SCRIPT fixo, e só quando o chatbot abre em modal. Ele não leva nenhum dado
// do usuário (textos e a chave do formulário ficam no HTML, escapados).
@Component
public class LandingPageHtmlRenderer {

    static final String CHAT_LINK = "#chat";
    static final String OPEN_IN_MODAL = "modal";

    private static final Pattern SAFE_URL = Pattern.compile("^(https?://|mailto:|tel:|#)[^\\s\"'<>`]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern FONT_NAME = Pattern.compile("^[A-Za-z0-9 \\-]{1,60}$");
    // valores do design system (ui/plataforma.css): brand-800, brand-400 e superficie
    private static final String DEFAULT_PRIMARY = "#0B6B4E";
    private static final String DEFAULT_HIGHLIGHT = "#4ADE80";
    private static final String DEFAULT_BACKGROUND = "#FFFFFF";
    private static final String DEFAULT_FONT = "Inter";
    private static final String DEFAULT_CHAT_LABEL = "Deixe seu contato";
    private static final String INTER_STYLESHEET =
            "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap";

    private static final String SVG_OPEN = "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"none\" "
            + "stroke=\"currentColor\" stroke-width=\"2.25\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\">";
    private static final String ICON_CHAT = SVG_OPEN + "<path d=\"M7.9 20A9 9 0 1 0 4 16.1L2 22Z\"/></svg>";
    private static final String ICON_ARROW = SVG_OPEN + "<path d=\"M5 12h14\"/><path d=\"m12 5 7 7-7 7\"/></svg>";
    private static final String ICON_SPARK = SVG_OPEN + "<path d=\"M13 2 3 14h9l-1 8 10-12h-9l1-8z\"/></svg>";
    private static final String ICON_CLOSE = SVG_OPEN + "<path d=\"M18 6 6 18\"/><path d=\"m6 6 12 12\"/></svg>";

    // Abre e fecha o chat em modo foco (centralizado, página escurecida atrás). Abre pelos links "#chat" da página
    // e pelo botão flutuante, se houver. Quem fecha é o X do próprio widget, que avisa por postMessage; enquanto
    // o widget não disser que sabe fechar ("pronto"), a página mostra o X dela, para o visitante nunca ficar preso.
    // Esc e clique fora também fecham. No fim da conversa, o widget pode pedir para levar o visitante a outra
    // página ("redirecionar"), e só endereço http(s) é aceito. Só vale mensagem do iframe do chat (event.source).
    private static final String CHAT_SCRIPT = """
            <script>
            (function () {
              var raiz = document.getElementById('lp-chat');
              var fundo = document.getElementById('lp-chat-fundo');
              var painel = document.getElementById('lp-chat-painel');
              var fecharPagina = document.getElementById('lp-chat-fechar');
              var flutuante = document.getElementById('lp-chat-botao');
              // enquanto o chat está aberto, a página atrás não recebe foco nem clique (modal de verdade)
              var pagina = document.querySelector('main');
              var frame = painel.querySelector('iframe');
              var temFlutuante = raiz.dataset.flutuante === 'true';
              var widgetFecha = false;
              var origem = null;
              function aberto() { return !painel.hidden; }
              function atualizar() {
                fecharPagina.hidden = widgetFecha;
                flutuante.hidden = !temFlutuante || aberto();
              }
              function abrir(quem) {
                origem = quem || null;
                if (!frame.src) { frame.src = frame.dataset.src; }
                painel.hidden = false;
                fundo.hidden = false;
                pagina.inert = true;
                document.documentElement.classList.add('lp-chat-aberto');
                atualizar();
                frame.focus();
              }
              function fechar() {
                painel.hidden = true;
                fundo.hidden = true;
                pagina.inert = false;
                document.documentElement.classList.remove('lp-chat-aberto');
                atualizar();
                if (origem && !origem.hidden) { origem.focus(); }
              }
              document.querySelectorAll('a[href="#chat"]').forEach(function (link) {
                link.addEventListener('click', function (e) { e.preventDefault(); abrir(link); });
              });
              flutuante.addEventListener('click', function () { abrir(flutuante); });
              fecharPagina.addEventListener('click', fechar);
              fundo.addEventListener('click', fechar);
              document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && aberto()) { fechar(); } });
              window.addEventListener('message', function (e) {
                if (e.source !== frame.contentWindow || !e.data || e.data.origem !== 'omnipages-chatbot') { return; }
                if (e.data.acao === 'pronto') { widgetFecha = true; atualizar(); }
                if (e.data.acao === 'fechar') { fechar(); }
                if (e.data.acao === 'redirecionar' && /^https?:\\/\\//i.test(String(e.data.url))) {
                  window.location.href = e.data.url;
                }
              });
              atualizar();
            })();
            </script>
            """;

    // cores neutras e tamanhos do design system; a cor principal, o destaque, o fundo e a fonte vêm do tema (--lp-*)
    private static final String BASE_CSS = """
            *{box-sizing:border-box}
            [hidden]{display:none !important}
            body{margin:0;background:var(--lp-fundo);color:#111827;font-family:var(--lp-fonte);line-height:1.6;-webkit-font-smoothing:antialiased}
            .lp-conteudo{max-width:1040px;margin:0 auto}
            .lp-bloco{padding:88px 16px}
            .lp-alt{background:#F8FAFC}
            .lp-bloco h2{margin:0 0 32px;font-size:clamp(1.75rem,3.5vw,2.5rem);line-height:1.15;font-weight:800;letter-spacing:-.02em;color:#0C1A2E}
            .lp-sobretitulo{display:block;margin-bottom:8px;color:var(--lp-primaria);font-size:.75rem;font-weight:800;letter-spacing:.14em;text-transform:uppercase}
            .lp-escuro{color:#E6EBF2;background:radial-gradient(60% 80% at 50% 0%,color-mix(in srgb,var(--lp-primaria) 45%,transparent),transparent 70%),#0C1A2E}
            .lp-escuro h2{color:#fff}
            .lp-escuro p{color:#B6C2D2}
            .lp-escuro .lp-sobretitulo{color:var(--lp-destaque)}
            .lp-titulo{position:relative;overflow:hidden;min-height:88vh;display:flex;align-items:center;padding:96px 16px;text-align:center;
              background:radial-gradient(60% 50% at 15% 10%,color-mix(in srgb,var(--lp-primaria) 60%,transparent),transparent 70%),
                radial-gradient(50% 45% at 85% 0%,color-mix(in srgb,var(--lp-destaque) 30%,transparent),transparent 70%),
                linear-gradient(180deg,#0F2340,#0C1A2E 70%)}
            .lp-titulo .lp-conteudo{width:100%}
            .lp-selo{display:inline-flex;align-items:center;gap:8px;padding:6px 14px;border-radius:999px;border:1px solid color-mix(in srgb,var(--lp-destaque) 40%,transparent);background:color-mix(in srgb,var(--lp-destaque) 8%,transparent);color:var(--lp-destaque);font-size:.75rem;font-weight:700;letter-spacing:.12em;text-transform:uppercase}
            .lp-selo svg{width:14px;height:14px}
            .lp-titulo h1,.lp-titulo h2{margin:24px auto 16px;max-width:880px;font-size:clamp(2.25rem,6vw,4.5rem);line-height:1.04;font-weight:900;letter-spacing:-.03em;color:#fff;text-transform:uppercase}
            .lp-titulo h1 em,.lp-titulo h2 em{display:block;font-style:italic;color:var(--lp-destaque)}
            .lp-subtitulo{max-width:620px;margin:0 auto;font-size:1.125rem;color:#B6C2D2}
            .lp-titulo img{margin-top:40px}
            .lp-bloco img{max-width:100%;height:auto;border-radius:12px}
            figure{margin:0}
            figcaption{margin-top:8px;color:#6B7280;font-size:.875rem}
            .lp-botao{display:inline-flex;align-items:center;gap:10px;margin-top:32px;padding:16px 28px;border-radius:8px;background:var(--lp-primaria);color:#fff;text-decoration:none;font-weight:700;letter-spacing:.01em;transition:transform .15s,box-shadow .15s}
            .lp-escuro .lp-botao{box-shadow:0 0 0 1px color-mix(in srgb,var(--lp-destaque) 40%,transparent),0 12px 40px color-mix(in srgb,var(--lp-primaria) 55%,transparent)}
            .lp-botao:hover{transform:translateY(-1px);background:color-mix(in srgb,var(--lp-primaria) 88%,#fff)}
            .lp-lista{list-style:none;margin:0;padding:0;display:grid;gap:16px;grid-template-columns:repeat(auto-fit,minmax(240px,1fr))}
            .lp-lista li{padding:28px;background:#fff;border:1px solid #E5E7EB;border-radius:12px}
            .lp-lista li::before{content:"";display:block;width:36px;height:4px;margin-bottom:20px;border-radius:999px;background:var(--lp-primaria)}
            .lp-lista strong{display:block;font-size:1.125rem;color:#0C1A2E}
            .lp-lista p{margin:6px 0 0;color:#4B5563}
            .lp-depoimento{margin:0 0 16px;padding:32px;background:#fff;border:1px solid #E5E7EB;border-radius:12px;font-size:1.25rem;color:#0C1A2E}
            .lp-depoimento p{margin:0}
            .lp-depoimento footer{margin-top:16px;font-size:.9375rem;font-weight:600;color:#4B5563}
            .lp-faq details{padding:18px 0;border-bottom:1px solid #E5E7EB}
            .lp-faq summary{cursor:pointer;font-weight:600;color:#0C1A2E}
            .lp-faq details p{margin:8px 0 0;color:#4B5563}
            .lp-chamada{text-align:center}
            .lp-chamada h2{margin-bottom:8px}
            .lp-chamada p{margin:0 auto;max-width:620px}
            .lp-rodape{padding:32px 16px;background:#0C1A2E;color:#7C8AA0;font-size:.875rem;border-top:1px solid #1E2F48}
            .lp-rodape p{margin:0 0 8px}
            .lp-rodape a{color:#B6C2D2;margin-right:16px}
            .lp-chat-aberto,.lp-chat-aberto body{overflow:hidden}
            .lp-chat__fundo{position:fixed;inset:0;z-index:20;background:rgba(7,17,30,.72);backdrop-filter:blur(2px)}
            .lp-chat__painel{position:fixed;z-index:21;top:50%;left:50%;transform:translate(-50%,-50%);width:min(480px,calc(100vw - 32px));height:min(720px,calc(100vh - 48px));border-radius:16px;overflow:hidden;background:#fff;box-shadow:0 24px 64px rgba(0,0,0,.45)}
            .lp-chat__painel iframe{display:block;width:100%;height:100%;border:0}
            .lp-chat__fechar{position:absolute;top:10px;right:10px;z-index:1;display:inline-flex;padding:8px;border:0;border-radius:999px;background:rgba(12,26,46,.75);color:#fff;cursor:pointer}
            .lp-chat__botao{position:fixed;right:16px;bottom:16px;z-index:10;display:inline-flex;align-items:center;gap:8px;padding:14px 22px;border:0;border-radius:999px;background:var(--lp-primaria);color:#fff;font:inherit;font-weight:700;text-decoration:none;cursor:pointer;box-shadow:0 8px 24px rgba(12,26,46,.3)}
            @media (max-width:600px){.lp-bloco{padding:56px 16px}.lp-titulo{min-height:auto;padding:88px 16px 72px}
              .lp-chat__painel{top:0;left:0;transform:none;width:100%;height:100%;border-radius:0}}
            """;

    private final String chatbotWidgetUrl;

    public LandingPageHtmlRenderer(@Value("${landing.chatbot.widget-url:/chatbot/index.html}") String chatbotWidgetUrl) {
        this.chatbotWidgetUrl = chatbotWidgetUrl;
    }

    public String render(LandingPage page, Map<String, Object> publishedSeo) {
        StringBuilder html = new StringBuilder(8192);
        html.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        appendSeo(html, publishedSeo);
        String font = matchOr(page.getTheme(), "fonte", FONT_NAME, DEFAULT_FONT);
        if (font.equals(DEFAULT_FONT)) {
            html.append("<link rel=\"stylesheet\" href=\"").append(INTER_STYLESHEET).append("\">\n");
        }
        html.append("<style>\n").append(themeVariables(page.getTheme(), font)).append(BASE_CSS).append("</style>\n")
                .append("</head>\n<body>\n<main class=\"lp\">\n");
        // a página tem um único <h1>: o título do primeiro bloco TITULO que tiver título; os demais usam <h2>.
        // As seções claras alternam o fundo; as escuras (abertura, chamada) não entram na alternância
        boolean h1Used = false;
        int lightSections = 0;
        LandingPageBlock chatBlock = page.getBlocks().stream().filter(LandingPageHtmlRenderer::isChatbot).findFirst().orElse(null);
        boolean chatInModal = chatBlock != null && opensInModal(chatBlock);
        // no modo página, os links "#chat" viram o endereço da página do chat; no modal, o CHAT_SCRIPT os intercepta
        String chatHref = chatBlock != null && !chatInModal ? HtmlUtils.htmlEscape(widgetSrc(chatBlock, "pagina"), "UTF-8") : null;
        for (LandingPageBlock block : page.getBlocks()) {
            boolean useH1 = !h1Used && block.getType() == BlockType.HERO && text(block.getContent(), "titulo") != null;
            boolean alternate = isLightSection(block) && lightSections % 2 == 1;
            String section = renderBlock(block, useH1, alternate, chatHref);
            if (!section.isEmpty()) {
                html.append(section).append('\n');
                h1Used |= useH1;
                if (isLightSection(block)) {
                    lightSections++;
                }
            }
        }
        html.append("</main>\n");
        if (chatInModal) {
            html.append(chatLauncher(chatBlock, hasChatLink(page.getBlocks()))).append('\n').append(CHAT_SCRIPT);
        } else if (chatBlock != null && showsFloatingButton(chatBlock, hasChatLink(page.getBlocks()))) {
            html.append("<a class=\"lp-chat__botao\" href=\"").append(chatHref).append("\">").append(ICON_CHAT)
                    .append(chatLabel(chatBlock)).append("</a>\n");
        }
        return html.append("</body>\n</html>\n").toString();
    }

    private static void appendSeo(StringBuilder html, Map<String, Object> seo) {
        String title = text(seo, "titulo");
        String description = text(seo, "descricao");
        String image = safeUrl(seo, "imagemCompartilhamento");
        html.append("<title>").append(title == null ? "" : title).append("</title>\n");
        if (description != null) {
            html.append("<meta name=\"description\" content=\"").append(description).append("\">\n");
        }
        if (Boolean.FALSE.equals(seo.get("indexar"))) {
            html.append("<meta name=\"robots\" content=\"noindex\">\n");
        }
        if (title != null) {
            html.append("<meta property=\"og:title\" content=\"").append(title).append("\">\n");
        }
        if (description != null) {
            html.append("<meta property=\"og:description\" content=\"").append(description).append("\">\n");
        }
        if (image != null) {
            html.append("<meta property=\"og:image\" content=\"").append(image).append("\">\n");
        }
    }

    // a cor de destaque (selo, linha colorida do título) é o brand-400 no tema padrão; com a cor da empresa,
    // é a própria cor clareada. Os valores já passaram pela regex, então podem ir para o CSS
    private static String themeVariables(Map<String, Object> theme, String font) {
        String primary = matchOr(theme, "corPrimaria", HEX_COLOR, DEFAULT_PRIMARY);
        String highlight = primary.equalsIgnoreCase(DEFAULT_PRIMARY)
                ? DEFAULT_HIGHLIGHT : "color-mix(in srgb," + primary + " 55%,#fff)";
        String background = matchOr(theme, "corFundo", HEX_COLOR, DEFAULT_BACKGROUND);
        String fontStack = "'" + font + "',ui-sans-serif,system-ui,'Segoe UI',sans-serif";
        return ":root{--lp-primaria:" + primary + ";--lp-destaque:" + highlight + ";--lp-fundo:" + background
                + ";--lp-fonte:" + fontStack + "}\n";
    }

    private static boolean isLightSection(LandingPageBlock block) {
        return block.getType() != BlockType.HERO && block.getType() != BlockType.CTA && block.getType() != BlockType.FOOTER;
    }

    private static String renderBlock(LandingPageBlock block, boolean useH1, boolean alternate, String chatHref) {
        Map<String, Object> c = block.getContent() == null ? Map.of() : block.getContent();
        StringBuilder body = new StringBuilder();
        switch (block.getType()) {
            case HERO -> {
                String selo = text(c, "selo");
                if (selo != null) {
                    body.append("<span class=\"lp-selo\">").append(ICON_SPARK).append(selo).append("</span>");
                }
                String title = text(c, "titulo");
                String highlight = text(c, "tituloDestaque");
                if (title != null) {
                    String tag = useH1 ? "h1" : "h2";
                    body.append('<').append(tag).append('>').append(title);
                    if (highlight != null) {
                        body.append(" <em>").append(highlight).append("</em>");
                    }
                    body.append("</").append(tag).append('>');
                }
                paragraph(body, "lp-subtitulo", text(c, "subtitulo"));
                button(body, text(c, "botaoTexto"), chatOr(safeUrl(c, "botaoLink"), chatHref), true);
                image(body, safeUrl(c, "imagemUrl"), text(c, "imagemAlt"));
            }
            case TEXT -> {
                heading(body, c);
                paragraphs(body, text(c, "texto"));
            }
            case IMAGE -> {
                String url = safeUrl(c, "url");
                if (url != null) {
                    heading(body, c);
                    body.append("<figure>");
                    image(body, url, text(c, "alt"));
                    String caption = text(c, "legenda");
                    if (caption != null) {
                        body.append("<figcaption>").append(caption).append("</figcaption>");
                    }
                    body.append("</figure>");
                }
            }
            case BENEFITS -> {
                StringBuilder items = new StringBuilder();
                for (Object item : items(c)) {
                    String title = item instanceof Map<?, ?> m ? text(m, "titulo") : escape(item);
                    String description = item instanceof Map<?, ?> m ? text(m, "descricao") : null;
                    if (title != null || description != null) {
                        items.append("<li>");
                        if (title != null) {
                            items.append("<strong>").append(title).append("</strong>");
                        }
                        if (description != null) {
                            items.append("<p>").append(description).append("</p>");
                        }
                        items.append("</li>");
                    }
                }
                if (!items.isEmpty() || text(c, "titulo") != null) {
                    heading(body, c);
                }
                if (!items.isEmpty()) {
                    body.append("<ul class=\"lp-lista\">").append(items).append("</ul>");
                }
            }
            case TESTIMONIALS -> {
                StringBuilder quotes = new StringBuilder();
                for (Object item : items(c)) {
                    if (item instanceof Map<?, ?> m && text(m, "texto") != null) {
                        quotes.append("<blockquote class=\"lp-depoimento\"><p>").append(text(m, "texto")).append("</p>");
                        String author = joinNonNull(text(m, "nome"), text(m, "cargo"));
                        if (author != null) {
                            quotes.append("<footer>").append(author).append("</footer>");
                        }
                        quotes.append("</blockquote>");
                    }
                }
                if (!quotes.isEmpty()) {
                    heading(body, c);
                    body.append(quotes);
                }
            }
            case FORM -> {
                // o chatbot não ocupa espaço na página: ele abre em modo foco pelos botões "#chat"
                if (isChatbot(block)) {
                    return "";
                }
                heading(body, c);
                paragraph(body, null, text(c, "descricao"));
                if (block.getForm() != null) {
                    body.append("<div class=\"lp-formulario-embed\" data-landing-form=\"")
                            .append(escape(block.getForm().getFormKey())).append("\"></div>");
                }
            }
            case CTA -> {
                heading(body, c);
                paragraph(body, null, text(c, "texto"));
                button(body, text(c, "botaoTexto"), chatOr(safeUrl(c, "botaoLink"), chatHref), false);
            }
            case FAQ -> {
                StringBuilder questions = new StringBuilder();
                for (Object item : items(c)) {
                    if (item instanceof Map<?, ?> m && text(m, "pergunta") != null) {
                        questions.append("<details><summary>").append(text(m, "pergunta")).append("</summary>");
                        paragraph(questions, null, text(m, "resposta"));
                        questions.append("</details>");
                    }
                }
                if (!questions.isEmpty()) {
                    heading(body, c);
                    body.append(questions);
                }
            }
            case FOOTER -> {
                paragraph(body, null, text(c, "texto"));
                StringBuilder links = new StringBuilder();
                for (Object item : list(c, "links")) {
                    if (item instanceof Map<?, ?> m) {
                        String label = text(m, "texto");
                        String url = safeUrl(m, "url");
                        if (label != null && url != null) {
                            links.append("<a href=\"").append(url).append("\">").append(label).append("</a>");
                        }
                    }
                }
                if (!links.isEmpty()) {
                    body.append("<nav>").append(links).append("</nav>");
                }
                return body.isEmpty() ? "" : "<footer class=\"lp-rodape\"><div class=\"lp-conteudo\">" + body + "</div></footer>";
            }
        }
        if (body.isEmpty()) {
            return "";
        }
        String css = "lp-bloco lp-" + cssName(block)
                + (block.getType() == BlockType.CTA ? " lp-escuro" : "")
                + (alternate ? " lp-alt" : "");
        return "<section class=\"" + css + "\"><div class=\"lp-conteudo\">" + body + "</div></section>";
    }

    static boolean isChatbot(LandingPageBlock block) {
        Form form = block.getForm();
        return block.getType() == BlockType.FORM && form != null && form.getRenderMode() == FormRenderMode.CHATBOT;
    }

    // algum botão da página abre o chat? (TITULO e CHAMADA_ACAO com botaoLink "#chat")
    private static boolean hasChatLink(List<LandingPageBlock> blocks) {
        return blocks.stream()
                .filter(b -> b.getType() == BlockType.HERO || b.getType() == BlockType.CTA)
                .anyMatch(b -> b.getContent() != null && text(b.getContent(), "botaoTexto") != null
                        && CHAT_LINK.equals(b.getContent().get("botaoLink") instanceof String s ? s.strip() : null));
    }

    private static boolean opensInModal(LandingPageBlock chatBlock) {
        return chatBlock.getContent() != null && OPEN_IN_MODAL.equals(chatBlock.getContent().get("abrirEm"));
    }

    // o botão flutuante só aparece se a empresa pedir (botaoFlutuante) ou se nenhum botão da página abrir o chat,
    // para ele nunca ficar inacessível
    private static boolean showsFloatingButton(LandingPageBlock chatBlock, boolean pageHasChatLink) {
        return Boolean.TRUE.equals(chatBlock.getContent() == null ? null : chatBlock.getContent().get("botaoFlutuante"))
                || !pageHasChatLink;
    }

    private static String chatLabel(LandingPageBlock chatBlock) {
        String label = text(chatBlock.getContent(), "botaoTexto");
        return label == null ? DEFAULT_CHAT_LABEL : label;
    }

    // botão com link "#chat": no modo página vai direto para o chat; nos outros casos o link fica como está
    private static String chatOr(String url, String chatHref) {
        return CHAT_LINK.equals(url) && chatHref != null ? chatHref : url;
    }

    // endereço do widget para este formulário; modo "pagina" (tela cheia) ou "embutido" (dentro do modal)
    private String widgetSrc(LandingPageBlock chatBlock, String mode) {
        return chatbotWidgetUrl + "?form=" + URLEncoder.encode(chatBlock.getForm().getFormKey(), StandardCharsets.UTF_8)
                + "&modo=" + mode;
    }

    // o chat em modo foco: fundo escuro, painel com o widget (modo=embutido), o X de reserva da página e o botão
    // flutuante. O endereço fica em data-src e só carrega no primeiro clique
    private String chatLauncher(LandingPageBlock block, boolean pageHasChatLink) {
        boolean floating = showsFloatingButton(block, pageHasChatLink);
        String src = widgetSrc(block, "embutido");
        return "<div class=\"lp-chat\" id=\"lp-chat\" data-flutuante=\"" + floating + "\">"
                + "<div class=\"lp-chat__fundo\" id=\"lp-chat-fundo\" hidden></div>"
                + "<div class=\"lp-chat__painel\" id=\"lp-chat-painel\" role=\"dialog\" aria-modal=\"true\" aria-label=\"Chat\" hidden>"
                + "<button type=\"button\" class=\"lp-chat__fechar\" id=\"lp-chat-fechar\" aria-label=\"Fechar\">" + ICON_CLOSE + "</button>"
                + "<iframe title=\"Chat\" data-src=\"" + HtmlUtils.htmlEscape(src, "UTF-8") + "\"></iframe></div>"
                + "<button type=\"button\" class=\"lp-chat__botao\" id=\"lp-chat-botao\"" + (floating ? "" : " hidden") + ">"
                + ICON_CHAT + chatLabel(block) + "</button></div>";
    }

    private static String cssName(LandingPageBlock block) {
        return switch (block.getType()) {
            case HERO -> "titulo";
            case TEXT -> "texto";
            case IMAGE -> "imagem";
            case BENEFITS -> "beneficios";
            case TESTIMONIALS -> "depoimentos";
            case FORM -> "formulario";
            case CTA -> "chamada";
            case FAQ -> "faq";
            case FOOTER -> "rodape";
        };
    }

    // sobretítulo (linha pequena em destaque) + título da seção
    private static void heading(StringBuilder body, Map<?, ?> content) {
        String overline = text(content, "sobretitulo");
        if (overline != null) {
            body.append("<span class=\"lp-sobretitulo\">").append(overline).append("</span>");
        }
        String title = text(content, "titulo");
        if (title != null) {
            body.append("<h2>").append(title).append("</h2>");
        }
    }

    private static void paragraph(StringBuilder body, String cssClass, String text) {
        if (text != null) {
            body.append(cssClass == null ? "<p>" : "<p class=\"" + cssClass + "\">").append(text).append("</p>");
        }
    }

    // linha em branco separa parágrafos; quebra simples vira <br>; o texto já chega escapado
    private static void paragraphs(StringBuilder body, String text) {
        if (text == null) {
            return;
        }
        for (String part : text.split("\\R\\s*\\R")) {
            if (!part.isBlank()) {
                body.append("<p>").append(part.strip().replaceAll("\\R", "<br>")).append("</p>");
            }
        }
    }

    private static void image(StringBuilder body, String url, String alt) {
        if (url != null) {
            body.append("<img src=\"").append(url).append("\" alt=\"").append(alt == null ? "" : alt)
                    .append("\" loading=\"lazy\">");
        }
    }

    private static void button(StringBuilder body, String label, String url, boolean withArrow) {
        if (label != null && url != null) {
            body.append("<a class=\"lp-botao\" href=\"").append(url).append("\">").append(label)
                    .append(withArrow ? ICON_ARROW : "").append("</a>");
        }
    }

    private static List<?> items(Map<?, ?> content) {
        return list(content, "itens");
    }

    private static List<?> list(Map<?, ?> content, String key) {
        return content.get(key) instanceof List<?> l ? l : List.of();
    }

    // valor de texto, já escapado; o que não for texto ou estiver em branco vira null
    static String text(Map<?, ?> content, String key) {
        if (content == null) {
            return null;
        }
        return escape(content.get(key));
    }

    private static String escape(Object value) {
        if (!(value instanceof String s) || s.isBlank()) {
            return null;
        }
        return HtmlUtils.htmlEscape(s.strip(), "UTF-8");
    }

    // só deixa passar http(s), mailto:, tel: e âncora; javascript:, data: e afins viram null
    static String safeUrl(Map<?, ?> content, String key) {
        if (content == null || !(content.get(key) instanceof String raw)) {
            return null;
        }
        String url = raw.strip();
        return SAFE_URL.matcher(url).matches() ? HtmlUtils.htmlEscape(url, "UTF-8") : null;
    }

    private static String matchOr(Map<String, Object> map, String key, Pattern pattern, String fallback) {
        if (map != null && map.get(key) instanceof String value && pattern.matcher(value).matches()) {
            return value;
        }
        return fallback;
    }

    private static String joinNonNull(String first, String second) {
        if (first == null) {
            return second;
        }
        return second == null ? first : first + ", " + second;
    }
}
