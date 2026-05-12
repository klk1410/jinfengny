<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { requestJson } from "../api.js";
import "./promo/promo-form.css";
import { entityOnOffPillClass } from "../utils/statusDisplay.js";

const shell = inject("appShell");
const route = useRoute();
const router = useRouter();

const err = ref("");
const loading = ref(true);
const pack = ref(null);

const salesmanId = computed(() => Number(route.params.salesmanId));

const salesman = computed(() => pack.value?.salesman ?? null);
const portalOpenid = computed(() => pack.value?.portalOpenid ?? null);
const profile = computed(() => pack.value?.profile ?? null);
const shares = computed(() => pack.value?.shares ?? []);
const accountNote = computed(() => pack.value?.accountNote ?? "");
const org = computed(() => profile.value?.orgInfo ?? {});

async function load() {
  err.value = "";
  loading.value = true;
  pack.value = null;
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    const sid = salesmanId.value;
    if (!sid) {
      throw new Error("运维 ID 无效");
    }
    pack.value = await requestJson(`/app-api/biz/salesmen/portal-account?openid=${oid}&salesmanId=${sid}`);
  } catch (e) {
    err.value = e.message || String(e);
    pack.value = null;
  } finally {
    loading.value = false;
  }
}

watch(
  () => [shell.loginOpenid, route.params.salesmanId],
  () => load(),
  { immediate: false }
);

onMounted(load);
</script>

<template>
  <div class="pf-page">
    <p v-if="loading" class="pf-muted">加载中…</p>
    <template v-else>
      <p v-if="err" class="pf-err">{{ err }}</p>

      <template v-if="!err && salesman">
        <button type="button" class="link-back" @click="router.push({ name: 'salesmen' })">← 返回运维列表</button>

        <div class="dc-stack" style="margin-top: 8px">
          <article class="dc-card dc-card--white">
            <div class="pf-line-strong">运维档案</div>
            <div class="pf-line-muted" style="margin-top: 8px">
              #{{ salesman.salesmanId }} {{ salesman.salesmanName }}
              <span :class="entityOnOffPillClass(salesman.statusCode)" style="margin-left: 8px">{{ salesman.status }}</span>
            </div>
            <div class="pf-line-muted">电话 {{ salesman.phone || "—" }} · 代理 #{{ salesman.agentId }}</div>
          </article>

          <template v-if="portalOpenid && profile">
            <div class="pf-panel-title" style="margin: 4px 0 0; font-size: 14px">小程序账户</div>
            <article class="dc-card dc-card--white">
              <div class="pf-line-strong">当前角色</div>
              <div class="pf-line-muted">{{ profile.roleName || "—" }}</div>
            </article>
            <article class="dc-card dc-card--white">
              <div class="pf-line-strong">openid</div>
              <div class="pf-line-muted" style="word-break: break-all">{{ profile.openid || "—" }}</div>
            </article>
            <article class="dc-card dc-card--white">
              <div class="pf-line-strong">绑定时间（可选）</div>
              <div class="pf-line-muted">{{ profile.bindTime || "—" }}</div>
            </article>
            <article class="dc-card dc-card--white">
              <div class="pf-line-strong">姓名</div>
              <div class="pf-line-muted">{{ org.salesmanName || "—" }}</div>
            </article>
            <article class="dc-card dc-card--white">
              <div class="pf-line-strong">电话</div>
              <div class="pf-line-muted">{{ org.phone || "—" }}</div>
            </article>
            <article class="dc-card dc-card--white">
              <div class="pf-line-strong">所属代理</div>
              <div class="pf-line-muted">{{ org.agentName || "—" }}</div>
            </article>

            <div class="pf-panel-title" style="margin: 6px 0 0; font-size: 14px">共享账号</div>
            <article v-if="!shares.length" class="dc-card dc-card--white">
              <div class="pf-line-muted">暂无共享账号</div>
            </article>
            <article v-for="s in shares" :key="s.shareId" class="dc-card dc-card--white">
              <div class="pf-line-muted" style="word-break: break-all">
                {{ s.sharedOpenid }} · {{ s.roleName }} · {{ s.createTime }}
              </div>
            </article>
          </template>

          <article v-else class="dc-card dc-card--white">
            <div class="pf-line-strong">小程序账户</div>
            <div class="pf-line-muted" style="margin-top: 8px">{{ accountNote || "暂无门户绑定信息。" }}</div>
          </article>
        </div>
      </template>
    </template>
  </div>
</template>

<style scoped>
.link-back {
  border: none;
  background: none;
  color: #1f6dff;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
</style>
