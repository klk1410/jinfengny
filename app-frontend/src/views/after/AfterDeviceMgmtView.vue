<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";
import "../promo/promo-form.css";
import "../../styles/deviceStatusBadge.css";
import { deviceStatusPillClass } from "../../utils/deviceStatusDisplay.js";

const shell = inject("appShell");
const rows = ref([]);
const devices = ref([]);
const err = ref("");
const showRemove = ref(false);
const showScrap = ref(false);
const rDeviceNo = ref("");
const sDeviceNo = ref("");
const rRemark = ref("");
const sRemark = ref("");
const agentId = ref("1");

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");
const canLogRemove = computed(
  () => roleCode.value === "main" || roleCode.value === "agent" || roleCode.value === "sales"
);
const canScrap = computed(() => canLogRemove.value);

const devicesOnMerchant = computed(() =>
  (devices.value || []).filter((d) => d.merchantId != null && d.deviceStatusCode === "1")
);

const devicesInStock = computed(() =>
  (devices.value || []).filter((d) => d.merchantId == null && d.deviceStatusCode === "0")
);

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
    await requestJson("/app-api/biz/device-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
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
    await requestJson("/app-api/biz/device-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
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
      设备列表与操作。登记移除仅<strong>在店且已绑定门店</strong>的设备；登记报废仅<strong>在库且未绑定门店</strong>的设备。跨店装机请使用订单「转移商家」。
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
      <button v-if="canLogRemove" type="button" class="pf-tool" @click="showRemove = true">登记移除</button>
      <button v-if="canScrap" type="button" class="pf-tool" @click="showScrap = true">登记报废</button>
    </div>

    <div class="pf-card">
      <div v-if="!rows.length" class="pf-muted" style="padding: 12px">暂无数据</div>
      <div v-for="(d, i) in rows" :key="i" class="pf-item">
        <div class="pf-line-strong">{{ d.deviceNo }}</div>
        <div class="pf-line-muted device-meta-row">
          <span>{{ d.deviceType }}</span>
          <span :class="deviceStatusPillClass(d.deviceStatusCode)">{{ d.deviceStatus }}</span>
        </div>
        <div class="pf-line-muted">
          门店 {{ d.merchantName || "—" }}{{ d.merchantId != null ? "（" + d.merchantId + "）" : "" }} · 代理
          {{ d.agentName || "#" + d.agentId }}{{ d.agentId != null ? "（" + d.agentId + "）" : "" }}
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
        <select v-model="rDeviceNo" style="padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0">
          <option value="">选择在店设备 *</option>
          <option v-for="d in devicesOnMerchant" :key="d.deviceId" :value="d.deviceNo">
            {{ d.deviceNo }} · {{ d.deviceStatus }} · {{ d.merchantName || "门店#" + d.merchantId }}
          </option>
        </select>
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
        <select v-model="sDeviceNo" style="padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0">
          <option value="">选择在库设备 *</option>
          <option v-for="d in devicesInStock" :key="d.deviceId" :value="d.deviceNo">{{ d.deviceNo }} · {{ d.deviceStatus }}</option>
        </select>
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
