package com.vivek.deskflow.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SimilarityServiceTest {

    @Test
    void keywordsDropShortAndCommonWords() {
        Set<String> words = SimilarityService.keywords("The export to Excel is not working!");
        assertTrue(words.contains("export"));
        assertTrue(words.contains("excel"));
        assertFalse(words.contains("the"));
        assertFalse(words.contains("to"));
    }

    @Test
    void identicalSetsScoreOneAndDifferentSetsScoreZero() {
        assertEquals(1.0, SimilarityService.jaccard(Set.of("export", "excel"), Set.of("excel", "export")));
        assertEquals(0.0, SimilarityService.jaccard(Set.of("export"), Set.of("refund")));
    }

    @Test
    void partialOverlapIsSharedDividedByTotal() {
        // shared = {export}; all words = {export, excel, timeout}; 1 / 3
        assertEquals(1.0 / 3, SimilarityService.jaccard(Set.of("export", "excel"), Set.of("export", "timeout")), 1e-9);
    }

    @Test
    void emptySetNeverMatches() {
        assertEquals(0.0, SimilarityService.jaccard(Set.of(), Set.of("export")));
    }
}
