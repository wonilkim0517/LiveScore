package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.service.TusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/upload/tus")
public class TusController {

    private final TusService tusService;

    @ResponseBody
    @RequestMapping(value = {"", "/**"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<String> tusUpload(HttpServletRequest request, HttpServletResponse response) {
        String result = tusService.tusUpload(request, response);
        if ("success".equals(result)) {
            return ResponseEntity.ok("업로드 완료! /matches 페이지로 리디렉션합니다.");
        }
        return ResponseEntity.ok(result);
    }
}
