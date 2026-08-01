package com.kiras.chaosevents.trivia;

import java.util.List;
import java.util.Locale;

public record TriviaQuestion(String category, String prompt, List<String> answers) {
    public boolean matches(String input) {
        String normalizedInput = normalize(input);
        return answers.stream().map(TriviaQuestion::normalize).anyMatch(normalizedInput::equals);
    }

    public String primaryAnswer() {
        return answers.getFirst();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
