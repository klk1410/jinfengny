<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import PfSelect from "../../components/PfSelect.vue";
import "./promo-form.css";
import { entityOnOffPillClass } from "../../utils/statusDisplay.js";

const shell = inject("appShell");
const route = useRoute();
const router = useRouter();
const err = ref("");
const loading = ref(true);

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
const oilUnitPrice = ref(0);
const oilTypeId = ref("1");
const oilTypes = ref([]);
const merchantCommission = ref(0);
const salesmanId = ref("");
const remark = ref("");
const storeImageUrl = ref("");
const imagePreview = ref("");
const contractImageUrl = ref("");
const contractPreview = ref("");
const mapLocationInfo = ref("");
const locating = ref(false);
const submitRemark = ref("");

const detail = ref(null);
const salesmen = ref([]);

const merchantId = computed(() => Number(route.params.merchantId));

const canDirectEdit = computed(() => detail.value?.canDirectEdit === true);
const canSubmitAudit = computed(() => detail.value?.canSubmitAudit === true);
const readOnly = computed(() => !canDirectEdit.value && !canSubmitAudit.value);

const industryOptions = ["餐饮", "酒店", "工厂", "学校", "企事业单位", "其他"];

const industrySelectOptions = computed(() => industryOptions.map((x) => ({ value: x, label: x })));
const salesmanSelectOptions = computed(() => [
  { value: "", label: "不指定" },
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

const hasCoords = computed(() => {
  const lon = Number(longitude.value);
  const lat = Number(latitude.value);
  return !Number.isNaN(lon) && !Number.isNaN(lat);
});

async function loadOilTypes() {
  try {
    oilTypes.value = (await requestJson("/app-api/biz/oil-types")) || [];
  } catch {
    oilTypes.value = [];
  }
}

async function loadPickers() {
  const oid = encodeURIComponent(shell.loginOpenid);
  salesmen.value = (await requestJson(`/app-api/biz/salesmen?openid=${oid}`)) || [];
}

function applyDetail(d) {
  detail.value = d;
  industryType.value = d.industryType || "";
  merchantName.value = d.merchantName || "";
  contactName.value = d.contactName || "";
  contactPhone.value = d.contactPhone || "";
  longitude.value = d.longitude ?? null;
  latitude.value = d.latitude ?? null;
  mapLocationInfo.value = d.mapLocationInfo || "";
  contractImageUrl.value = d.contractImageUrl || "";
  contractPreview.value = d.contractImageUrl || "";
  province.value = d.province || "";
  city.value = d.city || "";
  district.value = d.district || "";
  addressDetail.value = d.addressDetail || "";
  oilUnitPrice.value = d.oilUnitPrice ?? 0;
  oilTypeId.value = d.oilTypeId != null ? String(d.oilTypeId) : "1";
  merchantCommission.value = d.merchantCommission ?? 0;
  salesmanId.value = d.salesmanId != null ? String(d.salesmanId) : "";
  remark.value = d.remark || "";
  storeImageUrl.value = d.storeImageUrl || "";
  imagePreview.value = d.storeImageUrl || "";
}

async function load() {
  err.value = "";
  loading.value = true;
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    const d = await requestJson(`/app-api/biz/merchants/detail?openid=${oid}&merchantId=${merchantId.value}`);
    applyDetail(d);
    if (canDirectEdit.value) {
      await loadPickers();
    }
  } catch (e) {
    err.value = e.message || String(e);
    detail.value = null;
  } finally {
    loading.value = false;
  }
}

function pickLocationFromBrowser() {
  if (!navigator.geolocation) {
    err.value = "当前浏览器不支持定位；请手动填写经纬度。";
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
      err.value = (e && e.message) || "定位失败，请检查权限或手动填写经纬度。";
    },
    { enableHighAccuracy: true, timeout: 20000, maximumAge: 0 }
  );
}

function onManualCoordInput() {
  if (!mapLocationInfo.value.trim() && hasCoords.value) {
    mapLocationInfo.value = detail.value?.mapLocationInfo || "手动录入坐标";
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
    err.value = "合同图片请小于 600KB";
    return;
  }
  const r = new FileReader();
  r.onload = () => {
    const data = r.result;
    if (typeof data !== "string" || data.length > 8000) {
      err.value = "合同图片编码后过长";
      return;
    }
    err.value = "";
    contractImageUrl.value = data;
    contractPreview.value = data;
  };
  r.readAsDataURL(f);
}

function buildWriteBody() {
  let ml = mapLocationInfo.value.trim();
  if (!ml && hasCoords.value) {
    ml = detail.value?.mapLocationInfo || "手动录入坐标";
  }
  return {
    openid: shell.loginOpenid,
    merchantId: merchantId.value,
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
    oilUnitPrice: Number(oilUnitPrice.value) || 0,
    oilTypeId: Number(oilTypeId.value) || 1,
    merchantCommission: Number(merchantCommission.value) || 0,
    remark: remark.value.trim() || null,
    storeImageUrl: storeImageUrl.value || null,
    contractImageUrl: contractImageUrl.value || null,
    mapLocationInfo: ml || null,
    salesmanId: salesmanId.value ? Number(salesmanId.value) : null,
    linkedMerchantId: null
  };
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
  if (!contactName.value.trim() || !contactPhone.value.trim()) {
    err.value = "请填写联系人与电话";
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
  const ml = mapLocationInfo.value.trim() || detail.value?.mapLocationInfo || "";
  if (ml.length > 500) {
    err.value = "地图定位说明过长";
    return false;
  }
  return true;
}

async function saveDirect() {
  err.value = "";
  if (!validate()) return;
  try {
    await requestJson("/app-api/biz/merchants", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(buildWriteBody())
    });
    window.alert("已保存");
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function submitAudit() {
  err.value = "";
  if (!validate()) return;
  if (detail.value?.hasPendingMyAudit) {
    err.value = "已有待审修改单，请等待代理或主端处理";
    return;
  }
  try {
    const body = buildWriteBody();
    body.submitRemark = submitRemark.value.trim() || null;
    await requestJson("/app-api/biz/merchant-audits", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    window.alert("已提交审核，请通知代理或主端处理");
    submitRemark.value = "";
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

watch(
  () => [shell.loginOpenid, route.params.merchantId],
  () => {
    load();
  }
);
onMounted(() => {
  loadOilTypes();
  load();
});
</script>

<template>
  <div class="pf-page">
    <p v-if="loading" class="pf-muted">加载中…</p>
    <template v-else-if="detail">
      <p v-if="err" class="pf-err">{{ err }}</p>
      <p v-if="detail.hasPendingMyAudit && canSubmitAudit" class="pf-muted" style="margin-bottom: 10px">
        您已有一条待审修改，审批完成前不能再次提交。
      </p>
      <p v-if="readOnly" class="pf-muted" style="margin-bottom: 10px">当前账号仅可查看。</p>

      <div class="dc-stack">
        <article class="dc-card dc-card--white">
        <div class="pf-row">
          <div class="pf-label">店铺 ID</div>
          <div class="pf-field-wrap">
            <span class="pf-muted">#{{ detail.merchantId }}</span>
          </div>
        </div>
        <div class="pf-row">
          <div class="pf-label">代理</div>
          <div class="pf-field-wrap">
            <span class="pf-muted">{{ detail.agentName }}（#{{ detail.agentId }}）</span>
          </div>
        </div>
        </article>

        <article class="dc-card dc-card--white">
        <div class="pf-row">
          <div class="pf-label req">所属行业</div>
          <div class="pf-field-wrap pf-field-wrap--select">
            <PfSelect v-model="industryType" :options="industrySelectOptions" :disabled="readOnly" placeholder="选择所属行业" />
          </div>
        </div>

        <div class="pf-row">
          <div class="pf-label req">店铺名称</div>
          <div class="pf-field-wrap">
            <input v-model="merchantName" class="pf-field" type="text" :disabled="readOnly" />
          </div>
        </div>

        <div class="pf-row">
          <div class="pf-label req">联系人</div>
          <div class="pf-field-wrap">
            <input v-model="contactName" class="pf-field" type="text" :disabled="readOnly" />
          </div>
        </div>

        <div class="pf-row">
          <div class="pf-label req">联系电话</div>
          <div class="pf-field-wrap">
            <input v-model="contactPhone" class="pf-field" type="tel" :disabled="readOnly" />
          </div>
        </div>
        </article>

        <article class="dc-card dc-card--white">
        <div class="pf-row pf-row--stack pf-row--loc">
          <div class="pf-label req">地图定位</div>
          <div class="pf-field-wrap pf-loc-panel">
            <button
              type="button"
              class="pf-loc-btn"
              :disabled="readOnly || locating"
              @click="pickLocationFromBrowser"
            >
              {{ locating ? "定位中…" : "获取当前位置" }}
            </button>
            <p v-if="hasCoords" class="pf-loc-summary">
              经度 {{ Number(longitude).toFixed(6) }} · 纬度 {{ Number(latitude).toFixed(6) }}
            </p>
            <p v-if="mapLocationInfo" class="pf-loc-meta">{{ mapLocationInfo }}</p>
            <details class="pf-loc-fallback">
              <summary>手动输入经纬度</summary>
              <div class="pf-loc-manual">
                <label class="pf-loc-manual-line"
                  >经度
                  <input
                    v-model.number="longitude"
                    type="number"
                    step="any"
                    :disabled="readOnly"
                    @input="onManualCoordInput"
                /></label>
                <label class="pf-loc-manual-line"
                  >纬度
                  <input
                    v-model.number="latitude"
                    type="number"
                    step="any"
                    :disabled="readOnly"
                    @input="onManualCoordInput"
                /></label>
              </div>
            </details>
          </div>
        </div>

        <div class="pf-row">
          <div class="pf-label req">省市区</div>
          <div class="pf-field-wrap pf-region">
            <input v-model="province" class="pf-field" type="text" placeholder="省" :disabled="readOnly" />
            <input v-model="city" class="pf-field" type="text" placeholder="市" :disabled="readOnly" />
            <input v-model="district" class="pf-field" type="text" placeholder="区" :disabled="readOnly" />
          </div>
        </div>

        <div class="pf-row">
          <div class="pf-label req">详细地址</div>
          <div class="pf-field-wrap">
            <input v-model="addressDetail" class="pf-field" type="text" :disabled="readOnly" />
          </div>
        </div>
        </article>

        <article class="dc-card dc-card--white">
        <div class="pf-row">
          <div class="pf-label req">主营油品</div>
          <div class="pf-field-wrap pf-field-wrap--select">
            <PfSelect v-model="oilTypeId" :options="oilTypeSelectOptions" placeholder="油品种类" :disabled="readOnly" />
          </div>
        </div>

        <div class="pf-row">
          <div class="pf-label">单价(元/桶)</div>
          <div class="pf-field-wrap">
            <input v-model.number="oilUnitPrice" class="pf-field" type="number" step="any" :disabled="readOnly" />
          </div>
        </div>

        <div class="pf-row">
          <div class="pf-label">商家抽成</div>
          <div class="pf-field-wrap">
            <input v-model.number="merchantCommission" class="pf-field" type="number" step="any" :disabled="readOnly" />
          </div>
        </div>

        <div v-if="canDirectEdit" class="pf-row">
          <div class="pf-label">运维</div>
          <div class="pf-field-wrap pf-field-wrap--select">
            <PfSelect v-model="salesmanId" :options="salesmanSelectOptions" placeholder="不指定" />
          </div>
        </div>

        <div class="pf-row">
          <div class="pf-label">说明</div>
          <div class="pf-field-wrap">
            <input v-model="remark" class="pf-field" type="text" :disabled="readOnly" />
          </div>
        </div>

        <div class="pf-row pf-upload-row">
          <div class="pf-label">合同图片</div>
          <label class="pf-upload" :class="{ 'pf-upload--disabled': readOnly }">
            <input type="file" accept="image/*" :disabled="readOnly" @change="onContractImageChange" />
            <span>上传合同</span>
            <span aria-hidden="true">📄</span>
            <img v-if="contractPreview" :src="contractPreview" alt="" class="pf-thumb" />
          </label>
        </div>

        <div class="pf-row pf-upload-row">
          <div class="pf-label">店铺图片</div>
          <label class="pf-upload" :class="{ 'pf-upload--disabled': readOnly }">
            <input type="file" accept="image/*" :disabled="readOnly" @change="onImageChange" />
            <span>上传图片</span>
            <span aria-hidden="true">☁</span>
            <img v-if="imagePreview" :src="imagePreview" alt="" class="pf-thumb" />
          </label>
        </div>
        </article>

        <article class="dc-card dc-card--white">
        <div class="pf-row pf-row--stack">
          <div class="pf-label">经营信息</div>
          <div class="pf-field-wrap biz-info">
            <div class="biz-row">
              <span class="biz-k">账号状态</span>
              <span :class="entityOnOffPillClass(detail.statusCode)">{{ detail.status }}</span>
            </div>
            <div class="biz-row">
              <span class="biz-k">欠费金额</span>
              <span class="biz-v">¥{{ detail.arrearsAmount }}</span>
            </div>
            <div class="biz-row">
              <span class="biz-k">设备数量</span>
              <span class="biz-v">{{ detail.deviceCount }} 台</span>
            </div>
          </div>
        </div>
        </article>
      </div>

      <div v-if="canSubmitAudit" class="dc-stack" style="margin-top: 12px">
        <article class="dc-card dc-card--white">
        <div class="pf-row">
          <div class="pf-label">审核说明</div>
          <div class="pf-field-wrap">
            <input v-model="submitRemark" class="pf-field" type="text" placeholder="给代理/主端的备注（选填）" />
          </div>
        </div>
        </article>
      </div>

      <div class="pf-footer">
        <button v-if="canDirectEdit" type="button" class="pf-submit" @click="saveDirect">保存修改</button>
        <button
          v-if="canSubmitAudit"
          type="button"
          class="pf-submit"
          :disabled="detail.hasPendingMyAudit"
          @click="submitAudit"
        >
          提交审核
        </button>
        <button type="button" class="pf-tool pf-tool--ghost" style="margin-top: 10px; width: 100%" @click="router.push({ name: 'promo-stores' })">
          返回列表
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.pf-upload--disabled {
  opacity: 0.55;
  pointer-events: none;
}
.pf-row--stack .pf-field-wrap {
  align-items: stretch;
}
.biz-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.biz-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}
.biz-k {
  color: #64748b;
  flex-shrink: 0;
}
.biz-v {
  color: #0f172a;
  font-weight: 600;
  text-align: right;
}
</style>
