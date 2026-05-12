<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

/**
 * 自定义下拉：弹出选项列表可完全样式化（原生 select 的 option 无法统一美化）。
 * 选项面板 teleport 到 body，避免父级 overflow 裁剪。
 */
const props = defineProps({
  modelValue: { type: String, default: "" },
  /** { value: string, label: string, disabled?: boolean }[] */
  options: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: "请选择" },
  /** 紧凑高度与字号（如顶部测试用户条） */
  dense: { type: Boolean, default: false },
  /** 触发按钮 id（便于 label for 关联） */
  id: { type: String, default: "" }
});

const emit = defineEmits(["update:modelValue", "change"]);

const root = ref(null);
const triggerRef = ref(null);
const open = ref(false);
const panelTop = ref(0);
const panelLeft = ref(0);
const panelWidth = ref(0);

const displayLabel = computed(() => {
  const v = props.modelValue;
  const hit = props.options.find((o) => String(o.value) === String(v));
  if (hit) {
    return hit.label;
  }
  return props.placeholder;
});

function syncPanelRect() {
  const el = triggerRef.value;
  if (!el) {
    return;
  }
  const r = el.getBoundingClientRect();
  panelLeft.value = r.left;
  panelTop.value = r.bottom + 4;
  panelWidth.value = r.width;
}

function toggle() {
  if (props.disabled) {
    return;
  }
  open.value = !open.value;
  if (open.value) {
    nextTick(() => {
      syncPanelRect();
    });
  }
}

function pick(opt) {
  if (!opt || opt.disabled) {
    return;
  }
  emit("update:modelValue", String(opt.value));
  emit("change", String(opt.value));
  open.value = false;
}

function onDocPointerDown(ev) {
  const t = ev.target;
  if (root.value?.contains(t)) {
    return;
  }
  if (typeof t.closest === "function" && t.closest(".pf-select__panel")) {
    return;
  }
  open.value = false;
}

function onScrollResize() {
  if (open.value) {
    syncPanelRect();
  }
}

watch(open, (v) => {
  if (v) {
    nextTick(() => syncPanelRect());
  }
});

onMounted(() => {
  document.addEventListener("pointerdown", onDocPointerDown, true);
  window.addEventListener("scroll", onScrollResize, true);
  window.addEventListener("resize", onScrollResize);
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onDocPointerDown, true);
  window.removeEventListener("scroll", onScrollResize, true);
  window.removeEventListener("resize", onScrollResize);
});
</script>

<template>
  <div ref="root" class="pf-select" :class="{ 'pf-select--dense': dense }">
    <button
      :id="id || undefined"
      ref="triggerRef"
      type="button"
      class="pf-select__trigger"
      :class="{ 'pf-select__trigger--open': open, 'pf-select__trigger--disabled': disabled }"
      :disabled="disabled"
      :aria-expanded="open"
      aria-haspopup="listbox"
      @click.stop="toggle"
    >
      <span class="pf-select__value">{{ displayLabel }}</span>
      <span class="pf-select__icon" aria-hidden="true" />
    </button>

    <Teleport to="body">
      <Transition name="pf-select-pop">
        <ul
          v-show="open"
          :class="['pf-select__panel', dense ? 'pf-select__panel--dense' : '']"
          role="listbox"
          :style="{
            left: panelLeft + 'px',
            top: panelTop + 'px',
            width: panelWidth + 'px'
          }"
        >
          <li
            v-for="(opt, i) in options"
            :key="i + '-' + opt.value"
            role="option"
            class="pf-select__option"
            :class="{
              'pf-select__option--disabled': opt.disabled,
              'pf-select__option--active': String(modelValue) === String(opt.value)
            }"
            :aria-selected="String(modelValue) === String(opt.value)"
            @click.stop="pick(opt)"
          >
            {{ opt.label }}
          </li>
        </ul>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.pf-select {
  width: 100%;
  min-width: 0;
}

.pf-select__trigger {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
  min-height: 40px;
  margin: 0;
  padding: 9px 34px 9px 12px;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  background-color: #f8fafc;
  font-size: 14px;
  color: #0f172a;
  text-align: right;
  cursor: pointer;
  outline: none;
  box-sizing: border-box;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}

.pf-select__trigger:hover:not(:disabled) {
  border-color: #93c5fd;
  background-color: #fff;
}

.pf-select__trigger:focus-visible {
  border-color: #3b82f6;
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.18);
}

.pf-select__trigger--open {
  border-color: #3b82f6;
  background-color: #fff;
}

.pf-select__trigger--disabled {
  opacity: 0.72;
  cursor: not-allowed;
  background-color: #f1f5f9;
  border-color: #e2e8f0;
}

.pf-select__value {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pf-select__icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='%231f6dff' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E")
    center / contain no-repeat;
  pointer-events: none;
}

.pf-select__panel {
  position: fixed;
  z-index: 10000;
  margin: 0;
  padding: 6px 0;
  list-style: none;
  max-height: min(52vh, 320px);
  overflow-y: auto;
  background: #fff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  box-shadow:
    0 10px 40px rgba(15, 23, 42, 0.12),
    0 4px 12px rgba(31, 109, 255, 0.08);
  box-sizing: border-box;
}

.pf-select__option {
  padding: 11px 14px;
  font-size: 14px;
  line-height: 1.45;
  color: #0f172a;
  cursor: pointer;
  transition: background 0.12s ease;
}

.pf-select__option:hover:not(.pf-select__option--disabled) {
  background: #eff6ff;
  color: #1e40af;
}

.pf-select__option--active {
  background: #dbeafe;
  color: #1e3a8a;
  font-weight: 500;
}

.pf-select__option--disabled {
  color: #94a3b8;
  cursor: not-allowed;
  background: transparent;
}

.pf-select-pop-enter-active,
.pf-select-pop-leave-active {
  transition:
    opacity 0.12s ease,
    transform 0.12s ease;
}

.pf-select-pop-enter-from,
.pf-select-pop-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.pf-select--dense .pf-select__trigger {
  min-height: 34px;
  padding: 6px 30px 6px 8px;
  font-size: 12px;
}

.pf-select--dense .pf-select__icon {
  width: 14px;
  height: 14px;
}

.pf-select__panel--dense .pf-select__option {
  padding: 8px 12px;
  font-size: 12px;
}
</style>
