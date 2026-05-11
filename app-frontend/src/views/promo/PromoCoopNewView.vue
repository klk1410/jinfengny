<script setup>
import { computed, inject, ref } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../../api.js";

const shell = inject("appShell");
const router = useRouter();
const err = ref("");
const ok = ref("");
const partnerName = ref("");
const contactName = ref("");
const contactPhone = ref("");
const remark = ref("");
const agentId = ref("1");

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");

async function submit() {
  err.value = "";
  ok.value = "";
  try {
    const body = {
      openid: shell.loginOpenid,
      partnerName: partnerName.value.trim(),
      contactName: contactName.value.trim() || null,
      contactPhone: contactPhone.value.trim() || null,
      remark: remark.value.trim() || null
    };
    if (isMain.value) {
      body.agentId = Number(agentId.value) || null;
    }
    await requestJson("/app-api/promo/coops", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    ok.value = "已保存";
    router.push({ name: "promo-coops" });
  } catch (e) {
    err.value = e.message || String(e);
  }
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">推广 · 新增合作</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <p v-if="ok" class="ok">{{ ok }}</p>
    <div class="card">
      <div v-if="isMain" class="row">
        <label>代理 ID</label>
        <input v-model="agentId" class="inp" type="text" />
      </div>
      <div class="row">
        <label>合作方名称</label>
        <input v-model="partnerName" class="inp" type="text" placeholder="必填" />
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
        <label>备注</label>
        <input v-model="remark" class="inp" type="text" />
      </div>
      <button type="button" class="btn" @click="submit">提交</button>
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
.ok {
  color: #15803d;
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
