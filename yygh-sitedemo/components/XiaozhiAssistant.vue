<template>
  <div class="xiaozhi-assistant">
    <transition name="xiaozhi-panel">
      <section
        v-if="open"
        class="xiaozhi-panel"
        role="dialog"
        aria-label="硅谷小智预约助手"
      >
        <header class="xiaozhi-header">
          <div class="xiaozhi-identity">
            <span class="xiaozhi-mark" aria-hidden="true">智</span>
            <div>
              <h2>硅谷小智</h2>
              <p>
                <span class="xiaozhi-status-dot" :class="statusClass" aria-hidden="true" />
                {{ statusText }}
              </p>
            </div>
          </div>
          <button class="xiaozhi-icon-button" type="button" aria-label="关闭硅谷小智" @click="open = false">
            <i class="el-icon-close" aria-hidden="true" />
          </button>
        </header>

        <div ref="messageList" class="xiaozhi-messages" aria-live="polite">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="xiaozhi-message-row"
            :class="'is-' + message.role"
          >
            <div class="xiaozhi-message">
              {{ message.content }}
            </div>
          </div>
          <div v-if="sending" class="xiaozhi-message-row is-assistant">
            <div class="xiaozhi-message xiaozhi-thinking" aria-label="硅谷小智正在思考">
              <span />
              <span />
              <span />
            </div>
          </div>
        </div>

        <div v-if="showQuickActions" class="xiaozhi-quick-actions" aria-label="快捷功能">
          <button v-for="action in quickActions" :key="action.text" type="button" @click="sendQuickAction(action.prompt)">
            <i :class="action.icon" aria-hidden="true" />
            {{ action.text }}
          </button>
        </div>

        <div class="xiaozhi-privacy">
          <i class="el-icon-lock" aria-hidden="true" />
          预约前会再次确认；请勿发送完整证件号、密码或验证码
        </div>

        <form class="xiaozhi-composer" @submit.prevent="sendMessage">
          <textarea
            ref="input"
            v-model="draft"
            rows="2"
            maxlength="500"
            aria-label="向硅谷小智提问"
            placeholder="描述症状，或告诉我科室、日期和时段…"
            :disabled="sending"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <button type="submit" :disabled="sending || !draft.trim()" aria-label="发送消息">
            <i class="el-icon-s-promotion" aria-hidden="true" />
          </button>
        </form>
      </section>
    </transition>

    <button
      class="xiaozhi-launcher"
      type="button"
      :aria-expanded="String(open)"
      aria-label="打开硅谷小智预约助手"
      @click="toggle"
    >
      <span class="xiaozhi-launcher-mark" aria-hidden="true">智</span>
      <span>硅谷小智</span>
    </button>
  </div>
</template>

<script>
import xiaozhiApi from '~/api/xiaozhi'

const MEMORY_KEY = 'xiaozhi-memory-id'

export default {
  name: 'XiaozhiAssistant',

  data () {
    return {
      open: false,
      sending: false,
      serviceStatus: 'checking',
      draft: '',
      memoryId: null,
      messages: [
        {
          role: 'assistant',
          content: '你好，我是硅谷小智。可以帮你智能导诊、查询号源，也能在登录后协助预约或取消挂号。'
        }
      ],
      quickActions: [
        { text: '智能导诊', prompt: '请根据我的症状帮我推荐就诊科室', icon: 'el-icon-first-aid-kit' },
        { text: '查询号源', prompt: '我想查询号源', icon: 'el-icon-date' },
        { text: '预约挂号', prompt: '我想预约挂号', icon: 'el-icon-circle-check' },
        { text: '取消预约', prompt: '我想取消预约', icon: 'el-icon-refresh-left' }
      ]
    }
  },

  computed: {
    statusText () {
      if (this.serviceStatus === 'online') return '可导诊、查号源和预约'
      if (this.serviceStatus === 'offline') return 'Agent 服务未启动'
      return '正在检查服务状态'
    },

    statusClass () {
      return 'is-' + this.serviceStatus
    },

    showQuickActions () {
      return !this.sending && this.messages.filter(message => message.role === 'user').length === 0
    }
  },

  mounted () {
    this.memoryId = this.getMemoryId()
    this.checkStatus()
  },

  methods: {
    getMemoryId () {
      const stored = window.sessionStorage.getItem(MEMORY_KEY)
      if (stored && /^\d+$/.test(stored)) {
        return Number(stored)
      }
      let memoryId
      if (window.crypto && window.crypto.getRandomValues) {
        const values = new Uint32Array(2)
        window.crypto.getRandomValues(values)
        memoryId = (values[0] & 0x1fffff) * 0x100000000 + values[1]
      } else {
        memoryId = Date.now() * 1000 + Math.floor(Math.random() * 1000)
      }
      window.sessionStorage.setItem(MEMORY_KEY, String(memoryId))
      return memoryId
    },

    checkStatus () {
      xiaozhiApi.status().then(response => {
        this.serviceStatus = response && response.data && response.data.modelStatus === 'online'
          ? 'online'
          : 'offline'
      }).catch(() => {
        this.serviceStatus = 'offline'
      })
    },

    toggle () {
      this.open = !this.open
      if (this.open) {
        this.$nextTick(() => {
          this.scrollToBottom()
          this.$refs.input && this.$refs.input.focus()
        })
      }
    },

    sendQuickAction (prompt) {
      this.draft = prompt
      this.sendMessage()
    },

    sendMessage () {
      const message = this.draft.trim()
      if (!message || this.sending || !this.memoryId) return

      this.messages.push({ role: 'user', content: message })
      this.draft = ''
      this.sending = true
      this.scrollToBottom()

      xiaozhiApi.chat(this.memoryId, message).then(response => {
        const content = typeof response === 'string'
          ? response
          : (response && response.data && response.data.answer) || '硅谷小智暂时没有返回内容'
        this.messages.push({ role: 'assistant', content })
        this.serviceStatus = 'online'
      }).catch(error => {
        this.messages.push({ role: 'error', content: error.message })
        this.serviceStatus = 'offline'
      }).finally(() => {
        this.sending = false
        this.scrollToBottom()
      })
    },

    scrollToBottom () {
      this.$nextTick(() => {
        const list = this.$refs.messageList
        if (list) list.scrollTop = list.scrollHeight
      })
    }
  }
}
</script>

<style scoped>
.xiaozhi-assistant {
  --xiaozhi-brand: #4490f1;
  --xiaozhi-brand-strong: #2878dc;
  --xiaozhi-brand-soft: #edf6ff;
  --xiaozhi-canvas: #f6f9fc;
  --xiaozhi-surface: #ffffff;
  --xiaozhi-ink: #26364a;
  --xiaozhi-ink-secondary: #607086;
  --xiaozhi-ink-muted: #8e9bad;
  --xiaozhi-border: rgba(42, 76, 112, 0.12);
  --xiaozhi-danger-soft: #fff3f2;
  --xiaozhi-danger: #c74d45;
  position: relative;
  z-index: 2100;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  -webkit-font-smoothing: antialiased;
}

.xiaozhi-launcher {
  position: fixed;
  right: 28px;
  bottom: 28px;
  display: inline-flex;
  align-items: center;
  min-height: 48px;
  padding: 6px 18px 6px 7px;
  border: 0;
  border-radius: 24px;
  color: var(--xiaozhi-surface);
  background: var(--xiaozhi-brand);
  box-shadow: 0 0 0 1px rgba(31, 102, 184, 0.08), 0 8px 24px rgba(44, 116, 198, 0.22);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background-color 160ms ease-out, transform 140ms ease-out, box-shadow 160ms ease-out;
}

.xiaozhi-launcher:hover {
  background: var(--xiaozhi-brand-strong);
  box-shadow: 0 0 0 1px rgba(31, 102, 184, 0.1), 0 10px 28px rgba(44, 116, 198, 0.28);
}

.xiaozhi-launcher:active {
  transform: scale(0.97);
}

.xiaozhi-launcher:focus-visible,
.xiaozhi-icon-button:focus-visible,
.xiaozhi-quick-actions button:focus-visible,
.xiaozhi-composer button:focus-visible,
.xiaozhi-composer textarea:focus-visible {
  outline: 3px solid rgba(68, 144, 241, 0.28);
  outline-offset: 2px;
}

.xiaozhi-launcher-mark,
.xiaozhi-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-weight: 700;
}

.xiaozhi-launcher-mark {
  width: 36px;
  height: 36px;
  margin-right: 8px;
  border-radius: 18px;
  color: var(--xiaozhi-brand);
  background: var(--xiaozhi-surface);
}

.xiaozhi-panel {
  position: fixed;
  right: 28px;
  bottom: 88px;
  display: flex;
  flex-direction: column;
  width: 380px;
  height: min(620px, calc(100vh - 116px));
  overflow: hidden;
  border-radius: 16px;
  color: var(--xiaozhi-ink);
  background: var(--xiaozhi-surface);
  box-shadow: 0 0 0 1px var(--xiaozhi-border), 0 18px 50px rgba(37, 70, 108, 0.18);
  transform-origin: calc(100% - 46px) 100%;
}

.xiaozhi-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 0 0 auto;
  min-height: 72px;
  padding: 12px 12px 12px 16px;
  background: var(--xiaozhi-brand-soft);
}

.xiaozhi-identity {
  display: flex;
  align-items: center;
  min-width: 0;
}

.xiaozhi-mark {
  width: 44px;
  height: 44px;
  margin-right: 12px;
  border-radius: 12px;
  color: var(--xiaozhi-surface);
  background: var(--xiaozhi-brand);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.24);
  font-size: 18px;
}

.xiaozhi-identity h2 {
  margin: 0 0 5px;
  color: var(--xiaozhi-ink);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.2px;
  line-height: 1.2;
}

.xiaozhi-identity p {
  display: flex;
  align-items: center;
  margin: 0;
  color: var(--xiaozhi-ink-secondary);
  font-size: 12px;
  line-height: 1.3;
}

.xiaozhi-status-dot {
  width: 7px;
  height: 7px;
  margin-right: 6px;
  border-radius: 50%;
  background: #9aa7b7;
}

.xiaozhi-status-dot.is-online {
  background: #31a66a;
}

.xiaozhi-status-dot.is-offline {
  background: var(--xiaozhi-danger);
}

.xiaozhi-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 8px;
  color: var(--xiaozhi-ink-secondary);
  background: transparent;
  cursor: pointer;
  font-size: 18px;
  transition: color 140ms ease-out, background-color 140ms ease-out;
}

.xiaozhi-icon-button:hover {
  color: var(--xiaozhi-ink);
  background: rgba(68, 144, 241, 0.09);
}

.xiaozhi-messages {
  flex: 1 1 auto;
  min-height: 0;
  padding: 18px 16px;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: var(--xiaozhi-canvas);
}

.xiaozhi-message-row {
  display: flex;
  margin-bottom: 12px;
}

.xiaozhi-message-row:last-child {
  margin-bottom: 0;
}

.xiaozhi-message-row.is-user {
  justify-content: flex-end;
}

.xiaozhi-message {
  max-width: 84%;
  padding: 10px 12px;
  border-radius: 4px 12px 12px;
  color: var(--xiaozhi-ink);
  background: var(--xiaozhi-surface);
  box-shadow: 0 0 0 1px var(--xiaozhi-border), 0 2px 6px rgba(40, 73, 109, 0.05);
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.is-user .xiaozhi-message {
  border-radius: 12px 4px 12px 12px;
  color: var(--xiaozhi-surface);
  background: var(--xiaozhi-brand);
  box-shadow: none;
}

.is-error .xiaozhi-message {
  color: var(--xiaozhi-danger);
  background: var(--xiaozhi-danger-soft);
  box-shadow: 0 0 0 1px rgba(199, 77, 69, 0.12);
}

.xiaozhi-thinking {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
}

.xiaozhi-thinking span {
  width: 5px;
  height: 5px;
  margin-right: 5px;
  border-radius: 50%;
  background: var(--xiaozhi-ink-muted);
  animation: xiaozhi-thinking 1s ease-in-out infinite;
}

.xiaozhi-thinking span:nth-child(2) {
  animation-delay: 120ms;
}

.xiaozhi-thinking span:nth-child(3) {
  margin-right: 0;
  animation-delay: 240ms;
}

.xiaozhi-quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  flex: 0 0 auto;
  gap: 8px;
  padding: 12px 16px 4px;
  background: var(--xiaozhi-surface);
}

.xiaozhi-quick-actions button {
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 8px 10px;
  border: 1px solid var(--xiaozhi-border);
  border-radius: 8px;
  color: var(--xiaozhi-ink-secondary);
  background: var(--xiaozhi-surface);
  cursor: pointer;
  font-size: 13px;
  transition: color 140ms ease-out, border-color 140ms ease-out, background-color 140ms ease-out;
}

.xiaozhi-quick-actions button:hover {
  border-color: rgba(68, 144, 241, 0.32);
  color: var(--xiaozhi-brand-strong);
  background: var(--xiaozhi-brand-soft);
}

.xiaozhi-quick-actions i {
  margin-right: 7px;
  color: var(--xiaozhi-brand);
  font-size: 15px;
}

.xiaozhi-privacy {
  flex: 0 0 auto;
  padding: 8px 16px 4px;
  color: var(--xiaozhi-ink-muted);
  background: var(--xiaozhi-surface);
  font-size: 11px;
  line-height: 1.45;
}

.xiaozhi-privacy i {
  margin-right: 3px;
}

.xiaozhi-composer {
  display: flex;
  align-items: flex-end;
  flex: 0 0 auto;
  gap: 8px;
  padding: 8px 12px 12px;
  background: var(--xiaozhi-surface);
}

.xiaozhi-composer textarea {
  flex: 1 1 auto;
  min-height: 44px;
  max-height: 92px;
  padding: 10px 12px;
  resize: none;
  border: 1px solid var(--xiaozhi-border);
  border-radius: 10px;
  outline: none;
  color: var(--xiaozhi-ink);
  background: var(--xiaozhi-canvas);
  font-family: inherit;
  font-size: 14px;
  line-height: 1.5;
  transition: border-color 140ms ease-out, box-shadow 140ms ease-out;
}

.xiaozhi-composer textarea:focus {
  border-color: rgba(68, 144, 241, 0.52);
  box-shadow: 0 0 0 3px rgba(68, 144, 241, 0.1);
}

.xiaozhi-composer textarea::placeholder {
  color: var(--xiaozhi-ink-muted);
}

.xiaozhi-composer button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 10px;
  color: var(--xiaozhi-surface);
  background: var(--xiaozhi-brand);
  cursor: pointer;
  font-size: 17px;
  transition: background-color 140ms ease-out, transform 120ms ease-out, opacity 140ms ease-out;
}

.xiaozhi-composer button:hover:not(:disabled) {
  background: var(--xiaozhi-brand-strong);
}

.xiaozhi-composer button:active:not(:disabled) {
  transform: scale(0.97);
}

.xiaozhi-composer button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.xiaozhi-panel-enter-active,
.xiaozhi-panel-leave-active {
  transition: opacity 180ms cubic-bezier(0.23, 1, 0.32, 1), transform 180ms cubic-bezier(0.23, 1, 0.32, 1);
}

.xiaozhi-panel-enter,
.xiaozhi-panel-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.97);
}

@keyframes xiaozhi-thinking {
  0%, 60%, 100% { opacity: 0.35; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-2px); }
}

@media (max-width: 600px) {
  .xiaozhi-launcher {
    right: 16px;
    bottom: 16px;
  }

  .xiaozhi-panel {
    right: 12px;
    bottom: 76px;
    width: calc(100vw - 24px);
    height: min(640px, calc(100vh - 92px));
    border-radius: 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .xiaozhi-panel-enter-active,
  .xiaozhi-panel-leave-active,
  .xiaozhi-launcher,
  .xiaozhi-thinking span {
    animation: none;
    transition: opacity 120ms ease-out;
  }
}
</style>
