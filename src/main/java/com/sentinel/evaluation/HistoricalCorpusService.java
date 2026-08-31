package com.sentinel.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
public class HistoricalCorpusService {
    public static final String RESOURCE = "evaluation/razorpay-historical/manifest.jsonl";
    private final ObjectMapper objectMapper;

    public HistoricalCorpusService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<HistoricalValidationCase> frozenCases() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {
            List<HistoricalValidationCase> cases = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) cases.add(objectMapper.readValue(line, HistoricalValidationCase.class));
            }
            validate(cases);
            return List.copyOf(cases);
        } catch (Exception exception) {
            throw new IllegalStateException("Historical Razorpay corpus could not be loaded", exception);
        }
    }

    public String manifestSha256() {
        try {
            byte[] bytes = new ClassPathResource(RESOURCE).getInputStream().readAllBytes();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("Historical corpus manifest could not be hashed", exception);
        }
    }

    private void validate(List<HistoricalValidationCase> cases) {
        Set<String> urls = new HashSet<>();
        Set<String> sourceIds = new HashSet<>();
        for (HistoricalValidationCase item : cases) {
            if (!item.sourceUrl().startsWith("https://github.com/razorpay/")
                    || !item.canonicalSourceUrl().equals(item.sourceUrl())) {
                throw new IllegalStateException("Case lacks canonical public Razorpay provenance: " + item.caseId());
            }
            if (!urls.add(item.canonicalSourceUrl())
                    || !sourceIds.add(item.sourceRepository() + "#" + item.sourceId())) {
                throw new IllegalStateException("Duplicate canonical provenance: " + item.caseId());
            }
            if (!item.sourceContentHash().matches("sha256:[0-9a-f]{64}")
                    || item.sourceDate() == null || item.sourceDate().isBlank()
                    || item.expectedSafetyInvariants().isEmpty()
                    || !"FROZEN".equals(item.provenanceStatus())) {
                throw new IllegalStateException("Incomplete frozen provenance: " + item.caseId());
            }
            if (item.normalizedFailureReason().length() > 240 || item.sourceTitle().length() > 160) {
                throw new IllegalStateException("Case republishes excessive source text: " + item.caseId());
            }
        }
    }
}
