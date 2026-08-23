package io.nexcope.inform_note.config;

import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import com.p6spy.engine.spy.appender.Slf4JLogger;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class P6SpyConfig implements MessageFormattingStrategy {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setAppender(Slf4JLogger.class.getName());
        P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n----------------------------------------------------------------------------------------------------");
        sb.append("\n[P6Spy SQL] Execution Time: ").append(elapsed).append(" ms | Category: ").append(category);
        sb.append("\n----------------------------------------------------------------------------------------------------");
        sb.append("\n").append(sql.trim());
        sb.append("\n----------------------------------------------------------------------------------------------------");

        return sb.toString();
    }
}
