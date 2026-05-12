<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";
import "../promo/promo-form.css";
import "../../styles/deviceStatusBadge.css";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/biz/device-events?openid=${encodeURIComponent(oid)}`);
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
    <p class="pf-muted" style="margin: 0 0 8px">
      设备入库、转移、移除、报废等流水记录。移除与报废请在<strong>设备管理</strong>中操作；跨店转移设备请走<strong>转移商家</strong>订单并在工单完工时填写设备编号。
    </p>
    <div class="device-status-legend pf-muted" style="margin: 0 0 12px">
      <span style="margin-right: 6px">状态色标</span>
      <span class="ds-pill ds-pill--0">在库</span>
      <span class="ds-pill ds-pill--1">在店</span>
      <span class="ds-pill ds-pill--2">维修</span>
      <span class="ds-pill ds-pill--3">停用</span>
      <span class="ds-pill ds-pill--4">报废</span>
    </div>

    <div class="pf-toolbar">
      <button type="button" class="pf-tool pf-tool--ghost" @click="load">刷新</button>
    </div>

    <div class="pf-card">
      <div v-if="!rows.length" class="pf-muted" style="padding: 12px">暂无记录</div>
      <div v-for="(r, i) in rows" :key="i" class="pf-item">
        <div class="pf-line-strong">{{ r.deviceNo }} · {{ r.eventType }}</div>
        <div class="pf-line-muted">{{ r.createTime }} · 代理 #{{ r.agentId }}</div>
        <div class="pf-line-muted">门店 {{ r.merchantName || r.merchantId || "—" }}</div>
        <div v-if="r.remark" class="pf-line-muted">{{ r.remark }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.device-status-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
</style>
