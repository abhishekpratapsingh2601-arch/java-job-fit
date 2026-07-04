package com.javajobfit.domain;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "outcomes")
public class Outcome {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reportId;

    @Column(nullable = false)
    private String outcomeType;

    private Integer usefulnessRating;

    private Boolean gotRecruiterReply;

    private Boolean gotInterview;

    private Boolean appliedWithThisResume;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String email;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getOutcomeType() {
        return outcomeType;
    }

    public void setOutcomeType(String outcomeType) {
        this.outcomeType = outcomeType;
    }

    public Integer getUsefulnessRating() {
        return usefulnessRating;
    }

    public void setUsefulnessRating(Integer usefulnessRating) {
        this.usefulnessRating = usefulnessRating;
    }

    public Boolean getGotRecruiterReply() {
        return gotRecruiterReply;
    }

    public void setGotRecruiterReply(Boolean gotRecruiterReply) {
        this.gotRecruiterReply = gotRecruiterReply;
    }

    public Boolean getGotInterview() {
        return gotInterview;
    }

    public void setGotInterview(Boolean gotInterview) {
        this.gotInterview = gotInterview;
    }

    public Boolean getAppliedWithThisResume() {
        return appliedWithThisResume;
    }

    public void setAppliedWithThisResume(Boolean appliedWithThisResume) {
        this.appliedWithThisResume = appliedWithThisResume;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
