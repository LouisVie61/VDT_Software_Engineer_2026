package vdt.se.demo.adapter.in.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vdt.se.demo.adapter.in.rest.dto.QueryHistoryResponse;
import vdt.se.demo.adapter.in.rest.dto.SearchResponse;
import vdt.se.demo.application.dto.SearchRequest;
import vdt.se.demo.application.port.inboundPort.QueryUseCase;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
public class QueryController {

    private final QueryUseCase queryUseCase;

    public QueryController(QueryUseCase queryUseCase) {
        this.queryUseCase = queryUseCase;
    }

    @PostMapping
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return SearchResponse.from(queryUseCase.search(request));
    }

    @GetMapping("/history")
    public List<QueryHistoryResponse> history(
            @RequestParam(defaultValue = "soc-analyst-demo") String userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return queryUseCase.history(userId, limit).stream()
                .map(QueryHistoryResponse::from)
                .toList();
    }

    @GetMapping("/{queryId}/export.csv")
    public ResponseEntity<String> exportCsv(@PathVariable UUID queryId) {
        String csv = queryUseCase.exportCsv(queryId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"query-" + queryId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
