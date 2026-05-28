package com.scs.volunteer.mapper;

import com.scs.volunteer.vo.ChatConversationVO;
import com.scs.volunteer.vo.ChatMessageVO;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ChatMapper {
    private final JdbcTemplate jdbcTemplate;

    public ChatMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findPrivateConversation(Long userId, Long targetUserId) {
        Long a = Math.min(userId, targetUserId);
        Long b = Math.max(userId, targetUserId);
        List<Long> ids = jdbcTemplate.queryForList("select id from chat_conversation where user_a_id=? and user_b_id=? and type='PRIVATE'", Long.class, a, b);
        return ids.stream().findFirst();
    }

    public Long createPrivateConversation(Long userId, Long targetUserId) {
        Long a = Math.min(userId, targetUserId);
        Long b = Math.max(userId, targetUserId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("insert into chat_conversation(user_a_id,user_b_id,type) values(?,?,'PRIVATE')", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a);
            ps.setLong(2, b);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public boolean isParticipant(Long conversationId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from chat_conversation where id=? and (user_a_id=? or user_b_id=?)",
                Integer.class, conversationId, userId, userId);
        return count != null && count > 0;
    }

    public Long peerId(Long conversationId, Long userId) {
        return jdbcTemplate.queryForObject("select if(user_a_id=?, user_b_id, user_a_id) from chat_conversation where id=?", Long.class, userId, conversationId);
    }

    public List<ChatConversationVO> conversations(Long userId) {
        return jdbcTemplate.query("""
                select c.id,
                       if(c.user_a_id=?, c.user_b_id, c.user_a_id) as peer_user_id,
                       coalesce(u.nickname,u.name) as peer_name,
                       u.avatar_url as peer_avatar_url,
                       p.college as peer_college,
                       p.major_class as peer_major_class,
                       c.last_message,c.last_message_at,
                       (select count(*) from chat_message m where m.conversation_id=c.id and m.receiver_id=? and m.read_at is null and m.type<>'ACTIVITY_INVITE') as unread_count
                from chat_conversation c
                join user u on u.id=if(c.user_a_id=?, c.user_b_id, c.user_a_id)
                left join volunteer_profile p on p.user_id=u.id
                where (c.user_a_id=? or c.user_b_id=?)
                  and not exists (
                    select 1 from chat_block b
                    where b.blocked_user_id=?
                      and b.blocker_id=if(c.user_a_id=?, c.user_b_id, c.user_a_id)
                  )
                order by c.last_message_at desc,c.updated_at desc
                """, new BeanPropertyRowMapper<>(ChatConversationVO.class), userId, userId, userId, userId, userId, userId, userId);
    }

    public List<ChatMessageVO> messages(Long conversationId) {
        return jdbcTemplate.query("""
                select m.id,m.conversation_id,m.sender_id,m.receiver_id,m.type,m.content,m.activity_id,m.image_url,
                       m.invite_status,m.read_at,m.created_at,
                       a.name as activity_name,
                       concat(date_format(a.start_time,'%m-%d %H:%i'),' 至 ',date_format(a.end_time,'%m-%d %H:%i')) as activity_time,
                       a.location,
                       greatest(a.recruit_number-a.registered_number,0) as remaining_slots
                from chat_message m
                left join activity a on m.activity_id=a.id
                where m.conversation_id=?
                order by m.created_at asc,m.id asc
                """, new BeanPropertyRowMapper<>(ChatMessageVO.class), conversationId);
    }

    public ChatMessageVO insertMessage(Long conversationId, Long senderId, Long receiverId, String type, String content, Long activityId, String inviteStatus) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into chat_message(conversation_id,sender_id,receiver_id,type,content,activity_id,image_url,invite_status)
                    values(?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, conversationId);
            ps.setLong(2, senderId);
            ps.setLong(3, receiverId);
            ps.setString(4, type);
            ps.setString(5, content);
            if (activityId == null) ps.setObject(6, null); else ps.setLong(6, activityId);
            ps.setString(7, "IMAGE".equals(type) ? content : null);
            ps.setString(8, inviteStatus);
            return ps;
        }, keyHolder);
        String summary = "ACTIVITY_INVITE".equals(type) ? "[activity invite]" : ("ACTIVITY_CARD".equals(type) ? "[activity card]" : ("IMAGE".equals(type) ? "[image]" : content));
        jdbcTemplate.update("update chat_conversation set last_message=?,last_message_at=?,updated_at=? where id=?",
                summary, LocalDateTime.now(), LocalDateTime.now(), conversationId);
        return findMessage(keyHolder.getKey().longValue()).orElseThrow();
    }

    public Optional<ChatMessageVO> findMessage(Long messageId) {
        List<ChatMessageVO> list = jdbcTemplate.query("""
                select m.id,m.conversation_id,m.sender_id,m.receiver_id,m.type,m.content,m.activity_id,m.image_url,
                       m.invite_status,m.read_at,m.created_at,
                       a.name as activity_name,
                       concat(date_format(a.start_time,'%m-%d %H:%i'),' 至 ',date_format(a.end_time,'%m-%d %H:%i')) as activity_time,
                       a.location,
                       greatest(a.recruit_number-a.registered_number,0) as remaining_slots
                from chat_message m left join activity a on m.activity_id=a.id where m.id=?
                """, new BeanPropertyRowMapper<>(ChatMessageVO.class), messageId);
        return list.stream().findFirst();
    }

    public void markRead(Long conversationId, Long userId) {
        jdbcTemplate.update("update chat_message set read_at=coalesce(read_at,?) where conversation_id=? and receiver_id=?", LocalDateTime.now(), conversationId, userId);
    }

    public void updateInviteStatus(Long messageId, String status) {
        jdbcTemplate.update("update chat_message set invite_status=? where id=? and type='ACTIVITY_INVITE'", status, messageId);
    }

    public List<ChatMessageVO> activityInvites(Long userId) {
        return jdbcTemplate.query("""
                select m.id,m.conversation_id,m.sender_id,m.receiver_id,m.type,m.content,m.activity_id,m.image_url,
                       m.invite_status,m.read_at,m.created_at,
                       a.name as activity_name,
                       concat(date_format(a.start_time,'%m-%d %H:%i'),' 至 ',date_format(a.end_time,'%m-%d %H:%i')) as activity_time,
                       a.location,
                       greatest(a.recruit_number-a.registered_number,0) as remaining_slots
                from chat_message m
                left join activity a on m.activity_id=a.id
                where m.receiver_id=? and m.type='ACTIVITY_INVITE'
                order by m.created_at desc
                limit 50
                """, new BeanPropertyRowMapper<>(ChatMessageVO.class), userId);
    }

    public int unreadActivityInviteCount(Long userId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from chat_message where receiver_id=? and type='ACTIVITY_INVITE' and read_at is null", Integer.class, userId);
        return count == null ? 0 : count;
    }

    public void markMessageRead(Long userId, Long messageId) {
        jdbcTemplate.update("update chat_message set read_at=coalesce(read_at, ?) where id=? and receiver_id=?", LocalDateTime.now(), messageId, userId);
    }

    public boolean isBlocked(Long senderId, Long receiverId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from chat_block
                where (blocker_id=? and blocked_user_id=?)
                   or (blocker_id=? and blocked_user_id=?)
                """, Integer.class, receiverId, senderId, senderId, receiverId);
        return count != null && count > 0;
    }

    public boolean isBlockedByMe(Long blockerId, Long blockedUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from chat_block where blocker_id=? and blocked_user_id=?",
                Integer.class, blockerId, blockedUserId);
        return count != null && count > 0;
    }

    public void block(Long blockerId, Long blockedUserId) {
        jdbcTemplate.update("""
                insert into chat_block(blocker_id, blocked_user_id)
                values(?, ?)
                on duplicate key update created_at=created_at
                """, blockerId, blockedUserId);
    }

    public void unblock(Long blockerId, Long blockedUserId) {
        jdbcTemplate.update("delete from chat_block where blocker_id=? and blocked_user_id=?", blockerId, blockedUserId);
    }
}
