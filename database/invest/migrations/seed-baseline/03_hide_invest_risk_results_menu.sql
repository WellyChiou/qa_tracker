-- Slice E 收尾：先隱藏尚未落地頁面的 menu，避免前端導向不存在路由
USE invest;

UPDATE menu_items
SET is_active = 0,
    show_in_dashboard = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'INVEST_RISK_RESULTS';
