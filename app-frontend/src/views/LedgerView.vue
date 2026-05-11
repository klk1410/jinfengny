<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");
const agentFilter = ref("");
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    let url = `/app-api/biz/account/ledger?openid=${encodeURIComponent(oid)}`;
    if (isMain.value && agentFilter.value) {
      url += `&agentId=${encodeURIComponent(agentFilter.value)}`;
    }
    rows.value = await requestJson(url);
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
    <h2 class="page-title">账目流水</h2>
    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="isMain" class="card filter">
      <label class="lbl">按代理 ID 筛选</label>
      <input v-model="agentFilter" class="inp" type="text" placeholder="留空看全部" />
      <button type="button" class="btn-sm" @click="load">查询</button>
    </div>

    <div class="card">
      <div v-if="!rows.length" class="muted">暂无</div>
      <div v-for="(r, i) in rows" :key="i" class="item">
        <div class="line">
          <span class="strong">{{ r.amount }}</span>
          · {{ r.direction }} · {{ r.title }}
        </div>
        <div class="line muted">{{ r.createTime }} · {{ r.refType }} {{ r.refNo || "" }}</div>
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
  margin-bottom: 12px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}
.filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 12px;
}
.lbl {
  color: #64748b;
}
.inp {
  flex: 1;
  min-width: 100px;
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 12px;
}
.btn-sm {
  border: none;
  background: #64748b;
  color: #fff;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
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
