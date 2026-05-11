<script setup>
import { computed, inject, ref } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../../api.js";

const shell = inject("appShell");
const router = useRouter();
const err = ref("");

const merchantName = ref("");
const contactName = ref("");
const contactPhone = ref("");
const industryType = ref("餐饮");
const province = ref("");
const city = ref("");
const district = ref("");
const addressDetail = ref("");
const oilUnitPrice = ref(0);
const merchantCommission = ref(0);
const salesmanId = ref("");
const agentId = ref("1");

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");

async function submit() {
  err.value = "";
  try {
    const body = {
      openid: shell.loginOpenid,
      merchantName: merchantName.value.trim(),
      contactName: contactName.value.trim() || null,
      contactPhone: contactPhone.value.trim() || null,
      industryType: industryType.value.trim() || null,
      province: province.value.trim() || null,
      city: city.value.trim() || null,
      district: district.value.trim() || null,
      addressDetail: addressDetail.value.trim() || null,
      oilUnitPrice: Number(oilUnitPrice.value) || 0,
      merchantCommission: Number(merchantCommission.value) || 0
    };
    if (isMain.value) {
      body.agentId = Number(agentId.value);
    }
    const sid = salesmanId.value.trim();
    if (sid) {
      body.salesmanId = Number(sid);
    }
    const res = await requestJson("/app-api/biz/merchants", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    window.alert(`已创建店铺 #${res.merchantId}`);
    router.push({ name: "promo-stores" });
  } catch (e) {
    err.value = e.message || String(e);
  }
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">推广 · 新增店铺</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="card">
      <div v-if="isMain" class="row">
        <label>代理 ID</label>
        <input v-model="agentId" class="inp" type="text" />
      </div>
      <div class="row">
        <label>店铺名称</label>
        <input v-model="merchantName" class="inp" type="text" placeholder="必填" />
      </div>
      <div class="row">
        <label>业务员 ID（可选）</label>
        <input v-model="salesmanId" class="inp" type="text" placeholder="留空则主端/代理不绑业务员；业务员登录时默认本人" />
      </div>
      <div class="row">
        <label>联系人</label>
        <input v-model="contactName" class="inp" type="text" />
      </div>
      <div class="row">
        <label>电话</label>
        <input v-model="contactPhone" class="inp" type="text" />
      </div>
      <div class="row">
        <label>业态</label>
        <input v-model="industryType" class="inp" type="text" />
      </div>
      <div class="row">
        <label>省 / 市 / 区</label>
        <div class="triple">
          <input v-model="province" class="inp" type="text" placeholder="省" />
          <input v-model="city" class="inp" type="text" placeholder="市" />
          <input v-model="district" class="inp" type="text" placeholder="区" />
        </div>
      </div>
      <div class="row">
        <label>详细地址</label>
        <input v-model="addressDetail" class="inp" type="text" />
      </div>
      <div class="row">
        <label>油单价（元）</label>
        <input v-model.number="oilUnitPrice" class="inp" type="number" min="0" step="0.01" />
      </div>
      <div class="row">
        <label>商家佣金（元）</label>
        <input v-model.number="merchantCommission" class="inp" type="number" min="0" step="0.01" />
      </div>
      <button type="button" class="btn" @click="submit">创建</button>
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
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}
.row {
  margin-bottom: 10px;
}
.row label {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}
.inp {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d0d7e2;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
}
.triple {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 6px;
}
.btn {
  margin-top: 8px;
  border: none;
  background: #1f6dff;
  color: #fff;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
}
</style>
