# 上傳腳本到服務器

## 方法 1: 使用 SCP 上傳

```bash
# 從本地 Windows 上傳到服務器
scp migrate_mysql_from_volume.sh root@38.54.89.136:/root/project/work/docker-vue-java-mysql/
```

## 方法 2: 使用 SFTP

```bash
# 使用 SFTP 上傳
sftp root@38.54.89.136
put migrate_mysql_from_volume.sh /root/project/work/docker-vue-java-mysql/
```

## 方法 3: 直接在服務器上創建（推薦）

如果您的專案已經通過 Git 同步到服務器，可以直接在服務器上執行：

```bash
cd /root/project/work/docker-vue-java-mysql
# 然後執行
chmod +x migrate_mysql_from_volume.sh
./migrate_mysql_from_volume.sh
```

## 快速上傳命令（Windows PowerShell）

如果您在 Windows 上，可以使用：

```powershell
# 使用 SCP（需要安裝 OpenSSH 客戶端）
scp migrate_mysql_from_volume.sh root@38.54.89.136:/root/project/work/docker-vue-java-mysql/

# 或者使用 WinSCP（圖形化工具）
```

## 或者直接在服務器上創建腳本

如果您可以 SSH 到服務器，可以直接在服務器上創建：

```bash
# SSH 到服務器
ssh root@38.54.89.136

# 然後在服務器上執行
cd /root/project/work/docker-vue-java-mysql
nano migrate_mysql_from_volume.sh
# 複製腳本內容，保存
chmod +x migrate_mysql_from_volume.sh
./migrate_mysql_from_volume.sh
```

