package com.example.helloworld.dto.invest;

public class StrategySettingsUpdateRequestDto {

    private StrategySettingsDto.StrengthThresholds strength;
    private StrategySettingsDto.DataQualityThresholds dataQuality;
    private StrategySettingsDto.OpportunityThresholds opportunity;

    public StrategySettingsDto.StrengthThresholds getStrength() {
        return strength;
    }

    public void setStrength(StrategySettingsDto.StrengthThresholds strength) {
        this.strength = strength;
    }

    public StrategySettingsDto.DataQualityThresholds getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(StrategySettingsDto.DataQualityThresholds dataQuality) {
        this.dataQuality = dataQuality;
    }

    public StrategySettingsDto.OpportunityThresholds getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(StrategySettingsDto.OpportunityThresholds opportunity) {
        this.opportunity = opportunity;
    }
}
