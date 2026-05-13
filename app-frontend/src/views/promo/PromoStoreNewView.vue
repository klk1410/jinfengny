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
const longitude = ref(null);
const latitude = ref(null);
const province = ref("");
const city = ref("");
const district = ref("");
const addressDetail = ref("");
const salesmanId = ref("");
const remark = ref("");
const submitRemark = ref("");
const storeImageUrl = ref("");
const imagePreview = ref("");
const contractImageUrl = ref("");
const contractPreview = ref("");
const mapLocationInfo = ref("");
const locating = ref(false);
const agentId = ref("1");
const oilTypes = ref([]);
const oilTypeId = ref("1");
const depositAmount = ref(0);

const industryOptions = ["餐饮", "酒店", "工厂", "学校", "企事业单位", "其他"];
const salesmen = ref([]);

const industrySelectOptions = computed(() => industryOptions.map((x) => ({ value: x, label: x })));
const salesmanSelectOptions = computed(() => [
  { value: "", label: "选择运维" },
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
/** 运维、商家新增店铺走审核，不直接落库 */
const needsCreateAudit = computed(() => roleCode.value === "sales" || roleCode.value === "merchant");

const hasCoords = computed(() => {
  const lon = Number(longitude.value);
  const lat = Number(latitude.value);
  return !Number.isNaN(lon) && !Number.isNaN(lat);
});

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

function pickLocationFromBrowser() {
  if (!navigator.geolocation) {
    err.value = "当前浏览器不支持定位；请展开下方手动填写经纬度（建议使用 HTTPS）。";
    return;
  }
  locating.value = true;
  err.value = "";
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      locating.value = false;
      longitude.value = pos.coords.longitude;
      latitude.value = pos.coords.latitude;
      const acc = pos.coords.accuracy != null ? Math.round(pos.coords.accuracy) : "—";
      mapLocationInfo.value = `浏览器定位 · 精度约 ${acc} m`;
    },
    (e) => {
      locating.value = false;
      err.value = (e && e.message) || "定位失败，请检查浏览器定位权限，或使用下方手动填写经纬度。";
    },
    { enableHighAccuracy: true, timeout: 20000, maximumAge: 0 }
  );
}

function onManualCoordInput() {
  if (!mapLocationInfo.value.trim() && hasCoords.value) {
    mapLocationInfo.value = "手动录入坐标";
  }
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

function onContractImageChange(ev) {
  const f = ev.target.files && ev.target.files[0];
  if (!f) return;
  if (f.size > 600 * 1024) {
    err.value = "合同图片请小于 600KB，或改用图片外链";
    return;
  }
  const r = new FileReader();
  r.onload = () => {
    const data = r.result;
    if (typeof data !== "string" || data.length > 8000) {
      err.value = "合同图片编码后过长，请换更小图片或压缩后再传";
      return;
    }
    err.value = "";
    contractImageUrl.value = data;
    contractPreview.value = data;
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
    err.value = "请点击「获取定位」填写地图坐标，或手动输入经纬度";
    return false;
  }
  if (!mapLocationInfo.value.trim()) {
    mapLocationInfo.value = "手动录入坐标";
  }
  if (mapLocationInfo.value.length > 500) {
    err.value = "地图定位说明过长";
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
  if (!contractImageUrl.value.trim()) {
    err.value = "请上传合同图片";
    return false;
  }
  const dep = Number(depositAmount.value);
  if (Number.isNaN(dep)) {
    err.value = "请填写有效押金金额";
    return false;
  }
  if (dep < 0) {
    err.value = "押金须大于等于 0";
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
      depositAmount: Math.round(Number(depositAmount.value) * 100) / 100,
      remark: remark.value.trim() || null,
      storeImageUrl: storeImageUrl.value || null,
      contractImageUrl: contractImageUrl.value.trim(),
      mapLocationInfo: mapLocationInfo.value.trim()
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

      <div class="pf-row pf-row--stack pf-row--loc">
        <div class="pf-label req">地图定位</div>
        <div class="pf-field-wrap pf-loc-panel">
          <button type="button" class="pf-loc-btn" :disabled="locating" @click="pickLocationFromBrowser">
            {{ locating ? "定位中…" : "获取当前位置" }}
          </button>
          <p v-if="hasCoords" class="pf-loc-summary">
            经度 {{ Number(longitude).toFixed(6) }} · 纬度 {{ Number(latitude).toFixed(6) }}
          </p>
          <p v-if="mapLocationInfo" class="pf-loc-meta">{{ mapLocationInfo }}</p>
          <details class="pf-loc-fallback">
            <summary>无法自动定位？手动输入经纬度</summary>
            <div class="pf-loc-manual">
              <label class="pf-loc-manual-line"
                >经度 <input v-model.number="longitude" type="number" step="any" placeholder="如 113.264" @input="onManualCoordInput"
              /></label>
              <label class="pf-loc-manual-line"
                >纬度 <input v-model.number="latitude" type="number" step="any" placeholder="如 23.129" @input="onManualCoordInput"
              /></label>
            </div>
          </details>
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
        <div class="pf-label req">押金（元）</div>
        <div class="pf-field-wrap">
          <input v-model.number="depositAmount" class="pf-field" type="number" min="0" step="0.01" placeholder="0 表示无押金" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">运维</div>
        <div class="pf-field-wrap pf-field-wrap--select">
          <PfSelect v-model="salesmanId" :options="salesmanSelectOptions" placeholder="选择运维" />
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

      <div class="pf-row pf-upload-row">
        <div class="pf-label req">合同图片</div>
        <label class="pf-upload">
          <input type="file" accept="image/*" @change="onContractImageChange" />
          <span>上传合同</span>
          <span aria-hidden="true">📄</span>
          <img v-if="contractPreview" :src="contractPreview" alt="" class="pf-thumb" />
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
