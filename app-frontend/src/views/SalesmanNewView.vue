<script setup>
import { computed, inject, ref } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../api.js";
import "./promo/promo-form.css";

const shell = inject("appShell");
const router = useRouter();
const err = ref("");

const salesmanName = ref("");
const phone = ref("");

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isAgent = computed(() => roleCode.value === "agent");

function validate() {
  if (!isAgent.value) {
    err.value = "仅代理可新增运维";
    return false;
  }
  if (!salesmanName.value.trim()) {
    err.value = "请填写运维姓名";
    return false;
  }
  return true;
}

async function submit() {
  err.value = "";
  if (!validate()) return;
  try {
    await requestJson("/app-api/biz/salesmen", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        openid: shell.loginOpenid,
        salesmanName: salesmanName.value.trim(),
        phone: phone.value.trim() || null
      })
    });
    router.push({ name: "salesmen" });
  } catch (e) {
    err.value = e.message || String(e);
  }
}
</script>

<template>
  <div class="pf-page">
    <p v-if="!isAgent" class="pf-err">当前角色无权新增运维。</p>
    <p v-else-if="err" class="pf-err">{{ err }}</p>

    <div class="pf-card">
      <div class="pf-row">
        <div class="pf-label req">姓名</div>
        <div class="pf-field-wrap">
          <input v-model="salesmanName" class="pf-field" type="text" placeholder="请填写运维姓名" :disabled="!isAgent" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">手机</div>
        <div class="pf-field-wrap">
          <input v-model="phone" class="pf-field" type="tel" placeholder="选填" :disabled="!isAgent" />
        </div>
      </div>
    </div>

    <div class="pf-footer">
      <button type="button" class="pf-submit" :disabled="!isAgent" @click="submit">提交</button>
    </div>
  </div>
</template>
