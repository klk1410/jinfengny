<script setup>
import { computed, inject, onMounted, ref } from "vue";
import { requestJson } from "../../api.js";
import "./promo-form.css";

const shell = inject("appShell");
const err = ref("");

const deviceNo = ref("");
const merchantId = ref("");
const remark = ref("");
const agentId = ref("1");
const merchants = ref([]);

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");

async function loadMerchants() {
  const oid = encodeURIComponent(shell.loginOpenid);
  merchants.value = await requestJson(`/app-api/biz/merchants?openid=${oid}`);
}

function validate() {
  if (!deviceNo.value.trim()) {
    err.value = "请填写设备编号";
    return false;
  }
  if (isMain.value && !String(agentId.value).trim()) {
    err.value = "主端请填写代理 ID";
    return false;
  }
  return true;
}

async function submit() {
  err.value = "";
  if (!validate()) return;
  try {
    const body = {
      openid: shell.loginOpenid,
      eventType: "A",
      deviceNo: deviceNo.value.trim(),
      remark: remark.value.trim() || null
    };
    if (merchantId.value) {
      body.merchantId = Number(merchantId.value);
    }
    if (isMain.value) {
      body.agentId = Number(agentId.value);
    }
    await requestJson("/app-api/biz/device-events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    window.alert("已登记新增设备记录");
    deviceNo.value = "";
    remark.value = "";
  } catch (e) {
    err.value = e.message || String(e);
  }
}

onMounted(() => {
  loadMerchants().catch((e) => {
    err.value = e.message || String(e);
  });
});
</script>

<template>
  <div class="pf-page">
    <p class="pf-muted" style="margin: 0 0 12px">登记新设备安装 / 到货记录（写入设备日志-新增）。</p>
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div class="pf-card">
      <div v-if="isMain" class="pf-row">
        <div class="pf-label req">代理</div>
        <div class="pf-field-wrap">
          <input v-model="agentId" class="pf-field" type="text" placeholder="主端填写代理 ID" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">设备编号</div>
        <div class="pf-field-wrap">
          <input v-model="deviceNo" class="pf-field" type="text" placeholder="请填写设备编号" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">关联门店</div>
        <div class="pf-field-wrap">
          <select v-model="merchantId" class="pf-field">
            <option value="">选择门店（可选）</option>
            <option v-for="m in merchants" :key="m.merchantId" :value="String(m.merchantId)">
              {{ m.merchantName }}
            </option>
          </select>
        </div>
        <span class="pf-chevron">▼</span>
      </div>

      <div class="pf-row">
        <div class="pf-label">说明</div>
        <div class="pf-field-wrap">
          <input v-model="remark" class="pf-field" type="text" placeholder="备注" />
        </div>
      </div>
    </div>

    <div class="pf-footer">
      <button type="button" class="pf-submit" @click="submit">提交</button>
    </div>
  </div>
</template>
