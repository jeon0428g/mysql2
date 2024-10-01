package org.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE)
public class UserService {

    private boolean isMemory = true;
    private ConcurrentHashMap<String, Boolean> syncMap = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final SubRepository subRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserService(UserRepository userRepository, SubRepository subRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.subRepository = subRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> lockUserWithNowait(Long id, int idx) {
        Map<String, Object> resultMap = new HashMap<>();
        String idStr = String.valueOf(id);
        boolean isLock = isMemory && syncMap.containsKey(idStr);
        try {
            if (isLock) {
                throw new RuntimeException(String.format("(%s) locked !!!", idx));
            } else {
                syncMap.put(String.valueOf(id), true);
                User user = jdbcTemplate.queryForObject("SELECT * FROM user WHERE id = ? FOR UPDATE NOWAIT", new Object[]{id}, new UserRowMapper());
                String currentText = user.getText();
                String newText = String.valueOf(idx);
                String updatedText;
                if (currentText != null && !currentText.isEmpty()) {
                    updatedText = currentText + "|" + newText; // 기존 값이 있을 경우
                } else {
                    updatedText = newText; // 기존 값이 없을 경우
                }
                //            Thread.sleep(1000);
                int userUpdate = jdbcTemplate.update("UPDATE user SET text = ? WHERE id = ?", updatedText, idx);
                User userNew = jdbcTemplate.queryForObject("SELECT * FROM user WHERE id = ? FOR UPDATE NOWAIT", new Object[]{id}, new UserRowMapper());

                Sub sub = new Sub();
                sub.setText(updatedText);
                Sub subSave = subRepository.save(sub);

                String result = String.format("new:%s -> curr:%s, update:%s (select:%s,%s,%s)", newText, currentText, updatedText, userNew.getText(), userUpdate > 0, subSave.getId() != null);
                resultMap.put("ok", result);
            }
        } catch (Exception e) {
            throwException(e);
        } finally {
            if (!isLock) {
                syncMap.remove(idStr);
            }
        }
        return resultMap;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_UNCOMMITTED, rollbackFor = Exception.class)
    public User update(Long id) {
        try {
            int update = jdbcTemplate.update("UPDATE user SET text = 'test' WHERE id = ?", id);
            Thread.sleep(60000);
        } catch (Exception e) {
            throwException(e);
        }
        throw new RuntimeException("항상 롤백을 수행합니다."); // 예외를 발생시켜 롤백
    }

    public User get(Long id) {
        return innerGet(id);
    }

    private User innerGet(Long id){
        try {
            User user = jdbcTemplate.queryForObject("SELECT * FROM user WHERE id = ?", new Object[]{id}, new UserRowMapper());
            Thread.sleep(5000);
            return user;
        } catch (Exception e) {
            throwException(e);
        }
        return null;
    }

    private void throwException(Exception e) {
        if (e instanceof InterruptedException) {
            throw new RuntimeException("Thread was interrupted!", e);
        } else if (e instanceof DataAccessException) {
            throw new RuntimeException("User is already locked by another transaction.", e);
        }
        throw new RuntimeException(e.getMessage(), e);
    }

}