package vdt.se.demo.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import vdt.se.demo.domain.iql.IqlQuery;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmSearchRequest {
    @NotBlank
    private String confirmationId;
    private String sessionId;
    private IqlQuery editedQuery;
    @Min(0)
    @Builder.Default
    private Integer page = 0;
    @Min(1)
    @Max(500)
    @Builder.Default
    private Integer pageSize = 50;

    public String getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(String confirmationId) {
        this.confirmationId = confirmationId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public IqlQuery getEditedQuery() {
        return editedQuery;
    }

    public void setEditedQuery(IqlQuery editedQuery) {
        this.editedQuery = editedQuery;
    }

    public int getPage() {
        return page == null ? 0 : page;
    }

    public void setPage(Integer page) {
        this.page = page == null ? 0 : page;
    }

    public int getPageSize() {
        return pageSize == null ? 50 : pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize == null ? 50 : pageSize;
    }
}
