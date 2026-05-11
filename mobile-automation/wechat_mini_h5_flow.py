#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
在 Android 手机上自动化：浏览器/H5 → 跳转微信 → 小程序 → 抓取小程序信息摘要 → 拉起支付后的商户名称（可见文案）。

使用前准备（Android）：
  1) 开发者选项开启「USB 调试」，用数据线连接电脑，允许授权。
  2) ``pip install -r requirements.txt`` 然后 ``python -m uiautomator2 init``（每台手机装一次 ATX 助手，按提示操作）。
  3) 本页若在微信外打开后被系统拉起微信，仍以 uiautomator2 点按屏幕元素为主；分辨率/机型不同需在 config 里微调文案或改用 xpath。

重要限制（必读）：
  - 微信小程序、支付收银台大量使用 WebView / 原生混排，控件树因微信版本而异；脚本是「可调模板」，不能保证一套选择器适配所有机型/版本。
  - 微信可能限制无障碍/自动化，偶发控件不可见或点击失效，需手动辅助或换 XPath。
  - 涉及资金与合规，请仅在自有测试环境使用，遵守平台与用户协议。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
from datetime import datetime, timezone
from typing import Any, Iterable, Optional


def load_config(path: str) -> dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def now_slug() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")


def safe_mkdir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def try_click_text(device, texts: Iterable[str], timeout: float = 15.0) -> bool:
    """Try clicking the first matching visible widget whose text equals one of `texts`."""
    deadline = time.time() + timeout
    texts = list(texts)
    while time.time() < deadline:
        for t in texts:
            node = device(text=t)
            if node.exists(timeout=0.25):
                node.click()
                return True
        time.sleep(0.35)
    return False


def try_click_contains(device, substring: str, timeout: float = 12.0) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        node = device(textContains=substring)
        if node.exists(timeout=0.25):
            node.click()
            return True
        time.sleep(0.35)
    return False


def capture_screen(device, path: str) -> None:
    device.screenshot(path)


def merchant_hint_lines(ui_text: str, max_lines: int = 12) -> list[str]:
    """从无障碍/层级里提取的文案中，挑出可能含商户名的行（启发式，非 OCR）。"""
    out: list[str] = []
    keys = ("商户", "收款", "收款方", "支付给", "向商家", "商家")
    for ln in ui_text.split("\n"):
        if any(k in ln for k in keys):
            out.append(ln)
            if len(out) >= max_lines:
                break
    return out


def flatten_visible_texts(device, max_chars: int = 8000) -> str:
    """Best-effort: dump hierarchy and grep text= lines (fragile but works without OCR)."""
    xml = device.dump_hierarchy(compressed=False)
    lines = []
    for m in re.finditer(r'text="([^"]*)"', xml):
        t = m.group(1).strip()
        if t:
            lines.append(t)
    out = "\n".join(dict.fromkeys(lines))  # de-dupe preserve order
    return out[:max_chars]


def open_start_url(device, url: str, browser_pkg: Optional[str]) -> None:
    """用系统 VIEW intent 打开链接；若指定 browser_pkg（如 com.android.chrome）则强制该应用处理。"""
    safe = url.replace('"', "%22")
    if browser_pkg:
        cmd = f'am start -a android.intent.action.VIEW -d "{safe}" -p {browser_pkg}'
    else:
        cmd = f'am start -a android.intent.action.VIEW -d "{safe}"'
    device.shell(cmd)


def tap_capsule_more(device) -> bool:
    """
    小程序右上角「···」/菜单：不同版本可能是 contentDescription、控件在右上区域。
    这里用坐标点击兜底（可按分辨率改比例）。
    """
    w, h = device.window_size()
    # 常见：右上角偏左一点，避开状态栏
    x, y = int(w * 0.92), int(h * 0.085)
    device.click(x, y)
    return True


def record_step(
    out_dir: str,
    slug: str,
    step_name: str,
    device,
    extra: Optional[dict[str, Any]] = None,
) -> dict[str, Any]:
    safe_mkdir(out_dir)
    png = os.path.join(out_dir, f"{slug}__{step_name}.png")
    txt = os.path.join(out_dir, f"{slug}__{step_name}.txt")
    capture_screen(device, png)
    ui_text = flatten_visible_texts(device)
    payload: dict[str, Any] = {
        "step": step_name,
        "screenshot": png,
        "visible_text_digest": ui_text,
    }
    if extra:
        payload["extra"] = extra
    with open(txt, "w", encoding="utf-8") as f:
        f.write(json.dumps(payload, ensure_ascii=False, indent=2))
    return payload


def run_flow(config_path: str) -> int:
    import uiautomator2 as u2

    cfg = load_config(config_path)
    serial = cfg.get("device_serial")
    d = u2.connect_usb(serial) if serial else u2.connect()

    out_root = cfg.get("output_dir", "captures")
    slug = now_slug()
    session_dir = os.path.join(out_root, slug)
    safe_mkdir(session_dir)

    url = cfg["start_url"]
    labels = cfg.get("labels", {})
    timeouts = cfg.get("timeouts_sec", {})

    open_start_url(d, url, cfg.get("browser_pkg"))
    time.sleep(timeouts.get("after_open_url", 12))

    if not try_click_text(d, labels.get("pay_now", ["立即支付"]), timeout=timeouts.get("default", 25)):
        print("未点到「立即支付」，请在本机 Inspect 层级或改 labels.pay_now。", file=sys.stderr)
        record_step(session_dir, slug, "fail_pay_now", d)
        return 2

    time.sleep(1.5)
    candidates = labels.get(
        "goto_wechat_pay",
        ["跳转到微信支付", "微信支付", "去支付"],
    )
    if not (
        try_click_text(d, candidates, timeout=15)
        or try_click_contains(d, "微信", timeout=10)
    ):
        print("未点到跳转微信支付，请在小程序/H5实际按钮文案上扩展 labels.goto_wechat_pay。", file=sys.stderr)
        record_step(session_dir, slug, "fail_goto_wechat", d)
        return 3

    time.sleep(2)
    # 可能被拉到微信原生页或小程序首页
    deadline = time.time() + timeouts.get("wechat_launch", 45)
    while time.time() < deadline:
        if d.app_current().get("package") == cfg.get("wechat_pkg", "com.tencent.mm"):
            break
        time.sleep(0.8)
    time.sleep(2)

    tap_capsule_more(d)
    time.sleep(1)

    about_labels = labels.get("mini_program_about", ["小程序信息", "关于小程序"])
    if not try_click_text(d, about_labels, timeout=12):
        try_click_contains(d, "小程序", timeout=8)

    time.sleep(1.5)
    about_payload = record_step(session_dir, slug, "mini_program_about_panel", d)

    d.press("back")
    time.sleep(0.8)
    try_click_text(d, ["关闭", "取消", "知道了"], timeout=4)

    claim = labels.get("claim_now", ["立即领取"])
    if not try_click_text(d, claim, timeout=20):
        print("未点到「立即领取」，请截图检查层级或改坐标/文案。", file=sys.stderr)
        record_step(session_dir, slug, "fail_claim", d)
        return 4

    time.sleep(1.5)
    claim_popup = record_step(session_dir, slug, "after_claim_popup", d)

    time.sleep(1)
    pay_deadline = time.time() + timeouts.get("pay_sheet", 30)
    pay_payload = None
    while time.time() < pay_deadline:
        ui = flatten_visible_texts(d)
        if "支付" in ui or "付款" in ui or "商户" in ui or "收款" in ui:
            pay_payload = record_step(
                session_dir,
                slug,
                "payment_sheet",
                d,
                extra={
                    "hint": "heuristic matched pay UI",
                    "merchant_hint_lines": merchant_hint_lines(ui),
                },
            )
            break
        time.sleep(0.7)

    summary_path = os.path.join(session_dir, f"{slug}__summary.json")
    with open(summary_path, "w", encoding="utf-8") as f:
        json.dump(
            {
                "captured_at_utc": slug,
                "about_panel": about_payload,
                "after_claim": claim_popup,
                "payment_sheet": pay_payload,
            },
            f,
            ensure_ascii=False,
            indent=2,
        )

    print("完成。结果目录:", os.path.abspath(session_dir))
    print("摘要:", os.path.abspath(summary_path))
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description="H5 → 微信 → 小程序信息/支付商户名 采集（Android + uiautomator2）")
    parser.add_argument(
        "--config",
        default=os.path.join(os.path.dirname(__file__), "config.json"),
        help="配置文件路径（可从 config.example.json 复制）",
    )
    args = parser.parse_args()
    if not os.path.isfile(args.config):
        print("缺少配置文件。请复制 config.example.json 为 config.json 后编辑。", file=sys.stderr)
        sys.exit(1)
    raise SystemExit(run_flow(args.config))


if __name__ == "__main__":
    main()
