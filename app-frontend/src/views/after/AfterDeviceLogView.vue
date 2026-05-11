<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";
import "../promo/promo-form.css";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");
const showRemove = ref(false);
const rDeviceNo = ref("");
const rMerchantId = ref("");
const rRemark = ref("");
const agentId = ref("1");
const merchants = ref([]);

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");
const canLogRemove = computed(() => roleCode.value === "main" || roleCode.value === "agent" || roleCode.value === "sales");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/biz/device-events?openid=${encodeURIComponent(oid)}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
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

async function submitRemove() {
  err.value = "";
  if (!rDeviceNo.value.trim()) {
    err.value = "请填写设备编号";
    return;
  }
  try {
    const body = {
      openid: shell.loginOpenid,
      eventType: "R",
      deviceNo: rDeviceNo.value.trim(),
      remark: rRemark.value.trim() || null
    };
    if (rMerchantId.value) {
      body.merchantId = Number(rMerchantId.value);
    }
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
    rMerchantId.value = "";
    rRemark.value = "";
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

watch(() => shell.loginOpenid, () => {
  load();
  loadMerchants();
});
onMounted(() => {
  load();
  loadMerchants();
});
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>
    <p class="pf-muted" style="margin: 0 0 12px">设备新增、移除台账（推广「新增设备」登记为新增类记录）。</p>

    <div class="pf-toolbar">
      <button type="button" class="pf-tool pf-tool--ghost" @click="load">刷新</button>
      <button v-if="canLogRemove" type="button" class="pf-tool" @click="showRemove = true">登记移除</button>
    </div>

    <div class="pf-card">
      <div v-if="!rows.length" class="pf-muted" style="padding: 12px">暂无记录</div>
      <div v-for="(r, i) in rows" :key="i" class="pf-item">
        <div class="pf-line-strong">{{ r.deviceNo }} · {{ r.eventType }}</div>
        <div class="pf-line-muted">{{ r.createTime }} · 代理 #{{ r.agentId }}</div>
        <div class="pf-line-muted">门店 {{ r.merchantName || r.merchantId || "—" }}</div>
        <div v-if="r.remark" class="pf-line-muted">{{ r.remark }}</div>
      </div>
    </div>

    <div v-if="showRemove" class="pf-panel" style="margin-top: 14px">
      <h3 class="pf-panel-title">登记移除</h3>
      <div class="pf-row" style="border: none; padding: 0; min-height: auto; margin-bottom: 10px">
        <div v-if="isMain" class="pf-row" style="width: 100%; padding-left: 0; padding-right: 0">
          <div class="pf-label req">代理</div>
          <div class="pf-field-wrap">
            <input v-model="agentId" class="pf-field" type="text" style="border: 1px solid #e2e8f0; border-radius: 8px; padding: 8px" />
          </div>
        </div>
      </div>
      <div class="pf-row" style="border: none; padding: 0; flex-direction: column; align-items: stretch; gap: 10px">
        <input v-model="rDeviceNo" type="text" placeholder="设备编号 *" style="padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0" />
        <select v-model="rMerchantId" style="padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0">
          <option value="">关联门店（可选）</option>
          <option v-for="m in merchants" :key="m.merchantId" :value="String(m.merchantId)">{{ m.merchantName }}</option>
        </select>
        <input v-model="rRemark" type="text" placeholder="说明" style="padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0" />
        <div style="display: flex; gap: 8px">
          <button type="button" class="pf-submit" style="flex: 1" @click="submitRemove">提交</button>
          <button
            type="button"
            class="pf-tool pf-tool--ghost"
            style="flex: 1; padding: 14px"
            @click="showRemove = false"
          >
            取消
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
