// Widget do chatbot: cliente "leve" da API pública. Só guarda o sessionId e exibe o que o servidor devolve;
// ordem das perguntas, textos e fim da conversa são decididos no backend.
(function () {
  'use strict';

  var API = '/api/landing/public/chat';
  var MESSAGES = window.CHATBOT_MESSAGES || {};

  var chat = document.getElementById('chat');
  var body = document.getElementById('chat-body');
  var log = document.getElementById('chat-log');
  var status = document.getElementById('chat-status');
  var composer = document.getElementById('chat-composer');
  var input = document.getElementById('chat-input');
  var send = document.getElementById('chat-send');
  var done = document.getElementById('chat-done');
  var restart = document.getElementById('chat-restart');
  var closeBtn = document.getElementById('chat-close');
  var openBtn = document.getElementById('chat-open');

  var formKey = new URLSearchParams(window.location.search).get('form');
  var sessionId = null;
  var questionCount = 0;
  var busy = false;
  var finished = false;

  // i18n: mesma ideia do MessageSource do backend, chave -> texto com {0}, {1}...
  function t(key) {
    var args = Array.prototype.slice.call(arguments, 1);
    var text = MESSAGES[key] || key;
    return text.replace(/\{(\d+)\}/g, function (_, i) { return args[i] !== undefined ? args[i] : ''; });
  }

  function applyI18n() {
    document.querySelectorAll('[data-i18n]').forEach(function (el) {
      el.textContent = t(el.getAttribute('data-i18n'));
    });
    document.querySelectorAll('[data-i18n-placeholder]').forEach(function (el) {
      el.setAttribute('placeholder', t(el.getAttribute('data-i18n-placeholder')));
    });
    document.querySelectorAll('[data-i18n-aria-label]').forEach(function (el) {
      el.setAttribute('aria-label', t(el.getAttribute('data-i18n-aria-label')));
    });
  }

  function addMessage(kind, text) {
    var div = document.createElement('div');
    div.className = 'msg msg--' + kind;
    div.textContent = text;
    log.appendChild(div);
    body.scrollTop = body.scrollHeight;
  }

  // mensagens do bot com fieldName são perguntas; é assim que o widget sabe em qual pergunta está
  function showBotMessages(messages) {
    messages.forEach(function (m) {
      addMessage('bot', m.content);
      if (m.fieldName) {
        questionCount++;
      }
    });
    status.textContent = t('chatbot.widget.status.question', questionCount);
  }

  function setBusy(value) {
    busy = value;
    input.disabled = value || finished;
    send.disabled = value || finished;
  }

  function showDone() {
    finished = true;
    composer.hidden = true;
    done.hidden = false;
    setBusy(false);
  }

  // Envelope padrão da API: { success, data, message, errors }
  function call(path, payload) {
    var options = { method: 'POST', headers: {} };
    if (payload) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(payload);
    }
    return fetch(API + path, options)
      .catch(function () { throw new Error(t('chatbot.widget.error.network')); })
      .then(function (res) {
        return res.json()
          .catch(function () { return null; })
          .then(function (json) {
            if (!res.ok || !json || !json.success) {
              throw new Error(errorText(json));
            }
            return json.data;
          });
      });
  }

  // erros de validação vêm como "campo: mensagem"; para o visitante basta a mensagem
  function errorText(json) {
    if (!json) {
      return t('chatbot.widget.error.network');
    }
    if (json.errors && json.errors.length) {
      return json.errors[0].replace(/^[^:]+:\s*/, '');
    }
    return json.message || t('chatbot.widget.error.network');
  }

  function start() {
    log.innerHTML = '';
    sessionId = null;
    questionCount = 0;
    finished = false;
    composer.hidden = false;
    done.hidden = true;
    status.textContent = t('chatbot.widget.status.starting');

    if (!formKey) {
      addMessage('error', t('chatbot.widget.error.no-form'));
      finished = true;
      setBusy(false);
      status.textContent = '';
      return;
    }

    setBusy(true);
    call('/' + encodeURIComponent(formKey) + '/iniciar')
      .then(function (data) {
        sessionId = data.sessionId;
        showBotMessages(data.messages);
        setBusy(false);
        input.focus();
      })
      .catch(function (err) {
        addMessage('error', err.message);
        status.textContent = '';
        showDone();
      });
  }

  function answer(text) {
    addMessage('user', text);
    input.value = '';
    setBusy(true);
    call('/' + sessionId + '/responder', { answer: text })
      .then(function (data) {
        showBotMessages(data.messages);
        if (data.status === 'COMPLETED') {
          addMessage('system', t('chatbot.widget.system.completed'));
          status.textContent = t('chatbot.widget.status.done');
          showDone();
          return;
        }
        setBusy(false);
        input.focus();
      })
      .catch(function (err) {
        addMessage('error', err.message);
        setBusy(false);
        input.focus();
      });
  }

  composer.addEventListener('submit', function (e) {
    e.preventDefault();
    var text = input.value.trim();
    if (!text || busy || finished || !sessionId) {
      return;
    }
    answer(text);
  });

  restart.addEventListener('click', start);

  closeBtn.addEventListener('click', function () {
    chat.hidden = true;
    openBtn.hidden = false;
    openBtn.focus();
  });

  openBtn.addEventListener('click', function () {
    openBtn.hidden = true;
    chat.hidden = false;
    if (!input.disabled) {
      input.focus();
    }
  });

  applyI18n();
  start();
})();
