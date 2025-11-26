/**
 * Firebase 到 MySQL 資料遷移腳本
 * 
 * 使用方式：
 * 1. 從 Firebase Console 匯出資料為 JSON
 * 2. 執行此腳本進行轉換
 * 3. 匯入到 MySQL
 * 
 * 需要安裝：node-fetch, mysql2
 * npm install node-fetch mysql2
 */

const fs = require('fs');
const mysql = require('mysql2/promise');

// MySQL 連線配置
const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 3306,
  user: process.env.DB_USER || 'appuser',
  password: process.env.DB_PASSWORD || 'apppassword',
  database: process.env.DB_NAME || 'qa_tracker',
  charset: 'utf8mb4'
};

/**
 * 轉換 Firebase Timestamp 為 MySQL DATETIME
 */
function convertTimestamp(fbTimestamp) {
  if (!fbTimestamp) return null;
  
  // Firebase Timestamp 格式：{ seconds: 1234567890, nanoseconds: 0 }
  if (fbTimestamp.seconds) {
    return new Date(fbTimestamp.seconds * 1000).toISOString().slice(0, 19).replace('T', ' ');
  }
  
  // 如果是字串格式
  if (typeof fbTimestamp === 'string') {
    return new Date(fbTimestamp).toISOString().slice(0, 19).replace('T', ' ');
  }
  
  return null;
}

/**
 * 轉換 Firebase Date 為 MySQL DATE
 */
function convertDate(fbDate) {
  if (!fbDate) return null;
  
  if (fbDate.seconds) {
    return new Date(fbDate.seconds * 1000).toISOString().slice(0, 10);
  }
  
  if (typeof fbDate === 'string') {
    return fbDate.slice(0, 10);
  }
  
  return null;
}

/**
 * 遷移 Users 資料
 */
async function migrateUsers(connection, usersData) {
  console.log('開始遷移 Users...');
  
  for (const [uid, userData] of Object.entries(usersData)) {
    try {
      const [rows] = await connection.execute(
        `INSERT INTO users (uid, email, display_name, photo_url, provider_id, last_login_at, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
           email = VALUES(email),
           display_name = VALUES(display_name),
           photo_url = VALUES(photo_url),
           provider_id = VALUES(provider_id),
           last_login_at = VALUES(last_login_at),
           updated_at = VALUES(updated_at)`,
        [
          uid,
          userData.email || null,
          userData.displayName || null,
          userData.photoURL || null,
          userData.providerId || null,
          convertTimestamp(userData.lastLoginAt),
          convertTimestamp(userData.createdAt) || new Date().toISOString().slice(0, 19).replace('T', ' '),
          convertTimestamp(userData.updatedAt) || new Date().toISOString().slice(0, 19).replace('T', ' ')
        ]
      );
      console.log(`✓ 遷移 User: ${uid}`);
    } catch (error) {
      console.error(`✗ 遷移 User ${uid} 失敗:`, error.message);
    }
  }
  
  console.log('Users 遷移完成\n');
}

/**
 * 遷移 Records 資料
 */
async function migrateRecords(connection, recordsData) {
  console.log('開始遷移 Records...');
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const [firebaseId, recordData] of Object.entries(recordsData)) {
    try {
      // 檢查是否已存在
      const [existing] = await connection.execute(
        'SELECT id FROM records WHERE firebase_id = ?',
        [firebaseId]
      );
      
      if (existing.length > 0) {
        console.log(`⊘ 略過已存在的 Record: ${firebaseId}`);
        continue;
      }
      
      await connection.execute(
        `INSERT INTO records (
          firebase_id, issue_number, issue_link, status, category, feature, memo,
          test_plan, bug_found, optimization_points, verify_failed, test_cases, file_count,
          test_start_date, eta_date, completed_at, created_by_uid, created_at, updated_by_uid, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          firebaseId,
          recordData.issue_number || null,
          recordData.issue_link || null,
          recordData.status ?? 1,
          recordData.category || null,
          recordData.feature || null,
          recordData.memo || null,
          recordData.test_plan || '0',
          recordData.bug_found ?? 0,
          recordData.optimization_points ?? 0,
          recordData.verify_failed ?? 0,
          recordData.test_cases ?? 0,
          recordData.file_count ?? 0,
          convertDate(recordData.test_start_date),
          convertDate(recordData.eta_date),
          convertDate(recordData.completed_at),
          recordData.created_by_uid || null,
          convertTimestamp(recordData.created_at) || new Date().toISOString().slice(0, 19).replace('T', ' '),
          recordData.updated_by_uid || null,
          convertTimestamp(recordData.updated_at) || new Date().toISOString().slice(0, 19).replace('T', ' ')
        ]
      );
      
      successCount++;
      if (successCount % 100 === 0) {
        console.log(`  已遷移 ${successCount} 筆記錄...`);
      }
    } catch (error) {
      errorCount++;
      console.error(`✗ 遷移 Record ${firebaseId} 失敗:`, error.message);
    }
  }
  
  console.log(`\nRecords 遷移完成: 成功 ${successCount} 筆, 失敗 ${errorCount} 筆\n`);
}

/**
 * 主函數
 */
async function main() {
  const firebaseExportFile = process.argv[2];
  
  if (!firebaseExportFile) {
    console.error('請提供 Firebase 匯出檔案路徑');
    console.error('使用方式: node firebase-to-mysql.js <firebase-export.json>');
    process.exit(1);
  }
  
  if (!fs.existsSync(firebaseExportFile)) {
    console.error(`檔案不存在: ${firebaseExportFile}`);
    process.exit(1);
  }
  
  console.log('讀取 Firebase 匯出檔案...');
  const firebaseData = JSON.parse(fs.readFileSync(firebaseExportFile, 'utf8'));
  
  console.log('連接到 MySQL...');
  const connection = await mysql.createConnection(dbConfig);
  
  try {
    // 遷移 Users
    if (firebaseData.users) {
      await migrateUsers(connection, firebaseData.users);
    }
    
    // 遷移 Records
    if (firebaseData.records) {
      await migrateRecords(connection, firebaseData.records);
    }
    
    // 遷移 Config（如果有）
    if (firebaseData.config) {
      console.log('開始遷移 Config...');
      for (const [configKey, configData] of Object.entries(firebaseData.config)) {
        try {
          await connection.execute(
            `INSERT INTO config (config_key, config_value, description)
             VALUES (?, ?, ?)
             ON DUPLICATE KEY UPDATE
               config_value = VALUES(config_value),
               updated_at = CURRENT_TIMESTAMP`,
            [
              configKey,
              typeof configData === 'object' ? JSON.stringify(configData) : String(configData),
              '從 Firebase 遷移'
            ]
          );
          console.log(`✓ 遷移 Config: ${configKey}`);
        } catch (error) {
          console.error(`✗ 遷移 Config ${configKey} 失敗:`, error.message);
        }
      }
      console.log('Config 遷移完成\n');
    }
    
    console.log('✅ 遷移完成！');
    
  } catch (error) {
    console.error('遷移過程發生錯誤:', error);
    process.exit(1);
  } finally {
    await connection.end();
  }
}

// 執行主函數
main().catch(console.error);

