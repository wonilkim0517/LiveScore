package ac.su.suport.livescore.service;

import ac.su.suport.livescore.constant.LiveVideoStreamStatus;
import ac.su.suport.livescore.domain.LiveVideoStream;
import ac.su.suport.livescore.domain.Match;
import ac.su.suport.livescore.repository.LiveVideoStreamRepository;
import ac.su.suport.livescore.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveVideoStreamService {
    private final LiveVideoStreamRepository liveVideoStreamRepository;
    private final MatchRepository matchRepository;
    private final FFmpegService ffmpegService;

    public LiveVideoStream getStreamByMatchId(Long matchId) {
        return liveVideoStreamRepository.findByMatchMatchId(matchId)
                .orElse(null);
    }

    public LiveVideoStream startStream(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        LiveVideoStream stream = liveVideoStreamRepository.findByMatchMatchId(matchId)
                .orElse(new LiveVideoStream());

        stream.setMatch(match);
        stream.setStatus(LiveVideoStreamStatus.valueOf("LIVE"));
        LiveVideoStream savedStream = liveVideoStreamRepository.save(stream);

        ffmpegService.startStreaming(matchId.toString());
        log.info("Stream started for match {}", matchId);

        return savedStream;
    }

    public void stopStream(Long matchId) {
        LiveVideoStream stream = liveVideoStreamRepository.findByMatchMatchId(matchId)
                .orElseThrow(() -> new RuntimeException("Stream not found"));

        stream.setStatus(LiveVideoStreamStatus.valueOf("ENDED"));
        liveVideoStreamRepository.save(stream);

        ffmpegService.stopStreaming(matchId.toString());
        log.info("Stream stopped for match {}", matchId);
    }

    public List<LiveVideoStream> getAllActiveStreams() {
        return liveVideoStreamRepository.findByStatus("LIVE");
    }
}
