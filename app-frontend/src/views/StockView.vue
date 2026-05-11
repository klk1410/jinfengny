<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const summary = ref([]);
const flows = ref([]);
const err = ref("");
const inboundQty = ref(10);
const inboundTargetAgentId = ref("1");
const filterAgentId = ref("");
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");
const canInbound = computed(() => roleCode.value === "main" || roleCode.value === "agent");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    let q = "";
    if (isMain.value && filterAgentId.value) {
      q = `&agentId=${encodeURIComponent(filterAgentId.value)}`;
    }
    summary.value = await requestJson(
      `/app-api/biz/stock/summary?openid=${encodeURIComponent(oid)}${q}`
    );
    flows.value = await requestJson(
      `/app-api/biz/stock/flows?openid=${encodeURIComponent(oid)}${q}`
    );
  } catch (e) {
    err.value = e.message || String(e);
    summary.value = [];
    flows.value = [];
  }
}

async function onInbound() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    const q = new URLSearchParams();
    q.set("openid", oid);
    q.set("qty", String(inboundQty.value));
    if (isMain.value) {
      q.set("agentId", inboundTargetAgentId.value);
    }
    await requestJson(`/app-api/biz/stock/inbound?${q.toString()}`, { method: "POST" });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

watch(() => shell.loginOpenid, load);
onMounted(load);
</script>

<template>
  <div class="page">
    <h2 class="page-title">仓储库存</h2>
    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="isMain" class="card">
      <h3 class="sub">按代理筛选（留空为全部）</h3>
      <div class="row">
        <label>代理 ID</label>
        <input v-model="filterAgentId" class="inp" type="text" placeholder="可选" />
      </div>
      <button type="button" class="btn secondary" @click="load">刷新列表</button>
    </div>

    <div v-if="canInbound" class="card">
      <h3 class="sub">手工入库</h3>
      <div v-if="isMain" class="row">
        <label>代理 ID</label>
        <input v-model="inboundTargetAgentId" class="inp" type="text" />
      </div>
      <div class="row">
        <label>数量(桶)</label>
        <input v-model.number="inboundQty" class="inp" type="number" min="0.01" step="0.01" />
      </div>
      <button type="button" class="btn" @click="onInbound">入库</button>
    </div>

    <div class="card">
      <h3 class="sub">当前库存</h3>
      <div v-if="!summary.length" class="muted">暂无</div>
      <div v-for="(s, i) in summary" :key="i" class="item">
        <div class="line strong">{{ s.agentName }}（#{{ s.agentId }}）</div>
        <div class="line">在库 {{ s.qtyOnHand }} · 预扣 {{ s.qtyReserved }} · 可用 {{ s.qtyAvailable }}</div>
      </div>
    </div>

    <div class="card">
      <h3 class="sub">库存流水</h3>
      <div v-if="!flows.length" class="muted">暂无</div>
      <div v-for="(f, i) in flows" :key="i" class="item small">
        <div class="line">{{ f.createTime }} · {{ f.flowKind }} · {{ f.qty }} 桶</div>
        <div class="line muted">{{ f.refType }} {{ f.refNo || "—" }} · {{ f.remark || "" }}</div>
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
.sub {
  margin: 0 0 10px;
  font-size: 14px;
}
.row {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  align-items: center;
}
.inp {
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 12px;
}
.btn {
  border: none;
  background: #1f6dff;
  color: #fff;
  border-radius: 6px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
}
.btn.secondary {
  background: #64748b;
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
.item.small {
  font-size: 11px;
}
.line {
  margin-top: 4px;
}
.strong {
  font-weight: 600;
}
</style>
