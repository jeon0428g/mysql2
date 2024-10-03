package org.example.demo.svc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
//@Transactional
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_UNCOMMITTED)
//@Transactional(propagation = Propagation.REQUIRED)
public class UserService {

    private boolean isMemory = true;
    private ConcurrentHashMap<String, Integer> syncMap = new ConcurrentHashMap<>();
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
        boolean isLock = isMemory && checkLock(idStr);
        try {
            if (isLock) {
                throw new RuntimeException(String.format("(%s) locked !!!", idx));
            } else {

                User user = jdbcTemplate.queryForObject("SELECT * FROM user WHERE id = ? FOR UPDATE NOWAIT", new Object[]{id}, new UserRowMapper());
                String currentText = user.getText();
                String newText = String.valueOf(idx);
                String updatedText;
                if (currentText != null && !currentText.isEmpty()) {
                    updatedText = currentText + "|" + newText; // 기존 값이 있을 경우
                } else {
                    updatedText = newText; // 기존 값이 없을 경우
                }
                sleep(idx, 10001);
                int subUpdate = jdbcTemplate.update("INSERT INTO sub (text, rid) VALUES (?,?)", updatedText, idx);
                int userUpdate = jdbcTemplate.update("UPDATE user SET text = ? WHERE id = ?", updatedText, id);
                try {
                    User userNew = jdbcTemplate.queryForObject("SELECT * FROM user WHERE id = ? FOR UPDATE NOWAIT", new Object[]{id}, new UserRowMapper());
                    // User userNew = jdbcTemplate.queryForObject("SELECT * FROM user WHERE id = ?", new Object[]{id}, new UserRowMapper());
                    String result = String.format("new:%s -> curr:%s, update(%s):%s(db:%s) (user:%s,sub:%s) (%s)", newText, currentText, updatedText.equals(userNew.getText()), updatedText, userNew.getText(), userUpdate, subUpdate, userNew.getText().split("\\|").length);
                    resultMap.put("ok", result);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage(), e);
                }

            }
        } catch (Exception e) {
//            if (e instanceof DataAccessException) {
//                System.out.println(String.format("db lock!!! {%s}", idx));
//            }
            throwException(e);
        } finally {
            printId(idStr);
            if (!isLock) {
                syncMap.remove(idStr);
            }
        }
        return resultMap;
    }

    private boolean checkLock(String id) {
        if (syncMap.containsKey(id)) {
            return true;
        }
        syncMap.put(id, 1);
        return false;
    }

    private void printId(String id) {
//        System.out.println(String.format("printId (%s)", id));
        for (String key : syncMap.keySet()) {
            Integer value = syncMap.get(key);
            if (value > 1) {
                System.out.println(String.format("syncMap (%s) %s: %s", id, key, value));
            }
        }
    }

    private void sleep(int idx, int bound) {
        Random random = new Random();
        // 0부터 5000 밀리초 사이의 랜덤 시간을 생성
        int sleepTime = random.nextInt(bound); // 0부터 5000까지의 값
        try {
            System.out.println(String.format("(%s) 잠자기 전: %s(ms)", idx, sleepTime));
            Thread.sleep(sleepTime); // 랜덤으로 생성된 밀리초 동안 대기
            System.out.println(String.format("(%s) 잠자기 후", idx));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    public void clearTable() {
        int id = 1;
        int userUpdated = jdbcTemplate.update("UPDATE user SET text = ? WHERE id = ?", null, id);
        int statusUpdated = jdbcTemplate.update("UPDATE status SET text = ? WHERE userid = ?", null, id);
        int subUpdated = jdbcTemplate.update("DELETE FROM sub");
        System.out.println(String.format("initial user:%s, sub:%s, status:%s", userUpdated, subUpdated, statusUpdated));
    }
}