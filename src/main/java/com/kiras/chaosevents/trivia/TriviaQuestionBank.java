package com.kiras.chaosevents.trivia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TriviaQuestionBank {
    public static final List<TriviaQuestion> QUESTIONS;

    static {
        List<TriviaQuestion> questions = new ArrayList<>(180);
        questions.addAll(MathQuestions.QUESTIONS);
        questions.addAll(HistoryQuestions.QUESTIONS);
        questions.addAll(LiteratureQuestions.QUESTIONS);
        questions.addAll(LogicQuestions.QUESTIONS);
        questions.addAll(GeneralQuestions.QUESTIONS);
        QUESTIONS = Collections.unmodifiableList(questions);
    }

    private TriviaQuestionBank() {
    }
}
