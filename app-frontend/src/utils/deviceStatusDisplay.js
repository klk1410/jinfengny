/** 与 styles/deviceStatusBadge.css 中 .ds-pill--* 对应 */
export function deviceStatusPillClass(code) {
  if (code === "0" || code === "1" || code === "2" || code === "3" || code === "4") {
    return `ds-pill ds-pill--${code}`;
  }
  return "ds-pill ds-pill--x";
}
