<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";
import { entityOnOffPillClass } from "../utils/statusDisplay.js";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/biz/merchants?openid=${encodeURIComponent(oid)}`);
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
    <h2 class="page-title">商家</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="card">
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-for="(m, i) in rows" :key="i" class="item">
        <div class="row-head">
          <span class="line strong">#{{ m.merchantId }} {{ m.merchantName }}</span>
          <span :class="entityOnOffPillClass(m.statusCode)">{{ m.status }}</span>
        </div>
        <div class="line">{{ m.contactName }} {{ m.contactPhone }}</div>
        <div class="line muted">{{ m.city }} · {{ m.agentName }} · {{ m.salesmanName || "—" }}</div>
        <div class="line amt-line">单价 ¥{{ m.oilUnitPrice }} · 欠费 ¥{{ m.arrearsAmount }}</div>
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
.item {
  border-top: 1px solid #eef1f6;
  padding: 10px 0;
  font-size: 12px;
}
.item:first-of-type {
  border-top: none;
  padding-top: 0;
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
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
.amt-line {
  color: #334155;
}
</style>
