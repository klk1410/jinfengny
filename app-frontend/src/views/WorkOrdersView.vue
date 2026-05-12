<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";
import { orderWorkStatusPillClass } from "../utils/statusDisplay.js";
import PfSelect from "../components/PfSelect.vue";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");

const roleCode = computed(() => shell.roleCode ?? shell.portal?.roleCode ?? "");

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

const assignSalesSelectOptions = computed(() =>
  assignSalesmenOptions.value.map((s) => ({
    value: String(s.salesmanId),
    label: assignOptionLabel(s)
  }))
);

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

/** 维护、转移商家：打开结单弹窗；其余类型不应调用 */
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

/** 业务员：非维护、非转移商家 → 直接结单，不扣配件 */
async function finishSalesQuick(w) {
  err.value = "";
  if (!window.confirm("确认结单？（非维护工单不登记配件消耗）")) {
    return;
  }
  try {
    const oid = shell.loginOpenid;
    await requestJson(`/app-api/work-order/${encodeURIComponent(w.workOrderNo)}/finish?openid=${encodeURIComponent(oid)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ accessoryConsumes: [] })
    });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

function onSalesFinishClick(w) {
  const t = w.workOrderTypeCode;
  if (t === "2" || t === "4") {
    openFinishDialog(w);
  } else {
    finishSalesQuick(w);
  }
}

function onAgentFinishClick(w) {
  const t = w.workOrderTypeCode;
  if (t === "2") {
    openFinishDialog(w);
  } else {
    onFinishAgentMain(w.workOrderNo);
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
  if (
    roleCode.value === "sales" &&
    finishRow.value &&
    (finishRow.value.workOrderTypeCode === "2" || finishRow.value.workOrderTypeCode === "4")
  ) {
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
  } else if (!window.confirm("确认完工？（非维护工单不登记配件消耗）")) {
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

function formatHoursNum(v) {
  if (v == null || v === "") return "—";
  const n = Number(v);
  if (!Number.isFinite(n)) return "—";
  const s = Number.isInteger(n) ? String(n) : String(Math.round(n * 100) / 100);
  return `${s} 小时`;
}

function openMerchantMap(w) {
  const lat = w.latitude;
  const lng = w.longitude;
  if (lat == null || lng == null || !Number.isFinite(Number(lat)) || !Number.isFinite(Number(lng))) {
    window.alert("该门店暂无地图坐标，无法导航");
    return;
  }
  const title = encodeURIComponent(w.merchantName || "门店");
  const url = `https://uri.amap.com/marker?position=${encodeURIComponent(String(lng))},${encodeURIComponent(String(lat))}&name=${title}`;
  window.open(url, "_blank", "noopener,noreferrer");
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">工单</h2>
    <p class="wo-hint">
      <strong>预计用时</strong>来自订单确认时填写的预计作业时长（小时）。点击整张卡片可在地图中查看<strong>门店位置</strong>（需门店已维护经纬度）。
    </p>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="card">
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-else class="dc-stack">
        <article
          v-for="(w, i) in rows"
          :key="i"
          class="dc-card dc-card--white wo-card"
          role="button"
          tabindex="0"
          @click="openMerchantMap(w)"
        >
          <div class="wo-head">
            <div class="wo-title-block">
              <span class="wo-k">工单号</span>
              <span class="wo-v wo-v--no">{{ w.workOrderNo }}</span>
            </div>
            <span :class="orderWorkStatusPillClass(w.statusCode)">{{ w.status }}</span>
          </div>

          <div class="wo-field">
            <span class="wo-k">订单号</span>
            <span class="wo-v wo-v--order">{{ w.orderNo || "—" }}</span>
          </div>
          <div v-if="w.orderAmount != null" class="wo-field">
            <span class="wo-k">订单金额</span>
            <span class="wo-v">¥{{ Number(w.orderAmount).toFixed(2) }}</span>
          </div>
          <div class="wo-field">
            <span class="wo-k">店铺</span>
            <span class="wo-v wo-v--merchant">{{ w.merchantName || "—" }}</span>
          </div>
          <div v-if="w.workOrderTypeCode === '4' && w.toMerchantName" class="wo-field">
            <span class="wo-k">目标门店</span>
            <span class="wo-v">{{ w.toMerchantName }}<template v-if="w.toMerchantId"> #{{ w.toMerchantId }}</template></span>
          </div>
          <div class="wo-field">
            <span class="wo-k">工单类型</span>
            <span class="wo-v wo-v--type">{{ w.workOrderType }}</span>
          </div>
          <div class="wo-field">
            <span class="wo-k">预计用时</span>
            <span class="wo-v">{{ formatHoursNum(w.estimatedWorkHours) }}</span>
          </div>
          <div class="wo-field">
            <span class="wo-k">开始时间</span>
            <span class="wo-v wo-v--time">{{ w.workStartTime || "—" }}</span>
          </div>
          <div class="wo-field">
            <span class="wo-k">结束时间</span>
            <span class="wo-v wo-v--time">{{ w.workEndTime || "—" }}</span>
          </div>
          <div class="wo-field">
            <span class="wo-k">实际用时</span>
            <span class="wo-v wo-v--accent">{{ formatHoursNum(w.actualWorkHours) }}</span>
          </div>
          <div class="wo-field">
            <span class="wo-k">接单人</span>
            <span class="wo-v wo-v--person">{{ w.receiveSalesmanName || "—" }}</span>
          </div>
          <div class="wo-field wo-field--sub">
            <span class="wo-k">创建时间</span>
            <span class="wo-v wo-v--sub">{{ w.workOrderTime }}</span>
          </div>

          <div class="actions" @click.stop>
            <button v-if="showReceive(w)" type="button" class="btn-sm" @click="onReceive(w.workOrderNo)">抢单</button>
            <button v-if="showAssign(w)" type="button" class="btn-sm secondary" @click="openAssignDialog(w)">指派</button>
            <button
              v-if="showFinish(w) && roleCode === 'sales'"
              type="button"
              class="btn-sm ok"
              @click="onSalesFinishClick(w)"
            >
              结单
            </button>
            <button v-if="showFinish(w) && roleCode !== 'sales'" type="button" class="btn-sm ok" @click="onAgentFinishClick(w)">
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
          <PfSelect
            id="assign-sales-select"
            v-model="assignSalesmanId"
            :options="assignSalesSelectOptions"
            :disabled="assignBusy || !assignSalesSelectOptions.length"
            placeholder="请选择"
          />
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
          <template v-if="finishRow && finishRow.workOrderTypeCode === '4'">结单 · 转移商家</template>
          <template v-else-if="finishRow && finishRow.workOrderTypeCode === '2'">结单 · 维护 · 配件消耗</template>
          <template v-else>结单</template>
        </h3>
        <p v-if="finishRow && finishRow.workOrderTypeCode === '4'" class="dlg-hint">
          请填写从「{{ finishRow.merchantName }}」转至「{{ finishRow.toMerchantName || "目标门店" }}」的设备编号；可选填配件消耗。
        </p>
        <p v-else-if="finishRow && finishRow.workOrderTypeCode === '2'" class="dlg-hint">
          维护工单请填写本次消耗的配件数量（可为 0）；提交后从本代理配件库存扣减。
        </p>
        <div v-if="finishRow && finishRow.workOrderTypeCode === '4'" class="dlg-device">
          <label class="dlg-lab">设备编号</label>
          <input v-model="finishDeviceNo" class="dlg-inp-full" type="text" placeholder="必填" />
        </div>
        <p v-if="finishBusy" class="muted">加载中…</p>
        <div v-else-if="finishRow && (finishRow.workOrderTypeCode === '2' || finishRow.workOrderTypeCode === '4')" class="dlg-body">
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
.wo-hint {
  margin: 0 0 12px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.5;
  color: #475569;
  background: #f1f5f9;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.wo-hint strong {
  color: #0f172a;
  font-weight: 600;
}
.wo-card {
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: box-shadow 0.15s, border-color 0.15s;
}
.wo-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 2px 12px rgba(31, 109, 255, 0.08);
}
.wo-card:focus {
  outline: 2px solid #1f6dff;
  outline-offset: 2px;
}
.wo-title-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.wo-field {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: 8px 10px;
  align-items: baseline;
  margin-top: 8px;
  font-size: 12px;
}
.wo-field--sub {
  margin-top: 6px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}
.wo-k {
  color: #64748b;
  font-weight: 500;
  font-size: 11px;
  letter-spacing: 0.02em;
}
.wo-v {
  color: #0f172a;
  font-weight: 500;
  word-break: break-word;
  line-height: 1.45;
}
.wo-v--no {
  font-size: 15px;
  font-weight: 700;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  letter-spacing: -0.02em;
}
.wo-v--order {
  font-weight: 600;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  color: #1e40af;
}
.wo-v--merchant {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}
.wo-v--type {
  font-weight: 600;
  color: #0369a1;
}
.wo-v--time {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: #334155;
}
.wo-v--accent {
  font-weight: 700;
  color: #0d9488;
}
.wo-v--person {
  font-weight: 600;
  color: #7c3aed;
}
.wo-v--sub {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
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
  align-items: flex-start;
  gap: 10px;
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
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
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
.dialog :deep(.pf-select) {
  width: 100%;
  margin-bottom: 8px;
}
.dlg-warn {
  margin: 0 0 8px;
  font-size: 12px;
  color: #b45309;
  line-height: 1.45;
}
</style>
