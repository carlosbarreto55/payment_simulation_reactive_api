package checkout.common.exception;

import org.springframework.http.HttpStatus;

public class RiskAnalysisDeniedException extends BaseException {
    public RiskAnalysisDeniedException(String reason) {
        super(
            "Análise de risco negada: " + reason,
            HttpStatus.FORBIDDEN,
            "RISK_ANALYSIS_DENIED"
        );
    }
    
    public RiskAnalysisDeniedException() {
        super(
            "Análise de risco negada",
            HttpStatus.FORBIDDEN,
            "RISK_ANALYSIS_DENIED"
        );
    }
}

