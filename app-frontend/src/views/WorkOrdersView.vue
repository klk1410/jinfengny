<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";
import { orderWorkStatusPillClass } from "../utils/statusDisplay.js";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");

const roleCode = computed(() => shell.portal?.roleCode ?? "");

const finishOpen = ref(false);
const finishNo = ref("");
const finishRow = ref(null);
const finishDeviceNo = ref("");
const finishLines = ref([]);
const finishBusy = ref(false);

const assignOpen = ref(false);
const assignBusy = ref(false);
const assignWoNo = ref("");
const assignRow = ref(null);
const assignSalesmenRaw = ref([]);
const assignSalesmanId = ref("");

function assignSalesmenForRow(list, wo) {
  const active = (list || []).filter((s) => s.statusCode === "0");
  if (roleCode.value === "main" && wo?.agentId != null) {
    const aid = Number(wo.agentId);
    return active.filter((s) => Number(s.agentId) === aid);
  }
  return active;
}

const assignSalesmenOptions = computed(() => assignSalesmenForRow(assignSalesmenRaw.value, assignRow.value));

function assignOptionLabel(s) {
  const base = `${s.salesmanName} · ${s.phone || "无电话"}`;
  if (roleCode.value === "main") {
    return `${base}（代理 #${s.agentId}）`;
  }
  return base;
}

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

async function openAssignDialog(w) {
  err.value = "";
  assignRow.value = w;
  assignWoNo.value = w.workOrderNo;
  assignSalesmanId.value = "";
  assignBusy.value = true;
  assignOpen.value = true;
  assignSalesmenRaw.value = [];
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    assignSalesmenRaw.value = (await requestJson(`/app-api/biz/salesmen?openid=${oid}`)) || [];
    const opts = assignSalesmenForRow(assignSalesmenRaw.value, w);
    if (opts.length === 1) {
      assignSalesmanId.value = String(opts[0].salesmanId);
    }
  } catch (e) {
    err.value = e.message || String(e);
    assignOpen.value = false;
    assignRow.value = null;
  } finally {
    assignBusy.value = false;
  }
}

function closeAssignDialog() {
  assignOpen.value = false;
  assignWoNo.value = "";
  assignRow.value = null;
  assignSalesmenRaw.value = [];
  assignSalesmanId.value = "";
}

async function submitAssign() {
  const sid = Number(assignSalesmanId.value);
  if (!Number.isFinite(sid) || sid <= 0) {
    err.value = "请选择业务员";
    return;
  }
  err.value = "";
  assignBusy.value = true;
  try {
    const oid = shell.loginOpenid;
    await requestJson(
      `/app-api/work-order/${encodeURIComponent(assignWoNo.value)}/assign?openid=${encodeURIComponent(oid)}&salesmanId=${encodeURIComponent(String(sid))}`,
      { method: "POST" }
    );
    closeAssignDialog();
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  } finally {
    assignBusy.value = false;
  }
}

async function openFinishDialog(w) {
  err.value = "";
  finishRow.value = w;
  finishNo.value = w.workOrderNo;
  finishDeviceNo.value = "";
  finishBusy.value = true;
  finishOpen.value = true;
  finishLines.value = [];
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    const [types, sum] = await Promise.all([
      requestJson("/app-api/biz/accessory-types"),
      requestJson(`/app-api/biz/accessories?openid=${oid}`)
    ]);
    const bal = new Map((sum || []).map((s) => [Number(s.typeId), Number(s.qtyTotal) || 0]));
    finishLines.value = (types || []).map((t) => ({
      typeId: t.typeId,
      typeName: t.typeName,
      balance: bal.get(Number(t.typeId)) ?? 0,
      qty: 0
    }));
  } catch (e) {
    err.value = e.message || String(e);
    finishOpen.value = false;
    finishRow.value = null;
  } finally {
    finishBusy.value = false;
  }
}

function closeFinishDialog() {
  finishOpen.value = false;
  finishNo.value = "";
  finishRow.value = null;
  finishDeviceNo.value = "";
  finishLines.value = [];
}

async function submitFinish() {
  err.value = "";
  const oid = shell.loginOpenid;
  const consumes = [];
  for (const row of finishLines.value) {
    const q = Number(row.qty);
    if (Number.isFinite(q) && q > 0) {
      consumes.push({ typeId: row.typeId, qty: q });
    }
  }
  if (finishRow.value && finishRow.value.workOrderTypeCode === "4") {
    const dn = finishDeviceNo.value.trim();
    if (!dn) {
      err.value = "请填写要转移的设备编号";
      return;
    }
  }
  if (roleCode.value === "sales") {
    for (const row of finishLines.value) {
      const q = Number(row.qty);
      if (Number.isFinite(q) && q > row.balance + 1e-6) {
        err.value = `「${row.typeName}」填写数量超过当前库存 ${row.balance}`;
        return;
      }
    }
  }
  finishBusy.value = true;
  try {
    const body = { accessoryConsumes: consumes };
    if (finishRow.value && finishRow.value.workOrderTypeCode === "4") {
      body.deviceNo = finishDeviceNo.value.trim();
    }
    await requestJson(
      `/app-api/work-order/${encodeURIComponent(finishNo.value)}/finish?openid=${encodeURIComponent(oid)}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      }
    );
    closeFinishDialog();
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  } finally {
    finishBusy.value = false;
  }
}

async function onFinishAgentMain(no) {
  const w = rows.value.find((r) => r.workOrderNo === no);
  let deviceNo = null;
  if (w && w.workOrderTypeCode === "4") {
    const raw = window.prompt("转移商家完工：请输入设备编号", "");
    if (raw == null) {
      return;
    }
    deviceNo = raw.trim();
    if (!deviceNo) {
      err.value = "设备编号不能为空";
      return;
    }
  } else if (!window.confirm("确认完工？（未填写配件消耗，将不扣减配件库）")) {
    return;
  }
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    const body = { accessoryConsumes: [] };
    if (deviceNo) {
      body.deviceNo = deviceNo;
    }
    await requestJson(`/app-api/work-order/${encodeURIComponent(no)}/finish?openid=${encodeURIComponent(oid)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
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
      <div v-else class="dc-stack">
        <article v-for="(w, i) in rows" :key="i" class="dc-card dc-card--white">
        <div class="wo-head">
          <span class="wo-no">{{ w.workOrderNo }}</span>
          <span :class="orderWorkStatusPillClass(w.statusCode)">{{ w.status }}</span>
        </div>
        <div class="line muted">订单 {{ w.orderNo || "—" }} · {{ w.merchantName }}</div>
        <div v-if="w.workOrderTypeCode === '4' && w.toMerchantName" class="line muted">
          目标门店 {{ w.toMerchantName }}<template v-if="w.toMerchantId">（{{ w.toMerchantId }}）</template>
        </div>
        <div class="line wo-type">{{ w.workOrderType }}</div>
        <div v-if="w.acceptDeadline" class="line muted">
          抢单截止 {{ w.acceptDeadline }}
          <template v-if="w.grabExpired">（已结束，代理可指派）</template>
        </div>
        <div class="line muted">接单 {{ w.receiveSalesmanName || "—" }} · {{ w.workOrderTime }}</div>
        <div class="actions">
          <button v-if="showReceive(w)" type="button" class="btn-sm" @click="onReceive(w.workOrderNo)">抢单</button>
          <button v-if="showAssign(w)" type="button" class="btn-sm secondary" @click="openAssignDialog(w)">指派</button>
          <button
            v-if="showFinish(w) && roleCode === 'sales'"
            type="button"
            class="btn-sm ok"
            @click="openFinishDialog(w)"
          >
            结单
          </button>
          <button v-if="showFinish(w) && roleCode !== 'sales'" type="button" class="btn-sm ok" @click="onFinishAgentMain(w.workOrderNo)">
            完工
          </button>
        </div>
        </article>
      </div>
    </div>

    <div v-if="assignOpen" class="mask mask--assign" @click.self="closeAssignDialog">
      <div class="dialog">
        <h3 class="dlg-title">指派业务员</h3>
        <p v-if="assignRow" class="dlg-hint">
          工单 {{ assignRow.workOrderNo }} · {{ assignRow.merchantName }}
          <template v-if="roleCode === 'main' && assignRow.agentId != null"> · 代理 #{{ assignRow.agentId }}</template>
        </p>
        <p v-if="assignBusy" class="muted">加载中…</p>
        <template v-else>
          <label class="dlg-lab" for="assign-sales-select">选择业务员</label>
          <select
            id="assign-sales-select"
            v-model="assignSalesmanId"
            class="dlg-select"
            :disabled="!assignSalesmenOptions.length"
          >
            <option disabled value="">请选择</option>
            <option v-for="s in assignSalesmenOptions" :key="s.salesmanId" :value="String(s.salesmanId)">
              {{ assignOptionLabel(s) }}
            </option>
          </select>
          <p v-if="!assignSalesmenOptions.length" class="dlg-warn">暂无可指派的在职业务员，请先在业务员管理中维护。</p>
        </template>
        <div class="dlg-actions">
          <button type="button" class="btn-sm secondary" :disabled="assignBusy" @click="closeAssignDialog">取消</button>
          <button
            type="button"
            class="btn-sm ok"
            :disabled="assignBusy || !assignSalesmenOptions.length || !assignSalesmanId"
            @click="submitAssign"
          >
            确认指派
          </button>
        </div>
      </div>
    </div>

    <div v-if="finishOpen" class="mask" @click.self="closeFinishDialog">
      <div class="dialog">
        <h3 class="dlg-title">
          {{ finishRow && finishRow.workOrderTypeCode === "4" ? "结单 · 转移商家" : "结单 · 配件消耗" }}
        </h3>
        <p v-if="finishRow && finishRow.workOrderTypeCode === '4'" class="dlg-hint">
          请填写从「{{ finishRow.merchantName }}」转至「{{ finishRow.toMerchantName || "目标门店" }}」的设备编号；可选填配件消耗。
        </p>
        <p v-else class="dlg-hint">请填写本次工单消耗的配件数量（可为 0）；提交后从本代理配件库存自动扣减。</p>
        <div v-if="finishRow && finishRow.workOrderTypeCode === '4'" class="dlg-device">
          <label class="dlg-lab">设备编号</label>
          <input v-model="finishDeviceNo" class="dlg-inp-full" type="text" placeholder="必填" />
        </div>
        <p v-if="finishBusy" class="muted">加载中…</p>
        <div v-else class="dlg-body">
          <div v-for="(row, j) in finishLines" :key="j" class="dlg-row">
            <div class="dlg-name">
              {{ row.typeName }}
              <span class="dlg-bal">库存 {{ row.balance }}</span>
            </div>
            <input v-model.number="row.qty" class="dlg-inp" type="number" min="0" step="0.01" placeholder="0" />
          </div>
        </div>
        <div class="dlg-actions">
          <button type="button" class="btn-sm secondary" :disabled="finishBusy" @click="closeFinishDialog">取消</button>
          <button type="button" class="btn-sm ok" :disabled="finishBusy" @click="submitFinish">确认结单</button>
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
.line {
  margin-top: 4px;
}
.line:first-child {
  margin-top: 0;
}
.strong {
  font-weight: 600;
}
.wo-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}
.wo-no {
  min-width: 0;
  word-break: break-all;
}
.wo-type {
  color: #334155;
  font-weight: 500;
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
.mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 50;
  padding: 12px;
  box-sizing: border-box;
}
.dialog {
  width: 100%;
  max-width: 440px;
  max-height: 85vh;
  overflow: auto;
  background: #fff;
  border-radius: 12px 12px 0 0;
  padding: 14px 14px 18px;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.12);
}
.dlg-title {
  margin: 0 0 8px;
  font-size: 15px;
}
.dlg-hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
}
.dlg-device {
  margin-bottom: 12px;
}
.dlg-lab {
  display: block;
  font-size: 12px;
  color: #334155;
  margin-bottom: 6px;
}
.dlg-inp-full {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 14px;
}
.dlg-body {
  max-height: 50vh;
  overflow: auto;
}
.dlg-row {
  display: grid;
  grid-template-columns: 1fr 88px;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
  font-size: 12px;
}
.dlg-name {
  color: #0f172a;
}
.dlg-bal {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}
.dlg-inp {
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 13px;
  text-align: right;
}
.dlg-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.mask--assign {
  z-index: 55;
}
.dlg-select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 14px;
  color: #0f172a;
  background: #fff;
  margin-bottom: 8px;
}
.dlg-warn {
  margin: 0 0 8px;
  font-size: 12px;
  color: #b45309;
  line-height: 1.45;
}
</style>
