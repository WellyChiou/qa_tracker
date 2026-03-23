package com.example.helloworld.dto.invest;

public class SystemSchedulerJobUpsertRequestDto {
    private String jobCode;
    private String jobName;
    private String description;
    private Boolean enabled;
    private String scheduleExpression;
    private Boolean allowRunNow;

    public String getJobCode() {
        return jobCode;
    }

    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public void setScheduleExpression(String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
    }

    public Boolean getAllowRunNow() {
        return allowRunNow;
    }

    public void setAllowRunNow(Boolean allowRunNow) {
        this.allowRunNow = allowRunNow;
    }
}
