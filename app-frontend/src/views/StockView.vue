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

/** 1 入库 2 预扣 3 实扣 4 回滚 */
function flowVariant(code) {
  if (code === "1") {
    return "in";
  }
  if (code === "2") {
    return "reserve";
  }
  if (code === "3") {
    return "deduct";
  }
  if (code === "4") {
    return "rollback";
  }
  return "neutral";
}

function formatFlowQtyAbs(f) {
  const q = Number(f.qty);
  if (!Number.isFinite(q)) {
    return String(f.qty ?? "—");
  }
  const s = q.toFixed(2).replace(/\.?0+$/, "");
  return s;
}

/** 对库存/可用的语义符号：入库与回滚为增加；预扣与实扣为扣减方向展示 */
function flowQtyDisplay(f) {
  const abs = formatFlowQtyAbs(f);
  const v = flowVariant(f.flowKindCode);
  if (v === "in" || v === "rollback") {
    return `+${abs}`;
  }
  if (v === "reserve" || v === "deduct") {
    return `−${abs}`;
  }
  return abs;
}

function flowQtyHint(code) {
  if (code === "1") {
    return "增加在库数量";
  }
  if (code === "2") {
    return "占用可用 · 锁定待发";
  }
  if (code === "3") {
    return "完工出库 · 扣减在库";
  }
  if (code === "4") {
    return "取消释放 · 恢复可用";
  }
  return "";
}
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

    <div class="card card--flows">
      <h3 class="sub">库存流水</h3>
      <div v-if="!flows.length" class="muted">暂无</div>
      <div v-else class="flow-list">
        <article
          v-for="f in flows"
          :key="f.flowId"
          class="flow-card"
          :class="'flow-card--' + flowVariant(f.flowKindCode)"
        >
          <div class="flow-card__head">
            <span class="flow-kind-tag">{{ f.flowKind }}</span>
            <span class="flow-qty-line" :class="'flow-qty-line--' + flowVariant(f.flowKindCode)">
              <span class="flow-qty-num">{{ flowQtyDisplay(f) }}</span>
              <span class="flow-qty-unit">桶</span>
            </span>
          </div>
          <p class="flow-hint">{{ flowQtyHint(f.flowKindCode) }}</p>
          <div class="flow-meta">
            <span>{{ f.createTime }}</span>
            <span v-if="isMain" class="flow-meta-split">·</span>
            <span v-if="isMain">代理 #{{ f.agentId }}</span>
          </div>
          <div v-if="f.refNo" class="flow-ref">
            <span class="flow-ref-k">关联单号</span>
            <span class="flow-ref-v">{{ f.refNo }}</span>
          </div>
          <div v-if="f.remark" class="flow-remark">{{ f.remark }}</div>
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
.line {
  margin-top: 4px;
}
.strong {
  font-weight: 600;
}

.card--flows .sub {
  margin-bottom: 12px;
}
.flow-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.flow-card {
  border-radius: 10px;
  padding: 12px 12px 11px;
  border: 1px solid #e2e8f0;
  border-left-width: 4px;
  background: #fafbfc;
  font-size: 12px;
}
.flow-card--in {
  border-left-color: #059669;
  background: #f0fdf4;
}
.flow-card--reserve {
  border-left-color: #d97706;
  background: #fffbeb;
}
.flow-card--deduct {
  border-left-color: #dc2626;
  background: #fef2f2;
}
.flow-card--rollback {
  border-left-color: #0891b2;
  background: #ecfeff;
}
.flow-card--neutral {
  border-left-color: #64748b;
  background: #f8fafc;
}
.flow-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.flow-kind-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  flex-shrink: 0;
}
.flow-card--in .flow-kind-tag {
  background: #d1fae5;
  color: #047857;
  border-color: #6ee7b7;
}
.flow-card--reserve .flow-kind-tag {
  background: #fef3c7;
  color: #b45309;
  border-color: #fcd34d;
}
.flow-card--deduct .flow-kind-tag {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fca5a5;
}
.flow-card--rollback .flow-kind-tag {
  background: #cffafe;
  color: #0e7490;
  border-color: #67e8f9;
}
.flow-card--neutral .flow-kind-tag {
  background: #f1f5f9;
  color: #475569;
  border-color: #cbd5e1;
}
.flow-qty-line {
  text-align: right;
  flex-shrink: 0;
}
.flow-qty-num {
  font-size: 20px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}
.flow-qty-unit {
  font-size: 12px;
  font-weight: 600;
  margin-left: 2px;
  color: #64748b;
}
.flow-qty-line--in .flow-qty-num {
  color: #047857;
}
.flow-qty-line--reserve .flow-qty-num {
  color: #b45309;
}
.flow-qty-line--deduct .flow-qty-num {
  color: #b91c1c;
}
.flow-qty-line--rollback .flow-qty-num {
  color: #0e7490;
}
.flow-qty-line--neutral .flow-qty-num {
  color: #334155;
}
.flow-hint {
  margin: 8px 0 0;
  font-size: 11px;
  color: #64748b;
  line-height: 1.4;
}
.flow-meta {
  margin-top: 8px;
  font-size: 11px;
  color: #64748b;
}
.flow-meta-split {
  margin: 0 4px;
}
.flow-ref {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: baseline;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  font-size: 11px;
}
.flow-ref-k {
  color: #94a3b8;
  flex-shrink: 0;
}
.flow-ref-v {
  color: #0f172a;
  font-weight: 600;
  text-align: right;
  word-break: break-all;
}
.flow-remark {
  margin-top: 6px;
  font-size: 11px;
  color: #475569;
  line-height: 1.45;
}
</style>
