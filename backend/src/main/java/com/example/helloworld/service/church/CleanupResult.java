package com.example.helloworld.service.church;

import java.util.List;

/**
 * 清理操作的結果
 */
public class CleanupResult {
    private int count;
    private List<String> deletedFiles;

    public CleanupResult(int count, List<String> deletedFiles) {
        this.count = count;
        this.deletedFiles = deletedFiles;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(List<String> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }
}

