<script setup>
import { computed, onMounted, provide, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { requestJson } from "../api.js";

const router = useRouter();
const route = useRoute();

const loginOpenid = ref("merchant-openid-001");
const portal = ref(null);
const loadError = ref("");
const loading = ref(false);

const canOpenBusiness = computed(() => !!portal.value?.hasBusinessAccess);
const portalSections = computed(() => portal.value?.sections || []);
const roleLabel = computed(() => portal.value?.role || "—");
const isHome = computed(() => route.name === "home");

async function refreshPortal() {
  loadError.value = "";
  loading.value = true;
  try {
    await requestJson("/app-api/auth/wechat-login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ openid: loginOpenid.value })
    });
    portal.value = await requestJson(
      `/app-api/portal/modules?openid=${encodeURIComponent(loginOpenid.value)}`
    );
  } catch (e) {
    portal.value = null;
    loadError.value = e.message || String(e);
  } finally {
    loading.value = false;
  }
}

function onHeaderBack() {
  if (isHome.value) {
    return;
  }
  router.push({ name: "home" });
}

provide(
  "appShell",
  reactive({
    loginOpenid,
    portal,
    loadError,
    loading,
    refreshPortal,
    canOpenBusiness,
    portalSections,
    roleLabel
  })
);

onMounted(() => {
  refreshPortal();
});
</script>

<template>
  <div class="shell">
    <header class="mp-header">
      <button type="button" class="nav-btn" :class="{ 'nav-btn--muted': isHome }" aria-label="返回" @click="onHeaderBack">
        ‹
      </button>
      <h1 class="mp-title">环保油管理</h1>
      <div class="mp-actions" aria-hidden="true">
        <span class="caps">⋯</span>
        <span class="ring" />
      </div>
    </header>

    <div class="body">
      <div class="bulletin">
        <span class="bulletin-tag">通知</span>
        <p class="bulletin-text">
          请各位代理及时跟进异常门店与设备回收，避免产生不必要的费用与纠纷。功能入口以九宫格为准，由管理后台统一配置。
        </p>
      </div>

      <div class="dev-strip">
        <span class="dev-label">测试 openid</span>
        <input v-model="loginOpenid" class="dev-input" type="text" autocomplete="off" />
        <button type="button" class="dev-btn" :disabled="loading" @click="refreshPortal">
          {{ loading ? "…" : "刷新" }}
        </button>
        <span class="dev-role">角色：{{ roleLabel }}</span>
      </div>

      <div v-if="loadError" class="banner-err">{{ loadError }}</div>

      <main class="main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  background: #eef1f6;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  color: #1a1a1a;
}

.mp-header {
  display: grid;
  grid-template-columns: 44px 1fr 56px;
  align-items: center;
  padding: 10px 10px calc(10px + env(safe-area-inset-top, 0px));
  background: linear-gradient(180deg, #2d7cff 0%, #1f6dff 100%);
  color: #fff;
  box-shadow: 0 2px 10px rgba(31, 109, 255, 0.35);
}

.nav-btn {
  border: none;
  background: transparent;
  color: #fff;
  font-size: 28px;
  line-height: 1;
  padding: 4px 8px;
  cursor: pointer;
  opacity: 0.95;
}

.nav-btn--muted {
  opacity: 0.35;
  cursor: default;
}

.mp-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  text-align: center;
  letter-spacing: 0.02em;
}

.mp-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding-right: 4px;
}

.caps {
  font-size: 15px;
  letter-spacing: 2px;
  opacity: 0.95;
}

.ring {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  box-sizing: border-box;
}

.body {
  max-width: 480px;
  margin: 0 auto;
  padding: 0 0 28px;
}

.bulletin {
  margin: 0;
  padding: 12px 14px 14px;
  background: radial-gradient(ellipse 120% 80% at 50% 0%, #3a3a3a 0%, #121212 55%, #0a0a0a 100%);
  color: #f5e6b8;
  font-size: 12px;
  line-height: 1.55;
  border-bottom: 1px solid rgba(212, 175, 55, 0.25);
}

.bulletin-tag {
  display: inline-block;
  margin-bottom: 6px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(212, 175, 55, 0.2);
  color: #ffd666;
  font-size: 11px;
}

.bulletin-text {
  margin: 0;
  text-shadow: 0 0 1px rgba(0, 0, 0, 0.5);
}

.dev-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e4e8ef;
  font-size: 12px;
}

.dev-label {
  color: #64748b;
  white-space: nowrap;
}

.dev-input {
  flex: 1;
  min-width: 120px;
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 12px;
}

.dev-btn {
  border: none;
  background: #1f6dff;
  color: #fff;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}

.dev-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.dev-role {
  width: 100%;
  color: #94a3b8;
  font-size: 11px;
}

.banner-err,
.banner-warn {
  margin: 10px 12px 0;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.banner-err {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.main {
  padding: 10px 12px 0;
}
</style>
