#!/bin/sh
# 本地「国内源 + 内置文档基础包」改动自检：官方更新后在仓库根执行
#   sh tools/sandbox/check-local-cn-mirror.sh
# 每项输出 OK 或 MISSING(被冲刷)；全绿才说明本地改动完整。
B=tools/sandbox/build-runtime.py
L=tools/sandbox/runtime/sources.lock
ok=0; miss=0
check() { # $1=文件 $2=grep模式 $3=说明
  if grep -qE "$2" "$1" 2>/dev/null; then ok=$((ok+1)); printf "OK      %s\n" "$3"
  else miss=$((miss+1)); printf "MISSING %s  (in %s)\n" "$3" "$1"; fi
}
echo "== 国内源: build-runtime.py =="
check "$B" "ALPINE_CDN"            "官方源环境变量 ALPINE_CDN"
check "$B" "ALPINE_MIRROR_CN"     "国内主源(阿里)"
check "$B" "ALPINE_MIRROR_BACKUP" "国内备源(清华)"
check "$B" "Mozilla/5.0"          "浏览器 UA(躲403)"
check "$B" "404"                  "官方404 fallback"
check "$B" "pypi.tuna.tsinghua.edu.cn" "pip.conf 清华源"
check "$B" "npmmirror"            "npmrc npmmirror"
check "$B" "99-proxy.sh"          "99-proxy.sh 烘焙代理"
check "$B" "openssl.exe"          "openssl.exe 候选"
echo
echo "== 内置文档功能基础包: WANT + PIP_PACKAGES =="
check "$B" "poppler-utils"   "apk: poppler-utils (PDF)"
check "$B" "font-wqy-zenhei" "apk: font-wqy-zenhei (CJK 字体)"
check "$B" "fontconfig"      "apk: fontconfig"
check "$B" "reportlab"       "pip: reportlab"
check "$B" "fpdf2"           "pip: fpdf2"
check "$B" "python-docx"     "pip: python-docx"
check "$B" "openpyxl"        "pip: openpyxl"
check "$B" "xlsxwriter"      "pip: xlsxwriter"
check "$B" "cryptography"    "pip: cryptography"
check "$B" "beautifulsoup4"  "pip: beautifulsoup4"
echo
echo "== sources.lock python 版本 =="
check "$L" "python3 3\.12\.16-r0"          "python3 3.12.16-r0"
check "$L" "python3-pyc 3\.12\.16-r0"      "python3-pyc"
check "$L" "python3-pycache-pyc0 3\.12\.16-r0" "python3-pycache-pyc0"
echo
echo "== 生成产物 =="
[ -f app/src/main/assets/runtime/rootfs.tar.xz ] && { ok=$((ok+1)); echo "OK      rootfs.tar.xz"; } || { miss=$((miss+1)); echo "MISSING rootfs.tar.xz"; }
[ -f app/src/main/assets/runtime/packages.json ] && { ok=$((ok+1)); echo "OK      packages.json"; } || { miss=$((miss+1)); echo "MISSING packages.json"; }
[ -f core/src/main/resources/skills/documents/SKILL.md ] && { ok=$((ok+1)); echo "OK      skills/documents/SKILL.md"; } || { miss=$((miss+1)); echo "MISSING skills/documents/SKILL.md"; }
echo
echo "== 汇总: OK=$ok MISSING=$miss =="
[ "$miss" -eq 0 ] && echo "全绿, 本地改动完整" || echo "有 $miss 项被冲刷, 照 LOCAL-CUSTOM.md 恢复"
exit "$miss"
