# 部署腳本問題修復說明

## 問題描述

執行 `deploy-to-server.bat` 時出現以下錯誤：
```
'傳到虛擬主機' is not recognized as an internal or external command
'程目錄...' is not recognized as an internal or external command
```

## 問題原因

1. **括號未轉義**：批處理文件中，括號 `()` 有特殊含義，需要用 `^` 轉義
2. **編碼問題**：中文字符在批處理文件中可能出現編碼問題

## 已修復的問題

1. ✅ 修復了 `echo` 語句中的括號轉義問題
2. ✅ 改進了 UTF-8 編碼設置
3. ✅ 將包含變量的 echo 語句拆分為多行，避免解析錯誤

## 當前狀態

腳本現在應該可以正常執行。當提示輸入密碼時：
- 密碼是：`!Welly775`
- 需要手動輸入（Windows OpenSSH 不支持命令行傳遞密碼）

## 建議

為了避免每次都要輸入密碼，建議配置 SSH 密鑰認證：

```powershell
# 生成 SSH 密鑰（如果還沒有）
ssh-keygen -t rsa -b 4096

# 複製公鑰到服務器
type $env:USERPROFILE\.ssh\id_rsa.pub | ssh root@38.54.89.136 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

輸入密碼：`!Welly775`

之後就可以無密碼登錄了。

