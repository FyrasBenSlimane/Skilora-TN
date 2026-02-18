package com.skilora.formation.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.formation.entity.Quiz;
import com.skilora.formation.entity.QuizQuestion;
import com.skilora.formation.entity.QuizResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService {

    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);
    private static volatile QuizService instance;

    private QuizService() {}

    public static QuizService getInstance() {
        if (instance == null) {
            synchronized (QuizService.class) {
                if (instance == null) {
                    instance = new QuizService();
                }
            }
        }
        return instance;
    }

    // ── Quiz CRUD ──

    public int createQuiz(Quiz quiz) {
        if (quiz == null) throw new IllegalArgumentException("Quiz cannot be null");
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) throw new IllegalArgumentException("Quiz title is required");
        if (quiz.getFormationId() <= 0) throw new IllegalArgumentException("Valid formation ID is required");
        String sql = """
            INSERT INTO quizzes (formation_id, module_id, title, description,
                pass_score, max_attempts, time_limit_minutes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, quiz.getFormationId());
            if (quiz.getModuleId() != null) {
                stmt.setInt(2, quiz.getModuleId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, quiz.getTitle());
            stmt.setString(4, quiz.getDescription());
            stmt.setInt(5, quiz.getPassScore());
            stmt.setInt(6, quiz.getMaxAttempts());
            stmt.setInt(7, quiz.getTimeLimitMinutes());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Quiz created: id={}", id);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating quiz: {}", e.getMessage(), e);
        }
        return -1;
    }

    public Quiz findQuizById(int id) {
        String sql = "SELECT * FROM quizzes WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapQuiz(rs);
        } catch (SQLException e) {
            logger.error("Error finding quiz {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    public List<Quiz> findQuizzesByFormation(int formationId) {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quizzes WHERE formation_id = ? ORDER BY id";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, formationId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapQuiz(rs));
        } catch (SQLException e) {
            logger.error("Error finding quizzes for formation {}: {}", formationId, e.getMessage(), e);
        }
        return list;
    }

    public List<Quiz> findQuizzesByModule(int moduleId) {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quizzes WHERE module_id = ? ORDER BY id";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, moduleId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapQuiz(rs));
        } catch (SQLException e) {
            logger.error("Error finding quizzes for module {}: {}", moduleId, e.getMessage(), e);
        }
        return list;
    }

    public boolean updateQuiz(Quiz quiz) {
        String sql = """
            UPDATE quizzes SET title = ?, description = ?, pass_score = ?,
                max_attempts = ?, time_limit_minutes = ?, module_id = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, quiz.getTitle());
            stmt.setString(2, quiz.getDescription());
            stmt.setInt(3, quiz.getPassScore());
            stmt.setInt(4, quiz.getMaxAttempts());
            stmt.setInt(5, quiz.getTimeLimitMinutes());
            if (quiz.getModuleId() != null) {
                stmt.setInt(6, quiz.getModuleId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.setInt(7, quiz.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating quiz: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean deleteQuiz(int id) {
        String sql = "DELETE FROM quizzes WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting quiz: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── QuizQuestion CRUD ──

    public int addQuestion(QuizQuestion q) {
        if (q == null) throw new IllegalArgumentException("QuizQuestion cannot be null");
        if (q.getQuizId() <= 0) throw new IllegalArgumentException("quizId must be positive");
        if (q.getQuestionText() == null || q.getQuestionText().isBlank())
            throw new IllegalArgumentException("questionText is required");
        String sql = """
            INSERT INTO quiz_questions (quiz_id, question_text, option_a, option_b,
                option_c, option_d, correct_option, points, order_index)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, q.getQuizId());
            stmt.setString(2, q.getQuestionText());
            stmt.setString(3, q.getOptionA());
            stmt.setString(4, q.getOptionB());
            stmt.setString(5, q.getOptionC());
            stmt.setString(6, q.getOptionD());
            stmt.setString(7, String.valueOf(q.getCorrectOption()));
            stmt.setInt(8, q.getPoints());
            stmt.setInt(9, q.getOrderIndex());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Error adding question: {}", e.getMessage(), e);
        }
        return -1;
    }

    public List<QuizQuestion> getQuestions(int quizId) {
        List<QuizQuestion> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz_questions WHERE quiz_id = ? ORDER BY order_index, id";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapQuestion(rs));
        } catch (SQLException e) {
            logger.error("Error getting questions for quiz {}: {}", quizId, e.getMessage(), e);
        }
        return list;
    }

    public boolean updateQuestion(QuizQuestion q) {
        String sql = """
            UPDATE quiz_questions SET question_text = ?, option_a = ?, option_b = ?,
                option_c = ?, option_d = ?, correct_option = ?, points = ?, order_index = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, q.getQuestionText());
            stmt.setString(2, q.getOptionA());
            stmt.setString(3, q.getOptionB());
            stmt.setString(4, q.getOptionC());
            stmt.setString(5, q.getOptionD());
            stmt.setString(6, String.valueOf(q.getCorrectOption()));
            stmt.setInt(7, q.getPoints());
            stmt.setInt(8, q.getOrderIndex());
            stmt.setInt(9, q.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating question: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean deleteQuestion(int id) {
        String sql = "DELETE FROM quiz_questions WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting question: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── QuizResult operations ──

    public int submitResult(QuizResult result) {
        if (result == null) throw new IllegalArgumentException("QuizResult cannot be null");
        if (result.getQuizId() <= 0) throw new IllegalArgumentException("quizId must be positive");
        if (result.getUserId() <= 0) throw new IllegalArgumentException("userId must be positive");
        String sql = """
            INSERT INTO quiz_results (quiz_id, user_id, score, max_score, passed,
                attempt_number, taken_date, time_spent_seconds)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, result.getQuizId());
            stmt.setInt(2, result.getUserId());
            stmt.setInt(3, result.getScore());
            stmt.setInt(4, result.getMaxScore());
            stmt.setBoolean(5, result.isPassed());
            stmt.setInt(6, result.getAttemptNumber());
            stmt.setTimestamp(7, Timestamp.valueOf(result.getTakenDate()));
            stmt.setInt(8, result.getTimeSpentSeconds());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Quiz result submitted: id={}, score={}/{}, passed={}",
                        id, result.getScore(), result.getMaxScore(), result.isPassed());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error submitting quiz result: {}", e.getMessage(), e);
        }
        return -1;
    }

    public List<QuizResult> getResults(int quizId, int userId) {
        List<QuizResult> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz_results WHERE quiz_id = ? AND user_id = ? ORDER BY attempt_number";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapResult(rs));
        } catch (SQLException e) {
            logger.error("Error getting results for quiz {}, user {}: {}", quizId, userId, e.getMessage(), e);
        }
        return list;
    }

    public int getAttemptCount(int quizId, int userId) {
        String sql = "SELECT COUNT(*) FROM quiz_results WHERE quiz_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Error counting attempts: {}", e.getMessage(), e);
        }
        return 0;
    }

    public QuizResult getBestResult(int quizId, int userId) {
        String sql = "SELECT * FROM quiz_results WHERE quiz_id = ? AND user_id = ? ORDER BY score DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapResult(rs);
        } catch (SQLException e) {
            logger.error("Error getting best result: {}", e.getMessage(), e);
        }
        return null;
    }

    public boolean hasPassedQuiz(int quizId, int userId) {
        String sql = "SELECT 1 FROM quiz_results WHERE quiz_id = ? AND user_id = ? AND passed = TRUE LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, userId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            logger.error("Error checking pass status: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── Mappers ──

    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        Quiz q = new Quiz();
        q.setId(rs.getInt("id"));
        q.setFormationId(rs.getInt("formation_id"));
        int moduleId = rs.getInt("module_id");
        q.setModuleId(rs.wasNull() ? null : moduleId);
        q.setTitle(rs.getString("title"));
        q.setDescription(rs.getString("description"));
        q.setPassScore(rs.getInt("pass_score"));
        q.setMaxAttempts(rs.getInt("max_attempts"));
        q.setTimeLimitMinutes(rs.getInt("time_limit_minutes"));
        return q;
    }

    private QuizQuestion mapQuestion(ResultSet rs) throws SQLException {
        QuizQuestion q = new QuizQuestion();
        q.setId(rs.getInt("id"));
        q.setQuizId(rs.getInt("quiz_id"));
        q.setQuestionText(rs.getString("question_text"));
        q.setOptionA(rs.getString("option_a"));
        q.setOptionB(rs.getString("option_b"));
        q.setOptionC(rs.getString("option_c"));
        q.setOptionD(rs.getString("option_d"));
        String correct = rs.getString("correct_option");
        if (correct != null && !correct.isEmpty()) q.setCorrectOption(correct.charAt(0));
        q.setPoints(rs.getInt("points"));
        q.setOrderIndex(rs.getInt("order_index"));
        return q;
    }

    private QuizResult mapResult(ResultSet rs) throws SQLException {
        QuizResult r = new QuizResult();
        r.setId(rs.getInt("id"));
        r.setQuizId(rs.getInt("quiz_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setScore(rs.getInt("score"));
        r.setMaxScore(rs.getInt("max_score"));
        r.setPassed(rs.getBoolean("passed"));
        r.setAttemptNumber(rs.getInt("attempt_number"));
        Timestamp taken = rs.getTimestamp("taken_date");
        if (taken != null) r.setTakenDate(taken.toLocalDateTime());
        r.setTimeSpentSeconds(rs.getInt("time_spent_seconds"));
        return r;
    }
}
