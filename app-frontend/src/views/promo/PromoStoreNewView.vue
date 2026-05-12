<script setup>
import { computed, inject, onMounted, ref, unref } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import PfSelect from "../../components/PfSelect.vue";
import "./promo-form.css";

const shell = inject("appShell");
const router = useRouter();
const err = ref("");

const industryType = ref("");
const merchantName = ref("");
const contactName = ref("");
const contactPhone = ref("");
const longitude = ref(0);
const latitude = ref(0);
const province = ref("");
const city = ref("");
const district = ref("");
const addressDetail = ref("");
const salesmanId = ref("");
const remark = ref("");
const submitRemark = ref("");
const storeImageUrl = ref("");
const imagePreview = ref("");
const agentId = ref("1");
const oilTypes = ref([]);
const oilTypeId = ref("1");

const industryOptions = ["餐饮", "酒店", "工厂", "学校", "企事业单位", "其他"];
const salesmen = ref([]);

const industrySelectOptions = computed(() => industryOptions.map((x) => ({ value: x, label: x })));
const salesmanSelectOptions = computed(() => [
  { value: "", label: "选择业务员" },
  ...salesmen.value.map((s) => ({
    value: String(s.salesmanId),
    label: `${s.salesmanName} · ${s.phone || "—"}`
  }))
]);

const oilTypeSelectOptions = computed(() =>
  (oilTypes.value || []).map((x) => ({
    value: String(x.oilTypeId),
    label: x.typeName
  }))
);

const roleCode = computed(() => shell.roleCode ?? unref(shell.portal)?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");
/** 业务员、商家新增店铺走审核，不直接落库 */
const needsCreateAudit = computed(() => roleCode.value === "sales" || roleCode.value === "merchant");

async function loadPickers() {
  const oid = encodeURIComponent(unref(shell.loginOpenid));
  salesmen.value = (await requestJson(`/app-api/biz/salesmen?openid=${oid}`)) || [];
}

async function loadOilTypes() {
  try {
    oilTypes.value = (await requestJson("/app-api/biz/oil-types")) || [];
    if (oilTypes.value.length && !oilTypes.value.find((x) => String(x.oilTypeId) === oilTypeId.value)) {
      oilTypeId.value = String(oilTypes.value[0].oilTypeId);
    }
  } catch {
    oilTypes.value = [];
  }
}

function openMapHint() {
  window.alert("正式环境可在此接入腾讯地图 / 高德地图选点，当前请手工填写经纬度。");
}

function onImageChange(ev) {
  const f = ev.target.files && ev.target.files[0];
  if (!f) return;
  if (f.size > 600 * 1024) {
    err.value = "图片请小于 600KB，或改用图片外链";
    return;
  }
  const r = new FileReader();
  r.onload = () => {
    const data = r.result;
    if (typeof data !== "string" || data.length > 8000) {
      err.value = "图片编码后过长，请换更小图片或压缩后再传";
      return;
    }
    err.value = "";
    storeImageUrl.value = data;
    imagePreview.value = data;
  };
  r.readAsDataURL(f);
}

function validate() {
  if (!industryType.value) {
    err.value = "请选择所属行业";
    return false;
  }
  if (!merchantName.value.trim()) {
    err.value = "请填写店铺名称";
    return false;
  }
  if (!contactName.value.trim()) {
    err.value = "请填写联系人";
    return false;
  }
  if (!contactPhone.value.trim()) {
    err.value = "请填写联系电话";
    return false;
  }
  const lon = Number(longitude.value);
  const lat = Number(latitude.value);
  if (Number.isNaN(lon) || Number.isNaN(lat)) {
    err.value = "请填写有效经纬度";
    return false;
  }
  if (!province.value.trim() || !city.value.trim() || !district.value.trim()) {
    err.value = "请填写省、市、区";
    return false;
  }
  if (!addressDetail.value.trim()) {
    err.value = "请填写详细地址";
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
      openid: unref(shell.loginOpenid),
      industryType: industryType.value,
      merchantName: merchantName.value.trim(),
      contactName: contactName.value.trim(),
      contactPhone: contactPhone.value.trim(),
      longitude: Number(longitude.value),
      latitude: Number(latitude.value),
      province: province.value.trim(),
      city: city.value.trim(),
      district: district.value.trim(),
      addressDetail: addressDetail.value.trim(),
      oilUnitPrice: 0,
      oilTypeId: Number(oilTypeId.value) || 1,
      merchantCommission: 0,
      remark: remark.value.trim() || null,
      storeImageUrl: storeImageUrl.value || null
    };
    if (isMain.value) {
      body.agentId = Number(agentId.value);
    }
    if (salesmanId.value) {
      body.salesmanId = Number(salesmanId.value);
    }
    if (submitRemark.value.trim()) {
      body.submitRemark = submitRemark.value.trim();
    }
    const res = await requestJson("/app-api/biz/merchants", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    if (needsCreateAudit.value && res.auditId != null) {
      window.alert(`已提交审核，审核单 #${res.auditId}。请等待主端或代理审批通过后门店生效。`);
      router.push({ name: "promo-merchant-audits" });
      return;
    }
    window.alert(`提交成功，店铺 #${res.merchantId}`);
    router.push({ name: "promo-stores" });
  } catch (e) {
    err.value = e.message || String(e);
  }
}

onMounted(() => {
  loadOilTypes();
  loadPickers().catch((e) => {
    err.value = e.message || String(e);
  });
});
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div class="pf-card">
      <div class="pf-row">
        <div class="pf-label req">所属行业</div>
        <div class="pf-field-wrap pf-field-wrap--select">
          <PfSelect v-model="industryType" :options="industrySelectOptions" placeholder="选择所属行业" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">主营油品</div>
        <div class="pf-field-wrap pf-field-wrap--select">
          <PfSelect v-model="oilTypeId" :options="oilTypeSelectOptions" placeholder="选择油品种类" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">店铺名称</div>
        <div class="pf-field-wrap">
          <input v-model="merchantName" class="pf-field" type="text" placeholder="请填写店铺名称" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">联系人</div>
        <div class="pf-field-wrap">
          <input v-model="contactName" class="pf-field" type="text" placeholder="请填写联系人" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">联系电话</div>
        <div class="pf-field-wrap">
          <input v-model="contactPhone" class="pf-field" type="tel" placeholder="请填写联系电话" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">经纬度</div>
        <div class="pf-field-wrap pf-geo">
          <div class="pf-geo-line">
            <input v-model.number="longitude" type="number" step="any" placeholder="经度" />
          </div>
          <div class="pf-geo-line">
            <input v-model.number="latitude" type="number" step="any" placeholder="纬度" />
            <button type="button" class="pf-map-btn" title="地图选点" @click="openMapHint">📍</button>
          </div>
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">省市区</div>
        <div class="pf-field-wrap pf-region">
          <input v-model="province" class="pf-field" type="text" placeholder="省" />
          <input v-model="city" class="pf-field" type="text" placeholder="市" />
          <input v-model="district" class="pf-field" type="text" placeholder="区" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">详细地址</div>
        <div class="pf-field-wrap">
          <input v-model="addressDetail" class="pf-field" type="text" placeholder="请填写详细地址" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">业务员</div>
        <div class="pf-field-wrap pf-field-wrap--select">
          <PfSelect v-model="salesmanId" :options="salesmanSelectOptions" placeholder="选择业务员" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">说明</div>
        <div class="pf-field-wrap">
          <input v-model="remark" class="pf-field" type="text" placeholder="请填写说明" />
        </div>
      </div>

      <div v-if="needsCreateAudit" class="pf-row">
        <div class="pf-label">审核说明</div>
        <div class="pf-field-wrap">
          <input v-model="submitRemark" class="pf-field" type="text" placeholder="选填，提交给审批方" />
        </div>
      </div>

      <div class="pf-row pf-upload-row">
        <div class="pf-label">店铺图片</div>
        <label class="pf-upload">
          <input type="file" accept="image/*" @change="onImageChange" />
          <span>上传图片</span>
          <span aria-hidden="true">☁</span>
          <img v-if="imagePreview" :src="imagePreview" alt="" class="pf-thumb" />
        </label>
      </div>

      <div v-if="isMain" class="pf-row">
        <div class="pf-label req">代理</div>
        <div class="pf-field-wrap">
          <input v-model="agentId" class="pf-field" type="text" placeholder="主端填写代理 ID" />
        </div>
      </div>
    </div>

    <div class="pf-footer">
      <button type="button" class="pf-submit" @click="submit">{{ needsCreateAudit ? "提交审核" : "提交" }}</button>
    </div>
  </div>
</template>
