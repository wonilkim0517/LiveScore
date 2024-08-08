package ac.su.suport.livescore.domain;

import ac.su.suport.livescore.domain.LiveVideoStream;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "chat_room")
@Getter @Setter
@NoArgsConstructor
public class ChatRoom implements Serializable {

    private static final long serialVersionUID = 6494678977089006639L;

    @Id
    @Column(name = "room_id")
    private String roomId;

    @Column(name = "name")
    private String name;

    @Column(name = "user_count")
    private Long userCount;

    @OneToOne
    @JoinColumn(name = "stream_id")
    private LiveVideoStream liveVideoStream;

    public static ChatRoom create(String name) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.roomId = UUID.randomUUID().toString();
        chatRoom.name = name;
        return chatRoom;
    }
}