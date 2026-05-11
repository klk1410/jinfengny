<script setup>
import { computed, inject, ref } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import "./promo-form.css";

const shell = inject("appShell");
const router = useRouter();
const err = ref("");

const partnerName = ref("");
const contactName = ref("");
const contactPhone = ref("");
const remark = ref("");
const agentId = ref("1");

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");

function validate() {
  if (!partnerName.value.trim()) {
    err.value = "请填写合作方名称";
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
      partnerName: partnerName.value.trim(),
      contactName: contactName.value.trim() || null,
      contactPhone: contactPhone.value.trim() || null,
      remark: remark.value.trim() || null
    };
    if (isMain.value) {
      body.agentId = Number(agentId.value);
    }
    await requestJson("/app-api/promo/coops", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    router.push({ name: "promo-coops" });
  } catch (e) {
    err.value = e.message || String(e);
  }
}
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div class="pf-card">
      <div v-if="isMain" class="pf-row">
        <div class="pf-label req">代理</div>
        <div class="pf-field-wrap">
          <input v-model="agentId" class="pf-field" type="text" placeholder="主端填写代理 ID" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">合作方名称</div>
        <div class="pf-field-wrap">
          <input v-model="partnerName" class="pf-field" type="text" placeholder="请填写合作方名称" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">联系人</div>
        <div class="pf-field-wrap">
          <input v-model="contactName" class="pf-field" type="text" placeholder="请填写联系人" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">联系电话</div>
        <div class="pf-field-wrap">
          <input v-model="contactPhone" class="pf-field" type="tel" placeholder="请填写联系电话" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">说明</div>
        <div class="pf-field-wrap">
          <input v-model="remark" class="pf-field" type="text" placeholder="请填写说明" />
        </div>
      </div>
    </div>

    <div class="pf-footer">
      <button type="button" class="pf-submit" @click="submit">提交</button>
    </div>
  </div>
</template>
