package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.domain.User;
import ac.su.suport.livescore.dto.CommentDTO;
import ac.su.suport.livescore.dto.DeleteCommentRequest;
import ac.su.suport.livescore.service.CommentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping
    public List<CommentDTO> getAllComments() {
        return commentService.getAllComments();
    }

    @PostMapping("/{matchId}")
    public CommentDTO createComment(@PathVariable Long matchId, @RequestBody CommentDTO commentDTO, HttpSession session) {
        Long userId = ((User) session.getAttribute("currentUser")).getUserId();
        return commentService.createComment(matchId, userId, commentDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentDTO> updateComment(@PathVariable Long id, @RequestBody CommentDTO commentDTO) {
        Optional<CommentDTO> updatedComment = commentService.updateComment(id, commentDTO);
        return updatedComment.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    // 댓글 삭제 요청을 처리하는 엔드포인트
    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        // DeleteCommentRequest 객체 생성
        DeleteCommentRequest request = new DeleteCommentRequest(userId, commentId);

        // CommentService를 통해 댓글 삭제 시도
        boolean isDeleted = commentService.deleteComment(request);

        if (isDeleted) {
            // 삭제 성공 시 HTTP 200 응답 반환
            return ResponseEntity.ok("Comment deleted successfully.");
        } else {
            // 권한이 없거나 실패 시 HTTP 403 응답 반환
            return ResponseEntity.status(403).body("You are not authorized to delete this comment.");
        }
    }

    @GetMapping("/{matchId}")
    public List<CommentDTO> getCommentsByMatchId(@PathVariable Long matchId) {
        return commentService.getCommentsByMatchId(matchId);
    }
}
