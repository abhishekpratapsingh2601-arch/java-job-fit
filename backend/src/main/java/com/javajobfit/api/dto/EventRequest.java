package com.javajobfit.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class EventRequest {
    @NotBlank(message = "Event name is required.")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Event name must use lowercase letters, numbers, and underscores.")
    @Size(max = 80, message = "Event name is too long.")
    private String eventName;

    @Size(max = 255, message = "Page path is too long.")
    private String pagePath;

    @Size(max = 64, message = "Report public ID is too long.")
    @Pattern(
            regexp = "^$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Report public ID must be a valid UUID.")
    private String reportPublicId;

    @Size(max = 255, message = "Experience level is too long.")
    private String experienceLevel;

    @Size(max = 255, message = "Country is too long.")
    private String country;

    @Size(max = 255, message = "Source is too long.")
    private String source;

    @Size(max = 255, message = "UTM source is too long.")
    private String utmSource;

    @Size(max = 255, message = "UTM medium is too long.")
    private String utmMedium;

    @Size(max = 255, message = "UTM campaign is too long.")
    private String utmCampaign;

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getReportPublicId() {
        return reportPublicId;
    }

    public void setReportPublicId(String reportPublicId) {
        this.reportPublicId = reportPublicId;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUtmSource() {
        return utmSource;
    }

    public void setUtmSource(String utmSource) {
        this.utmSource = utmSource;
    }

    public String getUtmMedium() {
        return utmMedium;
    }

    public void setUtmMedium(String utmMedium) {
        this.utmMedium = utmMedium;
    }

    public String getUtmCampaign() {
        return utmCampaign;
    }

    public void setUtmCampaign(String utmCampaign) {
        this.utmCampaign = utmCampaign;
    }
}
