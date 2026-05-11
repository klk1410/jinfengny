<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");

const roleCode = computed(() => shell.portal?.roleCode ?? "");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/work-order/list?openid=${encodeURIComponent(oid)}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
  }
}

async function onReceive(no) {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    await requestJson(
      `/app-api/work-order/${encodeURIComponent(no)}/receive?openid=${encodeURIComponent(oid)}`,
      { method: "POST" }
    );
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function onAssign(no) {
  const raw = window.prompt("指派业务员 ID（数字）", "1");
  if (raw == null || raw === "") {
    return;
  }
  const sid = Number(raw);
  if (!Number.isFinite(sid) || sid <= 0) {
    err.value = "业务员 ID 无效";
    return;
  }
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    await requestJson(
      `/app-api/work-order/${encodeURIComponent(no)}/assign?openid=${encodeURIComponent(oid)}&salesmanId=${encodeURIComponent(String(sid))}`,
      { method: "POST" }
    );
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function onFinish(no) {
  if (!window.confirm("确认完工？")) {
    return;
  }
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    await requestJson(
      `/app-api/work-order/${encodeURIComponent(no)}/finish?openid=${encodeURIComponent(oid)}`,
      { method: "POST" }
    );
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

function showReceive(row) {
  return (
    roleCode.value === "sales" &&
    (row.statusCode === "0" || row.statusCode === "1") &&
    !row.receiveSalesmanId &&
    row.grabExpired !== true
  );
}

function showAssign(row) {
  if (!(roleCode.value === "agent" || roleCode.value === "main")) {
    return false;
  }
  if (!(row.statusCode === "0" || row.statusCode === "1")) {
    return false;
  }
  if (row.receiveSalesmanId) {
    return false;
  }
  if (roleCode.value === "main") {
    return true;
  }
  return row.grabExpired === true;
}

function showFinish(row) {
  if (row.statusCode !== "2" || roleCode.value === "merchant") {
    return false;
  }
  return roleCode.value === "main" || roleCode.value === "agent" || roleCode.value === "sales";
}

watch(() => shell.loginOpenid, load);
onMounted(load);
</script>

<template>
  <div class="page">
    <h2 class="page-title">工单</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="card">
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-for="(w, i) in rows" :key="i" class="item">
        <div class="line strong">{{ w.workOrderNo }}</div>
        <div class="line muted">订单 {{ w.orderNo || "—" }} · {{ w.merchantName }}</div>
        <div class="line">{{ w.workOrderType }} · {{ w.status }}</div>
        <div v-if="w.acceptDeadline" class="line muted">
          抢单截止 {{ w.acceptDeadline }}
          <template v-if="w.grabExpired">（已结束，代理可指派）</template>
        </div>
        <div class="line muted">接单 {{ w.receiveSalesmanName || "—" }} · {{ w.workOrderTime }}</div>
        <div class="actions">
          <button v-if="showReceive(w)" type="button" class="btn-sm" @click="onReceive(w.workOrderNo)">抢单</button>
          <button v-if="showAssign(w)" type="button" class="btn-sm secondary" @click="onAssign(w.workOrderNo)">指派</button>
          <button v-if="showFinish(w)" type="button" class="btn-sm ok" @click="onFinish(w.workOrderNo)">完工</button>
        </div>
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
.actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.btn-sm {
  border: none;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
  background: #1f6dff;
  color: #fff;
}
.btn-sm.secondary {
  background: #64748b;
}
.btn-sm.ok {
  background: #0d9488;
}
</style>
