<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const merchants = ref([]);
const err = ref("");
const busy = ref(false);

const form = ref({
  merchantId: "",
  toMerchantId: "",
  orderType: "加油",
  bucketCount: 1
});

const currentMerchant = computed(() => {
  const id = form.value.merchantId;
  if (!id) {
    return null;
  }
  return merchants.value.find((m) => String(m.merchantId) === String(id)) || null;
});

const unitPriceForCalc = computed(() => {
  const m = currentMerchant.value;
  if (!m || form.value.orderType !== "加油") {
    return 0;
  }
  return Number(m.oilUnitPrice) || 0;
});

const amountTotal = computed(() => {
  if (form.value.orderType !== "加油") {
    return 0;
  }
  const n = Number(form.value.bucketCount) || 0;
  return Math.round(unitPriceForCalc.value * n * 100) / 100;
});

const canTransferMerchant = computed(() => roleCode.value !== "merchant");

const transferTargetOptions = computed(() => {
  const src = form.value.merchantId;
  if (!src) {
    return [];
  }
  return merchants.value.filter((m) => String(m.merchantId) !== String(src));
});

async function loadMerchants() {
  err.value = "";
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    merchants.value = await requestJson(`/app-api/biz/merchants?openid=${oid}`);
    if (roleCode.value === "merchant" && merchants.value.length === 1) {
      form.value.merchantId = String(merchants.value[0].merchantId);
    }
  } catch (e) {
    err.value = e.message || String(e);
    merchants.value = [];
  }
}

async function submitWithPay(payType) {
  err.value = "";
  busy.value = true;
  try {
    if (roleCode.value !== "merchant") {
      const mid = Number(form.value.merchantId);
      if (!mid) {
        throw new Error("请选择商家");
      }
    }
    if (form.value.orderType === "加油") {
      const n = Number(form.value.bucketCount);
      if (!n || n < 1) {
        throw new Error("请填写桶数");
      }
    }
    if (form.value.orderType === "转移商家") {
      if (!canTransferMerchant.value) {
        throw new Error("转移商家订单请由代理或主端发起");
      }
      const to = Number(form.value.toMerchantId);
      if (!to) {
        throw new Error("请选择目标门店");
      }
      if (String(form.value.merchantId) === String(form.value.toMerchantId)) {
        throw new Error("源门店与目标门店不能相同");
      }
    }
    const body = {
      openid: shell.loginOpenid,
      orderType: form.value.orderType,
      bucketCount: Number(form.value.bucketCount),
      payType
    };
    if (roleCode.value !== "merchant") {
      body.merchantId = Number(form.value.merchantId);
    }
    if (form.value.orderType === "转移商家") {
      body.toMerchantId = Number(form.value.toMerchantId);
    }
    await requestJson("/app-api/order/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    if (form.value.orderType === "维护" || form.value.orderType === "转移商家") {
      form.value.bucketCount = 1;
    }
    alert("订单已提交");
  } catch (e) {
    err.value = e.message || String(e);
  } finally {
    busy.value = false;
  }
}

watch(() => shell.loginOpenid, loadMerchants);
watch(
  () => form.value.orderType,
  (t) => {
    if ((t === "维护" || t === "转移商家") && (!form.value.bucketCount || form.value.bucketCount < 1)) {
      form.value.bucketCount = 1;
    }
    if (t !== "转移商家") {
      form.value.toMerchantId = "";
    }
  }
);
watch(
  () => form.value.merchantId,
  () => {
    if (form.value.orderType === "转移商家" && form.value.toMerchantId === form.value.merchantId) {
      form.value.toMerchantId = "";
    }
  }
);
onMounted(loadMerchants);
</script>

<template>
  <div class="page">
    <h2 class="page-title">提交订单</h2>
    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="roleCode !== 'merchant'" class="card">
      <h3 class="sub">选择商家</h3>
      <p v-if="form.orderType === '转移商家'" class="hint">转移商家：先选<strong>设备当前所在</strong>门店（源门店），再选目标门店。</p>
      <select v-model="form.merchantId" class="inp full">
        <option disabled value="">请选择商家</option>
        <option v-for="m in merchants" :key="m.merchantId" :value="String(m.merchantId)">
          {{ m.merchantName }}（{{ m.merchantId }}）
        </option>
      </select>
    </div>

    <div v-else class="card">
      <h3 class="sub">商家</h3>
      <div v-if="currentMerchant" class="locked">
        {{ currentMerchant.merchantName }}（{{ currentMerchant.merchantId }}）
      </div>
      <p v-else class="muted">未找到绑定门店，请联系管理员。</p>
    </div>

    <div v-if="form.orderType === '转移商家' && canTransferMerchant" class="card">
      <h3 class="sub">目标门店</h3>
      <select v-model="form.toMerchantId" class="inp full">
        <option disabled value="">请选择目标门店</option>
        <option v-for="m in transferTargetOptions" :key="m.merchantId" :value="String(m.merchantId)">
          {{ m.merchantName }}（{{ m.merchantId }}）
        </option>
      </select>
      <p v-if="form.merchantId && !transferTargetOptions.length" class="hint">本代理下无其他可选门店。</p>
    </div>

    <div class="card">
      <h3 class="sub">订单类型</h3>
      <select v-model="form.orderType" class="inp full">
        <option>加油</option>
        <option>维护</option>
        <option v-if="canTransferMerchant">转移商家</option>
      </select>
      <div v-if="form.orderType === '加油'" class="row mt">
        <label>桶数</label>
        <input v-model.number="form.bucketCount" class="inp" type="number" min="1" step="1" />
      </div>
      <p v-if="form.orderType === '加油' && currentMerchant" class="hint">
        按门店油价 ¥{{ unitPriceForCalc }}/桶 计费（不在订单中单独展示单价）
      </p>
      <p v-if="form.orderType === '维护'" class="hint">维护单按系统规则计价（当前为 0 元展示）。</p>
      <p v-if="form.orderType === '转移商家'" class="hint">
        确认订单并完工后，由业务员在工单结单时填写设备编号，系统将设备从源门店迁至目标门店。
      </p>
    </div>

    <div class="footer-bar">
      <div class="total">
        应付合计
        <span class="amt">¥{{ amountTotal }}</span>
      </div>
      <div class="pay-btns">
        <button type="button" class="btn wx" :disabled="busy || (roleCode === 'merchant' && !currentMerchant)" @click="submitWithPay('微信支付')">
          微信支付
        </button>
        <button type="button" class="btn credit" :disabled="busy || (roleCode === 'merchant' && !currentMerchant)" @click="submitWithPay('赊销')">
          赊销
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
.inp.full {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
}
.row {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 8px;
  align-items: center;
  font-size: 13px;
}
.row.mt {
  margin-top: 10px;
}
.inp {
  border: 1px solid #d0d7e2;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 13px;
}
.hint {
  margin: 8px 0 0;
  font-size: 11px;
  color: #64748b;
}
.muted {
  color: #64748b;
  font-size: 12px;
}
.locked {
  padding: 10px 12px;
  background: #f1f5f9;
  border-radius: 8px;
  font-size: 14px;
  color: #334155;
}
.footer-bar {
  position: sticky;
  bottom: 0;
  margin: 16px -4px 0;
  padding: 12px;
  background: linear-gradient(180deg, rgba(238, 241, 246, 0) 0%, #eef1f6 12%);
}
.total {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  margin-bottom: 10px;
}
.amt {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
.pay-btns {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.btn {
  flex: 1;
  max-width: 160px;
  border: none;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn.wx {
  background: #07c160;
  color: #fff;
}
.btn.credit {
  background: #1f6dff;
  color: #fff;
}
</style>
