<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";
import "./promo-form.css";

const shell = inject("appShell");
const err = ref("");

const addMode = ref("inbound");
const deviceNo = ref("");
const merchantId = ref("");
const remark = ref("");
const agentId = ref("1");
const merchants = ref([]);

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");
const isMerchantMode = computed(() => addMode.value === "merchant");

async function loadMerchants() {
  const oid = encodeURIComponent(shell.loginOpenid);
  merchants.value = await requestJson(`/app-api/biz/merchants?openid=${oid}`);
}

watch(addMode, (v) => {
  if (v === "inbound") {
    merchantId.value = "";
  }
});

function validate() {
  if (!deviceNo.value.trim()) {
    err.value = "请填写设备编号";
    return false;
  }
  if (isMain.value && !String(agentId.value).trim()) {
    err.value = "主端请填写代理 ID";
    return false;
  }
  if (addMode.value === "merchant" && !String(merchantId.value).trim()) {
    err.value = "商家新增请选择商家";
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
      addMode: addMode.value,
      deviceNo: deviceNo.value.trim(),
      remark: remark.value.trim() || null
    };
    if (addMode.value === "merchant" && merchantId.value) {
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
    window.alert("已登记新增设备并写入设备台账");
    deviceNo.value = "";
    remark.value = "";
    merchantId.value = "";
    addMode.value = "inbound";
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
    <p class="pf-muted" style="margin: 0 0 12px">
      入库：设备状态为「在库」，不绑定商家；商家新增：须选择本代理下商家，状态为「在商家」。同时写入设备日志。
    </p>
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div class="pf-card">
      <div v-if="isMain" class="pf-row">
        <div class="pf-label req">代理</div>
        <div class="pf-field-wrap">
          <input v-model="agentId" class="pf-field" type="text" placeholder="主端填写代理 ID" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">新增方式</div>
        <div class="pf-field-wrap">
          <select v-model="addMode" class="pf-field">
            <option value="inbound">入库（在库）</option>
            <option value="merchant">商家新增</option>
          </select>
        </div>
        <span class="pf-chevron">▼</span>
      </div>

      <div class="pf-row">
        <div class="pf-label req">设备编号</div>
        <div class="pf-field-wrap">
          <input v-model="deviceNo" class="pf-field" type="text" placeholder="请填写设备编号" />
        </div>
      </div>

      <div v-if="isMerchantMode" class="pf-row">
        <div class="pf-label req">商家</div>
        <div class="pf-field-wrap">
          <select v-model="merchantId" class="pf-field">
            <option value="">请选择商家</option>
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
