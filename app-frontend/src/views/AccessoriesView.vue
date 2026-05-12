<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const router = useRouter();
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const summary = ref([]);
const types = ref([]);
const operators = ref([]);
const merchants = ref([]);
const err = ref("");
const form = ref({
  merchantId: "",
  typeId: "",
  inboundCost: 0,
  accCode: "",
  qty: 1,
  operatorKey: "",
  remark: ""
});

const canCreate = computed(() => roleCode.value === "agent" || roleCode.value === "sales");

async function load() {
  err.value = "";
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    const [sum, tp, ms] = await Promise.all([
      requestJson(`/app-api/biz/accessories?openid=${oid}`),
      requestJson("/app-api/biz/accessory-types"),
      requestJson(`/app-api/biz/merchants?openid=${oid}`)
    ]);
    summary.value = sum || [];
    types.value = tp || [];
    merchants.value = ms || [];
    if (canCreate.value) {
      operators.value = await requestJson(`/app-api/biz/accessory-operators?openid=${oid}`);
      if (!form.value.operatorKey && operators.value.length) {
        form.value.operatorKey = operators.value[0].operatorKey;
      }
    } else {
      operators.value = [];
    }
  } catch (e) {
    err.value = e.message || String(e);
    summary.value = [];
    types.value = [];
    merchants.value = [];
  }
}

function openType(row) {
  router.push({ name: "accessory-type-detail", params: { typeId: String(row.typeId) } });
}

async function onCreate() {
  err.value = "";
  try {
    const tid = Number(form.value.typeId);
    if (!tid) {
      throw new Error("请选择配件种类");
    }
    if (!form.value.operatorKey) {
      throw new Error("请选择入库操作人员");
    }
    const body = {
      openid: shell.loginOpenid,
      typeId: tid,
      inboundCost: Number(form.value.inboundCost),
      qty: Number(form.value.qty),
      operatorKey: form.value.operatorKey,
      remark: form.value.remark.trim() || null
    };
    const code = form.value.accCode.trim();
    if (code) {
      body.accCode = code;
    }
    if (form.value.merchantId) {
      body.merchantId = Number(form.value.merchantId);
    }
    await requestJson("/app-api/biz/accessories", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    form.value.inboundCost = 0;
    form.value.accCode = "";
    form.value.qty = 1;
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
      <h3 class="sub">配件入库</h3>
      <div class="row">
        <label>关联门店</label>
        <select v-model="form.merchantId" class="inp">
          <option value="">无（代理级库存）</option>
          <option v-for="m in merchants" :key="m.merchantId" :value="String(m.merchantId)">
            {{ m.merchantName }}（{{ m.merchantId }}）
          </option>
        </select>
      </div>
      <div class="row">
        <label>种类</label>
        <select v-model="form.typeId" class="inp">
          <option disabled value="">请选择</option>
          <option v-for="t in types" :key="t.typeId" :value="String(t.typeId)">{{ t.typeName }}</option>
        </select>
      </div>
      <div class="row">
        <label>入库成本</label>
        <input v-model.number="form.inboundCost" class="inp" type="number" min="0" step="0.01" />
      </div>
      <div class="row">
        <label>配件编号</label>
        <input v-model="form.accCode" class="inp" type="text" placeholder="选填" />
      </div>
      <div class="row">
        <label>数量</label>
        <input v-model.number="form.qty" class="inp" type="number" min="0.01" step="0.01" />
      </div>
      <div class="row">
        <label>操作人员</label>
        <select v-model="form.operatorKey" class="inp">
          <option v-for="o in operators" :key="o.operatorKey" :value="o.operatorKey">{{ o.label }}</option>
        </select>
      </div>
      <div class="row">
        <label>备注</label>
        <input v-model="form.remark" class="inp" type="text" />
      </div>
      <button type="button" class="btn" @click="onCreate">提交入库</button>
    </div>

    <div class="card">
      <h3 class="sub">按种类库存</h3>
      <p class="hint">点击查看该种类入库明细</p>
      <div v-if="!summary.length" class="muted">暂无数据</div>
      <button v-for="(r, i) in summary" :key="i" type="button" class="sum-row" @click="openType(r)">
        <span class="sum-name">{{ r.typeName }}</span>
        <span class="sum-meta">数量 {{ r.qtyTotal }} · 成本 ¥{{ r.costTotal }} · {{ r.lineCount }} 笔</span>
      </button>
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
.hint {
  margin: 0 0 8px;
  font-size: 11px;
  color: #94a3b8;
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
  grid-template-columns: 84px 1fr;
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
.sum-row {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
  width: 100%;
  text-align: left;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  cursor: pointer;
  font-size: 13px;
}
.sum-row:last-child {
  margin-bottom: 0;
}
.sum-name {
  font-weight: 600;
  color: #0f172a;
}
.sum-meta {
  font-size: 12px;
  color: #64748b;
}
</style>
