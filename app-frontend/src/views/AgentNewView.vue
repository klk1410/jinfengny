<script setup>
import { computed, inject, ref } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../api.js";
import "./promo/promo-form.css";

const shell = inject("appShell");
const router = useRouter();
const err = ref("");

const agentName = ref("");
const contactName = ref("");
const contactPhone = ref("");
const province = ref("");
const city = ref("");
const district = ref("");
const addressDetail = ref("");

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");

function validate() {
  if (!isMain.value) {
    err.value = "仅主端可新增代理";
    return false;
  }
  if (!agentName.value.trim()) {
    err.value = "请填写代理名称";
    return false;
  }
  return true;
}

async function submit() {
  err.value = "";
  if (!validate()) return;
  try {
    await requestJson("/app-api/biz/agents", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        openid: shell.loginOpenid,
        agentName: agentName.value.trim(),
        contactName: contactName.value.trim() || null,
        contactPhone: contactPhone.value.trim() || null,
        province: province.value.trim() || null,
        city: city.value.trim() || null,
        district: district.value.trim() || null,
        addressDetail: addressDetail.value.trim() || null
      })
    });
    router.push({ name: "agents" });
  } catch (e) {
    err.value = e.message || String(e);
  }
}
</script>

<template>
  <div class="pf-page">
    <p v-if="!isMain" class="pf-err">当前角色无权新增代理。</p>
    <p v-else-if="err" class="pf-err">{{ err }}</p>

    <div class="pf-card">
      <div class="pf-row">
        <div class="pf-label req">代理名称</div>
        <div class="pf-field-wrap">
          <input v-model="agentName" class="pf-field" type="text" placeholder="请填写代理名称" :disabled="!isMain" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">联系人</div>
        <div class="pf-field-wrap">
          <input v-model="contactName" class="pf-field" type="text" placeholder="选填" :disabled="!isMain" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">联系电话</div>
        <div class="pf-field-wrap">
          <input v-model="contactPhone" class="pf-field" type="tel" placeholder="选填" :disabled="!isMain" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">省</div>
        <div class="pf-field-wrap">
          <input v-model="province" class="pf-field" type="text" placeholder="选填" :disabled="!isMain" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">市</div>
        <div class="pf-field-wrap">
          <input v-model="city" class="pf-field" type="text" placeholder="选填" :disabled="!isMain" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">区</div>
        <div class="pf-field-wrap">
          <input v-model="district" class="pf-field" type="text" placeholder="选填" :disabled="!isMain" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">详细地址</div>
        <div class="pf-field-wrap">
          <input v-model="addressDetail" class="pf-field" type="text" placeholder="选填" :disabled="!isMain" />
        </div>
      </div>
    </div>

    <div class="pf-footer">
      <button type="button" class="pf-submit" :disabled="!isMain" @click="submit">提交</button>
    </div>
  </div>
</template>
