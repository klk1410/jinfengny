<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");
const busy = ref(false);

const form = ref({
  merchantId: "",
  orderType: "加油",
  unitPrice: 300,
  bucketCount: 1,
  payType: "现结"
});

async function load() {
  err.value = "";
  busy.value = true;
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/order/list?openid=${encodeURIComponent(oid)}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
  } finally {
    busy.value = false;
  }
}

async function onCancel(orderNo) {
  if (!window.confirm(`取消订单 ${orderNo}？`)) {
    return;
  }
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    await requestJson(`/app-api/order/cancel/${encodeURIComponent(orderNo)}?openid=${encodeURIComponent(oid)}`, {
      method: "POST"
    });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function onCreate() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    const mid = Number(form.value.merchantId);
    if (!mid) {
      throw new Error("请填写商家 ID");
    }
    await requestJson("/app-api/order/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        openid: oid,
        merchantId: mid,
        orderType: form.value.orderType,
        unitPrice: form.value.unitPrice,
        bucketCount: form.value.bucketCount,
        payType: form.value.payType
      })
    });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

watch(() => shell.loginOpenid, () => {
  load();
});

onMounted(() => {
  load();
});
</script>

<template>
  <div class="page">
    <h2 class="page-title">订单</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <p v-if="busy" class="muted">加载中…</p>

    <div class="card">
      <h3 class="sub">新建订单</h3>
      <div class="row">
        <label>商家 ID</label>
        <input v-model="form.merchantId" class="inp" type="number" min="1" placeholder="如 1" />
      </div>
      <div class="row">
        <label>类型</label>
        <select v-model="form.orderType" class="inp">
          <option>加油</option>
          <option>维护</option>
        </select>
      </div>
      <div class="row">
        <label>单价</label>
        <input v-model.number="form.unitPrice" class="inp" type="number" min="0" step="0.01" />
      </div>
      <div class="row">
        <label>桶数</label>
        <input v-model.number="form.bucketCount" class="inp" type="number" min="1" step="1" />
      </div>
      <div class="row">
        <label>支付</label>
        <select v-model="form.payType" class="inp">
          <option>现结</option>
          <option>赊欠</option>
        </select>
      </div>
      <button type="button" class="btn" @click="onCreate">提交</button>
    </div>

    <div class="card">
      <h3 class="sub">列表</h3>
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-for="(o, i) in rows" :key="i" class="item">
        <div class="item-top">
          <span class="no">{{ o.orderNo }}</span>
          <span class="st">{{ o.status }}</span>
        </div>
        <div class="item-mid">{{ o.merchantName }} · {{ o.orderType }} · {{ o.payType }}</div>
        <div class="item-bot">
          <span>¥{{ o.amountPayable }}</span>
          <span class="muted">{{ o.createTime }}</span>
        </div>
        <button
          v-if="o.status !== '订单取消' && o.status !== '已完成'"
          type="button"
          class="link"
          @click="onCancel(o.orderNo)"
        >
          取消
        </button>
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
.item-top {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
}
.item-mid,
.item-bot {
  margin-top: 4px;
  color: #334155;
}
.item-bot {
  display: flex;
  justify-content: space-between;
}
.link {
  margin-top: 6px;
  border: none;
  background: none;
  color: #1f6dff;
  cursor: pointer;
  padding: 0;
  font-size: 12px;
}
</style>
