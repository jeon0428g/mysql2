package org.example.demo.svc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubRepository extends JpaRepository<Sub, Long> {
    // 추가적인 쿼리 메서드를 정의할 수 있습니다.
}