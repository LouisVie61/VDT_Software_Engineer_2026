package vdt.se.demo.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class SearchRequest {
    @NotBlank
    private String question;
    @Min(0)
    @Builder.Default
    private Integer page = 0;
    @Min(1)
    @Max(500)
    @Builder.Default
    private Integer pageSize = 50;
    private String from;
    private String to;
    private String severity;
    private String eventType;
    private String user;
    private String host;
    private String ip;
    private String sessionId;
    private String historySelectionId;
    private String searchAfter;

    public int getPage() {
        return page == null ? 0 : page;
    }

    public int getPageSize() {
        return pageSize == null ? 50 : pageSize;
    }
}
