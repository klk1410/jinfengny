<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";
import "../styles/deviceStatusBadge.css";
import { deviceStatusPillClass } from "../utils/deviceStatusDisplay.js";

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
  <div class="page">
    <h2 class="page-title">设备</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="card">
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-else class="dc-stack">
        <article v-for="(d, i) in rows" :key="i" class="dc-card dc-card--white">
          <div class="line strong">{{ d.deviceNo }}</div>
          <div class="line meta-row">
            <span>{{ d.deviceType }}</span>
            <span :class="deviceStatusPillClass(d.deviceStatusCode)">{{ d.deviceStatus }}</span>
          </div>
          <div class="line muted">
            门店 {{ d.merchantName || "—" }}{{ d.merchantId != null ? "（" + d.merchantId + "）" : "" }} · 代理
            {{ d.agentName ? d.agentName + "（" + d.agentId + "）" : "#" + d.agentId }}
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 600;
}
.err {
  color: #b91c1c;
  font-size: 13px;
}
.card {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}
.muted {
  color: #64748b;
  font-size: 12px;
}
.line {
  margin-top: 4px;
}
.line:first-child {
  margin-top: 0;
}
.strong {
  font-weight: 600;
}
.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
