<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../../api.js";

const shell = inject("appShell");
const router = useRouter();
const rows = ref([]);
const err = ref("");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/promo/coops?openid=${encodeURIComponent(oid)}`);
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
    <h2 class="page-title">推广 · 合作管理</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="toolbar">
      <button type="button" class="btn" @click="router.push({ name: 'promo-coop-new' })">新增合作</button>
      <button type="button" class="btn secondary" @click="load">刷新</button>
      <button type="button" class="btn secondary" @click="router.push({ name: 'promo-stores' })">店铺</button>
    </div>
    <div class="card">
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-for="(r, i) in rows" :key="i" class="item">
        <div class="line strong">#{{ r.coopId }} {{ r.partnerName }}</div>
        <div class="line">{{ r.contactName || "—" }} {{ r.contactPhone || "" }}</div>
        <div class="line muted">代理 #{{ r.agentId }} · {{ r.status }} · {{ r.createTime }}</div>
        <div v-if="r.remark" class="line">{{ r.remark }}</div>
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
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.btn {
  border: none;
  background: #1f6dff;
  color: #fff;
  border-radius: 8px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
}
.btn.secondary {
  background: #e2e8f0;
  color: #334155;
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
.strong {
  font-weight: 600;
}
</style>
