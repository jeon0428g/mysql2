package org.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/{id}/lock")
    public Map<String, Object> lockUser(@PathVariable Long id, @RequestParam(value = "idx", required = false) int idx) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            return userService.lockUserWithNowait(id, idx);
        } catch (Exception e) {
            resultMap.put("fail", e.getMessage());
            return resultMap; // 오류 처리
        }
    }

    @PostMapping("/{id}/update")
    public Map<String, Object> update(@PathVariable Long id) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            User user = userService.update(id);
            resultMap.put("ok", user);
            return resultMap;
        } catch (Exception e) {
            resultMap.put("fail", e.getMessage());
            return resultMap; // 오류 처리
        }
    }

    @PostMapping("/{id}/get")
    public Map<String, Object> get(@PathVariable Long id) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            User user = userService.get(id);
            resultMap.put("ok", user);
            return resultMap;
        } catch (Exception e) {
            resultMap.put("fail", e.getMessage());
            return resultMap; // 오류 처리
        }
    }

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @PostMapping("/call")
    public void callTestApi(@RequestParam("count") int count) {
        ExecutorService executorService = Executors.newFixedThreadPool(5); // 스레드 풀 생성
        // 모든 작업을 제출하고 결과를 리스트로 수집
        List<Future<Map<String, Object>>> futures = IntStream.range(0, count)
                .mapToObj(i -> executorService.submit(() -> {
//                    Thread.sleep(10);
                    return lockUser(1L, i + 1);
                })) // 인덱스 i를 test()에 전달
                .collect(Collectors.toList());
        // 결과 카운트 집계
        List<Map<String, Object>> okResults = new ArrayList<>();
        List<Map<String, Object>> failResults = new ArrayList<>();
        for (Future<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> result = future.get(); // 결과 가져오기
                for (String key: result.keySet()) {
                    if ("ok".equals(key)) {
                        okResults.add(result);
                    } else {
                        failResults.add(result);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 현재 스레드의 인터럽트 상태를 복원
                System.err.println("Thread was interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                System.err.println("Error executing task: " + e.getCause());
            }
        }
        StringBuilder sb = new StringBuilder();

        List<Integer> userResults = new ArrayList<>();
        List<Integer> subResults = new ArrayList<>();
        for (int i = 0; i < okResults.size(); i++) {
            int number = (i + 1);
            Map<String, Object> resultMap = okResults.get(i);
            String value = resultMap.get("ok").toString();
            if (value.indexOf(",true,") > -1) {
                userResults.add(number);
            }
            if (value.indexOf(",true)") > -1) {
                subResults.add(number);
            }
//            if (value.indexOf(",true,") > -1) {
//                System.out.println(String.format("(%s) %s", (i+1), resultMap));
//            }
            System.out.println(String.format("(%s) %s", (i+1), resultMap));
        }

        List<Integer> dbLockResults = new ArrayList<>();
        List<Integer> memLockResults = new ArrayList<>();
        for (int i = 0; i < failResults.size(); i++) {
            int number = (i + 1);
            Map<String, Object> resultMap = failResults.get(i);
            String value = resultMap.get("fail").toString();
            if (value.indexOf("locked !!!") > -1) {
                memLockResults.add(number);
            }
            if (value.indexOf("locked by another transaction") > -1) {
                dbLockResults.add(number);
            }
        }

        // 결과 출력
        String ok = String.format("ok: %s, user: %s, sub: %s", okResults.size(), userResults.size(), subResults.size());
        String okUsers = String.format("ok users: %s", userResults);
        String okSubs = String.format("ok subs: %s", subResults);
        String fail = String.format("fail: %s, mem: %s, db: %s", failResults.size(), memLockResults.size(), dbLockResults.size());
        System.out.println(ok);
        System.out.println(okUsers);
        System.out.println(okSubs);
        System.out.println(fail);

        executorService.shutdown(); // ExecutorService 종료
    }

}