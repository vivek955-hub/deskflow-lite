package com.vivek.deskflow.ai;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vivek.deskflow.ai.AiService.TriageResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService ai;

    public AiController(AiService ai) {
        this.ai = ai;
    }

    public record TriageRequest(
            @NotBlank(message = "Subject is required") @Size(max = 200) String subject,
            @NotBlank(message = "Description is required") @Size(max = 5000) String description) { }

    /** Preview used by the "new ticket" form: shows what the AI would pick before the ticket is saved. */
    @PostMapping("/triage")
    public TriageResult triage(@Valid @RequestBody TriageRequest req) {
        return ai.triage(req.subject(), req.description());
    }
}
