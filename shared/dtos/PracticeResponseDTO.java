package shared.dtos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Server response providing the generated questions and time limit for practice mode.
 */
public class PracticeResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String topic;
    private String difficulty;
    private int durationSeconds;
    private List<QuestionDTO> questions = new ArrayList<>();

    public PracticeResponseDTO() {}

    public PracticeResponseDTO(String topic, String difficulty, int durationSeconds, List<QuestionDTO> questions) {
        this.topic = topic;
        this.difficulty = difficulty;
        this.durationSeconds = durationSeconds;
        this.questions = questions != null ? questions : new ArrayList<>();
    }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public int getDurationMinutes() { return Math.max(1, durationSeconds / 60); }

    public List<QuestionDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDTO> questions) {
        this.questions = questions != null ? questions : new ArrayList<>();
    }
}
