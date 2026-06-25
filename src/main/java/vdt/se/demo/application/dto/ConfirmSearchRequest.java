package vdt.se.demo.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import vdt.se.demo.domain.model.SearchIntent;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmSearchRequest {
    @NotBlank
    private String confirmationId;
    private String sessionId;
    private SearchIntent editedIntent;
    @Builder.Default
    private Integer page = 0;
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

    public SearchIntent getEditedIntent() {
        return editedIntent;
    }

    public void setEditedIntent(SearchIntent editedIntent) {
        this.editedIntent = editedIntent;
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
