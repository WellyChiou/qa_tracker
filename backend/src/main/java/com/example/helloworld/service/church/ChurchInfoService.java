package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.ChurchInfo;
import com.example.helloworld.repository.church.ChurchInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChurchInfoService {

    @Autowired
    private ChurchInfoRepository churchInfoRepository;

    /**
     * 獲取所有教會資訊（管理用，包含未啟用的）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<ChurchInfo> getAllInfo() {
        return churchInfoRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * 獲取所有啟用的教會資訊（以 Map 形式返回，key 為 infoKey）
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Map<String, String> getAllActiveInfo() {
        List<ChurchInfo> infoList = churchInfoRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        Map<String, String> infoMap = new HashMap<>();
        for (ChurchInfo info : infoList) {
            infoMap.put(info.getInfoKey(), info.getInfoValue());
        }
        return infoMap;
    }

    /**
     * 根據 key 獲取教會資訊
     */
    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<ChurchInfo> getInfoByKey(String infoKey) {
        return churchInfoRepository.findByInfoKey(infoKey);
    }

    /**
     * 創建或更新教會資訊
     */
    @Transactional(transactionManager = "churchTransactionManager")
    public ChurchInfo saveOrUpdateInfo(ChurchInfo churchInfo) {
        Optional<ChurchInfo> existing = churchInfoRepository.findByInfoKey(churchInfo.getInfoKey());
        if (existing.isPresent()) {
            ChurchInfo existingInfo = existing.get();
            existingInfo.setInfoValue(churchInfo.getInfoValue());
            existingInfo.setInfoType(churchInfo.getInfoType());
            existingInfo.setDisplayOrder(churchInfo.getDisplayOrder());
            existingInfo.setIsActive(churchInfo.getIsActive());
            return churchInfoRepository.save(existingInfo);
        } else {
            return churchInfoRepository.save(churchInfo);
        }
    }
}

