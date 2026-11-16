package com.vivek.deskflow.ai;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.vivek.deskflow.ticket.Ticket;
import com.vivek.deskflow.ticket.TicketDtos.SimilarTicket;

/**
 * Finds tickets that are probably about the same problem (possible duplicates).
 * Method: turn each ticket into a set of meaningful words, then compare sets with Jaccard similarity:
 *     similarity = (words in both) / (words in either)        0 = nothing shared, 1 = identical
 */
@Service
public class SimilarityService {
    private static final double MIN_SCORE = 0.12;
    private static final int MAX_RESULTS = 3;

    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "are", "but", "not", "you", "your",
            "with", "this", "that", "have", "has", "had", "was", "were", "from", "they", "them", "its", "our",
            "can", "could", "would", "should", "when", "what", "why", "how", "any", "all", "been", "being",
            "there", "here", "then", "than", "also", "just", "please", "thanks", "thank", "hello", "get", "got",
            "too", "very", "after", "before", "into", "out", "about", "still", "does", "did", "will", "since");

    public List<SimilarTicket> findSimilar(Ticket target, List<Ticket> candidates) {
        Set<String> targetWords = keywords(target.getSubject() + " " + target.getDescription());
        return candidates.stream()
                .filter(other -> !other.getId().equals(target.getId()))
                .map(other -> new Scored(other, jaccard(targetWords,
                        keywords(other.getSubject() + " " + other.getDescription()))))
                .filter(scored -> scored.score >= MIN_SCORE)
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed())
                .limit(MAX_RESULTS)
                .map(s -> new SimilarTicket(s.ticket.getId(), s.ticket.getSubject(), s.ticket.getStatus(),
                        (int) Math.round(s.score * 100)))
                .toList();
    }

    static Set<String> keywords(String text) {
        Set<String> words = new HashSet<>();
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (word.length() >= 3 && !STOP_WORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> shared = new HashSet<>(a);
        shared.retainAll(b);
        int union = a.size() + b.size() - shared.size();
        return (double) shared.size() / union;
    }

    private record Scored(Ticket ticket, double score) { }
}
