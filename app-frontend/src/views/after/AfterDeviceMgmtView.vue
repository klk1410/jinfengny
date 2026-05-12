<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";
import PfSelect from "../../components/PfSelect.vue";
import "../promo/promo-form.css";
import "../../styles/deviceStatusBadge.css";
import { deviceStatusPillClass } from "../../utils/deviceStatusDisplay.js";

const shell = inject("appShell");
const rows = ref([]);
const devices = ref([]);
const err = ref("");
const showRemove = ref(false);
const showScrap = ref(false);
const showTransfer = ref(false);
const rDeviceNo = ref("");
const sDeviceNo = ref("");
const tDeviceNo = ref("");
const tMerchantId = ref("");
const tRemark = ref("");
const rRemark = ref("");
const sRemark = ref("");
const merchants = ref([]);
const agentId = ref("1");

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");
const canLogRemove = computed(
  () => roleCode.value === "main" || roleCode.value === "agent" || roleCode.value === "sales"
);
const canScrap = computed(() => canLogRemove.value);
const canTransferToMerchant = computed(() => canLogRemove.value);

const devicesOnMerchant = computed(() =>
  (devices.value || []).filter((d) => d.merchantId != null && d.deviceStatusCode === "1")
);

const devicesInStock = computed(() =>
  (devices.value || []).filter((d) => d.merchantId == null && d.deviceStatusCode === "0")
);

const transferDeviceOptions = computed(() => [
  { value: "", label: "选择在库设备 *" },
  ...devicesInStock.value.map((d) => ({
    value: d.deviceNo,
    label: `${d.deviceNo} · ${d.deviceStatus}`
  }))
]);

const transferMerchantOptions = computed(() => [
  { value: "", label: "选择目标门店 *" },
  ...merchants.value.map((m) => ({
    value: String(m.merchantId),
    label: `${m.merchantName} · ${m.city || "—"} · #${m.merchantId}`
  }))
]);

const removeDeviceOptions = computed(() => [
  { value: "", label: "选择在店设备 *" },
  ...devicesOnMerchant.value.map((d) => ({
    value: d.deviceNo,
    label: `${d.deviceNo} · ${d.deviceStatus} · ${d.merchantName || "门店#" + d.merchantId}`
  }))
]);

const scrapDeviceOptions = computed(() => [
  { value: "", label: "选择在库设备 *" },
  ...devicesInStock.value.map((d) => ({
    value: d.deviceNo,
    label: `${d.deviceNo} · ${d.deviceStatus}`
  }))
]);

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

async function loadDevices() {
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    devices.value = await requestJson(`/app-api/biz/devices?openid=${oid}`);
  } catch {
    devices.value = [];
  }
}

async function loadMerchants() {
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    merchants.value = await requestJson(`/app-api/biz/merchants?openid=${oid}`);
  } catch {
    merchants.value = [];
  }
}

function isPendingDeviceAudit(res) {
  return res && res.auditId != null && res.pendingReview === true;
}

async function openTransferPanel() {
  showTransfer.value = true;
  tDeviceNo.value = "";
  tMerchantId.value = "";
  tRemark.value = "";
  await loadMerchants();
}

async function submitTransfer() {
  err.value = "";
  if (!tDeviceNo.value.trim()) {
    err.value = "请选择设备";
    return;
  }
  if (!tMerchantId.value) {
    err.value = "请选择目标门店";
    return;
  }
  try {
    const body = {
      openid: shell.loginOpenid,
      eventType: "T",
      deviceNo: tDeviceNo.value.trim(),
      merchantId: Number(tMerchantId.value),
      remark: tRemark.value.trim() || null
    };
    if (isMain.value) {
      body.agentId = Number(agentId.value);
    }
    const res = await requestJson("/app-api/biz/device-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    if (isPendingDeviceAudit(res)) {
      window.alert(`已提交审核 #${res.auditId}，待代理确认后生效`);
    }
    showTransfer.value = false;
    tDeviceNo.value = "";
    tMerchantId.value = "";
    tRemark.value = "";
    await load();
    await loadDevices();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function submitRemove() {
  err.value = "";
  if (!rDeviceNo.value.trim()) {
    err.value = "请选择设备";
    return;
  }
  try {
    const body = {
      openid: shell.loginOpenid,
      eventType: "R",
      deviceNo: rDeviceNo.value.trim(),
      remark: rRemark.value.trim() || null
    };
    if (isMain.value) {
      body.agentId = Number(agentId.value);
    }
    const res = await requestJson("/app-api/biz/device-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    if (isPendingDeviceAudit(res)) {
      window.alert(`已提交审核 #${res.auditId}，待代理确认后生效`);
    }
    showRemove.value = false;
    rDeviceNo.value = "";
    rRemark.value = "";
    await load();
    await loadDevices();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function submitScrap() {
  err.value = "";
  if (!sDeviceNo.value.trim()) {
    err.value = "请选择设备";
    return;
  }
  try {
    const body = {
      openid: shell.loginOpenid,
      eventType: "S",
      deviceNo: sDeviceNo.value.trim(),
      remark: sRemark.value.trim() || null
    };
    if (isMain.value) {
      body.agentId = Number(agentId.value);
    }
    const res = await requestJson("/app-api/biz/device-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    if (isPendingDeviceAudit(res)) {
      window.alert(`已提交审核 #${res.auditId}，待代理确认后生效`);
    }
    showScrap.value = false;
    sDeviceNo.value = "";
    sRemark.value = "";
    await load();
    await loadDevices();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

watch(() => shell.loginOpenid, () => {
  load();
  loadDevices();
});
onMounted(() => {
  load();
  loadDevices();
});
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>
    <p class="pf-muted" style="margin: 0 0 8px">
      设备列表与操作。<strong>转移商家</strong>：将在库设备调至指定门店（装机）；登记移除仅<strong>在店且已绑定门店</strong>的设备；登记报废仅<strong>在库且未绑定门店</strong>的设备。门店之间的设备迁移（跨店）请使用订单类型「转移商家」工单。
      <span v-if="roleCode === 'sales'">运维提交的变更将进入<strong>设备操作审核</strong>，由代理确认后生效。</span>
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
      <button type="button" class="pf-tool pf-tool--ghost" @click="load(); loadDevices()">刷新</button>
      <button v-if="canTransferToMerchant" type="button" class="pf-tool" @click="openTransferPanel">转移商家</button>
      <button v-if="canLogRemove" type="button" class="pf-tool" @click="showRemove = true">登记移除</button>
      <button v-if="canScrap" type="button" class="pf-tool" @click="showScrap = true">登记报废</button>
    </div>

    <div class="pf-card">
      <div v-if="!rows.length" class="pf-muted" style="padding: 12px">暂无数据</div>
      <div v-else class="dc-stack">
        <article v-for="(d, i) in rows" :key="i" class="dc-card dc-card--white">
          <div class="pf-line-strong">{{ d.deviceNo }}</div>
          <div class="pf-line-muted device-meta-row">
            <span>{{ d.deviceType }}</span>
            <span :class="deviceStatusPillClass(d.deviceStatusCode)">{{ d.deviceStatus }}</span>
          </div>
          <div class="pf-line-muted">
            门店 {{ d.merchantName || "—" }}{{ d.merchantId != null ? "（" + d.merchantId + "）" : "" }} · 代理
            {{ d.agentName || "#" + d.agentId }}{{ d.agentId != null ? "（" + d.agentId + "）" : "" }}
          </div>
        </article>
      </div>
    </div>

    <div v-if="showTransfer" class="pf-panel" style="margin-top: 14px">
      <h3 class="pf-panel-title">转移商家（在库 → 门店）</h3>
      <div class="pf-row" style="border: none; padding: 0; min-height: auto; margin-bottom: 10px">
        <div v-if="isMain" class="pf-row" style="width: 100%; padding-left: 0; padding-right: 0">
          <div class="pf-label req">代理</div>
          <div class="pf-field-wrap">
            <input v-model="agentId" class="pf-field" type="text" style="border: 1px solid #e2e8f0; border-radius: 8px; padding: 8px" />
          </div>
        </div>
      </div>
      <div class="pf-row" style="border: none; padding: 0; flex-direction: column; align-items: stretch; gap: 10px">
        <PfSelect v-model="tDeviceNo" :options="transferDeviceOptions" placeholder="选择在库设备 *" />
        <PfSelect v-model="tMerchantId" :options="transferMerchantOptions" placeholder="选择目标门店 *" />
        <input v-model="tRemark" type="text" placeholder="说明（选填）" style="padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0" />
        <p v-if="!devicesInStock.length" class="pf-muted" style="margin: 0">当前无可调出设备（须为在库、未绑定门店）。</p>
        <p v-if="devicesInStock.length && !merchants.length" class="pf-muted" style="margin: 0">暂无可用门店，请先维护门店资料。</p>
        <div style="display: flex; gap: 8px">
          <button type="button" class="pf-submit" style="flex: 1" @click="submitTransfer">提交转移</button>
          <button type="button" class="pf-tool pf-tool--ghost" style="flex: 1; padding: 14px" @click="showTransfer = false">取消</button>
        </div>
      </div>
    </div>

    <div v-if="showRemove" class="pf-panel" style="margin-top: 14px">
      <h3 class="pf-panel-title">登记移除（仅已在店的设备）</h3>
      <div class="pf-row" style="border: none; padding: 0; min-height: auto; margin-bottom: 10px">
        <div v-if="isMain" class="pf-row" style="width: 100%; padding-left: 0; padding-right: 0">
          <div class="pf-label req">代理</div>
          <div class="pf-field-wrap">
            <input v-model="agentId" class="pf-field" type="text" style="border: 1px solid #e2e8f0; border-radius: 8px; padding: 8px" />
          </div>
        </div>
      </div>
      <div class="pf-row" style="border: none; padding: 0; flex-direction: column; align-items: stretch; gap: 10px">
        <PfSelect v-model="rDeviceNo" :options="removeDeviceOptions" placeholder="选择在店设备 *" />
        <input v-model="rRemark" type="text" placeholder="说明（选填）" style="padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0" />
        <p v-if="!devicesOnMerchant.length" class="pf-muted" style="margin: 0">当前无可移除设备（须为在店、已绑定门店）。</p>
        <div style="display: flex; gap: 8px">
          <button type="button" class="pf-submit" style="flex: 1" @click="submitRemove">提交</button>
          <button type="button" class="pf-tool pf-tool--ghost" style="flex: 1; padding: 14px" @click="showRemove = false">取消</button>
        </div>
      </div>
    </div>

    <div v-if="showScrap" class="pf-panel" style="margin-top: 14px">
      <h3 class="pf-panel-title">登记报废（仅在库设备）</h3>
      <div class="pf-row" style="border: none; padding: 0; min-height: auto; margin-bottom: 10px">
        <div v-if="isMain" class="pf-row" style="width: 100%; padding-left: 0; padding-right: 0">
          <div class="pf-label req">代理</div>
          <div class="pf-field-wrap">
            <input v-model="agentId" class="pf-field" type="text" style="border: 1px solid #e2e8f0; border-radius: 8px; padding: 8px" />
          </div>
        </div>
      </div>
      <div class="pf-row" style="border: none; padding: 0; flex-direction: column; align-items: stretch; gap: 10px">
        <PfSelect v-model="sDeviceNo" :options="scrapDeviceOptions" placeholder="选择在库设备 *" />
        <input v-model="sRemark" type="text" placeholder="说明（选填）" style="padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0" />
        <p v-if="!devicesInStock.length" class="pf-muted" style="margin: 0">当前无可报废设备（须为在库、未绑定门店）。</p>
        <div style="display: flex; gap: 8px">
          <button type="button" class="pf-submit" style="flex: 1" @click="submitScrap">提交报废</button>
          <button type="button" class="pf-tool pf-tool--ghost" style="flex: 1; padding: 14px" @click="showScrap = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pf-panel :deep(.pf-select) {
  width: 100%;
}

.device-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.device-status-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
</style>
