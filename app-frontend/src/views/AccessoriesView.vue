<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const rows = ref([]);
const merchants = ref([]);
const err = ref("");
const form = ref({
  agentId: "",
  merchantId: "",
  accName: "",
  qty: 1,
  unitPrice: 0,
  remark: ""
});

const canCreate = computed(() => roleCode.value === "main" || roleCode.value === "agent" || roleCode.value === "sales");
const isMain = computed(() => roleCode.value === "main");

async function load() {
  err.value = "";
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    const [acc, ms] = await Promise.all([
      requestJson(`/app-api/biz/accessories?openid=${oid}`),
      requestJson(`/app-api/biz/merchants?openid=${oid}`)
    ]);
    rows.value = acc || [];
    merchants.value = ms || [];
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
    merchants.value = [];
  }
}

async function onCreate() {
  err.value = "";
  try {
    if (!form.value.accName.trim()) {
      throw new Error("请填写配件名称");
    }
    const body = {
      openid: shell.loginOpenid,
      accessoryName: form.value.accName.trim(),
      qty: Number(form.value.qty),
      unitPrice: Number(form.value.unitPrice),
      remark: form.value.remark.trim() || null
    };
    if (isMain.value) {
      body.agentId = Number(form.value.agentId);
    }
    if (form.value.merchantId) {
      body.merchantId = Number(form.value.merchantId);
    }
    await requestJson("/app-api/biz/accessories", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    form.value.accName = "";
    form.value.qty = 1;
    form.value.unitPrice = 0;
    form.value.remark = "";
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
    <h2 class="page-title">配件管理</h2>
    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="canCreate" class="card">
      <h3 class="sub">新增配件记录</h3>
      <div v-if="isMain" class="row">
        <label>代理 ID</label>
        <input v-model="form.agentId" class="inp" type="number" min="1" />
      </div>
      <div class="row">
        <label>关联门店</label>
        <select v-model="form.merchantId" class="inp">
          <option value="">无（代理级）</option>
          <option v-for="m in merchants" :key="m.merchantId" :value="String(m.merchantId)">
            {{ m.merchantName }}
          </option>
        </select>
      </div>
      <div class="row">
        <label>配件名称</label>
        <input v-model="form.accName" class="inp" type="text" />
      </div>
      <div class="row">
        <label>数量</label>
        <input v-model.number="form.qty" class="inp" type="number" min="0.01" step="0.01" />
      </div>
      <div class="row">
        <label>单价</label>
        <input v-model.number="form.unitPrice" class="inp" type="number" min="0" step="0.01" />
      </div>
      <div class="row">
        <label>备注</label>
        <input v-model="form.remark" class="inp" type="text" />
      </div>
      <button type="button" class="btn" @click="onCreate">提交</button>
    </div>

    <div class="card">
      <h3 class="sub">配件列表</h3>
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-for="(r, i) in rows" :key="i" class="item">
        <div class="line strong">{{ r.accName }}</div>
        <div class="line">数量 {{ r.qty }} · 单价 ¥{{ r.unitPrice }} · 金额 ¥{{ r.amount }}</div>
        <div class="line muted">代理 #{{ r.agentId }} · 门店 {{ r.merchantName || r.merchantId || "—" }}</div>
        <div v-if="r.remark" class="line muted">{{ r.remark }}</div>
        <div class="line muted">{{ r.createTime }}</div>
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
.muted {
  color: #64748b;
  font-size: 12px;
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
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}
.inp {
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 12px;
}
.btn {
  margin-top: 6px;
  border: none;
  background: #1f6dff;
  color: #fff;
  border-radius: 6px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
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
</style>

