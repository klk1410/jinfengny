<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";
import "../promo/promo-form.css";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/biz/devices?openid=${encodeURIComponent(oid)}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
  }
}

watch(() => shell.loginOpenid, load);
onMounted(load);
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>
    <p class="pf-muted" style="margin: 0 0 12px">监测在库设备状态（与业务设备表同步）。</p>

    <div class="pf-toolbar">
      <button type="button" class="pf-tool pf-tool--ghost" @click="load">刷新</button>
    </div>

    <div class="pf-card">
      <div v-if="!rows.length" class="pf-muted" style="padding: 12px">暂无数据</div>
      <div v-for="(d, i) in rows" :key="i" class="pf-item">
        <div class="pf-line-strong">{{ d.deviceNo }}</div>
        <div class="pf-line-muted">{{ d.deviceType }} · {{ d.deviceStatus }}</div>
        <div class="pf-line-muted">门店 {{ d.merchantName || "—" }} · 代理 #{{ d.agentId }}</div>
      </div>
    </div>
  </div>
</template>
