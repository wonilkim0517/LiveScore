package ac.su.suport.livescore.logger;

import ac.su.suport.livescore.controller.MatchController;
import ac.su.suport.livescore.controller.MatchTeamController;
import ac.su.suport.livescore.util.CustomIpUtil;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.time.LocalDateTime;

public class AdminLogger {
    private static final Logger logger = LoggerFactory.getLogger(AdminLogger.class.getName());

    public static void logRequest(
            String logType,
            String url,
            String method,
            String userId,
            String payload  // JSON 리터럴 입력
    ) {
        logger.info(String.format("%s\t%s\t%s\t%s\t%s\t%s",
                logType,  // 로그 타입을 맨 앞에 두어서 로그 타입에 따라 필터링이 용이 하도록 함
                LocalDateTime.now(),  // 로그 시간은 파라미터 수신할 필요가 없으므로 로그 메서드 내에서 생성
                url,
                method,
                userId,
                payload
        ));
    }
}