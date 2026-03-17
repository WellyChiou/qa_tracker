package com.example.helloworld.service.church;

import java.util.List;

/**
 * 停用操作的結果
 */
public class DeactivationResult {
    private int count;
    private List<ItemInfo> items;

    public DeactivationResult(int count, List<ItemInfo> items) {
        this.count = count;
        this.items = items;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<ItemInfo> getItems() {
        return items;
    }

    public void setItems(List<ItemInfo> items) {
        this.items = items;
    }

    /**
     * 項目信息
     */
    public static class ItemInfo {
        private Long id;
        private String title;
        private String date;

        public ItemInfo(Long id, String title, String date) {
            this.id = id;
            this.title = title;
            this.date = date;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }
}

